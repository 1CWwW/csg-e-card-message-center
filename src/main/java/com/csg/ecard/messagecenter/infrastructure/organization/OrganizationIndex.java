package com.csg.ecard.messagecenter.infrastructure.organization;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 全量组织快照的扁平索引，支持按父节点直接定位一层数据。
 */
final class OrganizationIndex {

    private static final Comparator<OrganizationNode> NODE_COMPARATOR = Comparator
            .comparing((OrganizationNode node) -> node.getSortNo() == null ? Integer.MAX_VALUE : node.getSortNo())
            .thenComparing(node -> node.getOrgName() == null ? "" : node.getOrgName())
            .thenComparing(OrganizationNode::getOrgId);

    private final Map<String, OrganizationNode> nodeById;
    private final Map<String, List<OrganizationNode>> childrenByParentId;
    private final List<OrganizationNode> searchableNodes;

    OrganizationIndex(Collection<OrganizationNode> source) {
        Map<String, OrganizationNode> nodes = new LinkedHashMap<>();
        if (source != null) {
            for (OrganizationNode item : source) {
                if (item == null || !StringUtils.hasText(item.getOrgId())) {
                    continue;
                }
                OrganizationNode node = copy(item);
                node.setOrgId(node.getOrgId().trim());
                node.setParentOrgId(trimToNull(node.getParentOrgId()));
                nodes.put(node.getOrgId(), node);
            }
        }
        this.nodeById = Map.copyOf(nodes);

        Map<String, List<OrganizationNode>> children = new HashMap<>();
        for (OrganizationNode node : nodes.values()) {
            String parentKey = rootIfMissingParent(node.getParentOrgId(), nodes);
            children.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(node);
        }
        children.values().forEach(items -> items.sort(NODE_COMPARATOR));
        Map<String, List<OrganizationNode>> immutableChildren = new HashMap<>();
        children.forEach((key, value) -> immutableChildren.put(key, List.copyOf(value)));
        this.childrenByParentId = Map.copyOf(immutableChildren);

        List<OrganizationNode> searchable = new ArrayList<>(nodes.values());
        searchable.sort(NODE_COMPARATOR);
        this.searchableNodes = List.copyOf(searchable);
    }

    List<OrganizationNode> all() {
        return nodeById.values().stream().map(OrganizationIndex::copy).toList();
    }

    List<OrganizationNode> children(String parentOrgId) {
        String key = trimToNull(parentOrgId);
        return childrenByParentId.getOrDefault(key == null ? "" : key, List.of()).stream()
                .filter(OrganizationIndex::visible)
                .map(OrganizationIndex::copy)
                .toList();
    }

    Set<String> parentOrgIdsWithChildren(Collection<String> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String orgId : orgIds) {
            String normalized = trimToNull(orgId);
            if (normalized != null && childrenByParentId.getOrDefault(normalized, List.of()).stream()
                    .anyMatch(OrganizationIndex::visible)) {
                result.add(normalized);
            }
        }
        return Set.copyOf(result);
    }

    List<OrganizationPath> resolve(Collection<String> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        List<OrganizationPath> result = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        for (String orgId : orgIds) {
            String normalized = trimToNull(orgId);
            if (normalized == null || !resolved.add(normalized)) {
                continue;
            }
            OrganizationNode node = nodeById.get(normalized);
            if (node != null) {
                result.add(new OrganizationPath(copy(node), ancestors(node)));
            }
        }
        return result;
    }

    List<OrganizationPath> search(String keyword, int limit, String scopeOrgId) {
        String normalized = trimToNull(keyword);
        if (normalized == null || limit <= 0) {
            return List.of();
        }
        String lowerKeyword = normalized.toLowerCase(Locale.ROOT);
        return searchableNodes.stream()
                .filter(OrganizationIndex::visible)
                .filter(node -> containsIgnoreCase(node.getOrgName(), lowerKeyword)
                        || containsIgnoreCase(node.getOrgCode(), lowerKeyword))
                .filter(node -> scopeOrgId == null || isWithinScope(node.getOrgId(), scopeOrgId))
                .limit(limit)
                .map(node -> new OrganizationPath(copy(node), ancestors(node)))
                .toList();
    }

    boolean isWithinScope(String orgId, String scopeOrgId) {
        String current = trimToNull(orgId);
        String scope = trimToNull(scopeOrgId);
        if (current == null || scope == null) {
            return scope == null;
        }
        Set<String> visited = new HashSet<>();
        for (int depth = 0; current != null && depth < 100 && visited.add(current); depth++) {
            if (scope.equals(current)) {
                return true;
            }
            OrganizationNode node = nodeById.get(current);
            current = node == null ? null : node.getParentOrgId();
        }
        return false;
    }

    private List<OrganizationNode> ancestors(OrganizationNode node) {
        List<OrganizationNode> reversed = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = node.getParentOrgId();
        for (int depth = 0; current != null && depth < 100 && visited.add(current); depth++) {
            OrganizationNode parent = nodeById.get(current);
            if (parent == null) {
                break;
            }
            reversed.add(copy(parent));
            current = parent.getParentOrgId();
        }
        java.util.Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private String rootIfMissingParent(String parentOrgId, Map<String, OrganizationNode> nodes) {
        return parentOrgId == null || !nodes.containsKey(parentOrgId) ? "" : parentOrgId;
    }

    private static boolean visible(OrganizationNode node) {
        return !Integer.valueOf(0).equals(node.getState());
    }

    private static boolean containsIgnoreCase(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static OrganizationNode copy(OrganizationNode source) {
        OrganizationNode target = new OrganizationNode();
        target.setOrgId(source.getOrgId());
        target.setOrgName(source.getOrgName());
        target.setOrgCode(source.getOrgCode());
        target.setParentOrgId(source.getParentOrgId());
        target.setNameFullPath(source.getNameFullPath());
        target.setOrgLevel(source.getOrgLevel());
        target.setState(source.getState());
        target.setSortNo(source.getSortNo());
        return target;
    }
}
