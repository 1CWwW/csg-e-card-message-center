package com.csg.ecard.messagecenter.infrastructure.organization;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 内网组织接口实现。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.organization", name = "mode", havingValue = "remote")
public class RemoteOrganizationProvider extends AbstractOrganizationProvider {

    private final OrganizationProperties organizationProperties;
    private final RestClient restClient;

    public RemoteOrganizationProvider(OrganizationProperties organizationProperties, RestClient.Builder builder) {
        this.organizationProperties = organizationProperties;
        this.restClient = builder
                .requestFactory(requestFactory(organizationProperties))
                .build();
    }

    @Override
    public List<OrganizationNode> tree() {
        return buildTree(loadAll());
    }

    @Override
    public List<String> resolveUnitPath(String orgId) {
        return resolveUnitPath(orgId, loadAll(), 50);
    }

    private List<OrganizationNode> loadAll() {
        OrganizationProperties.Remote remote = organizationProperties.getRemote();
        if (!StringUtils.hasText(remote.getBaseUrl())) {
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "组织服务地址未配置");
        }
        String url = buildUrl(remote, "/v1/organization/all/");
        try {
            RemoteOrganizationDTO[] response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RemoteOrganizationDTO[].class);
            if (response == null || response.length == 0) {
                return List.of();
            }
            return flatten(Arrays.asList(response));
        } catch (RestClientException ex) {
            log.warn("Organization remote query failed. url={}, cause={}", url, ex.getMessage());
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "组织服务调用失败");
        }
    }

    private List<OrganizationNode> flatten(List<RemoteOrganizationDTO> source) {
        List<OrganizationNode> result = new ArrayList<>();
        for (RemoteOrganizationDTO item : source) {
            if (item == null) {
                continue;
            }
            result.add(toNode(item));
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                result.addAll(flatten(item.getChildren()));
            }
        }
        return result;
    }

    private OrganizationNode toNode(RemoteOrganizationDTO dto) {
        OrganizationNode node = new OrganizationNode();
        node.setOrgId(dto.getOrgId());
        node.setOrgName(dto.getOrgName());
        node.setOrgCode(dto.getOrgCode());
        node.setParentOrgId(dto.getParentOrgId());
        node.setNameFullPath(dto.getNameFullPath());
        node.setOrgLevel(dto.getOrgLevel());
        node.setState(dto.getState());
        node.setSortNo(dto.getSortNo());
        return node;
    }

    private String buildUrl(OrganizationProperties.Remote remote, String path) {
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
        String result = trimToNull(value);
        if (result == null) {
            return null;
        }
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? null : result;
    }

    private ClientHttpRequestFactory requestFactory(OrganizationProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getRemote().getConnectTimeout())
                .withReadTimeout(properties.getRemote().getReadTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
