package com.csg.ecard.messagecenter.module.push.sender.mock;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.sender.BatchChannelSender;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 本地模拟渠道发送公共逻辑。
 */
public abstract class AbstractMockChannelSender implements BatchChannelSender {

    private static final Logger log = LoggerFactory.getLogger(AbstractMockChannelSender.class);

    private final MockSenderProperties properties;

    protected AbstractMockChannelSender(MockSenderProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelSendResult send(ChannelSendRequest request) {
        simulateDelay();
        MockSendResult result = properties.resolveResult(channelType());
        printMockLog(request, result);
        return toSendResult(result);
    }

    @Override
    public ChannelSendResult sendBatch(List<ChannelSendRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ChannelSendResult.succeeded();
        }
        ChannelSendResult lastResult = ChannelSendResult.succeeded();
        for (ChannelSendRequest request : requests) {
            lastResult = send(request);
            if (!lastResult.success()) {
                return lastResult;
            }
        }
        return lastResult;
    }

    private void simulateDelay() {
        long delayMs = properties.getDelayMs();
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("本地模拟消息发送延迟被中断");
        }
    }

    private void printMockLog(ChannelSendRequest request, MockSendResult result) {
        MsgChannel channel = request.channel();
        SyncPushDTO pushRequest = request.pushRequest();
        MessageSendInfo info = request.sendInfo();
        log.info("""
                ========== 本地模拟消息发送 ==========
                渠道类型：{}
                渠道ID：{}
                渠道名称：{}
                接收人ID：{}
                接收人单位ID：{}
                接收地址：{}
                消息标题：{}
                消息内容：{}
                模拟结果：{}
                =====================================
                """,
                channelType(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getChannelName(),
                firstText(info == null ? null : info.getReceiveUserId(), pushRequest == null ? null : pushRequest.getUserId()),
                firstText(info == null ? null : info.getReceiveCorpId(), pushRequest == null ? null : pushRequest.getUserOrgId()),
                maskedRecipient(info, pushRequest),
                firstText(info == null ? null : info.getTitle(), pushRequest == null ? null : pushRequest.getTitle()),
                firstText(request.messageContent(), info == null ? null : info.getContent()),
                result);
    }

    private ChannelSendResult toSendResult(MockSendResult result) {
        return switch (result) {
            case SUCCESS -> new ChannelSendResult(true, "本地模拟发送成功", false);
            case RETRYABLE_FAILED -> ChannelSendResult.failed("本地模拟：第三方服务暂时不可用");
            case NON_RETRYABLE_FAILED -> ChannelSendResult.failedNonRetryable("本地模拟：接收人地址或渠道配置无效");
        };
    }

    private String maskedRecipient(MessageSendInfo info, SyncPushDTO request) {
        String type = channelType();
        String value;
        if (ChannelType.SMS.getCode().equals(type)) {
            value = firstText(info == null ? null : info.getReceivePhone(), request == null ? null : request.getUserPhone());
        } else if (ChannelType.EMAIL.getCode().equals(type)) {
            value = firstText(info == null ? null : info.getReceiveEmail(), request == null ? null : request.getUserEmail());
        } else if (ChannelType.ELINK.getCode().equals(type)) {
            value = firstText(info == null ? null : info.getReceiveUserId(), request == null ? null : request.getUserId());
        } else {
            value = firstText(info == null ? null : info.getReceiveUserId(), request == null ? null : request.getUserId());
        }
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (ChannelType.SMS.getCode().equals(type)) {
            return maskPhone(value);
        }
        if (ChannelType.EMAIL.getCode().equals(type)) {
            return maskEmail(value);
        }
        return value;
    }

    private String maskPhone(String value) {
        String trimmed = value.trim();
        if (trimmed.length() <= 7) {
            return "***";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String maskEmail(String value) {
        String trimmed = value.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String name = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
