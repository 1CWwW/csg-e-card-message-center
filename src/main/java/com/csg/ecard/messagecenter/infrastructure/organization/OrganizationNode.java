package com.csg.ecard.messagecenter.infrastructure.organization;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织节点，承接外部组织接口和本地预设树的最小字段。
 */
@Getter
@Setter
public class OrganizationNode {

    private String orgId;

    private String orgName;

    private String orgCode;

    private String parentOrgId;

    private String nameFullPath;

    private Integer orgLevel;

    private Integer state;

    private Integer sortNo;

    private List<OrganizationNode> children = new ArrayList<>();
}
