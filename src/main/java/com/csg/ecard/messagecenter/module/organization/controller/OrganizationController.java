package com.csg.ecard.messagecenter.module.organization.controller;

import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.csg.ecard.messagecenter.module.organization.service.OrganizationService;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ApiResult<List<OrganizationNodeVO>> tree() {
        return ApiResult.success(organizationService.tree());
    }
}
