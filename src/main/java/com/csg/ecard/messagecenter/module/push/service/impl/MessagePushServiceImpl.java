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
import com.csg.ecard.messagecenter.framework.context.CurrentUserContext;
import com.csg.ecard.messagecenter.infrastructure.employee.EmployeeInfo;
import com.csg.ecard.messagecenter.infrastructure.employee.EmployeeInfoProvider;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.channel.service.ChannelMatcher;
import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;
import com.csg.ecard.messagecenter.module.push.dto.GroupPushDTO;
import com.csg.ecard.messagecenter.module.push.dto.GroupPushItemDTO;
import com.csg.ecard.messagecenter.module.push.dto.MassPushDTO;
import com.csg.ecard.messagecenter.module.push.dto.PushMessageDataDTO;
import com.csg.ecard.messagecenter.module.push.dto.PushRecipientDTO;
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
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
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
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
    private final MsgChannelMapper msgChannelMapper;
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
    private final EmployeeInfoProvider employeeInfoProvider;

    private static final TypeReference<List<EmailFileDTO>> EMAIL_FILE_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncPushVO pushSync(SyncPushDTO request) {
        normalize(request);
        requireEnabledScene(request.getSceneCode());

        String pcId = messageIdGenerator.nextId();
        if (StringUtils.hasText(request.getBizId())) {
            PushIdempotencyService.AcquireResult acquired =
                    pushIdempotencyService.acquire(request.getBizId(), pcId);
            if (!acquired.acquired()) {
                SyncPushVO existing = pushIdempotencyService.getResult(request.getBizId());
                return existing == null ? duplicateInProgress(acquired.msgId()) : existing;
            }
            registerRollbackRelease(request.getBizId());
            pcId = acquired.msgId();
        }

        CoreExecutionResult executed = execute(
                pcId, request, MessageCallType.SYNC, 0, List.of(), false, false);
        if (StringUtils.hasText(request.getBizId())) {
            pushIdempotencyService.saveResult(request.getBizId(), executed.response());
        }
        return executed.response();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncPushVO pushMass(MassPushDTO request) {
        MassPushDTO safeRequest = request == null ? new MassPushDTO() : request;
        BatchAcquireResult acquired = acquireBatch(safeRequest.getBizId());
        if (!acquired.acquired()) {
            return acquired.response();
        }
        String pcId = acquired.pcId();
        try {
            List<ChannelResultVO> results = new ArrayList<>();
            for (SyncPushDTO item : expandMassRequests(safeRequest)) {
                normalize(item);
                CoreExecutionResult executed = execute(
                        pcId, item, MessageCallType.SYNC, 0, List.of(), false, true);
                results.addAll(executed.response().getChannelResults());
            }
            dispatchPendingInAppBatch(pcId);
            dispatchPendingEmailBatch(pcId);
            refreshResultsFromRecords(pcId, results);
            SyncPushVO response = batchResponse(pcId, results);
            saveBatchIdempotentResult(safeRequest.getBizId(), response);
            return response;
        } catch (RuntimeException ex) {
            releaseBatchIdOnFailure(safeRequest.getBizId(), acquired.acquired());
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncPushVO pushGroup(GroupPushDTO request) {
        GroupPushDTO safeRequest = request == null ? new GroupPushDTO() : request;
        BatchAcquireResult acquired = acquireBatch(safeRequest.getBizId());
        if (!acquired.acquired()) {
            return acquired.response();
        }
        String pcId = acquired.pcId();
        try {
            List<ChannelResultVO> results = new ArrayList<>();
            for (SyncPushDTO item : expandGroupRequests(safeRequest)) {
                normalize(item);
                CoreExecutionResult executed = execute(
                        pcId, item, MessageCallType.SYNC, 0, List.of(), false, true);
                results.addAll(executed.response().getChannelResults());
            }
            dispatchPendingInAppBatch(pcId);
            dispatchPendingEmailBatch(pcId);
            refreshResultsFromRecords(pcId, results);
            SyncPushVO response = batchResponse(pcId, results);
            saveBatchIdempotentResult(safeRequest.getBizId(), response);
            return response;
        } catch (RuntimeException ex) {
            releaseBatchIdOnFailure(safeRequest.getBizId(), acquired.acquired());
            throw ex;
        }
    }

    private BatchAcquireResult acquireBatch(String bizId) {
        String pcId = messageIdGenerator.nextId();
        String normalizedBizId = trimToNull(bizId);
        if (!StringUtils.hasText(normalizedBizId)) {
            return new BatchAcquireResult(true, pcId, null);
        }
        PushIdempotencyService.AcquireResult acquired =
                pushIdempotencyService.acquire(normalizedBizId, pcId);
        if (!acquired.acquired()) {
            SyncPushVO existing = pushIdempotencyService.getResult(normalizedBizId);
            SyncPushVO response = existing == null ? duplicateInProgress(acquired.msgId()) : existing;
            return new BatchAcquireResult(false, acquired.msgId(), response);
        }
        registerRollbackRelease(normalizedBizId);
        return new BatchAcquireResult(true, acquired.msgId(), null);
    }

    private List<SyncPushDTO> expandMassRequests(MassPushDTO request) {
        PushMessageDataDTO data = request.getData();
        if (data != null) {
            return expandMessageData(
                    request.getUserId(), request.getRegisterCode(), request.getReceiveCorpId(),
                    request.getRegisterXtbs(), request.getScheduleTime(), request.getPriority(),
                    request.getBizId(), data);
        }
        List<PushRecipientDTO> recipients = request.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            throw MessagePushException.badRequest("data或recipients不能为空");
        }
        List<SyncPushDTO> result = new ArrayList<>();
        for (PushRecipientDTO recipient : recipients) {
            SyncPushDTO target = baseRequest(
                    request.getUserId(), sceneCodeOf(request.getRegisterCode(), request.getSceneCode()),
                    request.getRegisterCode(), request.getReceiveCorpId(), request.getRegisterXtbs(),
                    request.getScheduleTime(), request.getPriority(), request.getBizId());
            target.setSceneParams(safeSceneParams(request.getSceneParams()));
            target.setType(request.getType());
            target.setTitle(request.getTitle());
            target.setUrl(request.getUrl());
            applyRecipient(target, recipient, request.getReceiveCorpId(), null);
            result.add(target);
        }
        return result;
    }

    private List<SyncPushDTO> expandGroupRequests(GroupPushDTO request) {
        if (request.getDataList() != null && !request.getDataList().isEmpty()) {
            List<SyncPushDTO> result = new ArrayList<>();
            for (PushMessageDataDTO data : request.getDataList()) {
                result.addAll(expandMessageData(
                        request.getUserId(), request.getRegisterCode(), request.getReceiveCorpId(),
                        request.getRegisterXtbs(), request.getScheduleTime(), request.getPriority(),
                        request.getBizId(), data));
            }
            return result;
        }
        List<GroupPushItemDTO> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw MessagePushException.badRequest("dataList或messages不能为空");
        }
        List<SyncPushDTO> result = new ArrayList<>();
        for (GroupPushItemDTO message : messages) {
            SyncPushDTO target = baseRequest(
                    request.getUserId(), sceneCodeOf(request.getRegisterCode(), message.getSceneCode()),
                    request.getRegisterCode(), request.getReceiveCorpId(), request.getRegisterXtbs(),
                    request.getScheduleTime(), request.getPriority(), request.getBizId());
            target.setSceneParams(safeSceneParams(message.getSceneParams()));
            target.setType(request.getType());
            target.setTitle(StringUtils.hasText(message.getTitle()) ? message.getTitle() : request.getTitle());
            target.setUrl(StringUtils.hasText(message.getUrl()) ? message.getUrl() : request.getUrl());
            applyRecipient(target, message.getRecipient(), request.getReceiveCorpId(), null);
            result.add(target);
        }
        return result;
    }

    private List<SyncPushDTO> expandMessageData(String senderUserId,
                                                String registerCode,
                                                String receiveCorpId,
                                                String registerXtbs,
                                                LocalDateTime scheduleTime,
                                                MessagePriority priority,
                                                String bizId,
                                                PushMessageDataDTO data) {
        PushMessageDataDTO safeData = data == null ? new PushMessageDataDTO() : data;
        List<SyncPushDTO> result = new ArrayList<>();
        Map<String, EmployeeInfo> userMap = employeeInfoProvider.listUsers(safeData.getReceiveUserIds());
        for (String userId : safeList(safeData.getReceiveUserIds())) {
            if (!StringUtils.hasText(userId)) {
                continue;
            }
            SyncPushDTO target = baseRequest(senderUserId, registerCode, registerCode, receiveCorpId,
                    registerXtbs, scheduleTime, priority, bizId);
            applyMessageData(target, safeData);
            applyUserId(target, userId, receiveCorpId, userMap.get(userId), safeData.getElinkIdMap());
            result.add(target);
        }
        if (result.isEmpty()) {
            if (!safeList(safeData.getReceivePhones()).isEmpty() && !StringUtils.hasText(receiveCorpId)) {
                throw MessagePushException.badRequest("receivePhones发送时receiveCorpId不能为空");
            }
            for (String phone : safeList(safeData.getReceivePhones())) {
                if (!StringUtils.hasText(phone)) {
                    continue;
                }
                SyncPushDTO target = baseRequest(senderUserId, registerCode, registerCode, receiveCorpId,
                        registerXtbs, scheduleTime, priority, bizId);
                applyMessageData(target, safeData);
                target.setUserId(phone.trim());
                target.setUserOrgId(receiveCorpId);
                target.setReceiveCorpId(receiveCorpId);
                target.setUserPhone(phone.trim());
                result.add(target);
            }
        }
        if (result.isEmpty()) {
            throw MessagePushException.badRequest("receiveUserIds或receivePhones不能为空");
        }
        return result;
    }

    private SyncPushDTO baseRequest(String senderUserId,
                                    String sceneCode,
                                    String registerCode,
                                    String receiveCorpId,
                                    String registerXtbs,
                                    LocalDateTime scheduleTime,
                                    MessagePriority priority,
                                    String bizId) {
        SyncPushDTO target = new SyncPushDTO();
        target.setSenderUserId(senderUserId);
        target.setSceneCode(sceneCode);
        target.setRegisterCode(registerCode);
        target.setReceiveCorpId(receiveCorpId);
        target.setRegisterXtbs(registerXtbs);
        target.setScheduleTime(scheduleTime);
        target.setPriority(priority);
        target.setBizId(bizId);
        return target;
    }

    private void applyMessageData(SyncPushDTO target, PushMessageDataDTO data) {
        target.setSceneParams(safeSceneParams(data.getSceneParams()));
        target.setType(data.getType());
        target.setTitle(data.getTitle());
        target.setUrl(data.getUrl());
        target.setContent(data.getContent());
        target.setEmailId(data.getEmailId());
        target.setSenderEmail(data.getSenderEmail());
        target.setSenderEmailPassword(data.getSenderEmailPassword());
        target.setSenderEmailUrl(data.getSenderEmailUrl());
        target.setCopyToUsers(safeList(data.getCopyToUsers()));
        target.setCopyEmails(safeList(data.getCopyEmails()));
        target.setFile(data.getFile() == null ? List.of() : data.getFile());
    }

    private void applyRecipient(SyncPushDTO target,
                                PushRecipientDTO recipient,
                                String receiveCorpId,
                                Map<String, String> elinkIdMap) {
        applyUserId(target, recipient.getUserId(), receiveCorpId,
                new EmployeeInfo(recipient.getUserId(), recipient.getUserName(), recipient.getUserPhone(),
                        recipient.getUserEmail(), recipient.getUserOrgId(), recipient.getUserOrgName(), null),
                elinkIdMap);
    }

    private void applyUserId(SyncPushDTO target,
                             String userId,
                             String receiveCorpId,
                             EmployeeInfo employeeInfo,
                             Map<String, String> elinkIdMap) {
        target.setUserId(userId.trim());
        target.setUserName(employeeInfo == null ? null : employeeInfo.userName());
        target.setUserOrgId(firstText(employeeInfo == null ? null : employeeInfo.orgId(), receiveCorpId));
        target.setUserOrgName(employeeInfo == null ? null : employeeInfo.orgName());
        target.setUserPhone(employeeInfo == null ? null : employeeInfo.phone());
        target.setUserEmail(employeeInfo == null ? null : employeeInfo.email());
        target.setReceiveCorpId(firstText(receiveCorpId, target.getUserOrgId()));
        target.setElinkUserId(resolveElinkUserId(userId, employeeInfo, elinkIdMap));
    }

    private String resolveElinkUserId(String userId, EmployeeInfo employeeInfo, Map<String, String> elinkIdMap) {
        if (elinkIdMap != null && StringUtils.hasText(elinkIdMap.get(userId))) {
            return elinkIdMap.get(userId).trim();
        }
        if (employeeInfo != null && StringUtils.hasText(employeeInfo.elinkUserId())) {
            return employeeInfo.elinkUserId();
        }
        return null;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : trimToNull(second);
    }

    private String sceneCodeOf(String registerCode, String sceneCode) {
        return StringUtils.hasText(sceneCode) ? sceneCode.trim() : trimToNull(registerCode);
    }

    private Map<String, Object> safeSceneParams(Map<String, Object> sceneParams) {
        return sceneParams == null ? Collections.emptyMap() : sceneParams;
    }

    private List<String> safeList(Collection<String> values) {
        return values == null ? List.of() : values.stream().toList();
    }

    private SyncPushVO batchResponse(String pcId, List<ChannelResultVO> results) {
        SyncPushVO response = new SyncPushVO();
        response.setPcId(pcId);
        response.setMsgId(pcId);
        response.getChannelResults().addAll(results);
        response.setStatus(summarize(response.getChannelResults()));
        return response;
    }

    private void saveBatchIdempotentResult(String bizId, SyncPushVO response) {
        String normalizedBizId = trimToNull(bizId);
        if (StringUtils.hasText(normalizedBizId)) {
            pushIdempotencyService.saveResult(normalizedBizId, response);
        }
    }

    private void releaseBatchIdOnFailure(String bizId, boolean acquired) {
        String normalizedBizId = trimToNull(bizId);
        if (acquired && StringUtils.hasText(normalizedBizId)) {
            pushIdempotencyService.release(normalizedBizId);
        }
    }

    @Override
    public AsyncPushVO pushAsync(SyncPushDTO request) {
        normalize(request);
        requireEnabledScene(request.getSceneCode());

        String pcId = messageIdGenerator.nextId();
        if (StringUtils.hasText(request.getBizId())) {
            PushIdempotencyService.AcquireResult acquired =
                    pushIdempotencyService.acquire(request.getBizId(), pcId);
            if (!acquired.acquired()) {
                return accepted(acquired.msgId());
            }
            pcId = acquired.msgId();
        }

        try {
            rabbitTemplate.convertAndSend(
                    MessagePushRabbitConstants.EXCHANGE,
                    MessagePushRabbitConstants.ROUTING_KEY,
                    new AsyncPushMessage(pcId, request, MessageCallType.ASYNC),
                    message -> {
                        message.getMessageProperties()
                                .setPriority(request.getPriority().getMqPriority());
                        return message;
                    });
            log.info("Async push enqueued. pcId={}, priority={}", pcId, request.getPriority());
            return accepted(pcId);
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
                true,
                false);
        return new AsyncPushExecutionResult(executed.retryTemplateIds());
    }

    private CoreExecutionResult execute(String pcId,
                                        SyncPushDTO request,
                                        MessageCallType callType,
                                        int retryCount,
                                        List<Long> pendingTemplateIds,
                                        boolean asyncMode,
                                        boolean deferInAppBatch) {
        requireCallType(callType);
        MsgScene scene = requireEnabledScene(request.getSceneCode());
        List<MsgSceneParam> sceneParams = loadSceneParams(scene.getId());
        Map<String, Object> rawSceneParams = safeSceneParams(request.getSceneParams());
        Map<String, JsonNode> values = StringUtils.hasText(request.getContent())
                ? toJsonValues(rawSceneParams) : validateSceneParams(sceneParams, rawSceneParams);
        String serializedSceneParams = serializeSceneParams(rawSceneParams);
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
        response.setPcId(pcId);
        response.setMsgId(pcId);
        List<Long> retryTemplateIds = new ArrayList<>();
        for (MsgTemplate template : templates) {
            TemplateExecutionResult result = processTemplate(
                    pcId, request, scene, template, paramMap, values,
                    serializedSceneParams, matchedChannels.get(template.getId()),
                    callType, retryCount, asyncMode, deferInAppBatch);
            if (result.retryRequired()) {
                retryTemplateIds.add(template.getId());
            } else {
                response.getChannelResults().add(result.channelResult());
            }
        }
        response.setStatus(summarize(response.getChannelResults()));
        return new CoreExecutionResult(response, retryTemplateIds);
    }

    private TemplateExecutionResult processTemplate(String pcId,
                                                    SyncPushDTO request,
                                                    MsgScene scene,
                                                    MsgTemplate template,
                                                    Map<Long, MsgSceneParam> paramMap,
                                                    Map<String, JsonNode> values,
                                                    String serializedSceneParams,
                                                    Optional<MsgChannel> matchedChannel,
                                                    MessageCallType callType,
                                                    int retryCount,
                                                    boolean asyncMode,
                                                    boolean deferInAppBatch) {
        ChannelResultVO result = new ChannelResultVO();
        String messageId = resolveMessageId(pcId, template.getId(), request.getUserId());
        result.setPcId(pcId);
        result.setMsgId(messageId);
        result.setChannelType(template.getChannelType());
        result.setTemplateName(template.getTemplateName());
        MsgChannel channel = null;
        MessageSendInfo sendInfo = null;

        try {
            ChannelType channelType = ChannelType.fromCode(template.getChannelType());
            if (matchedChannel.isEmpty()) {
                throw new BizException(ErrorCode.CHANNEL_SEND_FAILED,
                        "用户所属单位及上级单位均未配置" + channelType.getCode() + "类型渠道");
            }
            channel = matchedChannel.get();
            result.setChannelName(channel.getChannelName());

            if (StringUtils.hasText(request.getContent())) {
                result.setMessageContent(request.getContent());
            } else {
                BlocklyValidationResult validation = blocklyJsonValidator.validateStored(
                        template.getBlocklyJson(), scene.getId(), paramMap, BlocklyValidationMode.ENABLE);
                BlocklyRenderResult rendered = blocklyRenderer.render(
                        validation.getBlocklyJson(), scene.getId(), paramMap, values);
                result.setMessageContent(rendered.renderedContent());
            }
            sendInfo = buildSendInfo(pcId, messageId, request, scene, template, result.getMessageContent());
            fillResultSnapshot(result, request, sendInfo);
            if (request.getScheduleTime() != null) {
                result.setStatus(SendStatus.PENDING);
                result.setResultCode(SendStatus.PENDING.name());
                result.setResultMsg(null);
                saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                        serializedSceneParams, result, callType, null);
                return new TemplateExecutionResult(result, false);
            }
            validateRecipient(channelType, request);
            validateInAppUrl(channelType, request);
            if (deferInAppBatch && (channelType == ChannelType.IN_APP || channelType == ChannelType.EMAIL)) {
                result.setStatus(SendStatus.SENDING);
                result.setResultCode(SendStatus.SENDING.name());
                saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                        serializedSceneParams, result, callType, null);
                return new TemplateExecutionResult(result, false);
            }
        } catch (RuntimeException ex) {
            result.setStatus(SendStatus.FAILED);
            result.setErrorMsg(messageOf(ex));
            result.setResultCode(SendStatus.FAILED.name());
            result.setResultMsg(result.getErrorMsg());
            saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                    serializedSceneParams, result, callType, technicalCause(ex));
            return new TemplateExecutionResult(result, false);
        }

        boolean senderConfigured = channelSenderDispatcher.hasSender(template.getChannelType());
        try {
            ChannelSendResult sendResult = channelSenderDispatcher.dispatch(
                    template.getChannelType(),
                    new ChannelSendRequest(
                            channel, request, result.getMessageContent(), request.getPriority(), sendInfo));
            if (senderConfigured) {
                result.setSendTime(LocalDateTime.now());
            }
            if (sendResult.success()) {
                result.setStatus(SendStatus.SUCCESS);
                result.setResultCode(SendStatus.SUCCESS.name());
                saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                        serializedSceneParams, result, callType, null);
                return new TemplateExecutionResult(result, false);
            }
            result.setStatus(SendStatus.FAILED);
            result.setErrorMsg(sendResult.errorMsg());
            result.setResultCode(SendStatus.FAILED.name());
            result.setResultMsg(result.getErrorMsg());
            if (shouldRetry(asyncMode, retryCount, sendResult.retryable())) {
                saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                        serializedSceneParams, result, callType, null);
                return new TemplateExecutionResult(result, true);
            }
        } catch (RuntimeException ex) {
            if (senderConfigured) {
                result.setSendTime(LocalDateTime.now());
            }
            result.setStatus(SendStatus.FAILED);
            result.setErrorMsg(messageOf(ex));
            result.setResultCode(SendStatus.FAILED.name());
            result.setResultMsg(result.getErrorMsg());
            if (shouldRetry(asyncMode, retryCount, true)) {
                log.warn("Channel send will retry. pcId={}, msgId={}, templateId={}, retryCount={}, priority={}, cause={}",
                        pcId, messageId, template.getId(), retryCount, request.getPriority(), ex.getMessage());
                saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                        serializedSceneParams, result, callType, ex);
                return new TemplateExecutionResult(result, true);
            }
            saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                    serializedSceneParams, result, callType, ex);
            return new TemplateExecutionResult(result, false);
        }

        saveOrUpdateRecord(pcId, messageId, request, scene, template, channel,
                serializedSceneParams, result, callType, null);
        return new TemplateExecutionResult(result, false);
    }

    private void dispatchPendingInAppBatch(String pcId) {
        List<MsgRecord> records = msgRecordMapper.selectList(new LambdaQueryWrapper<MsgRecord>()
                .eq(MsgRecord::getPcId, pcId)
                .eq(MsgRecord::getMsgType, externalMsgType(ChannelType.IN_APP))
                .eq(MsgRecord::getSendStatus, SendStatus.SENDING.name())
                .orderByAsc(MsgRecord::getId));
        if (records.isEmpty()) {
            return;
        }
        Map<Long, MsgChannel> channelMap = msgChannelMapper.selectBatchIds(records.stream()
                        .map(MsgRecord::getChannelId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(MsgChannel::getId, Function.identity()));
        for (int start = 0; start < records.size(); start += 100) {
            List<MsgRecord> batch = records.subList(start, Math.min(start + 100, records.size()));
            dispatchInAppRecordBatch(batch, channelMap);
        }
    }

    private void dispatchInAppRecordBatch(List<MsgRecord> batch, Map<Long, MsgChannel> channelMap) {
        LocalDateTime completedAt = LocalDateTime.now();
        List<ChannelSendRequest> requests = new ArrayList<>();
        List<MsgRecord> validRecords = new ArrayList<>();
        for (MsgRecord record : batch) {
            MsgChannel channel = channelMap.get(record.getChannelId());
            if (channel == null) {
                markRecordFailed(record, completedAt, "站内信渠道不存在", null);
                continue;
            }
            requests.add(new ChannelSendRequest(
                    channel,
                    buildRequestFromRecord(record),
                    record.getMessageContent(),
                    record.getPriority() == null ? MessagePriority.NORMAL : record.getPriority(),
                    buildSendInfoFromRecord(record, ChannelType.IN_APP)));
            validRecords.add(record);
        }
        if (requests.isEmpty()) {
            return;
        }

        ChannelSendResult sendResult;
        Throwable technicalError = null;
        try {
            sendResult = channelSenderDispatcher.dispatchBatch(ChannelType.IN_APP.getCode(), requests);
        } catch (RuntimeException ex) {
            sendResult = ChannelSendResult.failedNonRetryable(messageOf(ex));
            technicalError = ex;
        }
        for (MsgRecord record : validRecords) {
            if (sendResult.success()) {
                markRecordSuccess(record, completedAt);
            } else {
                markRecordFailed(record, completedAt, sendResult.errorMsg(), technicalError);
            }
        }
    }

    private void dispatchPendingEmailBatch(String pcId) {
        List<MsgRecord> records = msgRecordMapper.selectList(new LambdaQueryWrapper<MsgRecord>()
                .eq(MsgRecord::getPcId, pcId)
                .eq(MsgRecord::getMsgType, externalMsgType(ChannelType.EMAIL))
                .eq(MsgRecord::getSendStatus, SendStatus.SENDING.name())
                .orderByAsc(MsgRecord::getEmailId, MsgRecord::getId));
        if (records.isEmpty()) {
            return;
        }
        Map<Long, MsgChannel> channelMap = msgChannelMapper.selectBatchIds(records.stream()
                        .map(MsgRecord::getChannelId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(MsgChannel::getId, Function.identity()));
        Map<String, List<MsgRecord>> groupedRecords = records.stream()
                .collect(Collectors.groupingBy(this::emailGroupKey, LinkedHashMap::new, Collectors.toList()));
        groupedRecords.values().forEach(group -> dispatchEmailRecordBatch(group, channelMap));
    }

    private void dispatchEmailRecordBatch(List<MsgRecord> batch, Map<Long, MsgChannel> channelMap) {
        LocalDateTime completedAt = LocalDateTime.now();
        List<ChannelSendRequest> requests = new ArrayList<>();
        List<MsgRecord> validRecords = new ArrayList<>();
        for (MsgRecord record : batch) {
            MsgChannel channel = channelMap.get(record.getChannelId());
            if (channel == null) {
                markRecordFailed(record, completedAt, "邮件渠道不存在", null);
                continue;
            }
            requests.add(new ChannelSendRequest(
                    channel,
                    buildRequestFromRecord(record),
                    record.getMessageContent(),
                    record.getPriority() == null ? MessagePriority.NORMAL : record.getPriority(),
                    buildSendInfoFromRecord(record, ChannelType.EMAIL)));
            validRecords.add(record);
        }
        if (requests.isEmpty()) {
            return;
        }

        ChannelSendResult sendResult;
        Throwable technicalError = null;
        try {
            sendResult = channelSenderDispatcher.dispatchBatch(ChannelType.EMAIL.getCode(), requests);
        } catch (RuntimeException ex) {
            sendResult = ChannelSendResult.failedNonRetryable(messageOf(ex));
            technicalError = ex;
        }
        for (MsgRecord record : validRecords) {
            if (sendResult.success()) {
                markRecordSuccess(record, completedAt);
            } else {
                markRecordFailed(record, completedAt, sendResult.errorMsg(), technicalError);
            }
        }
    }

    private String emailGroupKey(MsgRecord record) {
        String emailId = StringUtils.hasText(record.getEmailId())
                ? record.getEmailId().trim()
                : String.valueOf(record.getId());
        return record.getChannelId() + "::" + emailId;
    }

    private SyncPushDTO buildRequestFromRecord(MsgRecord record) {
        SyncPushDTO request = new SyncPushDTO();
        request.setSceneCode(record.getSceneCode());
        request.setRegisterCode(record.getRegisterCode());
        request.setUserId(record.getUserId());
        request.setUserOrgId(record.getUserOrgId());
        request.setBizId(record.getBizId());
        request.setRegisterXtbs(record.getRegisterXtbs());
        request.setReceiveCorpId(record.getReceiveCorpId());
        request.setType(record.getNoticeType());
        request.setTitle(record.getTitle());
        request.setUrl(record.getUrl());
        request.setContent(record.getMessageContent());
        request.setScheduleTime(record.getScheduleTime());
        request.setSenderUserId(record.getSenderUserId());
        request.setElinkUserId(record.getElinkUserId());
        request.setUserPhone(record.getReceivePhone());
        request.setUserEmail(record.getReceiveEmail());
        request.setEmailId(record.getEmailId());
        request.setSenderEmail(record.getSenderEmail());
        request.setSenderEmailPassword(record.getSenderEmailPassword());
        request.setSenderEmailUrl(record.getSenderEmailUrl());
        request.setCopyEmails(splitEmails(record.getCopyEmail()));
        request.setFile(parseEmailFiles(record.getFile()));
        request.setPriority(record.getPriority() == null ? MessagePriority.NORMAL : record.getPriority());
        return request;
    }

    private MessageSendInfo buildSendInfoFromRecord(MsgRecord record, ChannelType channelType) {
        return MessageSendInfo.builder()
                .pcId(record.getPcId())
                .msgInfoId(record.getId() == null ? null : String.valueOf(record.getId()))
                .msgId(record.getMsgId())
                .registerCode(record.getRegisterCode())
                .registerName(record.getRegisterName())
                .registerXtbs(record.getRegisterXtbs())
                .msgType(externalMsgType(channelType))
                .type(record.getNoticeType())
                .content(record.getMessageContent())
                .url(record.getUrl())
                .sendTime(LocalDateTime.now())
                .sendUserId(record.getSenderUserId())
                .receiveUserId(record.getReceiveUserId())
                .receiveCorpId(record.getReceiveCorpId())
                .receivePhone(record.getReceivePhone())
                .receiveEmail(record.getReceiveEmail())
                .emailId(record.getEmailId())
                .senderEmail(record.getSenderEmail())
                .senderEmailPassword(record.getSenderEmailPassword())
                .senderEmailUrl(record.getSenderEmailUrl())
                .copyEmails(splitEmails(record.getCopyEmail()))
                .files(parseEmailFiles(record.getFile()))
                .title(record.getTitle())
                .elinkUserid(record.getElinkUserId())
                .build();
    }

    private void markRecordSuccess(MsgRecord record, LocalDateTime completedAt) {
        record.setSendStatus(SendStatus.SUCCESS.name());
        record.setErrorMsg(null);
        record.setErrorStack(null);
        record.setSendTime(completedAt);
        msgRecordMapper.updateById(record);
    }

    private void markRecordFailed(MsgRecord record, LocalDateTime completedAt, String errorMsg, Throwable cause) {
        record.setSendStatus(SendStatus.FAILED.name());
        record.setErrorMsg(StringUtils.hasText(errorMsg) ? errorMsg : "站内信批量发送失败");
        record.setErrorStack(ExceptionStackTraceUtils.getStackTrace(cause));
        record.setSendTime(completedAt);
        msgRecordMapper.updateById(record);
    }

    private void refreshResultsFromRecords(String pcId, List<ChannelResultVO> results) {
        if (results.isEmpty()) {
            return;
        }
        Map<Long, MsgRecord> recordMap = msgRecordMapper.selectList(new LambdaQueryWrapper<MsgRecord>()
                        .eq(MsgRecord::getPcId, pcId))
                .stream()
                .collect(Collectors.toMap(MsgRecord::getId, Function.identity()));
        for (ChannelResultVO result : results) {
            MsgRecord record = recordMap.get(result.getId());
            if (record == null) {
                continue;
            }
            result.setStatus(SendStatus.valueOf(record.getSendStatus()));
            result.setResultCode(record.getSendStatus());
            result.setErrorMsg(record.getErrorMsg());
            result.setResultMsg(record.getErrorMsg());
            result.setSendTime(record.getSendTime());
        }
    }

    private boolean shouldRetry(boolean asyncMode, int retryCount, boolean retryable) {
        return asyncMode
                && retryable
                && retryCount < MessagePushRabbitConstants.MAX_RETRY_COUNT;
    }

    private String resolveMessageId(String pcId, Long templateId, String userId) {
        MsgRecord record = msgRecordMapper.selectOne(new LambdaQueryWrapper<MsgRecord>()
                .eq(MsgRecord::getPcId, pcId)
                .eq(MsgRecord::getTemplateId, templateId)
                .eq(MsgRecord::getUserId, userId)
                .last("FETCH FIRST 1 ROWS ONLY"));
        return record == null ? messageIdGenerator.nextId() : record.getMsgId();
    }

    private MessageSendInfo buildSendInfo(String pcId,
                                          String messageId,
                                          SyncPushDTO request,
                                          MsgScene scene,
                                          MsgTemplate template,
                                          String messageContent) {
        ChannelType channelType = ChannelType.fromCode(template.getChannelType());
        return MessageSendInfo.builder()
                .pcId(pcId)
                .msgId(messageId)
                .registerCode(StringUtils.hasText(request.getRegisterCode()) ? request.getRegisterCode() : scene.getSceneCode())
                .registerName(scene.getSceneName())
                .registerXtbs(request.getRegisterXtbs())
                .msgType(externalMsgType(channelType))
                .type(request.getType())
                .content(messageContent)
                .url(request.getUrl())
                .sendTime(LocalDateTime.now())
                .sendUserId(StringUtils.hasText(request.getSenderUserId())
                        ? request.getSenderUserId() : CurrentUserContext.getUserId())
                .receiveUserId(request.getUserId())
                .receiveCorpId(firstText(request.getReceiveCorpId(), request.getUserOrgId()))
                .receivePhone(request.getUserPhone())
                .receiveEmail(request.getUserEmail())
                .emailId(request.getEmailId())
                .senderEmail(request.getSenderEmail())
                .senderEmailPassword(request.getSenderEmailPassword())
                .senderEmailUrl(request.getSenderEmailUrl())
                .copyEmails(resolveCopyEmails(request))
                .files(request.getFile())
                .title(request.getTitle())
                .elinkUserid(request.getElinkUserId())
                .build();
    }

    private void fillResultSnapshot(ChannelResultVO result, SyncPushDTO request, MessageSendInfo sendInfo) {
        result.setUserId(sendInfo.getSendUserId());
        result.setMsgType(sendInfo.getMsgType());
        result.setContent(sendInfo.getContent());
        result.setMessageContent(sendInfo.getContent());
        result.setScheduleTime(request.getScheduleTime());
        result.setReceiveUserId(sendInfo.getReceiveUserId());
        result.setReceiveCorpId(sendInfo.getReceiveCorpId());
        result.setReceivePhone(sendInfo.getReceivePhone());
        if (result.getStatus() != null) {
            result.setResultCode(result.getStatus().name());
        }
        result.setResultMsg(result.getErrorMsg());
    }

    private String externalMsgType(ChannelType channelType) {
        return switch (channelType) {
            case SMS -> "sms";
            case EMAIL -> "email";
            case ELINK -> "elink";
            case IN_APP -> "sym";
        };
    }

    private void saveOrUpdateRecord(String pcId,
                                    String messageId,
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
                .eq(MsgRecord::getPcId, pcId)
                .eq(MsgRecord::getTemplateId, template.getId())
                .eq(MsgRecord::getUserId, request.getUserId())
                .last("FETCH FIRST 1 ROWS ONLY"));
        boolean existing = record != null;
        if (!existing) {
            record = new MsgRecord();
        }
        record.setPcId(pcId);
        record.setMsgId(messageId);
        record.setBizId(request.getBizId());
        record.setSceneCode(scene.getSceneCode());
        record.setRegisterCode(StringUtils.hasText(request.getRegisterCode()) ? request.getRegisterCode() : scene.getSceneCode());
        record.setRegisterName(scene.getSceneName());
        record.setRegisterXtbs(request.getRegisterXtbs());
        record.setMsgType(externalMsgType(ChannelType.fromCode(template.getChannelType())));
        record.setNoticeType(request.getType());
        record.setTitle(request.getTitle());
        record.setUrl(request.getUrl());
        record.setScheduleTime(request.getScheduleTime());
        record.setTemplateId(template.getId());
        record.setChannelId(channel == null ? null : channel.getId());
        record.setSceneParams(serializedSceneParams);
        record.setMessageContent(result.getMessageContent());
        record.setUserId(request.getUserId());
        record.setUserOrgId(request.getUserOrgId());
        record.setReceiveUserId(request.getUserId());
        record.setReceiveCorpId(firstText(request.getReceiveCorpId(), request.getUserOrgId()));
        record.setReceivePhone(request.getUserPhone());
        record.setReceiveEmail(request.getUserEmail());
        record.setEmailId(request.getEmailId());
        record.setSenderEmail(request.getSenderEmail());
        record.setSenderEmailPassword(request.getSenderEmailPassword());
        record.setSenderEmailUrl(request.getSenderEmailUrl());
        record.setCopyEmail(joinEmails(resolveCopyEmails(request)));
        record.setFile(serializeEmailFiles(request.getFile()));
        record.setSenderUserId(StringUtils.hasText(request.getSenderUserId())
                ? request.getSenderUserId() : CurrentUserContext.getUserId());
        record.setElinkUserId(request.getElinkUserId());
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
        result.setId(record.getId());
        fillResultSnapshot(result, request, buildSendInfo(pcId, messageId, request, scene, template, result.getMessageContent()));
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
                .eq(MsgRecord::getPcId, message.getMsgId())
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

    private Map<String, JsonNode> toJsonValues(Map<String, Object> rawValues) {
        Map<String, JsonNode> values = new LinkedHashMap<>();
        rawValues.forEach((key, value) -> values.put(key, objectMapper.valueToTree(value)));
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
        validateEmailCopyUsers(channelType, request);
        if (channelType == ChannelType.ELINK
                && !StringUtils.hasText(request.getElinkUserId())
                && !StringUtils.hasText(request.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "ELINK渠道要求userId不能为空");
        }
    }

    private void validateInAppUrl(ChannelType channelType, SyncPushDTO request) {
        if (channelType == ChannelType.IN_APP && !StringUtils.hasText(request.getUrl())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "IN_APP渠道要求url不能为空");
        }
    }

    private void validateEmailCopyUsers(ChannelType channelType, SyncPushDTO request) {
        if (channelType != ChannelType.EMAIL || safeList(request.getCopyToUsers()).isEmpty()) {
            return;
        }
        Map<String, EmployeeInfo> userMap = employeeInfoProvider.listUsers(request.getCopyToUsers());
        boolean hasMissingEmail = request.getCopyToUsers().stream()
                .filter(StringUtils::hasText)
                .anyMatch(userId -> {
                    EmployeeInfo info = userMap.get(userId);
                    return info == null || !StringUtils.hasText(info.email());
                });
        if (hasMissingEmail) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮件抄送用户邮箱为空或用户中心未接入");
        }
    }

    private List<String> resolveCopyEmails(SyncPushDTO request) {
        List<String> result = new ArrayList<>(safeList(request.getCopyEmails()));
        List<String> copyToUsers = safeList(request.getCopyToUsers());
        if (!copyToUsers.isEmpty()) {
            Map<String, EmployeeInfo> userMap = employeeInfoProvider.listUsers(copyToUsers);
            copyToUsers.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .map(EmployeeInfo::email)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(result::add);
        }
        return result.stream().filter(StringUtils::hasText).distinct().toList();
    }

    private String joinEmails(List<String> emails) {
        List<String> values = safeList(emails);
        return values.isEmpty() ? null : String.join(",", values);
    }

    private List<String> splitEmails(String emails) {
        if (!StringUtils.hasText(emails)) {
            return List.of();
        }
        return List.of(emails.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String serializeEmailFiles(List<EmailFileDTO> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(files);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮件附件无法序列化为JSON");
        }
    }

    private List<EmailFileDTO> parseEmailFiles(String files) {
        if (!StringUtils.hasText(files)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(files, EMAIL_FILE_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮件附件JSON解析失败");
        }
    }

    private List<String> trimList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
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
        long pendingCount = results.stream()
                .filter(item -> item.getStatus() == SendStatus.PENDING)
                .count();
        if (!results.isEmpty() && pendingCount == results.size()) {
            return PushStatus.PENDING;
        }
        if (successCount > 0) {
            return PushStatus.PARTIAL;
        }
        return PushStatus.FAILED;
    }

    private AsyncPushVO accepted(String pcId) {
        AsyncPushVO response = new AsyncPushVO();
        response.setPcId(pcId);
        response.setMsgId(pcId);
        response.setStatus(AsyncPushStatus.ACCEPTED);
        return response;
    }

    private SyncPushVO duplicateInProgress(String pcId) {
        SyncPushVO response = new SyncPushVO();
        response.setPcId(pcId);
        response.setMsgId(pcId);
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
        request.setRegisterCode(trimToNull(request.getRegisterCode()));
        if (!StringUtils.hasText(request.getSceneCode())) {
            request.setSceneCode(request.getRegisterCode());
        }
        request.setSceneCode(request.getSceneCode().trim());
        request.setUserId(request.getUserId().trim());
        request.setReceiveCorpId(trimToNull(request.getReceiveCorpId()));
        if (!StringUtils.hasText(request.getUserOrgId())) {
            request.setUserOrgId(request.getReceiveCorpId());
        }
        request.setUserOrgId(request.getUserOrgId().trim());
        request.setBizId(trimToNull(request.getBizId()));
        request.setRegisterXtbs(trimToNull(request.getRegisterXtbs()));
        request.setType(trimToNull(request.getType()));
        request.setTitle(trimToNull(request.getTitle()));
        request.setUrl(trimToNull(request.getUrl()));
        request.setContent(trimToNull(request.getContent()));
        request.setSenderUserId(trimToNull(request.getSenderUserId()));
        request.setElinkUserId(trimToNull(request.getElinkUserId()));
        request.setUserPhone(trimToNull(request.getUserPhone()));
        request.setUserEmail(trimToNull(request.getUserEmail()));
        request.setEmailId(trimToNull(request.getEmailId()));
        request.setSenderEmail(trimToNull(request.getSenderEmail()));
        request.setSenderEmailPassword(trimToNull(request.getSenderEmailPassword()));
        request.setSenderEmailUrl(trimToNull(request.getSenderEmailUrl()));
        request.setCopyToUsers(trimList(request.getCopyToUsers()));
        request.setCopyEmails(trimList(request.getCopyEmails()));
        request.setFile(request.getFile() == null ? List.of() : request.getFile());
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

    private record BatchAcquireResult(boolean acquired, String pcId, SyncPushVO response) {
    }
}
