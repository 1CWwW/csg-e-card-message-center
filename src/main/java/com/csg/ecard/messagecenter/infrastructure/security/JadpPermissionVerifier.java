package com.csg.ecard.messagecenter.infrastructure.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * JADP 菜单权限校验器。
 * <p>
 * 负责调用 JADP 权限接口并将远端 HTTP 状态转换为消息中心内部校验结果。
 */
@Component
public class JadpPermissionVerifier {

    private static final String ACCESS_TOKEN = "access-token";

    private final RestTemplate restTemplate;
    private final String jadpUrl;

    public JadpPermissionVerifier(@Qualifier("httpsRestTemplate") RestTemplate restTemplate,
                                  @Value("${message-center.permission.jadp-url:}") String jadpUrl) {
        this.restTemplate = restTemplate;
        this.jadpUrl = jadpUrl;
    }

    /**
     * 校验当前登录用户是否拥有指定菜单权限。
     *
     * @param accessToken JADP 登录令牌
     * @param pageCode    菜单编码
     * @return 权限校验结果
     */
    public PermissionCheckResult verify(String accessToken, String pageCode) {
        if (!StringUtils.hasText(jadpUrl)) {
            return PermissionCheckResult.UNAVAILABLE;
        }

        URI requestUri = buildRequestUri(pageCode);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(ACCESS_TOKEN, accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUri, HttpMethod.GET, requestEntity, String.class);
            Boolean allowed = parseResult(response.getBody());
            if (allowed == null) {
                return PermissionCheckResult.UNAVAILABLE;
            }

            return allowed
                    ? PermissionCheckResult.ALLOWED
                    : PermissionCheckResult.DENIED;
        } catch (RestClientResponseException ex) {
            return convertStatus(ex.getStatusCode().value());
        } catch (RuntimeException ignored) {
            return PermissionCheckResult.UNAVAILABLE;
        }
    }

    private URI buildRequestUri(String pageCode) {
        return UriComponentsBuilder.fromUriString(jadpUrl)
                .pathSegment("func", "verifyAccess")
                .queryParam("pageCode", pageCode)
                .build()
                .encode()
                .toUri();
    }

    private Boolean parseResult(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        String result = responseBody.trim();
        if ("true".equalsIgnoreCase(result)) {
            return true;
        }
        if ("false".equalsIgnoreCase(result)) {
            return false;
        }
        return null;
    }

    private PermissionCheckResult convertStatus(int status) {
        if (status == 401) {
            return PermissionCheckResult.UNAUTHORIZED;
        }
        if (status == 403 || status == 404) {
            return PermissionCheckResult.DENIED;
        }
        return PermissionCheckResult.UNAVAILABLE;
    }
}
