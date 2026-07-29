package com.csg.ecard.messagecenter.module.template.controller;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCopyDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateContentSaveDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCreateDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePreviewDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateReferencePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.service.MsgTemplateService;
import com.csg.ecard.messagecenter.module.template.vo.TemplateCopyVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateContentVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateFilterOptionVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateOverviewVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplatePreviewVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateToolboxVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息模板基础管理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "消息模板管理")
@RequestMapping("/api/msg/template")
public class MsgTemplateController {

    private final MsgTemplateService msgTemplateService;

    /**
     * 查询模板概览。
     *
     * @return 模板概览
     */
    @GetMapping("/overview")
    @Operation(summary = "模板概览查询",
            description = "统计未删除模板的总数、内容编辑状态及启用数量")
    public CommonResult<TemplateOverviewVO> overview() {
        return CommonResult.success(msgTemplateService.overview());
    }

    @GetMapping("/list")
    @Operation(summary = "模板分页查询")
    public CommonResult<PageResult<TemplateListVO>> list(@ParameterObject @Valid TemplatePageQueryDTO query) {
        return CommonResult.success(msgTemplateService.page(query));
    }

    /**
     * 按需查询模板页面筛选项。
     *
     * @param type 筛选项类型
     * @return 筛选项列表
     */
    @GetMapping("/filter-options")
    @Operation(summary = "模板管理筛选项查询",
            description = "场景下拉框首次展开时调用；type当前仅支持scene")
    public CommonResult<List<TemplateFilterOptionVO>> filterOptions(
            @Parameter(description = "筛选项类型，当前仅支持scene", required = true)
            @RequestParam
            @Pattern(regexp = "scene", message = "type仅支持scene") String type) {
        return CommonResult.success(msgTemplateService.sceneFilterOptions());
    }

    @GetMapping("/reference-list")
    @Operation(summary = "参考模板分页查询")
    public CommonResult<PageResult<TemplateReferenceListVO>> referenceList(
            @ParameterObject @Valid TemplateReferencePageQueryDTO query) {
        return CommonResult.success(msgTemplateService.referencePage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模板详情查询")
    public CommonResult<TemplateDetailVO> detail(@PathVariable Long id) {
        return CommonResult.success(msgTemplateService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增模板")
    public CommonResult<TemplateDetailVO> create(@RequestBody @Valid TemplateCreateDTO request) {
        return CommonResult.success(msgTemplateService.create(request));
    }

    @PostMapping("/preview")
    @Operation(summary = "预览模板正文")
    public CommonResult<TemplatePreviewVO> preview(@RequestBody @Valid TemplatePreviewDTO request) {
        return CommonResult.success(msgTemplateService.preview(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑模板基础信息")
    public CommonResult<TemplateDetailVO> update(@PathVariable Long id,
                                               @RequestBody @Valid TemplateUpdateDTO request) {
        return CommonResult.success(msgTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public CommonResult<Void> delete(@PathVariable Long id) {
        msgTemplateService.delete(id);
        return CommonResult.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "模板启停切换")
    public CommonResult<TemplateDetailVO> toggle(@PathVariable Long id) {
        return CommonResult.success(msgTemplateService.toggle(id));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制模板")
    public CommonResult<TemplateCopyVO> copy(@PathVariable Long id,
                                          @RequestBody @Valid TemplateCopyDTO request) {
        return CommonResult.success(msgTemplateService.copy(id, request));
    }

    @PutMapping("/{id}/content")
    @Operation(summary = "保存模板Blockly内容")
    public CommonResult<TemplateContentVO> saveContent(@PathVariable Long id,
                                                     @RequestBody @Valid TemplateContentSaveDTO request) {
        return CommonResult.success(msgTemplateService.saveContent(id, request));
    }

    @GetMapping("/{id}/toolbox")
    @Operation(summary = "获取模板场景参数工具箱")
    public CommonResult<TemplateToolboxVO> toolbox(@PathVariable Long id) {
        return CommonResult.success(msgTemplateService.toolbox(id));
    }

    @GetMapping("/{id}/references")
    @Operation(summary = "查询参考模板")
    public CommonResult<List<TemplateReferenceVO>> references(@PathVariable Long id) {
        return CommonResult.success(msgTemplateService.references(id));
    }

    @GetMapping("/{id}/references/{referenceId}")
    @Operation(summary = "加载参考模板内容")
    public CommonResult<TemplateReferenceDetailVO> referenceDetail(@PathVariable Long id,
                                                                 @PathVariable Long referenceId) {
        return CommonResult.success(msgTemplateService.referenceDetail(id, referenceId));
    }
}
