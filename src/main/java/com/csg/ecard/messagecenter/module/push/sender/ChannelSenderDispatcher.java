package com.csg.ecard.messagecenter.module.push.sender;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 渠道发送适配器分发器。
 */
@Component
public class ChannelSenderDispatcher {

    private final Map<String, ChannelSender> senders;

    public ChannelSenderDispatcher(List<ChannelSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toUnmodifiableMap(ChannelSender::channelType, Function.identity()));
    }

    /**
     * 按渠道类型分发发送；未配置适配器时明确返回失败。
     *
     * @param channelType 渠道类型
     * @param request     发送上下文
     * @return 发送结果
     */
    public ChannelSendResult dispatch(String channelType, ChannelSendRequest request) {
        ChannelSender sender = senders.get(channelType);
        if (sender == null) {
            return ChannelSendResult.failedNonRetryable("未配置 " + channelType + " 渠道发送适配器");
        }
        return sender.send(request);
    }

    /**
     * 按渠道类型批量分发。
     *
     * @param channelType 渠道类型
     * @param requests    发送上下文集合
     * @return 发送结果
     */
    public ChannelSendResult dispatchBatch(String channelType, List<ChannelSendRequest> requests) {
        ChannelSender sender = senders.get(channelType);
        if (sender == null) {
            return ChannelSendResult.failedNonRetryable("未配置" + channelType + "渠道发送适配器");
        }
        if (!(sender instanceof BatchChannelSender batchSender)) {
            return ChannelSendResult.failedNonRetryable(channelType + "渠道发送适配器不支持批量发送");
        }
        return batchSender.sendBatch(requests);
    }

    /**
     * 判断指定渠道类型是否配置发送适配器。
     *
     * @param channelType 渠道类型
     * @return 是否已配置
     */
    public boolean hasSender(String channelType) {
        return senders.containsKey(channelType);
    }
}
