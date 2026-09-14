package com.csg.ecard.messagecenter.module.push.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.common.utils.ExceptionStackTraceUtils;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbDecision;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbPolicyService;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbPolicySnapshot;
import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.entity.MsgRecord;
import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import com.csg.ecard.messagecenter.module.push.mapper.MsgRecordMapper;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSenderDispatcher;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import com.csg.ecard.messagecenter.module.push.service.ScheduledMessageDispatchService;
import com.csg.ecard.messagecenter.module.push.vo.ScheduledDispatchResultVO;
import com.csg.ecard.messagecenter.module.record.entity.MsgRecordResendLog;
import com.csg.ecard.messagecenter.module.record.mapper.MsgRecordResendLogMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于消息记录快照执行到期消息发送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledMessageDispatchServiceImpl implements ScheduledMessageDispatchService {

    private static final TypeReference<Map<String, Object>> SCENE_PARAMS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<EmailFileDTO>> EMAIL_FILE_LIST_TYPE = new TypeReference<>() {
    };

    private final MsgRecordMapper msgRecordMapper;
    private final MsgChannelMapper msgChannelMapper;
    private final MsgRecordResendLogMapper resendLogMapper;
    private final ChannelSenderDispatcher channelSenderDispatcher;
    private final ObjectMapper objectMapper;
    private final DoNotDisturbPolicyService doNotDisturbPolicyService;

    @Value("${app.message-schedule.batch-size:100}")
    private int batchSize;

    @Override
    public ScheduledDispatchResultVO dispatchDueMessages() {
        int limit = Math.max(1, batchSize);
        Page<MsgRecord> page = new Page<>(1, limit, false);
        Page<MsgRecord> records = msgRecordMapper.selectPage(page, new LambdaQueryWrapper<MsgRecord>()
                .eq(MsgRecord::getSendStatus, SendStatus.PENDING.name())
                .le(MsgRecord::getScheduleTime, LocalDateTime.now())
                .orderByAsc(MsgRecord::getScheduleTime, MsgRecord::getId));

        ScheduledDispatchResultVO result = new ScheduledDispatchResultVO();
        result.setScannedCount(records.getRecords().size());
        if (records.getRecords().isEmpty()) {
            return result;
        }
        DoNotDisturbPolicySnapshot policySnapshot = doNotDisturbPolicyService.loadSnapshot();
        Map<Long, MsgRecordResendLog> pendingResendLogs = loadPendingResendLogs(records.getRecords());
        List<MsgRecord> inAppRecords = new ArrayList<>();
        List<MsgRecord> emailRecords = new ArrayList<>();
        for (MsgRecord record : records.getRecords()) {
            DoNotDisturbDecision decision = doNotDisturbPolicyService.evaluate(
                    policySnapshot,
                    record.getElinkUserId(),
                    effectiveUnitId(record),
                    null);
            if (decision.delayed()) {
                postpone(record, decision);
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            if (externalMsgType(ChannelType.IN_APP).equals(record.getMsgType())) {
                inAppRecords.add(record);
                continue;
            }
            if (externalMsgType(ChannelType.EMAIL).equals(record.getMsgType())) {
                emailRecords.add(record);
                continue;
            }
            DispatchOutcome outcome = dispatchOne(record, pendingResendLogs);
            switch (outcome) {
                case SENT -> result.setSentCount(result.getSentCount() + 1);
                case FAILED -> result.setFailedCount(result.getFailedCount() + 1);
                case SKIPPED -> result.setSkippedCount(result.getSkippedCount() + 1);
            }
        }
        for (int start = 0; start < inAppRecords.size(); start += 100) {
            List<MsgRecord> batch = inAppRecords.subList(start, Math.min(start + 100, inAppRecords.size()));
            BatchDispatchCount count = dispatchInAppBatch(batch, pendingResendLogs);
            result.setSentCount(result.getSentCount() + count.sentCount());
            result.setFailedCount(result.getFailedCount() + count.failedCount());
            result.setSkippedCount(result.getSkippedCount() + count.skippedCount());
        }
        Map<String, List<MsgRecord>> emailGroups = emailRecords.stream()
                .collect(Collectors.groupingBy(this::emailGroupKey, LinkedHashMap::new, Collectors.toList()));
        for (List<MsgRecord> batch : emailGroups.values()) {
            BatchDispatchCount count = dispatchEmailBatch(batch, pendingResendLogs);
            result.setSentCount(result.getSentCount() + count.sentCount());
            result.setFailedCount(result.getFailedCount() + count.failedCount());
            result.setSkippedCount(result.getSkippedCount() + count.skippedCount());
        }
        return result;
    }

    private void postpone(MsgRecord record, DoNotDisturbDecision decision) {
        int updated = msgRecordMapper.update(null, new LambdaUpdateWrapper<MsgRecord>()
                .eq(MsgRecord::getId, record.getId())
                .eq(MsgRecord::getSendStatus, SendStatus.PENDING.name())
                .set(MsgRecord::getScheduleTime, decision.effectiveScheduleTime()));
        if (updated > 0) {
            log.info("Scheduled message postponed by do-not-disturb rule. recordId={}, ruleId={}, scheduleTime={}",
                    record.getId(), decision.ruleId(), decision.effectiveScheduleTime());
        }
    }

    private BatchDispatchCount dispatchInAppBatch(List<MsgRecord> records,
                                                  Map<Long, MsgRecordResendLog> pendingResendLogs) {
        List<MsgRecord> claimedRecords = new ArrayList<>();
        List<ChannelSendRequest> requests = new ArrayList<>();
        int failedCount = 0;
        int skippedCount = 0;
        for (MsgRecord record : records) {
            if (!claim(record.getId())) {
                skippedCount++;
                continue;
            }
            MsgRecord sendingRecord = msgRecordMapper.selectById(record.getId());
            if (sendingRecord == null) {
                skippedCount++;
                continue;
            }
            try {
                MsgChannel channel = requireChannel(sendingRecord);
                SyncPushDTO request = buildRequest(sendingRecord);
                requests.add(new ChannelSendRequest(
                        channel, request, sendingRecord.getMessageContent(),
                        request.getPriority(), buildSendInfo(sendingRecord, ChannelType.IN_APP)));
                claimedRecords.add(sendingRecord);
            } catch (RuntimeException ex) {
                failedCount++;
                markCompleted(sendingRecord, ChannelSendResult.failedNonRetryable(messageOf(ex)), ex,
                        pendingResendLogs);
            }
        }
        if (requests.isEmpty()) {
            return new BatchDispatchCount(0, failedCount, skippedCount);
        }

        ChannelSendResult sendResult;
        Throwable technicalError = null;
        try {
            sendResult = channelSenderDispatcher.dispatchBatch(ChannelType.IN_APP.getCode(), requests);
        } catch (RuntimeException ex) {
            sendResult = ChannelSendResult.failedNonRetryable(messageOf(ex));
            technicalError = ex;
        }
        for (MsgRecord record : claimedRecords) {
            markCompleted(record, sendResult, technicalError, pendingResendLogs);
        }
        if (sendResult.success()) {
            return new BatchDispatchCount(claimedRecords.size(), failedCount, skippedCount);
        }
        return new BatchDispatchCount(0, failedCount + claimedRecords.size(), skippedCount);
    }

    private BatchDispatchCount dispatchEmailBatch(List<MsgRecord> records,
                                                  Map<Long, MsgRecordResendLog> pendingResendLogs) {
        List<MsgRecord> claimedRecords = new ArrayList<>();
        List<ChannelSendRequest> requests = new ArrayList<>();
        int failedCount = 0;
        int skippedCount = 0;
        for (MsgRecord record : records) {
            if (!claim(record.getId())) {
                skippedCount++;
                continue;
            }
            MsgRecord sendingRecord = msgRecordMapper.selectById(record.getId());
            if (sendingRecord == null) {
                skippedCount++;
                continue;
            }
            try {
                MsgChannel channel = requireChannel(sendingRecord);
                SyncPushDTO request = buildRequest(sendingRecord);
                requests.add(new ChannelSendRequest(
                        channel, request, sendingRecord.getMessageContent(),
                        request.getPriority(), buildSendInfo(sendingRecord, ChannelType.EMAIL)));
                claimedRecords.add(sendingRecord);
            } catch (RuntimeException ex) {
                failedCount++;
                markCompleted(sendingRecord, ChannelSendResult.failedNonRetryable(messageOf(ex)), ex,
                        pendingResendLogs);
            }
        }
        if (requests.isEmpty()) {
            return new BatchDispatchCount(0, failedCount, skippedCount);
        }

        ChannelSendResult sendResult;
        Throwable technicalError = null;
        try {
            sendResult = channelSenderDispatcher.dispatchBatch(ChannelType.EMAIL.getCode(), requests);
        } catch (RuntimeException ex) {
            sendResult = ChannelSendResult.failedNonRetryable(messageOf(ex));
            technicalError = ex;
        }
        for (MsgRecord record : claimedRecords) {
            markCompleted(record, sendResult, technicalError, pendingResendLogs);
        }
        if (sendResult.success()) {
            return new BatchDispatchCount(claimedRecords.size(), failedCount, skippedCount);
        }
        return new BatchDispatchCount(0, failedCount + claimedRecords.size(), skippedCount);
    }

    private String emailGroupKey(MsgRecord record) {
        String emailId = StringUtils.hasText(record.getEmailId())
                ? record.getEmailId().trim()
                : String.valueOf(record.getId());
        return record.getChannelId() + "::" + emailId;
    }

    @Transactional(rollbackFor = Exception.class)
    protected DispatchOutcome dispatchOne(MsgRecord record,
                                          Map<Long, MsgRecordResendLog> pendingResendLogs) {
        if (!claim(record.getId())) {
            return DispatchOutcome.SKIPPED;
        }
        MsgRecord sendingRecord = msgRecordMapper.selectById(record.getId());
        if (sendingRecord == null) {
            return DispatchOutcome.SKIPPED;
        }

        ChannelSendResult sendResult;
        Throwable technicalError = null;
        try {
            MsgChannel channel = requireChannel(sendingRecord);
            ChannelType channelType = ChannelType.fromCode(channel.getChannelType());
            SyncPushDTO request = buildRequest(sendingRecord);
            MessageSendInfo sendInfo = buildSendInfo(sendingRecord, channelType);
            sendResult = channelSenderDispatcher.dispatch(
                    channel.getChannelType(),
                    new ChannelSendRequest(
                            channel, request, sendingRecord.getMessageContent(), request.getPriority(), sendInfo));
        } catch (RuntimeException ex) {
            sendResult = ChannelSendResult.failedNonRetryable(messageOf(ex));
            technicalError = ex;
        }

        boolean success = sendResult.success();
        markCompleted(sendingRecord, sendResult, technicalError, pendingResendLogs);
        return success ? DispatchOutcome.SENT : DispatchOutcome.FAILED;
    }

    private void markCompleted(MsgRecord record,
                               ChannelSendResult sendResult,
                               Throwable technicalError,
                               Map<Long, MsgRecordResendLog> pendingResendLogs) {
        boolean success = sendResult.success();
        LocalDateTime completedAt = LocalDateTime.now();
        record.setSendStatus(success ? SendStatus.SUCCESS.name() : SendStatus.FAILED.name());
        record.setErrorMsg(success ? null : defaultError(sendResult.errorMsg()));
        record.setErrorStack(success ? null : ExceptionStackTraceUtils.getStackTrace(technicalError));
        record.setSendTime(completedAt);
        updateRecord(record);
        completePendingResendLog(record, completedAt, pendingResendLogs);
    }

    private void completePendingResendLog(MsgRecord record,
                                          LocalDateTime completedAt,
                                          Map<Long, MsgRecordResendLog> pendingResendLogs) {
        MsgRecordResendLog resendLog = pendingResendLogs.remove(record.getId());
        if (resendLog == null) {
            return;
        }
        resendLog.setSendStatus(record.getSendStatus());
        resendLog.setErrorMsg(record.getErrorMsg());
        resendLog.setErrorStack(record.getErrorStack());
        resendLog.setEndTime(completedAt);
        resendLogMapper.updateById(resendLog);
    }

    private Map<Long, MsgRecordResendLog> loadPendingResendLogs(List<MsgRecord> records) {
        List<Long> recordIds = records.stream().map(MsgRecord::getId).toList();
        Map<Long, MsgRecordResendLog> result = new LinkedHashMap<>();
        for (MsgRecordResendLog resendLog : resendLogMapper.selectPendingByRecordIds(recordIds)) {
            result.putIfAbsent(resendLog.getRecordId(), resendLog);
        }
        return result;
    }

    private boolean claim(Long id) {
        return msgRecordMapper.update(null, new LambdaUpdateWrapper<MsgRecord>()
                .eq(MsgRecord::getId, id)
                .eq(MsgRecord::getSendStatus, SendStatus.PENDING.name())
                .set(MsgRecord::getSendStatus, SendStatus.ACCEPTED.name())) > 0;
    }

    private MsgChannel requireChannel(MsgRecord record) {
        if (record.getChannelId() == null) {
            throw new IllegalStateException("消息记录缺少渠道ID");
        }
        MsgChannel channel = msgChannelMapper.selectById(record.getChannelId());
        if (channel == null) {
            throw new IllegalStateException("消息记录对应渠道不存在");
        }
        return channel;
    }

    private SyncPushDTO buildRequest(MsgRecord record) {
        SyncPushDTO request = new SyncPushDTO();
        request.setSceneCode(record.getSceneCode());
        request.setRegisterCode(record.getRegisterCode());
        request.setSceneParams(parseSceneParams(record));
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

    private Map<String, Object> parseSceneParams(MsgRecord record) {
        if (!StringUtils.hasText(record.getSceneParams())) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(record.getSceneParams(), SCENE_PARAMS_TYPE);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("消息记录场景参数不是合法JSON", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("消息记录场景参数解析失败", ex);
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
            throw new IllegalStateException("邮件附件JSON解析失败", ex);
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

    private void updateRecord(MsgRecord record) {
        try {
            msgRecordMapper.updateById(record);
        } catch (RuntimeException ex) {
            if (record.getErrorStack() == null) {
                throw ex;
            }
            log.warn("定时消息发送失败堆栈过长，改为仅保存错误摘要，recordId={}", record.getId());
            record.setErrorStack(null);
            msgRecordMapper.updateById(record);
        }
    }

    private String messageOf(RuntimeException ex) {
        if (StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private String defaultError(String errorMsg) {
        return StringUtils.hasText(errorMsg) ? errorMsg : "渠道发送失败";
    }

    private String effectiveUnitId(MsgRecord record) {
        return StringUtils.hasText(record.getReceiveCorpId())
                ? record.getReceiveCorpId().trim()
                : record.getUserOrgId();
    }

    private enum DispatchOutcome {
        SENT,
        FAILED,
        SKIPPED
    }

    private record BatchDispatchCount(int sentCount, int failedCount, int skippedCount) {
    }
}
