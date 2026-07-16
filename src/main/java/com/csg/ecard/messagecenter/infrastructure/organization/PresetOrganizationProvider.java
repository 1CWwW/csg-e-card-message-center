package com.csg.ecard.messagecenter.infrastructure.organization;

import com.csg.ecard.messagecenter.infrastructure.employee.LocalUnitPathProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本地预设组织树，用于本地无法访问内网组织接口时开发联调。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.organization", name = "mode", havingValue = "preset", matchIfMissing = true)
public class PresetOrganizationProvider extends AbstractOrganizationProvider {

    private final LocalUnitPathProperties localUnitPathProperties;
    private final OrganizationProperties organizationProperties;

    @Override
    public List<OrganizationNode> tree() {
        return buildTree(nodes());
    }

    @Override
    public List<OrganizationNode> children(String parentOrgId) {
        return index().children(parentOrgId);
    }

    @Override
    public Set<String> parentOrgIdsWithChildren(Collection<String> orgIds) {
        return index().parentOrgIdsWithChildren(orgIds);
    }

    @Override
    public List<OrganizationPath> resolve(Collection<String> orgIds) {
        return index().resolve(orgIds);
    }

    @Override
    public List<OrganizationPath> search(String keyword, int limit, String scopeOrgId) {
        return index().search(keyword, limit, scopeOrgId);
    }

    @Override
    public boolean isWithinScope(String orgId, String scopeOrgId) {
        return index().isWithinScope(orgId, scopeOrgId);
    }

    @Override
    public List<String> resolveUnitPath(String orgId) {
        return resolveUnitPath(orgId, nodes(), localUnitPathProperties.getMaxDepth());
    }

    private List<OrganizationNode> nodes() {
        Map<String, OrganizationNode> nodeById = new LinkedHashMap<>();
        localUnitPathProperties.getParentByUnit().forEach((unitId, parentUnitId) -> {
            OrganizationNode node = nodeById.computeIfAbsent(unitId, this::newNode);
            node.setParentOrgId(parentUnitId);
            if (trimToNull(parentUnitId) != null) {
                nodeById.computeIfAbsent(parentUnitId, this::newNode);
            }
        });
        return List.copyOf(nodeById.values());
    }

    private OrganizationIndex index() {
        return new OrganizationIndex(nodes());
    }

    private OrganizationNode newNode(String unitId) {
        OrganizationNode node = new OrganizationNode();
        node.setOrgId(unitId);
        node.setOrgName(organizationProperties.getPreset().getNameByUnit().getOrDefault(unitId, unitId));
        node.setState(1);
        return node;
    }
}
