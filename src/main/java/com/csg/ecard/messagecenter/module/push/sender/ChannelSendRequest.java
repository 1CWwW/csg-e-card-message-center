package com.csg.ecard.messagecenter.module.push.sender;

import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;

/**
 * 渠道发送上下文。
 *
 * @param channel        匹配到的渠道
 * @param pushRequest    推送请求
 * @param messageContent 渲染后的消息正文
 */
public record ChannelSendRequest(MsgChannel channel,
                                 SyncPushDTO pushRequest,
                                 String messageContent) {
}
