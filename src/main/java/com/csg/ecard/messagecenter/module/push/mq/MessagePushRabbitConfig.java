package com.csg.ecard.messagecenter.module.push.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 异步消息推送队列及固定间隔重试队列配置。
 */
@Configuration
public class MessagePushRabbitConfig {

    @Bean
    public DirectExchange messagePushExchange() {
        return new DirectExchange(MessagePushRabbitConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue messagePushQueue() {
        return QueueBuilder.durable(MessagePushRabbitConstants.QUEUE).build();
    }

    @Bean
    public Binding messagePushBinding(Queue messagePushQueue,
                                      DirectExchange messagePushExchange) {
        return BindingBuilder.bind(messagePushQueue)
                .to(messagePushExchange)
                .with(MessagePushRabbitConstants.ROUTING_KEY);
    }

    @Bean
    public Queue messagePushRetryQueue() {
        return QueueBuilder.durable(MessagePushRabbitConstants.RETRY_QUEUE)
                .ttl(MessagePushRabbitConstants.RETRY_DELAY_MILLIS)
                .deadLetterExchange(MessagePushRabbitConstants.EXCHANGE)
                .deadLetterRoutingKey(MessagePushRabbitConstants.ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding messagePushRetryBinding(Queue messagePushRetryQueue,
                                           DirectExchange messagePushExchange) {
        return BindingBuilder.bind(messagePushRetryQueue)
                .to(messagePushExchange)
                .with(MessagePushRabbitConstants.RETRY_ROUTING_KEY);
    }
}
