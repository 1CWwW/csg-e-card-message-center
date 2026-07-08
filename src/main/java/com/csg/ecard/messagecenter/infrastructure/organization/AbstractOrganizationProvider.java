package com.csg.ecard.messagecenter.infrastructure.organization;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组织树组装和路径解析公共逻辑。
 */
abstract class AbstractOrganizationProvider implements OrganizationProvider {

    protected List<OrganizationNode> buildTree(Collection<OrganizationNode> nodes) {
        Map<String, OrganizationNode> nodeById = new LinkedHashMap<>();
        for (OrganizationNode node : nodes) {
            if (!StringUtils.hasText(node.getOrgId())) {
                continue;
            }
            node.setChildren(new ArrayList<>());
            nodeById.put(node.getOrgId().trim(), node);
        }

        List<OrganizationNode> roots = new ArrayList<>();
        for (OrganizationNode node : nodeById.values()) {
            String parentOrgId = trimToNull(node.getParentOrgId());
            OrganizationNode parent = parentOrgId == null ? null : nodeById.get(parentOrgId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    protected List<String> resolveUnitPath(String orgId, Collection<OrganizationNode> nodes, int maxDepth) {
        String current = trimToNull(orgId);
        if (current == null) {
            return List.of();
        }

        Map<String, String> parentByOrgId = new LinkedHashMap<>();
        for (OrganizationNode node : nodes) {
            String nodeOrgId = trimToNull(node.getOrgId());
            if (nodeOrgId != null) {
                parentByOrgId.put(nodeOrgId, trimToNull(node.getParentOrgId()));
            }
        }

        List<String> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        int depthLimit = Math.max(maxDepth, 1);
        for (int depth = 0; current != null && depth < depthLimit; depth++) {
            if (!visited.add(current)) {
                break;
            }
            path.add(current);
            current = parentByOrgId.get(current);
        }
        return path;
    }

    protected String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void sortTree(List<OrganizationNode> nodes) {
        nodes.sort(Comparator
                .comparing((OrganizationNode node) -> node.getSortNo() == null ? Integer.MAX_VALUE : node.getSortNo())
                .thenComparing(node -> node.getOrgName() == null ? "" : node.getOrgName())
                .thenComparing(OrganizationNode::getOrgId));
        for (OrganizationNode node : nodes) {
            sortTree(node.getChildren());
        }
    }
}
