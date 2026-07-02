package com.csg.ecard.messagecenter.module.push.sender.impl;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.config.message.MessageSendProperties;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSender;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

/**
 * eLink 应用消息发送适配器。
 */
@Component
public class ElinkChannelSender implements ChannelSender {

    private static final String SUCCESS_CODE = "0";
    private static final String TOKEN_INVALID_CODE = "42001";
    private static final String MSG_RETRY_CODE = "40058";
    private static final long DEFAULT_TOKEN_EXPIRE_SECONDS = 7200L;

    private final MessageSendProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;
    private volatile String accessToken;
    private volatile Instant accessTokenExpireAt = Instant.EPOCH;

    public ElinkChannelSender(MessageSendProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Override
    public String channelType() {
        return ChannelType.ELINK.getCode();
    }

    @Override
    public ChannelSendResult send(ChannelSendRequest request) {
        MessageSendInfo info = request.sendInfo();
        MessageSendProperties.Elink elink = properties.getElink();
        if (!elink.isEnabled()) {
            return ChannelSendResult.failedNonRetryable("eLink发送未启用");
        }
        String configError = validateConfig(elink);
        if (configError != null) {
            return ChannelSendResult.failedNonRetryable(configError);
        }
        if (!StringUtils.hasText(info.getElinkUserid())) {
            return ChannelSendResult.failedNonRetryable("eLink用户ID不能为空");
        }

        try {
            return sendWithToken(info, elink, false);
        } catch (RestClientException ex) {
            return ChannelSendResult.failed("eLink平台调用失败：" + ex.getMessage());
        }
    }

    private ChannelSendResult sendWithToken(MessageSendInfo info,
                                            MessageSendProperties.Elink elink,
                                            boolean tokenRefreshed) {
        RestTemplate restTemplate = restTemplate(elink);
        String requestUrl = UriComponentsBuilder.fromUriString(resolveSendUrl(elink))
                .queryParam("access_token", getAccessToken(elink, restTemplate))
                .toUriString();
        ElinkMessageResponse response = restTemplate.postForObject(
                requestUrl,
                new HttpEntity<>(buildMessage(info, elink), jsonHeaders()),
                ElinkMessageResponse.class);
        if (response == null) {
            return ChannelSendResult.failed("eLink平台无返回结果");
        }
        if (TOKEN_INVALID_CODE.equals(response.errcode()) && !tokenRefreshed) {
            clearAccessToken();
            return sendWithToken(info, elink, true);
        }
        if (SUCCESS_CODE.equals(response.errcode())) {
            return ChannelSendResult.succeeded();
        }
        String message = StringUtils.hasText(response.errmsg()) ? response.errmsg() : "eLink平台返回失败";
        if (MSG_RETRY_CODE.equals(response.errcode())) {
            return ChannelSendResult.failed(message);
        }
        return ChannelSendResult.failedNonRetryable(message);
    }

    private ElinkMessageRequest buildMessage(MessageSendInfo info, MessageSendProperties.Elink elink) {
        if (StringUtils.hasText(info.getTitle()) && StringUtils.hasText(info.getUrl())) {
            return new ElinkMessageRequest(messageRecordId(info), elink.getAgentId(), info.getElinkUserid(), "textcard",
                    new ElinkText(info.getContent()),
                    new ElinkTextCard(info.getTitle(), info.getContent(), info.getUrl()));
        }
        return new ElinkMessageRequest(messageRecordId(info), elink.getAgentId(), info.getElinkUserid(), "text",
                new ElinkText(info.getContent()), null);
    }

    private String messageRecordId(MessageSendInfo info) {
        return StringUtils.hasText(info.getMsgInfoId()) ? info.getMsgInfoId() : info.getMsgId();
    }

    private String getAccessToken(MessageSendProperties.Elink elink, RestTemplate restTemplate) {
        if (StringUtils.hasText(accessToken) && Instant.now().isBefore(accessTokenExpireAt)) {
            return accessToken;
        }
        synchronized (this) {
            if (StringUtils.hasText(accessToken) && Instant.now().isBefore(accessTokenExpireAt)) {
                return accessToken;
            }
            String tokenUrl = UriComponentsBuilder.fromUriString(resolveTokenUrl(elink))
                    .queryParam("corpid", elink.getAppId())
                    .queryParam("corpsecret", elink.getSecret())
                    .toUriString();
            ElinkTokenResponse response = restTemplate.getForObject(tokenUrl, ElinkTokenResponse.class);
            if (response == null || !SUCCESS_CODE.equals(response.errcode()) || !StringUtils.hasText(response.access_token())) {
                throw new IllegalStateException("获取eLink accessToken失败");
            }
            accessToken = response.access_token();
            long expiresIn = response.expires_in() == null || response.expires_in() <= 0
                    ? DEFAULT_TOKEN_EXPIRE_SECONDS : response.expires_in();
            accessTokenExpireAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
            return accessToken;
        }
    }

    private void clearAccessToken() {
        accessToken = null;
        accessTokenExpireAt = Instant.EPOCH;
    }

    private RestTemplate restTemplate(MessageSendProperties.Elink elink) {
        return restTemplateBuilder
                .setConnectTimeout(elink.getConnectTimeout())
                .setReadTimeout(elink.getReadTimeout())
                .build();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String validateConfig(MessageSendProperties.Elink elink) {
        if (!StringUtils.hasText(resolveTokenUrl(elink))) {
            return "eLink token地址未配置";
        }
        if (!StringUtils.hasText(resolveSendUrl(elink))) {
            return "eLink发送地址未配置";
        }
        if (!StringUtils.hasText(elink.getAppId())) {
            return "eLink应用ID未配置";
        }
        if (!StringUtils.hasText(elink.getSecret())) {
            return "eLink应用密钥未配置";
        }
        if (!StringUtils.hasText(elink.getAgentId())) {
            return "eLink agentId未配置";
        }
        return null;
    }

    private String resolveTokenUrl(MessageSendProperties.Elink elink) {
        if (StringUtils.hasText(elink.getTokenUrl())) {
            return elink.getTokenUrl();
        }
        return buildUrl(elink.getBaseUrl(), elink.getTokenPath());
    }

    private String resolveSendUrl(MessageSendProperties.Elink elink) {
        if (StringUtils.hasText(elink.getSendUrl())) {
            return elink.getSendUrl();
        }
        return buildUrl(elink.getBaseUrl(), elink.getSendMsgPath());
    }

    private String buildUrl(String baseUrl, String path) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(path)) {
            return null;
        }
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private record ElinkMessageRequest(String msgInfoId,
                                       String agentid,
                                       String touser,
                                       String msgtype,
                                       ElinkText text,
                                       ElinkTextCard textcard) {
    }

    private record ElinkText(String content) {
    }

    private record ElinkTextCard(String title, String description, String url) {
    }

    private record ElinkMessageResponse(String errcode, String errmsg) {
    }

    private record ElinkTokenResponse(String errcode, String errmsg, String access_token, Long expires_in) {
    }
}
