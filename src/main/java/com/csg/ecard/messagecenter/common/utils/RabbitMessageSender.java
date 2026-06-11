package com.csg.ecard.messagecenter.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * RabbitMQ 消息发送工具类。
 * <p>
 * 封装 RabbitTemplate 发送逻辑，统一生成或透传 correlationId，便于结合发布确认日志追踪消息投递状态。
 */
@Component
@RequiredArgsConstructor
public class RabbitMessageSender {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送消息并自动生成 correlationId。
     *
     * @param exchange   交换机名称
     * @param routingKey 路由键
     * @param message    消息体
     * @return 本次发送使用的 correlationId
     */
    public String send(String exchange, String routingKey, Object message) {
        String correlationId = UUID.randomUUID().toString().replace("-", "");
        send(exchange, routingKey, message, correlationId);
        return correlationId;
    }

    /**
     * 使用指定 correlationId 发送消息。
     *
     * @param exchange      交换机名称
     * @param routingKey    路由键
     * @param message       消息体
     * @param correlationId 消息关联 ID；为空时自动生成
     */
    public void send(String exchange, String routingKey, Object message, String correlationId) {
        CorrelationData correlationData = new CorrelationData(
                StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString().replace("-", "")
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }
}
