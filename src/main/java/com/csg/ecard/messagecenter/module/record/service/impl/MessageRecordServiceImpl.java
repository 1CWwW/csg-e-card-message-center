package com.csg.ecard.messagecenter.module.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.constant.CommonConstants;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.utils.ExceptionStackTraceUtils;
import com.csg.ecard.messagecenter.common.utils.RedisUtil;
import com.csg.ecard.messagecenter.framework.context.CurrentUserContext;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbDecision;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbPolicyService;
import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.entity.MsgRecord;
import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSenderDispatcher;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import com.csg.ecard.messagecenter.module.record.assembler.MessageRecordAssembler;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordFilterDTO;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordPageQueryDTO;
import com.csg.ecard.messagecenter.module.record.enums.MessageRecordFilterType;
import com.csg.ecard.messagecenter.module.record.entity.MsgRecordResendLog;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordDetailRow;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordExportRow;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordFilterOptionRow;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordMapper;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordOverviewRow;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordQueryCriteria;
import com.csg.ecard.messagecenter.module.record.mapper.MsgRecordResendLogMapper;
import com.csg.ecard.messagecenter.module.record.service.MessageRecordService;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordDetailVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordFilterOptionVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordListVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordOverviewVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordPageResult;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendLogVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendVO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息记录查询与手工重发服务实现。
 */
@Service
@RequiredArgsConstructor
public class MessageRecordServiceImpl implements MessageRecordService {

    private static final int EXPORT_LIMIT = 10_000;
    private static final Duration RESEND_LOCK_TTL = Duration.ofMinutes(2);
    private static final String RESEND_LOCK_PREFIX = "msg:record:resend:";
    private static final TypeReference<List<EmailFileDTO>> EMAIL_FILE_LIST_TYPE = new TypeReference<>() {
    };

    private final MessageRecordMapper messageRecordMapper;
    private final MsgRecordResendLogMapper resendLogMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;
    private final MsgTemplateMapper msgTemplateMapper;
    private final MsgChannelMapper msgChannelMapper;
    private final ChannelSenderDispatcher channelSenderDispatcher;
    private final DoNotDisturbPolicyService doNotDisturbPolicyService;
    private final RedisUtil redisUtil;
    private final MessageRecordAssembler assembler;
    private final ObjectMapper objectMapper;

    @Override
    public MessageRecordOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        MessageRecordOverviewRow row =
                messageRecordMapper.selectOverview(yesterdayStart, todayStart, tomorrowStart);

        long todayTotal = value(row == null ? null : row.getTodayTotal());
        long todaySuccess = value(row == null ? null : row.getTodaySuccess());
        long todayFailed = value(row == null ? null : row.getTodayFailed());
        long todayPending = value(row == null ? null : row.getTodayPending());
        long yesterdayTotal = value(row == null ? null : row.getYesterdayTotal());

