package com.csg.ecard.messagecenter.module.push.mq;

import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.push.exception.MessagePushException;
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
        MessagePriority priority = resolvePriority(pushMessage);
        String msgId = pushMessage == null ? null : pushMessage.getMsgId();
        int retryCount = pushMessage == null ? 0 : pushMessage.getRetryCount();
        try {
            if (pushMessage == null) {
                throw MessagePushException.badRequest("异步消息结构错误：消息体不能为空");
            }
            if (!pushIdempotencyService.acquireConsumption(
                    msgId, retryCount)) {
                log.info("Duplicate async push consumption ignored. msgId={}, retryCount={}, priority={}",
                        msgId, retryCount, priority);
                return;
            }

            log.info("Async push consumption started. msgId={}, retryCount={}, priority={}",
                    msgId, retryCount, priority);
            AsyncPushExecutionResult result = messagePushService.consumeAsync(pushMessage);
            if (result.requiresRetry()) {
                publishRetry(pushMessage, result);
            }
        } catch (Exception ex) {
            if (!(ex instanceof BizException) && !(ex instanceof MessagePushException)) {
                try {
                    messagePushService.recordAsyncTechnicalFailure(pushMessage, ex);
                } catch (RuntimeException updateEx) {
                    log.error("Async push technical failure record update failed. msgId={}",
                            msgId, updateEx);
                }
            }
            log.error("Async push consumption failed. msgId={}, retryCount={}, priority={}",
                    msgId, retryCount, priority, ex);
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }

    private void publishRetry(AsyncPushMessage current,
                              AsyncPushExecutionResult result) {
        AsyncPushMessage retryMessage = new AsyncPushMessage();
        retryMessage.setMsgId(current.getMsgId());
        retryMessage.setRequest(current.getRequest());
        retryMessage.setCallType(current.getCallType());
        retryMessage.setPriority(resolvePriority(current));
        retryMessage.setRetryCount(current.getRetryCount() + 1);
        retryMessage.setPendingTemplateIds(result.retryTemplateIds());
        try {
            rabbitTemplate.convertAndSend(
                    MessagePushRabbitConstants.EXCHANGE,
                    MessagePushRabbitConstants.RETRY_ROUTING_KEY,
                    retryMessage,
                    message -> {
                        message.getMessageProperties()
                                .setPriority(retryMessage.getPriority().getMqPriority());
                        return message;
                    });
            log.info("Async push retry enqueued. msgId={}, retryCount={}, priority={}",
                    retryMessage.getMsgId(), retryMessage.getRetryCount(), retryMessage.getPriority());
        } catch (RuntimeException ex) {
            log.error("Async push retry publish failed, finalize current failures. msgId={}, retryCount={}",
                    current.getMsgId(), current.getRetryCount(), ex);
            retryMessage.setRetryCount(MessagePushRabbitConstants.MAX_RETRY_COUNT);
            messagePushService.consumeAsync(retryMessage);
        }
    }

    private MessagePriority resolvePriority(AsyncPushMessage message) {
        if (message == null) {
            return MessagePriority.NORMAL;
        }
        if (message.getPriority() != null) {
            return message.getPriority();
        }
        if (message.getRequest() != null && message.getRequest().getPriority() != null) {
            return message.getRequest().getPriority();
        }
        message.setPriority(MessagePriority.NORMAL);
        if (message.getRequest() != null) {
            message.getRequest().setPriority(MessagePriority.NORMAL);
        }
        return MessagePriority.NORMAL;
    }
}
