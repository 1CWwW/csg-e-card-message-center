package com.csg.ecard.messagecenter.module.push.mq;

import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 异步推送 RabbitMQ 消息。
 */
@Getter
@Setter
@NoArgsConstructor
public class AsyncPushMessage {

    private String msgId;
    private SyncPushDTO request;
    private int retryCount;
    private List<Long> pendingTemplateIds = new ArrayList<>();

    public AsyncPushMessage(String msgId, SyncPushDTO request) {
        this.msgId = msgId;
        this.request = request;
    }
}
