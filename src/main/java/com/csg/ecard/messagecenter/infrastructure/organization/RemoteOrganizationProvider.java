package com.csg.ecard.messagecenter.infrastructure.organization;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 内网组织接口实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.organization", name = "mode", havingValue = "remote", matchIfMissing = true)
public class RemoteOrganizationProvider extends AbstractOrganizationProvider {

    private final JadpOrganizationClient jadpOrganizationClient;
    private final OrganizationProperties organizationProperties;
    private final ApplicationEventPublisher eventPublisher;

    private volatile CachedOrganizations cachedOrganizations;

    @Override
    public List<OrganizationNode> tree() {
        return buildTree(visibleNodes(loadIndex().all()));
    }

    @Override
    public List<OrganizationNode> children(String parentOrgId) {
        return loadIndex().children(parentOrgId);
    }

    @Override
    public Set<String> parentOrgIdsWithChildren(Collection<String> orgIds) {
        return loadIndex().parentOrgIdsWithChildren(orgIds);
    }

    @Override
    public List<OrganizationPath> resolve(Collection<String> orgIds) {
        return loadIndex().resolve(orgIds);
    }

    @Override
    public List<OrganizationPath> search(String keyword, int limit, String scopeOrgId) {
        return loadIndex().search(keyword, limit, scopeOrgId);
    }

    @Override
    public boolean isWithinScope(String orgId, String scopeOrgId) {
        return loadIndex().isWithinScope(orgId, scopeOrgId);
    }

    @Override
    public List<String> resolveUnitPath(String orgId) {
        return resolveUnitPath(orgId, loadIndex().all(), 50);
    }

    private OrganizationIndex loadIndex() {
        long now = System.nanoTime();
        CachedOrganizations current = cachedOrganizations;
        if (current != null && current.expiresAtNanos() > now) {
            return current.index();
        }

        synchronized (this) {
            now = System.nanoTime();
            current = cachedOrganizations;
            if (current != null && current.expiresAtNanos() > now) {
                return current.index();
            }

            boolean refreshingExistingSnapshot = current != null;
            List<OrganizationNode> nodes = queryAll();
            OrganizationIndex index = new OrganizationIndex(nodes);
            if (cacheEnabled()) {
                cachedOrganizations = new CachedOrganizations(
                        index,
                        now + organizationProperties.getRemote().getCacheTtl().toNanos()
                );
            }
            if (refreshingExistingSnapshot) {
                eventPublisher.publishEvent(OrganizationChangedEvent.fullSyncEvent());
            }
            return index;
        }
    }

    private List<OrganizationNode> queryAll() {
        try {
            List<RemoteOrganizationDTO> response = jadpOrganizationClient.queryAll();
            if (response == null || response.isEmpty()) {
                return List.of();
            }
            return flatten(response);
        } catch (FeignException ex) {
            log.warn("Organization remote query failed. status={}, cause={}", ex.status(), ex.getMessage());
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "组织服务调用失败");
        }
    }

    private boolean cacheEnabled() {
        return organizationProperties.getRemote().getCacheTtl() != null
                && !organizationProperties.getRemote().getCacheTtl().isZero()
                && !organizationProperties.getRemote().getCacheTtl().isNegative();
    }

    /**
     * 停用组织仅不在组织树中展示，仍保留在单位路径解析中以兼容已有单位配置。
     */
    private Collection<OrganizationNode> visibleNodes(Collection<OrganizationNode> nodes) {
        return nodes.stream()
                .filter(node -> !Integer.valueOf(0).equals(node.getState()))
                .toList();
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

    private record CachedOrganizations(OrganizationIndex index, long expiresAtNanos) {
    }
}
