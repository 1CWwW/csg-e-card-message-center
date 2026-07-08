package com.csg.ecard.messagecenter.infrastructure.organization;

import com.csg.ecard.messagecenter.infrastructure.employee.LocalUnitPathProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private OrganizationNode newNode(String unitId) {
        OrganizationNode node = new OrganizationNode();
        node.setOrgId(unitId);
        node.setOrgName(organizationProperties.getPreset().getNameByUnit().getOrDefault(unitId, unitId));
        node.setState(1);
        return node;
    }
}
