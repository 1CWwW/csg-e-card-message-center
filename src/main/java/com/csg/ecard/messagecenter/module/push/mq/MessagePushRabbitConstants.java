package com.csg.ecard.messagecenter.module.push.mq;

/**
 * 消息推送 RabbitMQ 常量。
 */
public final class MessagePushRabbitConstants {

    public static final String EXCHANGE = "message.center.push.exchange";
    public static final String QUEUE = "message.center.push.queue";
    public static final String ROUTING_KEY = "message.center.push.execute";
    public static final String RETRY_QUEUE = "message.center.push.retry.queue";
    public static final String RETRY_ROUTING_KEY = "message.center.push.retry";
    public static final int MAX_QUEUE_PRIORITY = 10;
    public static final int RETRY_DELAY_MILLIS = 5000;
    public static final int MAX_RETRY_COUNT = 3;

    private MessagePushRabbitConstants() {
    }
}
