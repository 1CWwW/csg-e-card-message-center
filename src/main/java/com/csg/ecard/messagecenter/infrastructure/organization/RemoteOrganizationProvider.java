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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private volatile CachedOrganizations cachedPathOrganizations;

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
        // 员工可能归属部门，不能用仅包含单位的展示树截断原有匹配路径。
        return resolveUnitPath(orgId, loadPathIndex().all(), 50);
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
            List<OrganizationNode> nodes = queryUnitTree();
            OrganizationIndex index = new OrganizationIndex(nodes);
            if (cacheEnabled()) {
                cachedOrganizations = new CachedOrganizations(
                        index,
                        System.nanoTime() + organizationProperties.getRemote().getCacheTtl().toNanos()
                );
            }
            if (refreshingExistingSnapshot) {
                eventPublisher.publishEvent(OrganizationChangedEvent.fullSyncEvent());
            }
            return index;
        }
    }

    private OrganizationIndex loadPathIndex() {
        CachedOrganizations current = cachedPathOrganizations;
        if (current != null && current.expiresAtNanos() > System.nanoTime()) {
            return current.index();
        }
        synchronized (this) {
            current = cachedPathOrganizations;
            if (current != null && current.expiresAtNanos() > System.nanoTime()) {
                return current.index();
            }
            OrganizationIndex index = new OrganizationIndex(queryAll());
            if (cacheEnabled()) {
                cachedPathOrganizations = new CachedOrganizations(index,
                        System.nanoTime() + organizationProperties.getRemote().getCacheTtl().toNanos());
            }
            return index;
        }
    }

    private List<OrganizationNode> queryUnitTree() {
        String rootOrgId = trimToNull(organizationProperties.getRemote().getRootOrgId());
        if (rootOrgId == null) {
            throw new BizException(ErrorCode.INFRASTRUCTURE_ERROR, "未配置单位树根单位ID");
        }
        long start = System.nanoTime();
        try {
            List<RemoteOrganizationDTO> response = jadpOrganizationClient.queryCorpTree(rootOrgId);
            // 异常响应不作为空树缓存，避免后续查询持续返回空结果。
            if (response == null || response.isEmpty()) {
                throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "单位树响应为空，预期为包含根节点的组织数组");
            }
            return normalizeUnitTree(response, rootOrgId);
        } catch (FeignException ex) {
            // Feign 的异常消息可能包含完整响应体，不输出消息及鉴权信息。
            log.warn("Organization unit tree request failed. rootOrgId={}, status={}, exceptionType={}, cost={}ms",
                    rootOrgId, ex.status(), ex.getClass().getSimpleName(), elapsedMillis(start));
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "单位树服务调用失败");
        } catch (BizException ex) {
            log.warn("Organization unit tree validation failed. rootOrgId={}, reason={}, cost={}ms",
                    rootOrgId, ex.getMessage(), elapsedMillis(start));
            throw ex;
        }
    }

    /**
     * 上游返回扁平列表，保留虚拟分组的父子关系；拒绝断链或循环数据，避免前端不断懒加载。
     */
    private List<OrganizationNode> normalizeUnitTree(List<RemoteOrganizationDTO> response, String rootOrgId) {
        Map<String, OrganizationNode> nodes = new LinkedHashMap<>();
        for (OrganizationNode node : flatten(response)) {
            node.setParentOrgId(trimToNull(node.getParentOrgId()));
            nodes.put(node.getOrgId(), node);
        }
        OrganizationNode root = nodes.get(rootOrgId);
        if (root == null) {
            throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "单位树返回的根单位ID不匹配");
        }
        // 查询起点可以是任意单位，其上级不属于本次返回范围。
        root.setParentOrgId(null);
        Set<String> connected = new HashSet<>();
        connected.add(rootOrgId);
        for (OrganizationNode node : nodes.values()) {
            Set<String> path = new HashSet<>();
            String current = node.getOrgId();
            while (!connected.contains(current)) {
                if (current == null || !nodes.containsKey(current)) {
                    throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "单位树存在缺失的父节点");
                }
                if (!path.add(current)) {
                    throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "单位树存在循环父子关系");
                }
                current = nodes.get(current).getParentOrgId();
            }
            connected.addAll(path);
        }
        return new ArrayList<>(nodes.values());
    }

    private long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
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
                .filter(node -> Integer.valueOf(1).equals(node.getState()))
                .toList();
    }

    /**
     * 保留上游 parentOrgId，不将扁平数组中的每个节点误认为根节点。
     */
    private List<OrganizationNode> flatten(List<RemoteOrganizationDTO> source) {
        List<OrganizationNode> result = new ArrayList<>();
        Deque<RemoteOrganizationDTO> pending = new ArrayDeque<>();
        for (RemoteOrganizationDTO item : source) {
            if (item != null) {
                pending.addLast(item);
            }
        }
        Set<String> visited = new HashSet<>();
        int duplicateCount = 0;
        while (!pending.isEmpty()) {
            RemoteOrganizationDTO item = pending.removeFirst();
            String orgId = trimToNull(item.getOrgId());
            if (orgId == null) {
                throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "组织节点缺少orgId");
            }
            if (!visited.add(orgId)) {
                duplicateCount++;
                continue;
            }
            OrganizationNode node = toNode(item);
            node.setOrgId(orgId);
            result.add(node);
            if (item.getChildren() != null) {
                for (RemoteOrganizationDTO child : item.getChildren()) {
                    if (child != null) {
                        pending.addLast(child);
                    }
                }
            }
        }
        if (duplicateCount > 0) {
            log.warn("Organization repeated nodes skipped. duplicateCount={}", duplicateCount);
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
