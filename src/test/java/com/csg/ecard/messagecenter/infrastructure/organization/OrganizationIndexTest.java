package com.csg.ecard.messagecenter.infrastructure.organization;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationIndexTest {

    @Test
    void shouldQueryOneLevelInOriginalTreeOrderAndBatchDetectChildren() {
        OrganizationIndex index = new OrganizationIndex(List.of(
                node("100", "根", null, null, 1),
                node("102", "乙单位", "100", "B002", 2),
                node("101", "甲单位", "100", "A001", 1),
                node("1011", "叶子", "101", null, 1)));

        assertThat(index.children("100")).extracting(OrganizationNode::getOrgId)
                .containsExactly("101", "102");
        assertThat(index.parentOrgIdsWithChildren(List.of("101", "102"))).isEqualTo(Set.of("101"));
    }

    @Test
    void shouldSearchByCodeWithinScopeAndReturnRootFirstAncestors() {
        OrganizationNode root = node("100", "根", null, "ROOT", 1);
        OrganizationNode child = node("101", "广东电网", "100", "GD001", 1);
        OrganizationNode leaf = node("102", "广州供电局", "101", "GZ001", 1);
        OrganizationNode outside = node("200", "广西电网", null, "GX001", 1);
        OrganizationIndex index = new OrganizationIndex(List.of(root, child, leaf, outside));

        List<OrganizationPath> result = index.search("GZ001", 50, "101");

        assertThat(result).singleElement().satisfies(path -> {
            assertThat(path.node().getOrgId()).isEqualTo("102");
            assertThat(path.ancestors()).extracting(OrganizationNode::getOrgId)
                    .containsExactly("100", "101");
        });
        assertThat(index.search("GX", 50, "101")).isEmpty();
    }

    private OrganizationNode node(String orgId,
                                  String orgName,
                                  String parentOrgId,
                                  String orgCode,
                                  Integer sortNo) {
        OrganizationNode node = new OrganizationNode();
        node.setOrgId(orgId);
        node.setOrgName(orgName);
        node.setParentOrgId(parentOrgId);
        node.setOrgCode(orgCode);
        node.setSortNo(sortNo);
        node.setState(1);
        return node;
    }
}
