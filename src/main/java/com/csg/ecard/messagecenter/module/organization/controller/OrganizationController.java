package com.csg.ecard.messagecenter.module.organization.controller;

import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.csg.ecard.messagecenter.module.organization.dto.OrganizationResolveDTO;
import com.csg.ecard.messagecenter.module.organization.service.OrganizationService;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationLazyNodeVO;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织架构查询接口。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "组织架构")
@RequestMapping("/api/msg/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 查询组织树。
     *
     * @return 组织树
     */
    @GetMapping("/tree")
    @Operation(summary = "查询组织树")
    public CommonResult<List<OrganizationNodeVO>> tree() {
        return CommonResult.success(organizationService.tree());
    }

    /**
     * 按父节点懒加载一层组织。
     *
     * @param parentOrgId 父组织ID，不传时查询当前可见范围根节点
     * @return 单层组织节点
     */
    @GetMapping("/tree/children")
    @Operation(summary = "按父节点查询直接子组织")
    public CommonResult<List<OrganizationLazyNodeVO>> children(
            @RequestParam(required = false) String parentOrgId) {
        return CommonResult.success(organizationService.children(parentOrgId));
    }

    /**
     * 批量解析组织，用于编辑回显。
     *
     * @param request 组织ID列表
     * @return 扁平组织节点
     */
    @PostMapping("/tree/resolve")
    @Operation(summary = "批量解析已选组织")
    public CommonResult<List<OrganizationLazyNodeVO>> resolve(@Valid @RequestBody OrganizationResolveDTO request) {
        return CommonResult.success(organizationService.resolve(request));
    }

    /**
     * 按组织名称或编码搜索全部可见组织。
     *
     * @param keyword 搜索关键词
     * @return 匹配的扁平组织节点
     */
    @GetMapping("/search")
    @Operation(summary = "搜索组织")
    public CommonResult<List<OrganizationLazyNodeVO>> search(@RequestParam(required = false) String keyword) {
        return CommonResult.success(organizationService.search(keyword));
    }
}
