package com.csg.ecard.messagecenter.config.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置。
 * <p>
 * 统一消息 JSON 序列化、发布确认和不可路由消息回调，发送方可通过日志定位投递失败原因。
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    /**
     * RabbitMQ 消息 JSON 转换器。
     *
     * @param objectMapper 全局 Jackson 配置
     * @return 消息转换器
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate 发送模板。
     * <p>
     * 开启 mandatory 后，不可路由消息会触发 ReturnsCallback；发布确认由 ConfirmCallback 记录。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param messageConverter  消息转换器
     * @return RabbitTemplate 实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ message confirm failed, correlationData={}, cause={}", correlationData, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.warn(
                "RabbitMQ message returned, exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));
        return rabbitTemplate;
    }
}
