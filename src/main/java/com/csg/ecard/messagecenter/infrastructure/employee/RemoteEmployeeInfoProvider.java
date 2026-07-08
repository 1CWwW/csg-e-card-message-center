package com.csg.ecard.messagecenter.infrastructure.employee;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 内网员工中心实现，用于补齐接收人联系方式和所属组织。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.employee", name = "mode", havingValue = "remote")
public class RemoteEmployeeInfoProvider implements EmployeeInfoProvider {

    private final EmployeeProperties employeeProperties;
    private final RestClient restClient;

    public RemoteEmployeeInfoProvider(EmployeeProperties employeeProperties, RestClient.Builder builder) {
        this.employeeProperties = employeeProperties;
        this.restClient = builder
                .requestFactory(requestFactory(employeeProperties))
                .build();
    }

    @Override
    public Map<String, EmployeeInfo> listUsers(Collection<String> userIds) {
        List<String> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return Map.of();
        }
        EmployeeProperties.Remote remote = employeeProperties.getRemote();
        if (!StringUtils.hasText(remote.getBaseUrl())) {
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "员工中心地址未配置");
        }

        String url = buildUrl(remote, "/v1/user/getByIds");
        try {
            RemoteUserDTO[] response = restClient.post()
                    .uri(url)
                    .body(normalizedUserIds)
                    .retrieve()
                    .body(RemoteUserDTO[].class);
            if (response == null || response.length == 0) {
                return Map.of();
            }
            Map<String, EmployeeInfo> result = new LinkedHashMap<>();
            for (RemoteUserDTO user : response) {
                EmployeeInfo info = toEmployeeInfo(user);
                if (info != null && StringUtils.hasText(info.userId())) {
                    result.put(info.userId(), info);
                }
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Employee remote query failed. url={}, userCount={}, cause={}",
                    url, normalizedUserIds.size(), ex.getMessage());
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "员工中心调用失败");
        }
    }

    @Override
    public List<String> getUnitPath(String unitId) {
        return StringUtils.hasText(unitId) ? List.of(unitId.trim()) : List.of();
    }

    private List<String> normalizeUserIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(userId -> !containsChinese(userId))
                .distinct()
                .toList();
    }

    private boolean containsChinese(String value) {
        return value.chars()
                .mapToObj(Character.UnicodeScript::of)
                .anyMatch(script -> script == Character.UnicodeScript.HAN);
    }

    private EmployeeInfo toEmployeeInfo(RemoteUserDTO user) {
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            return null;
        }
        return new EmployeeInfo(
                user.getUserId(),
                user.getEmployeeName(),
                user.getMobilePhone(),
                user.getEmail(),
                user.getOrgId(),
                null,
                user.getElinkUserId()
        );
    }

    private String buildUrl(EmployeeProperties.Remote remote, String path) {
        String baseUrl = trimTrailingSlash(remote.getBaseUrl());
        String contextPath = trimSlashes(remote.getContextPath());
        return contextPath == null ? baseUrl + path : baseUrl + "/" + contextPath + path;
    }

    private String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String result = value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? null : result;
    }

    private ClientHttpRequestFactory requestFactory(EmployeeProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getRemote().getConnectTimeout())
                .withReadTimeout(properties.getRemote().getReadTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
