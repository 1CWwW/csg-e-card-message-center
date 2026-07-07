package com.csg.ecard.messagecenter.module.push.sender.impl;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.config.message.MessageSendProperties;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSender;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 南网短信平台发送适配器。
 */
@Component
@ConditionalOnProperty(prefix = "message.sender", name = "mode", havingValue = "real", matchIfMissing = true)
public class SmsChannelSender implements ChannelSender {

    private static final String SMS_PLATFORM_NANWANG = "NANWANG";
    private static final String MASS_SEND_API = "api/message/group/send";
    private static final String SMS_MSG_TYPE = "sms";
    private static final String SUCCESS_CODE = "0";

    private final MessageSendProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public SmsChannelSender(MessageSendProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Override
    public String channelType() {
        return ChannelType.SMS.getCode();
    }

    @Override
    public ChannelSendResult send(ChannelSendRequest request) {
        MessageSendInfo info = request.sendInfo();
        MessageSendProperties.Sms sms = properties.getSms();
        if (!sms.isEnabled()) {
            return ChannelSendResult.failedNonRetryable("短信发送未启用");
        }
        if (!SMS_PLATFORM_NANWANG.equalsIgnoreCase(sms.getXxptType())) {
            return ChannelSendResult.failedNonRetryable("短信平台" + sms.getXxptType() + "暂未接入");
        }
        if (!StringUtils.hasText(sms.getBaseUrl())) {
            return ChannelSendResult.failedNonRetryable("短信平台地址未配置");
        }
        if (!StringUtils.hasText(info.getReceivePhone())) {
            return ChannelSendResult.failedNonRetryable("短信接收手机号不能为空");
        }

        SmsMessageRequest body = new SmsMessageRequest(
                StringUtils.hasText(info.getMsgInfoId()) ? info.getMsgInfoId() : info.getMsgId(),
                SMS_MSG_TYPE,
                List.of(new SmsMessageItem(info.getReceivePhone(), info.getContent())),
                info.getContent());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (StringUtils.hasText(sms.getAuthorization())) {
            headers.add(HttpHeaders.AUTHORIZATION, sms.getAuthorization());
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(sms.getConnectTimeout())
                    .setReadTimeout(sms.getReadTimeout())
                    .build();
            SmsMessageResponse response = restTemplate.postForObject(
                    buildUrl(sms.getBaseUrl(), MASS_SEND_API),
                    new HttpEntity<>(body, headers),
                    SmsMessageResponse.class);
            if (response == null) {
                return ChannelSendResult.failed("短信平台无返回结果");
            }
            if (SUCCESS_CODE.equals(response.code())) {
                return ChannelSendResult.succeeded();
            }
            return ChannelSendResult.failedNonRetryable(
                    StringUtils.hasText(response.msg()) ? response.msg() : "短信平台返回失败");
        } catch (RestClientException ex) {
            return ChannelSendResult.failed("短信平台调用失败：" + ex.getMessage());
        }
    }

    private String buildUrl(String baseUrl, String path) {
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private record SmsMessageRequest(String msgInfoId,
                                     String msgType,
                                     List<SmsMessageItem> items,
                                     String content) {
    }

    private record SmsMessageItem(String to, String content) {
    }

    private record SmsMessageResponse(String code, String msg, String uuid) {
    }
}