        MessageRecordOverviewVO vo = new MessageRecordOverviewVO();
        vo.setTodayTotal(todayTotal);
        vo.setTodaySuccess(todaySuccess);
        vo.setTodayFailed(todayFailed);
        vo.setTodayPending(todayPending);
        vo.setSuccessRate(rate(todaySuccess, todayTotal));
        vo.setYesterdayTotal(yesterdayTotal);
        vo.setDayOverDayRate(dayOverDay(todayTotal, yesterdayTotal));
        return vo;
    }

    @Override
    public List<MessageRecordFilterOptionVO> filterOptions(MessageRecordFilterType type,
                                                           String channelType) {
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "type不能为空");
        }
        String normalizedChannelType = type == MessageRecordFilterType.CHANNEL
                ? validateChannelType(trimToNull(channelType))
                : null;
        List<MessageRecordFilterOptionRow> rows = switch (type) {
            case SCENE -> messageRecordMapper.selectSceneFilterOptions();
            case CHANNEL -> messageRecordMapper.selectChannelFilterOptions(normalizedChannelType);
            case TEMPLATE -> messageRecordMapper.selectTemplateFilterOptions();
        };
        return rows.stream().map(this::toFilterOption).toList();
    }

    @Override
    public MessageRecordPageResult page(MessageRecordPageQueryDTO query) {
        if (query == null) {
            query = new MessageRecordPageQueryDTO();
        }
        if (query.getPageNum() == null || query.getPageSize() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageNum和pageSize不能为空");
        }
        MessageRecordQueryCriteria criteria = normalizeAndValidate(query);
        Page<MessageRecordListVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<MessageRecordListVO> result = messageRecordMapper.selectRecordPage(page, criteria);
        assembler.enrichList(result.getRecords());
        return new MessageRecordPageResult(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MessageRecordDetailVO detail(Long id) {
        MessageRecordDetailRow row = requireDetail(id);
        List<MsgSceneParam> definitions = row.getSceneId() == null
                ? Collections.emptyList()
                : msgSceneParamMapper.selectList(new LambdaQueryWrapper<MsgSceneParam>()
                .eq(MsgSceneParam::getSceneId, row.getSceneId())
                .orderByAsc(MsgSceneParam::getSortOrder, MsgSceneParam::getId));
        return assembler.toDetail(row, definitions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageRecordResendVO resend(Long id) {
        MsgRecord record = messageRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "消息记录不存在");
        }
        if (!SendStatus.FAILED.name().equals(record.getSendStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "仅发送失败的记录允许重发");
        }
        assembler.normalizeCallType(record.getCallType() == null ? null : record.getCallType().getCode());
        if (record.getTemplateId() == null || record.getChannelId() == null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "消息记录缺少模板或渠道，无法重发");
        }

        String lockKey = RESEND_LOCK_PREFIX + id;
        String lockValue = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(lockKey, lockValue, RESEND_LOCK_TTL))) {
            throw new BizException(ErrorCode.CONFLICT, "记录正在重发，请勿重复操作");
        }
        try {
            int resendCount = assembler.normalizeResendCount(record.getResendCount());
            int maxResendCount = assembler.normalizeMaxResendCount(record.getMaxResendCount());
            record.setResendCount(resendCount);
            record.setMaxResendCount(maxResendCount);
            if (resendCount >= maxResendCount) {
                throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "手动重发次数已达到上限");
            }
            MsgTemplate template = msgTemplateMapper.selectById(record.getTemplateId());
            if (template == null) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND, "原消息模板不存在，无法重发");
            }
            MsgChannel channel = msgChannelMapper.selectById(record.getChannelId());
            if (channel == null) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND, "原消息渠道不存在，无法重发");
            }
            DoNotDisturbDecision decision = doNotDisturbPolicyService.evaluate(
                    doNotDisturbPolicyService.loadSnapshot(),
                    record.getElinkUserId(),
                    effectiveUnitId(record),
                    null);
            return executeResend(record, channel, decision);
        } finally {
            redisUtil.delete(lockKey);
        }
    }

    @Override
    public List<MessageRecordResendLogVO> resendLogs(Long id) {
        if (messageRecordMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "消息记录不存在");
        }
        return resendLogMapper.selectByRecordId(id).stream()
                .map(this::toResendLogVO)
                .toList();
    }

    @Override
    public List<MessageRecordExportRow> exportRows(MessageRecordFilterDTO query) {
        MessageRecordQueryCriteria criteria = normalizeAndValidate(query);
        Page<MessageRecordExportRow> page = new Page<>(1, EXPORT_LIMIT + 1L, false);
        Page<MessageRecordExportRow> result = messageRecordMapper.selectExportPage(page, criteria);
        if (result.getRecords().size() > EXPORT_LIMIT) {
            throw new BizException(ErrorCode.PARAM_ERROR, "导出数据超过10000条，请缩小查询条件");
        }
        return result.getRecords();
    }

    private MessageRecordResendVO executeResend(MsgRecord record,
                                                MsgChannel channel,
                                                DoNotDisturbDecision doNotDisturbDecision) {
        LocalDateTime startedAt = LocalDateTime.now();
        int nextResendNo = assembler.normalizeResendCount(record.getResendCount()) + 1;
        MsgRecordResendLog log = new MsgRecordResendLog();
        log.setRecordId(record.getId());
        log.setResendNo(nextResendNo);
        log.setStartTime(startedAt);
        log.setOperatorId(CurrentUserContext.getUserIdOrDefault(CommonConstants.DEFAULT_OPERATOR));

        if (doNotDisturbDecision.delayed()) {
            return scheduleResend(record, log, nextResendNo, doNotDisturbDecision);
        }

        LocalDateTime completedAt;
        ChannelSendResult sendResult;
        Throwable technicalError = null;
        try {
            ChannelType channelType = ChannelType.fromCode(channel.getChannelType());
            validateHistoricalRecipient(channelType, record);
            Map<String, Object> sceneParams =
                    assembler.parseSceneParamsForResend(record.getId(), record.getSceneParams());
            SyncPushDTO request = new SyncPushDTO();
            request.setSceneCode(record.getSceneCode());
            request.setSceneParams(sceneParams);
            request.setUserId(record.getUserId());
            request.setUserOrgId(record.getUserOrgId());
            request.setBizId(record.getBizId());
            request.setRegisterXtbs(record.getRegisterXtbs());
            request.setType(record.getNoticeType());
            request.setTitle(record.getTitle());
            request.setUrl(record.getUrl());
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
            request.setPriority(record.getPriority() == null
                    ? MessagePriority.NORMAL
                    : record.getPriority());
            MessageSendInfo sendInfo = buildSendInfo(record, channelType);
            sendResult = channelSenderDispatcher.dispatch(
                    channel.getChannelType(),
                    new ChannelSendRequest(
                            channel, request, record.getMessageContent(), request.getPriority(), sendInfo));
        } catch (RuntimeException ex) {
            sendResult = ChannelSendResult.failedNonRetryable(messageOf(ex));
            if (!(ex instanceof BizException)) {
                technicalError = ex;
            }
        }
        completedAt = LocalDateTime.now();

        boolean success = sendResult.success();
        record.setSendStatus(success ? SendStatus.SUCCESS.name() : SendStatus.FAILED.name());
        record.setErrorMsg(success ? null : defaultError(sendResult.errorMsg()));
        record.setErrorStack(success ? null : ExceptionStackTraceUtils.getStackTrace(technicalError));
        record.setSendTime(completedAt);
        record.setResendCount(nextResendNo);
        record.setMaxResendCount(assembler.normalizeMaxResendCount(record.getMaxResendCount()));
        log.setSendStatus(record.getSendStatus());
        log.setErrorMsg(record.getErrorMsg());
        log.setErrorStack(record.getErrorStack());
        log.setEndTime(completedAt);
        updateResendRecord(record);
        saveResendLog(log);

        return toResendVO(record, success);
    }

    private MessageRecordResendVO scheduleResend(MsgRecord record,
                                                 MsgRecordResendLog log,
                                                 int nextResendNo,
                                                 DoNotDisturbDecision decision) {
        record.setSendStatus(SendStatus.PENDING.name());
        record.setScheduleTime(decision.effectiveScheduleTime());
        record.setErrorMsg(null);
        record.setErrorStack(null);
        record.setSendTime(null);
        record.setResendCount(nextResendNo);
        record.setMaxResendCount(assembler.normalizeMaxResendCount(record.getMaxResendCount()));
        log.setSendStatus(SendStatus.PENDING.name());
        updateResendRecord(record);
        saveResendLog(log);
        return toResendVO(record, false);
    }

    private MessageRecordResendVO toResendVO(MsgRecord record, boolean success) {
        MessageRecordResendVO vo = new MessageRecordResendVO();
        vo.setId(record.getId());
        vo.setMsgId(record.getMsgId());
        vo.setSendStatus(record.getSendStatus());
        vo.setSendStatusDesc(assembler.sendStatusDesc(record.getSendStatus()));
        vo.setResendCount(record.getResendCount());
        vo.setMaxResendCount(record.getMaxResendCount());
        vo.setErrorMsg(record.getErrorMsg());
        vo.setScheduleTime(record.getScheduleTime());
        vo.setSendTime(record.getSendTime());
        vo.setSuccess(success);
        return vo;
    }

    private MessageRecordResendLogVO toResendLogVO(MsgRecordResendLog log) {
        MessageRecordResendLogVO vo = new MessageRecordResendLogVO();
        vo.setId(log.getId());
        vo.setRecordId(log.getRecordId());
        vo.setResendNo(log.getResendNo());
        vo.setSendStatus(log.getSendStatus());
        vo.setSendStatusDesc(assembler.sendStatusDesc(log.getSendStatus()));
        vo.setErrorMsg(log.getErrorMsg());
        vo.setErrorStack(log.getErrorStack());
        vo.setStartTime(log.getStartTime());
        vo.setEndTime(log.getEndTime());
        vo.setOperatorId(log.getOperatorId());
        vo.setCreatedAt(log.getCreateTime());
        return vo;
    }

    private void updateResendRecord(MsgRecord record) {
        try {
            messageRecordMapper.updateById(record);
        } catch (RuntimeException ex) {
            if (record.getErrorStack() == null) {
                throw ex;
            }
            record.setErrorStack(null);
            messageRecordMapper.updateById(record);
        }
    }

    private void saveResendLog(MsgRecordResendLog log) {
        try {
            resendLogMapper.insert(log);
        } catch (RuntimeException ex) {
            if (log.getErrorStack() == null) {
                throw ex;
            }
            log.setErrorStack(null);
            resendLogMapper.insert(log);
        }
    }

    private MessageSendInfo buildSendInfo(MsgRecord record, ChannelType channelType) {
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
                .sendUserId(record.getSenderUserId())
                .sendTime(LocalDateTime.now())
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

    private List<String> splitEmails(String emails) {
        if (!StringUtils.hasText(emails)) {
            return List.of();
        }
        return List.of(emails.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<EmailFileDTO> parseEmailFiles(String files) {
        if (!StringUtils.hasText(files)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(files, EMAIL_FILE_LIST_TYPE);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮件附件JSON解析失败");
        }
    }

    private String externalMsgType(ChannelType channelType) {
        return switch (channelType) {
            case SMS -> "sms";
            case EMAIL -> "email";
            case ELINK -> "elink";
            case IN_APP -> "sym";
        };
    }

    private void validateHistoricalRecipient(ChannelType channelType, MsgRecord record) {
        if (channelType == ChannelType.SMS && !StringUtils.hasText(record.getReceivePhone())) {
            throw new BizException(ErrorCode.CHANNEL_SEND_FAILED,
                    "历史记录未保存手机号，当前无法执行短信重发");
        }
        if (channelType == ChannelType.EMAIL && !StringUtils.hasText(record.getReceiveEmail())) {
            throw new BizException(ErrorCode.CHANNEL_SEND_FAILED,
                    "历史记录未保存邮箱，当前无法执行邮件重发");
        }
        if (channelType == ChannelType.ELINK && !StringUtils.hasText(record.getReceiveUserId())) {
            throw new BizException(ErrorCode.CHANNEL_SEND_FAILED, "历史记录缺少用户ID，无法执行eLink重发");
        }
    }

    private MessageRecordDetailRow requireDetail(Long id) {
        MessageRecordDetailRow row = messageRecordMapper.selectRecordDetail(id);
        if (row == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "消息记录不存在");
        }
        return row;
    }

    private MessageRecordFilterOptionVO toFilterOption(MessageRecordFilterOptionRow row) {
        MessageRecordFilterOptionVO option = new MessageRecordFilterOptionVO();
        option.setValue(row.getOptionValue());
        option.setLabel(row.getOptionLabel());
        return option;
    }

    private MessageRecordQueryCriteria normalizeAndValidate(MessageRecordFilterDTO query) {
        MessageRecordFilterDTO source = query == null ? new MessageRecordFilterDTO() : query;
        if (source.getStartTime() != null && source.getEndTime() != null
                && source.getStartTime().isAfter(source.getEndTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "startTime不能大于endTime");
        }
        MessageRecordQueryCriteria criteria = new MessageRecordQueryCriteria();
        criteria.setMsgId(trimToNull(source.getMsgId()));
        criteria.setBizId(trimToNull(source.getBizId()));
        criteria.setSceneCode(trimToNull(source.getSceneCode()));
        criteria.setChannelType(validateChannelType(trimToNull(source.getChannelType())));
        criteria.setChannelId(parseId(trimToNull(source.getChannelId()), "channelId"));
        criteria.setChannelName(trimToNull(source.getChannelName()));
        criteria.setTemplateId(parseId(trimToNull(source.getTemplateId()), "templateId"));
        criteria.setTemplateName(trimToNull(source.getTemplateName()));
        criteria.setSendStatus(validateSendStatus(trimToNull(source.getSendStatus())));
        criteria.setPriority(validatePriority(trimToNull(source.getPriority())));
        criteria.setCallType(source.getCallType());
        criteria.setUserId(trimToNull(source.getUserId()));
        criteria.setUserOrgId(trimToNull(source.getUserOrgId()));
        criteria.setStartTime(source.getStartTime());
        criteria.setEndTime(source.getEndTime());
        return criteria;
    }

    private String validateChannelType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ChannelType.fromCode(value).getCode();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.CHANNEL_TYPE_INVALID, "渠道类型不合法");
        }
    }

    private String validateSendStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return SendStatus.valueOf(value).name();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "sendStatus仅支持SUCCESS、FAILED、PENDING或ACCEPTED");
        }
    }

    private String validatePriority(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MessagePriority.fromCode(value).getCode();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "priority仅支持HIGH、NORMAL或LOW");
        }
    }

    private Long parseId(String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, field + "格式不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String effectiveUnitId(MsgRecord record) {
        return StringUtils.hasText(record.getReceiveCorpId())
                ? record.getReceiveCorpId().trim()
                : record.getUserOrgId();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal dayOverDay(long todayTotal, long yesterdayTotal) {
        if (yesterdayTotal == 0) {
            return BigDecimal.valueOf(todayTotal > 0 ? 100 : 0).setScale(1);
        }
        return BigDecimal.valueOf(todayTotal - yesterdayTotal)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(yesterdayTotal), 1, RoundingMode.HALF_UP);
    }

    private String messageOf(RuntimeException ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "渠道发送失败";
    }

    private String defaultError(String error) {
        return StringUtils.hasText(error) ? error : "渠道发送失败";
    }
}
