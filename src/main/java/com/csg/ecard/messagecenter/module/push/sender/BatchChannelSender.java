package com.csg.ecard.messagecenter.module.push.sender;

import java.util.List;

/**
 * 支持同渠道批量发送的适配器。
 */
public interface BatchChannelSender extends ChannelSender {

    /**
     * 批量发送同一渠道消息。
     *
     * @param requests 发送上下文集合
     * @return 批量发送结果
     */
    ChannelSendResult sendBatch(List<ChannelSendRequest> requests);
}
