package com.csg.ecard.messagecenter.module.push.sender.impl;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.config.message.MessageSendProperties;
import com.csg.ecard.messagecenter.module.push.sender.BatchChannelSender;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 站内信发送适配器。
 */
@Component
public class InAppChannelSender implements BatchChannelSender {

    private final MessageSendProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public InAppChannelSender(MessageSendProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Override
    public String channelType() {
        return ChannelType.IN_APP.getCode();
    }

    @Override
    public ChannelSendResult send(ChannelSendRequest request) {
        return sendBatch(List.of(request));
    }

    @Override
    public ChannelSendResult sendBatch(List<ChannelSendRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return ChannelSendResult.succeeded();
        }
        MessageSendProperties.InApp inApp = properties.getInApp();
        if (!inApp.isEnabled()) {
            return ChannelSendResult.failedNonRetryable("站内信发送未启用");
        }
        if (!StringUtils.hasText(inApp.getNotificationUrl())) {
            return ChannelSendResult.failedNonRetryable("站内信通知地址未配置");
        }

        List<InAppNotification> body = requests.stream()
                .filter(Objects::nonNull)
                .map(ChannelSendRequest::sendInfo)
                .map(this::buildNotification)
                .toList();
        if (body.isEmpty()) {
            return ChannelSendResult.succeeded();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(inApp.getConnectTimeout())
                    .setReadTimeout(inApp.getReadTimeout())
                    .build();
            Object response = restTemplate.postForObject(
                    inApp.getNotificationUrl(),
                    new HttpEntity<>(body, headers),
                    Object.class);
            return parseResponse(response);
        } catch (RestClientException ex) {
            return ChannelSendResult.failed("站内信平台调用失败：" + ex.getMessage());
        }
    }

    private InAppNotification buildNotification(MessageSendInfo info) {
        if (!StringUtils.hasText(info.getUrl())) {
            throw new IllegalArgumentException("站内信url不能为空");
        }
        return new InAppNotification(
                info.getRegisterXtbs(),
                StringUtils.hasText(info.getMsgInfoId()) ? info.getMsgInfoId() : info.getMsgId(),
                info.getContent(),
                info.getUrl(),
                info.getType(),
                info.getSendUserId(),
                info.getReceiveUserId());
    }

    private ChannelSendResult parseResponse(Object response) {
        if (response instanceof Boolean success) {
            return success ? ChannelSendResult.succeeded()
                    : ChannelSendResult.failedNonRetryable("站内信平台返回失败");
        }
        if (response instanceof Map<?, ?> result) {
            Object success = result.get("success");
            if (Boolean.FALSE.equals(success)) {
                return ChannelSendResult.failedNonRetryable(messageOf(result));
            }
            Object code = result.get("code");
            if (code != null && !"0".equals(String.valueOf(code)) && !"200".equals(String.valueOf(code))) {
                return ChannelSendResult.failedNonRetryable(messageOf(result));
            }
        }
        return ChannelSendResult.succeeded();
    }

    private String messageOf(Map<?, ?> result) {
        Object message = result.get("message");
        if (message == null) {
            message = result.get("msg");
        }
        return message == null ? "站内信平台返回失败" : String.valueOf(message);
    }

    private record InAppNotification(String xtbs,
                                     String ywid,
                                     String title,
                                     String url,
                                     String type,
                                     String sendUserId,
                                     String receiveUserId) {
    }
}
