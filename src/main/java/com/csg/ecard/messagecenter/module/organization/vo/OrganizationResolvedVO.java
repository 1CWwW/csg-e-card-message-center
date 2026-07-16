package com.csg.ecard.messagecenter.module.organization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 组织节点批量回显与搜索响应。
 */
@Getter
@Setter
@Schema(description = "组织节点及祖先路径")
public class OrganizationResolvedVO {

    private String orgId;

    private String orgName;

    private String orgCode;

    private String parentOrgId;

    private String nameFullPath;

    private Integer orgLevel;

    private Integer state;

    @Schema(description = "从可见范围根节点到当前节点的祖先信息，不包含当前节点")
    private List<OrganizationLazyNodeVO> ancestors;
}
