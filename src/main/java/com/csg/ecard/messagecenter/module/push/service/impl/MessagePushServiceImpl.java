package com.csg.ecard.messagecenter.module.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.utils.ExceptionStackTraceUtils;
import com.csg.ecard.messagecenter.common.utils.MessageIdGenerator;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.service.ChannelMatcher;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.entity.MsgRecord;
import com.csg.ecard.messagecenter.module.push.enums.AsyncPushStatus;
import com.csg.ecard.messagecenter.module.push.enums.PushStatus;
import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import com.csg.ecard.messagecenter.module.push.exception.MessagePushException;
import com.csg.ecard.messagecenter.module.push.mapper.MsgRecordMapper;
import com.csg.ecard.messagecenter.module.push.mq.AsyncPushExecutionResult;
import com.csg.ecard.messagecenter.module.push.mq.AsyncPushMessage;
import com.csg.ecard.messagecenter.module.push.mq.MessagePushRabbitConstants;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSenderDispatcher;
import com.csg.ecard.messagecenter.module.push.service.MessagePushService;
import com.csg.ecard.messagecenter.module.push.service.PushIdempotencyService;
import com.csg.ecard.messagecenter.module.push.service.UnitPathResolver;
import com.csg.ecard.messagecenter.module.push.vo.AsyncPushVO;
import com.csg.ecard.messagecenter.module.push.vo.ChannelResultVO;
import com.csg.ecard.messagecenter.module.push.vo.SyncPushVO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderResult;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationMode;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationResult;
import com.csg.ecard.messagecenter.module.template.blockly.SceneParamValueValidator;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息推送服务实现，同步接口和异步消费者共用同一执行主链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushServiceImpl implements MessagePushService {

    private final MsgSceneMapper msgSceneMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;
    private final MsgTemplateMapper msgTemplateMapper;
    private final MsgRecordMapper msgRecordMapper;
    private final ChannelMatcher channelMatcher;
    private final UnitPathResolver unitPathResolver;
    private final BlocklyJsonValidator blocklyJsonValidator;
    private final BlocklyRenderer blocklyRenderer;
    private final SceneParamValueValidator sceneParamValueValidator;
    private final ChannelSenderDispatcher channelSenderDispatcher;
    private final MessageIdGenerator messageIdGenerator;
    private final PushIdempotencyService pushIdempotencyService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncPushVO pushSync(SyncPushDTO request) {
        normalize(request);
        requireEnabledScene(request.getSceneCode());

        String msgId = messageIdGenerator.nextId();
        if (StringUtils.hasText(request.getBizId())) {
            PushIdempotencyService.AcquireResult acquired =
                    pushIdempotencyService.acquire(request.getBizId(), msgId);
            if (!acquired.acquired()) {
                SyncPushVO existing = pushIdempotencyService.getResult(request.getBizId());
                return existing == null ? duplicateInProgress(acquired.msgId()) : existing;
            }
            registerRollbackRelease(request.getBizId());
            msgId = acquired.msgId();
        }

        CoreExecutionResult executed = execute(
                msgId, request, MessageCallType.SYNC, 0, List.of(), false);
        if (StringUtils.hasText(request.getBizId())) {
            pushIdempotencyService.saveResult(request.getBizId(), executed.response());
        }
        return executed.response();
    }

    @Override
    public AsyncPushVO pushAsync(SyncPushDTO request) {
        normalize(request);
        requireEnabledScene(request.getSceneCode());

        String msgId = messageIdGenerator.nextId();
        if (StringUtils.hasText(request.getBizId())) {
            PushIdempotencyService.AcquireResult acquired =
                    pushIdempotencyService.acquire(request.getBizId(), msgId);
            if (!acquired.acquired()) {
                return accepted(acquired.msgId());
            }
            msgId = acquired.msgId();
        }

        try {
            rabbitTemplate.convertAndSend(
                    MessagePushRabbitConstants.EXCHANGE,
                    MessagePushRabbitConstants.ROUTING_KEY,
                    new AsyncPushMessage(msgId, request, MessageCallType.ASYNC),
                    message -> {
                        message.getMessageProperties()
                                .setPriority(request.getPriority().getMqPriority());
                        return message;
                    });
            log.info("Async push enqueued. msgId={}, priority={}", msgId, request.getPriority());
            return accepted(msgId);
        } catch (RuntimeException ex) {
            if (StringUtils.hasText(request.getBizId())) {
                pushIdempotencyService.release(request.getBizId());
            }
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AsyncPushExecutionResult consumeAsync(AsyncPushMessage message) {
        SyncPushDTO request = message.getRequest();
        MessageCallType callType = requireAsyncCallType(message);
        MessagePriority priority = message.getPriority();
        if (priority == null && request.getPriority() != null) {
            priority = request.getPriority();
        }
        if (priority == null) {
            priority = MessagePriority.NORMAL;
        }
        message.setPriority(priority);
        request.setPriority(priority);
        normalize(request);
        CoreExecutionResult executed = execute(
                message.getMsgId(),
                request,
                callType,
                message.getRetryCount(),
                message.getPendingTemplateIds(),
                true);
        return new AsyncPushExecutionResult(executed.retryTemplateIds());
    }

    private CoreExecutionResult execute(String msgId,
                                        SyncPushDTO request,
                                        MessageCallType callType,
                                        int retryCount,
                                        List<Long> pendingTemplateIds,
                                        boolean asyncMode) {
        requireCallType(callType);
        MsgScene scene = requireEnabledScene(request.getSceneCode());
        List<MsgSceneParam> sceneParams = loadSceneParams(scene.getId());
        Map<String, JsonNode> values = validateSceneParams(sceneParams, request.getSceneParams());
        String serializedSceneParams = serializeSceneParams(request.getSceneParams());
        Map<Long, MsgSceneParam> paramMap = sceneParams.stream()
                .collect(Collectors.toMap(MsgSceneParam::getId, item -> item));

        List<MsgTemplate> templates = msgTemplateMapper.selectEnabledApplicableTemplates(
                scene.getId(), request.getUserOrgId());
        if (pendingTemplateIds != null && !pendingTemplateIds.isEmpty()) {
            Set<Long> pendingIds = Set.copyOf(pendingTemplateIds);
            templates = templates.stream()
                    .filter(template -> pendingIds.contains(template.getId()))
                    .toList();
        }
        if (templates.isEmpty()) {
            throw MessagePushException.badRequest("场景下无可用模板（含单位过滤后无匹配模板）");
        }

        List<String> unitPath = unitPathResolver.resolve(request.getUserOrgId());
        Map<Long, Optional<MsgChannel>> matchedChannels = new LinkedHashMap<>();
        for (MsgTemplate template : templates) {
            matchedChannels.put(template.getId(),
                    channelMatcher.match(template.getChannelType(), unitPath));
        }
        if (matchedChannels.values().stream().allMatch(Optional::isEmpty)) {
            throw MessagePushException.badRequest(
                    "用户所属单位及上级单位均未配置" + templates.get(0).getChannelType() + "类型渠道");
        }

        SyncPushVO response = new SyncPushVO();
        response.setMsgId(msgId);
        List<Long> retryTemplateIds = new ArrayList<>();
        for (MsgTemplate template : templates) {
            TemplateExecutionResult result = processTemplate(
                    msgId, request, scene, template, paramMap, values,
                    serializedSceneParams, matchedChannels.get(template.getId()),
                    callType, retryCount, asyncMode);
            if (result.retryRequired()) {
                retryTemplateIds.add(template.getId());
            } else {
                response.getChannelResults().add(result.channelResult());
            }
        }
        response.setStatus(summarize(response.getChannelResults()));
        return new CoreExecutionResult(response, retryTemplateIds);
    }

    private TemplateExecutionResult processTemplate(String msgId,
                                                    SyncPushDTO request,
                                                    MsgScene scene,
                                                    MsgTemplate template,
                                                    Map<Long, MsgSceneParam> paramMap,
                                                    Map<String, JsonNode> values,
                                                    String serializedSceneParams,
                                                    Optional<MsgChannel> matchedChannel,
                                                    MessageCallType callType,
                                                    int retryCount,
                                                    boolean asyncMode) {
        ChannelResultVO result = new ChannelResultVO();
        result.setChannelType(template.getChannelType());
        result.setTemplateName(template.getTemplateName());
        MsgChannel channel = null;

        try {
            ChannelType channelType = ChannelType.fromCode(template.getChannelType());
            if (matchedChannel.isEmpty()) {
                throw new BizException(ErrorCode.CHANNEL_SEND_FAILED,
                        "用户所属单位及上级单位均未配置" + channelType.getCode() + "类型渠道");
            }
            channel = matchedChannel.get();
            result.setChannelName(channel.getChannelName());

            BlocklyValidationResult validation = blocklyJsonValidator.validateStored(
                    template.getBlocklyJson(), scene.getId(), paramMap, BlocklyValidationMode.ENABLE);
            BlocklyRenderResult rendered = blocklyRenderer.render(
                    validation.getBlocklyJson(), scene.getId(), paramMap, values);
            result.setMessageContent(rendered.renderedContent());
            validateRecipient(channelType, request);
        } catch (RuntimeException ex) {
            result.setStatus(SendStatus.FAILED);
            result.setErrorMsg(messageOf(ex));
            saveOrUpdateRecord(msgId, request, scene, template, channel,
                    serializedSceneParams, result, callType, technicalCause(ex));
            return new TemplateExecutionResult(result, false);
        }

        boolean senderConfigured = channelSenderDispatcher.hasSender(template.getChannelType());
        try {
            ChannelSendResult sendResult = channelSenderDispatcher.dispatch(
                    template.getChannelType(),
                    new ChannelSendRequest(
                            channel, request, result.getMessageContent(), request.getPriority()));
            if (senderConfigured) {
                result.setSendTime(LocalDateTime.now());
            }
            if (sendResult.success()) {
                result.setStatus(SendStatus.SUCCESS);
                saveOrUpdateRecord(msgId, request, scene, template, channel,
                        serializedSceneParams, result, callType, null);
                return new TemplateExecutionResult(result, false);
            }
            result.setStatus(SendStatus.FAILED);
            result.setErrorMsg(sendResult.errorMsg());
            if (shouldRetry(asyncMode, retryCount, sendResult.retryable())) {
                saveOrUpdateRecord(msgId, request, scene, template, channel,
                        serializedSceneParams, result, callType, null);
                return new TemplateExecutionResult(result, true);
            }
        } catch (RuntimeException ex) {
            if (senderConfigured) {
                result.setSendTime(LocalDateTime.now());
            }
            result.setStatus(SendStatus.FAILED);
            result.setErrorMsg(messageOf(ex));
            if (shouldRetry(asyncMode, retryCount, true)) {
                log.warn("Channel send will retry. msgId={}, templateId={}, retryCount={}, priority={}, cause={}",
                        msgId, template.getId(), retryCount, request.getPriority(), ex.getMessage());
                saveOrUpdateRecord(msgId, request, scene, template, channel,
                        serializedSceneParams, result, callType, ex);
                return new TemplateExecutionResult(result, true);
            }
            saveOrUpdateRecord(msgId, request, scene, template, channel,
                    serializedSceneParams, result, callType, ex);
            return new TemplateExecutionResult(result, false);
        }

        saveOrUpdateRecord(msgId, request, scene, template, channel,
                serializedSceneParams, result, callType, null);
        return new TemplateExecutionResult(result, false);
    }

    private boolean shouldRetry(boolean asyncMode, int retryCount, boolean retryable) {
        return asyncMode
                && retryable
                && retryCount < MessagePushRabbitConstants.MAX_RETRY_COUNT;
    }

    private void saveOrUpdateRecord(String msgId,
                                    SyncPushDTO request,
                                    MsgScene scene,
                                    MsgTemplate template,
                                    MsgChannel channel,
                                    String serializedSceneParams,
                                    ChannelResultVO result,
                                    MessageCallType callType,
                                    Throwable technicalError) {
        requireCallType(callType);
        MsgRecord record = msgRecordMapper.selectOne(new LambdaQueryWrapper<MsgRecord>()
                .eq(MsgRecord::getMsgId, msgId)
                .eq(MsgRecord::getTemplateId, template.getId())
                .last("FETCH FIRST 1 ROWS ONLY"));
        boolean existing = record != null;
        if (!existing) {
            record = new MsgRecord();
        }
        record.setMsgId(msgId);
        record.setBizId(request.getBizId());
        record.setSceneCode(scene.getSceneCode());
        record.setTemplateId(template.getId());
        record.setChannelId(channel == null ? null : channel.getId());
        record.setSceneParams(serializedSceneParams);
        record.setMessageContent(result.getMessageContent());
        record.setUserId(request.getUserId());
        record.setUserOrgId(request.getUserOrgId());
        if (record.getPriority() == null) {
            record.setPriority(request.getPriority());
        }
        if (record.getCallType() == null) {
            record.setCallType(callType);
        }
        record.setSendStatus(result.getStatus().name());
        record.setErrorMsg(result.getErrorMsg());
        record.setErrorStack(ExceptionStackTraceUtils.getStackTrace(technicalError));
        record.setSendTime(result.getSendTime());
        persistRecord(record, existing);
    }

    private void persistRecord(MsgRecord record, boolean existing) {
        try {
            if (existing) {
                msgRecordMapper.updateById(record);
            } else {
                msgRecordMapper.insert(record);
            }
        } catch (RuntimeException ex) {
            if (record.getErrorStack() == null) {
                throw ex;
            }
            record.setErrorStack(null);
            if (existing) {
                msgRecordMapper.updateById(record);
            } else {
                msgRecordMapper.insert(record);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAsyncTechnicalFailure(AsyncPushMessage message, Throwable cause) {
        if (message == null || !StringUtils.hasText(message.getMsgId()) || cause == null) {
            return;
        }
        LocalDateTime completedAt = LocalDateTime.now();
        msgRecordMapper.update(null, new LambdaUpdateWrapper<MsgRecord>()
                .eq(MsgRecord::getMsgId, message.getMsgId())
                .eq(MsgRecord::getDeleted, 0)
                .set(MsgRecord::getSendStatus, SendStatus.FAILED.name())
                .set(MsgRecord::getErrorMsg, messageOf(cause))
                .set(MsgRecord::getErrorStack, ExceptionStackTraceUtils.getStackTrace(cause))
                .set(MsgRecord::getSendTime, completedAt));
    }

    private MsgScene requireEnabledScene(String sceneCode) {
        MsgScene scene = msgSceneMapper.selectOne(new LambdaQueryWrapper<MsgScene>()
                .eq(MsgScene::getSceneCode, sceneCode)
                .last("FETCH FIRST 1 ROWS ONLY"));
        if (scene == null) {
            throw MessagePushException.badRequest("场景编码不存在：" + sceneCode);
        }
        if (!CommonStatus.ENABLE.getCode().equals(scene.getStatus())) {
            throw MessagePushException.badRequest("场景已停用，拒绝推送：" + sceneCode);
        }
        return scene;
    }

    private List<MsgSceneParam> loadSceneParams(Long sceneId) {
        return msgSceneParamMapper.selectList(new LambdaQueryWrapper<MsgSceneParam>()
                .eq(MsgSceneParam::getSceneId, sceneId)
                .orderByAsc(MsgSceneParam::getSortOrder, MsgSceneParam::getId));
    }

    private Map<String, JsonNode> validateSceneParams(List<MsgSceneParam> definitions,
                                                      Map<String, Object> rawValues) {
        Map<String, JsonNode> values = new LinkedHashMap<>();
        rawValues.forEach((key, value) -> values.put(key, objectMapper.valueToTree(value)));
        Set<String> definedNames = definitions.stream()
                .map(MsgSceneParam::getParamName)
                .collect(Collectors.toSet());
        for (String providedName : values.keySet()) {
            if (!definedNames.contains(providedName)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "场景参数未定义：" + providedName);
            }
        }
        List<String> missingRequiredNames = definitions.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getIsRequired()))
                .filter(item -> isMissing(values, item.getParamName()))
                .map(MsgSceneParam::getParamName)
                .toList();
        if (!missingRequiredNames.isEmpty()) {
            throw MessagePushException.badRequest(
                    "缺少必填参数：" + String.join(", ", missingRequiredNames));
        }
        for (MsgSceneParam definition : definitions) {
            String name = definition.getParamName();
            sceneParamValueValidator.validateAndFormat(
                    definition, values.get(name), values.containsKey(name));
        }
        return values;
    }

    private boolean isMissing(Map<String, JsonNode> values, String name) {
        JsonNode value = values.get(name);
        return !values.containsKey(name)
                || value == null
                || value.isNull()
                || (value.isTextual() && value.textValue().isEmpty())
                || (value.isArray() && value.isEmpty());
    }

    private void validateRecipient(ChannelType channelType, SyncPushDTO request) {
        if (channelType == ChannelType.SMS && !StringUtils.hasText(request.getUserPhone())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "SMS渠道要求userPhone不能为空");
        }
        if (channelType == ChannelType.EMAIL && !StringUtils.hasText(request.getUserEmail())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "EMAIL渠道要求userEmail不能为空");
        }
        if (channelType == ChannelType.ELINK && !StringUtils.hasText(request.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "ELINK渠道要求userId不能为空");
        }
    }

    private String serializeSceneParams(Map<String, Object> sceneParams) {
        try {
            return objectMapper.writeValueAsString(sceneParams);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "sceneParams无法序列化为JSON");
        }
    }

    private PushStatus summarize(List<ChannelResultVO> results) {
        long successCount = results.stream()
                .filter(item -> item.getStatus() == SendStatus.SUCCESS)
                .count();
        if (!results.isEmpty() && successCount == results.size()) {
            return PushStatus.SUCCESS;
        }
        if (successCount > 0) {
            return PushStatus.PARTIAL;
        }
        return PushStatus.FAILED;
    }

    private AsyncPushVO accepted(String msgId) {
        AsyncPushVO response = new AsyncPushVO();
        response.setMsgId(msgId);
        response.setStatus(AsyncPushStatus.ACCEPTED);
        return response;
    }

    private SyncPushVO duplicateInProgress(String msgId) {
        SyncPushVO response = new SyncPushVO();
        response.setMsgId(msgId);
        response.setStatus(PushStatus.FAILED);
        return response;
    }

    private void registerRollbackRelease(String bizId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    pushIdempotencyService.release(bizId);
                }
            }
        });
    }

    private void normalize(SyncPushDTO request) {
        request.setSceneCode(request.getSceneCode().trim());
        request.setUserId(request.getUserId().trim());
        request.setUserOrgId(request.getUserOrgId().trim());
        request.setBizId(trimToNull(request.getBizId()));
        request.setUserPhone(trimToNull(request.getUserPhone()));
        request.setUserEmail(trimToNull(request.getUserEmail()));
        if (request.getPriority() == null) {
            request.setPriority(MessagePriority.NORMAL);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String messageOf(Throwable ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "渠道发送失败";
    }

    private Throwable technicalCause(RuntimeException ex) {
        return ex instanceof BizException || ex instanceof MessagePushException ? null : ex;
    }

    private void requireCallType(MessageCallType callType) {
        if (callType == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "消息原始调用方式不能为空");
        }
    }

    private MessageCallType requireAsyncCallType(AsyncPushMessage message) {
        if (message == null || message.getCallType() == null) {
            throw MessagePushException.badRequest("异步消息结构错误：callType不能为空");
        }
        if (message.getCallType() != MessageCallType.ASYNC) {
            throw MessagePushException.badRequest("异步消息结构错误：callType只能为ASYNC");
        }
        return message.getCallType();
    }

    private record TemplateExecutionResult(ChannelResultVO channelResult, boolean retryRequired) {
    }

    private record CoreExecutionResult(SyncPushVO response, List<Long> retryTemplateIds) {
    }
}
