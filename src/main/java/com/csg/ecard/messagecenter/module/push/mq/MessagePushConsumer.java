package com.csg.ecard.messagecenter.module.push.mq;

import com.csg.ecard.messagecenter.module.push.service.MessagePushService;
import com.csg.ecard.messagecenter.module.push.service.PushIdempotencyService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 异步消息推送消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePushConsumer {

    private final MessagePushService messagePushService;
    private final PushIdempotencyService pushIdempotencyService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 消费推送消息。所有异常均在消费者内收敛并确认，避免容器无限重投。
     */
    @RabbitListener(queues = MessagePushRabbitConstants.QUEUE)
    public void consume(AsyncPushMessage pushMessage,
                        Message amqpMessage,
                        Channel channel) throws Exception {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            if (!pushIdempotencyService.acquireConsumption(
                    pushMessage.getMsgId(), pushMessage.getRetryCount())) {
                log.info("Duplicate async push consumption ignored. msgId={}, retryCount={}",
                        pushMessage.getMsgId(), pushMessage.getRetryCount());
                return;
            }

            AsyncPushExecutionResult result = messagePushService.consumeAsync(pushMessage);
            if (result.requiresRetry()) {
                publishRetry(pushMessage, result);
            }
        } catch (Exception ex) {
            log.error("Async push consumption failed. msgId={}, retryCount={}",
                    pushMessage.getMsgId(), pushMessage.getRetryCount(), ex);
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }

    private void publishRetry(AsyncPushMessage current,
                              AsyncPushExecutionResult result) {
        AsyncPushMessage retryMessage = new AsyncPushMessage();
        retryMessage.setMsgId(current.getMsgId());
        retryMessage.setRequest(current.getRequest());
        retryMessage.setRetryCount(current.getRetryCount() + 1);
        retryMessage.setPendingTemplateIds(result.retryTemplateIds());
        try {
            rabbitTemplate.convertAndSend(
                    MessagePushRabbitConstants.EXCHANGE,
                    MessagePushRabbitConstants.RETRY_ROUTING_KEY,
                    retryMessage);
        } catch (RuntimeException ex) {
            log.error("Async push retry publish failed, finalize current failures. msgId={}, retryCount={}",
                    current.getMsgId(), current.getRetryCount(), ex);
            retryMessage.setRetryCount(MessagePushRabbitConstants.MAX_RETRY_COUNT);
            messagePushService.consumeAsync(retryMessage);
        }
    }
}
