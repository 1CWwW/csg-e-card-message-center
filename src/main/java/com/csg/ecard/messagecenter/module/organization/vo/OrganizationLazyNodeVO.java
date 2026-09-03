package com.csg.ecard.messagecenter.module.organization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 懒加载组织节点响应，不递归包含子节点。
 */
@Getter
@Setter
@Schema(description = "懒加载组织节点")
public class OrganizationLazyNodeVO {

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

    @Schema(description = "组织状态：1启用")
    private Integer state;

    @Schema(description = "是否存在可加载的直接子节点")
    private boolean hasChildren;

    @Schema(description = "子组织；懒加载与扁平查询固定返回空数组")
    private List<OrganizationLazyNodeVO> children = new ArrayList<>();
}
