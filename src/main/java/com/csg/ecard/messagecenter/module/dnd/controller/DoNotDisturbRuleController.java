package com.csg.ecard.messagecenter.module.dnd.controller;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbBatchCreateDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbPageQueryDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbUpdateDTO;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbRuleService;
import com.csg.ecard.messagecenter.module.dnd.vo.DoNotDisturbRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息免打扰规则管理接口。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "消息免打扰管理")
@RequestMapping("/api/msg/do-not-disturb")
public class DoNotDisturbRuleController {

    private final DoNotDisturbRuleService ruleService;

    @GetMapping("/list")
    @Operation(summary = "免打扰规则分页查询")
    public CommonResult<PageResult<DoNotDisturbRuleVO>> list(
            @ParameterObject @Valid DoNotDisturbPageQueryDTO query) {
        return CommonResult.success(ruleService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "免打扰规则详情查询")
    public CommonResult<DoNotDisturbRuleVO> detail(@PathVariable Long id) {
        return CommonResult.success(ruleService.detail(id));
    }

    @PostMapping("/batch")
    @Operation(summary = "批量新增免打扰规则",
            description = "多选单位或用户时，每个作用对象分别创建一条规则；GLOBAL只创建一条全局规则")
    public CommonResult<List<DoNotDisturbRuleVO>> createBatch(
            @RequestBody @Valid DoNotDisturbBatchCreateDTO request) {
        return CommonResult.success(ruleService.createBatch(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑免打扰规则")
    public CommonResult<DoNotDisturbRuleVO> update(@PathVariable Long id,
                                                   @RequestBody @Valid DoNotDisturbUpdateDTO request) {
        return CommonResult.success(ruleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除免打扰规则")
    public CommonResult<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return CommonResult.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "免打扰规则启停切换")
    public CommonResult<DoNotDisturbRuleVO> toggle(@PathVariable Long id) {
        return CommonResult.success(ruleService.toggle(id));
    }
}
