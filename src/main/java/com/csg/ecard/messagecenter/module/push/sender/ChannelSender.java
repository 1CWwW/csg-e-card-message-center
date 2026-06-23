package com.csg.ecard.messagecenter.module.push.sender;

/**
 * 统一渠道发送适配器。
 */
public interface ChannelSender {

    /**
     * 返回适配器支持的渠道类型。
     *
     * @return 渠道类型编码
     */
    String channelType();

    /**
     * 发送消息。
     *
     * @param request 发送上下文
     * @return 发送结果
     */
    ChannelSendResult send(ChannelSendRequest request);
}
