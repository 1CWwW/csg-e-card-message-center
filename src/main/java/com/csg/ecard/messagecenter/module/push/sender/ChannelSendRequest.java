package com.csg.ecard.messagecenter.module.push.sender;

import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;

/**
 * 渠道发送上下文。
 *
 * @param channel        匹配到的渠道
 * @param pushRequest    推送请求
 * @param messageContent 渲染后的消息正文
 * @param priority       消息业务优先级，与渠道匹配优先级无关
 */
public record ChannelSendRequest(MsgChannel channel,
                                 SyncPushDTO pushRequest,
                                 String messageContent,
                                 MessagePriority priority) {
}
