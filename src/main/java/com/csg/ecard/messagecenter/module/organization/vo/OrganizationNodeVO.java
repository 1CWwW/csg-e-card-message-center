package com.csg.ecard.messagecenter.module.organization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织树节点响应。
 */
@Getter
@Setter
@Schema(description = "组织树节点")
public class OrganizationNodeVO {

    @Schema(description = "组织ID")
    private String orgId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织编码")
    private String orgCode;

    @Schema(description = "父组织ID")
    private String parentOrgId;

    @Schema(description = "组织名称全路径")
    private String nameFullPath;

    @Schema(description = "组织层级")
    private Integer orgLevel;

    @Schema(description = "组织状态，外部组织接口为1启用、2注销")
    private Integer state;

    @Schema(description = "子组织")
    private List<OrganizationNodeVO> children = new ArrayList<>();
}
