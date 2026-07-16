package com.csg.ecard.messagecenter.module.organization.service;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.utils.RedisUtil;
import com.csg.ecard.messagecenter.framework.context.CurrentUserContext;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationNode;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationPath;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProperties;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.organization.dto.OrganizationResolveDTO;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationLazyNodeVO;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationNodeVO;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationResolvedVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 组织架构查询服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService, OrganizationCacheInvalidator {

    private static final String CACHE_PREFIX = "organization:children:";
    private static final String ROOT_KEY = "root";
    private static final String ALL_SCOPE = "all";
    private static final String GLOBAL_VERSION_KEY = CACHE_PREFIX + "version:all";
    private static final TypeReference<List<OrganizationLazyNodeVO>> CHILDREN_TYPE = new TypeReference<>() {
    };

    private final OrganizationProvider organizationProvider;
    private final OrganizationProperties organizationProperties;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Object> cacheLocks = new ConcurrentHashMap<>();

    @Override
    public List<OrganizationNodeVO> tree() {
        return organizationProvider.tree().stream()
                .map(this::toTreeVO)
                .toList();
    }

    @Override
    public List<OrganizationLazyNodeVO> children(String parentOrgId) {
        long start = System.nanoTime();
        String parent = trimToNull(parentOrgId);
        String scopeOrgId = trimToNull(CurrentUserContext.getOrgId());
        String cacheKey = childrenCacheKey(parent, scopeOrgId);
        List<OrganizationLazyNodeVO> result = readChildrenCache(cacheKey);
        if (result == null) {
            Object lock = cacheLocks.computeIfAbsent(cacheKey, ignored -> new Object());
            synchronized (lock) {
                result = readChildrenCache(cacheKey);
                if (result == null) {
                    result = queryChildren(parent, scopeOrgId);
                    writeChildrenCache(childrenCacheKey(parent, scopeOrgId), result);
                }
            }
        }
        long costMillis = (System.nanoTime() - start) / 1_000_000;
        log.info("Organization children queried. parentOrgId={}, scopeOrgId={}, count={}, cost={}ms",
                parent == null ? ROOT_KEY : parent,
                scopeOrgId == null ? ALL_SCOPE : scopeOrgId,
                result.size(), costMillis);
        return result;
    }

    @Override
    public List<OrganizationResolvedVO> resolve(OrganizationResolveDTO request) {
        List<String> orgIds = normalizeOrgIds(request == null ? null : request.getOrgIds());
        if (orgIds.size() > organizationProperties.getTree().getResolveLimit()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "orgIds数量不能超过" + organizationProperties.getTree().getResolveLimit());
        }
        return toResolvedVOs(filterByScope(organizationProvider.resolve(orgIds), currentScope()));
    }

    @Override
    public List<OrganizationResolvedVO> search(String keyword) {
        String normalized = trimToNull(keyword);
        if (normalized == null) {
            return List.of();
        }
        int limit = Math.max(1, organizationProperties.getTree().getSearchLimit());
        String scopeOrgId = currentScope();
        return toResolvedVOs(filterByScope(
                organizationProvider.search(normalized, limit, scopeOrgId), scopeOrgId));
    }

    @Override
    public void invalidateChildren(Collection<String> parentOrgIds) {
        if (parentOrgIds == null || parentOrgIds.isEmpty()) {
            incrementVersion(parentVersionKey(null));
            return;
        }
        parentOrgIds.stream()
                .map(this::trimToNull)
                .distinct()
                .forEach(parent -> incrementVersion(parentVersionKey(parent)));
    }

    @Override
    public void invalidateAllChildren() {
        incrementVersion(GLOBAL_VERSION_KEY);
    }

    private List<OrganizationLazyNodeVO> queryChildren(String parentOrgId, String scopeOrgId) {
        List<OrganizationNode> nodes;
        if (parentOrgId == null && scopeOrgId != null) {
            nodes = organizationProvider.resolve(List.of(scopeOrgId)).stream()
                    .map(OrganizationPath::node)
                    .filter(node -> !Integer.valueOf(0).equals(node.getState()))
                    .toList();
        } else if (scopeOrgId != null && !organizationProvider.isWithinScope(parentOrgId, scopeOrgId)) {
            nodes = List.of();
        } else {
            nodes = organizationProvider.children(parentOrgId);
        }
        if (scopeOrgId != null) {
            nodes = nodes.stream()
                    .filter(node -> organizationProvider.isWithinScope(node.getOrgId(), scopeOrgId))
                    .toList();
        }
        Set<String> parentIds = organizationProvider.parentOrgIdsWithChildren(
                nodes.stream().map(OrganizationNode::getOrgId).toList());
        return nodes.stream().map(node -> toLazyVO(node, parentIds.contains(node.getOrgId()))).toList();
    }

    private List<OrganizationPath> filterByScope(List<OrganizationPath> paths, String scopeOrgId) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        return paths.stream()
                .filter(path -> scopeOrgId == null
                        || organizationProvider.isWithinScope(path.node().getOrgId(), scopeOrgId))
                .map(path -> scopeOrgId == null ? path : new OrganizationPath(path.node(), path.ancestors().stream()
                        .filter(node -> organizationProvider.isWithinScope(node.getOrgId(), scopeOrgId))
                        .toList()))
                .toList();
    }

    private List<OrganizationResolvedVO> toResolvedVOs(List<OrganizationPath> paths) {
        Set<String> ancestorIds = paths.stream()
                .flatMap(path -> path.ancestors().stream())
                .map(OrganizationNode::getOrgId)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> parentsWithChildren = organizationProvider.parentOrgIdsWithChildren(ancestorIds);
        return paths.stream().map(path -> {
            OrganizationNode node = path.node();
            OrganizationResolvedVO vo = new OrganizationResolvedVO();
            vo.setOrgId(node.getOrgId());
            vo.setOrgName(node.getOrgName());
            vo.setOrgCode(node.getOrgCode());
            vo.setParentOrgId(node.getParentOrgId());
            vo.setNameFullPath(node.getNameFullPath());
            vo.setOrgLevel(node.getOrgLevel());
            vo.setState(node.getState());
            vo.setAncestors(path.ancestors().stream()
                    .map(ancestor -> toLazyVO(ancestor, parentsWithChildren.contains(ancestor.getOrgId())))
                    .toList());
            return vo;
        }).toList();
    }

    private OrganizationLazyNodeVO toLazyVO(OrganizationNode node, boolean hasChildren) {
        OrganizationLazyNodeVO vo = new OrganizationLazyNodeVO();
        vo.setOrgId(node.getOrgId());
        vo.setOrgName(node.getOrgName());
        vo.setOrgCode(node.getOrgCode());
        vo.setParentOrgId(node.getParentOrgId());
        vo.setNameFullPath(node.getNameFullPath());
        vo.setOrgLevel(node.getOrgLevel());
        vo.setState(node.getState());
        vo.setHasChildren(hasChildren);
        return vo;
    }

    private OrganizationNodeVO toTreeVO(OrganizationNode node) {
        OrganizationNodeVO vo = new OrganizationNodeVO();
        vo.setOrgId(node.getOrgId());
        vo.setOrgName(node.getOrgName());
        vo.setOrgCode(node.getOrgCode());
        vo.setParentOrgId(node.getParentOrgId());
        vo.setNameFullPath(node.getNameFullPath());
        vo.setOrgLevel(node.getOrgLevel());
        vo.setState(node.getState());
        vo.setChildren(node.getChildren() == null ? List.of() : node.getChildren().stream()
                .map(this::toTreeVO)
                .toList());
        return vo;
    }

    private List<String> normalizeOrgIds(List<String> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        orgIds.stream().map(this::trimToNull).filter(java.util.Objects::nonNull).forEach(normalized::add);
        return List.copyOf(normalized);
    }

    private List<OrganizationLazyNodeVO> readChildrenCache(String key) {
        if (!childrenCacheEnabled()) {
            return null;
        }
        Object cached = redisUtil.get(key);
        if (!(cached instanceof String json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CHILDREN_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Organization children cache parse failed. key={}, cause={}", key, ex.getMessage());
            redisUtil.delete(key);
            return null;
        }
    }

    private void writeChildrenCache(String key, List<OrganizationLazyNodeVO> nodes) {
        if (!childrenCacheEnabled()) {
            return;
        }
        try {
            redisUtil.set(key, objectMapper.writeValueAsString(nodes),
                    organizationProperties.getTree().getChildrenCacheTtl());
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INFRASTRUCTURE_ERROR, "组织子节点缓存序列化失败");
        }
    }

    private String childrenCacheKey(String parentOrgId, String scopeOrgId) {
        String parent = parentOrgId == null ? ROOT_KEY : parentOrgId;
        String scope = scopeOrgId == null ? ALL_SCOPE : scopeOrgId;
        return CACHE_PREFIX + "v" + version(GLOBAL_VERSION_KEY) + ":p" + version(parentVersionKey(parentOrgId))
                + ":" + scope + ":" + parent;
    }

    private boolean childrenCacheEnabled() {
        return organizationProperties.getTree().getChildrenCacheTtl() != null
                && !organizationProperties.getTree().getChildrenCacheTtl().isZero()
                && !organizationProperties.getTree().getChildrenCacheTtl().isNegative();
    }

    private String parentVersionKey(String parentOrgId) {
        return CACHE_PREFIX + "version:" + (parentOrgId == null ? ROOT_KEY : parentOrgId);
    }

    private long version(String key) {
        Object value = redisUtil.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private void incrementVersion(String key) {
        redisUtil.increment(key);
    }

    private String currentScope() {
        return trimToNull(CurrentUserContext.getOrgId());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
