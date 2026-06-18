package com.csg.ecard.messagecenter.module.template.controller;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.result.ApiResult;
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
import com.csg.ecard.messagecenter.module.template.vo.TemplateListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplatePreviewVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateToolboxVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @GetMapping("/list")
    @Operation(summary = "模板分页查询")
    public ApiResult<PageResult<TemplateListVO>> list(@ParameterObject @Valid TemplatePageQueryDTO query) {
        return ApiResult.success(msgTemplateService.page(query));
    }

    @GetMapping("/reference-list")
    @Operation(summary = "参考模板分页查询")
    public ApiResult<PageResult<TemplateReferenceListVO>> referenceList(
            @ParameterObject @Valid TemplateReferencePageQueryDTO query) {
        return ApiResult.success(msgTemplateService.referencePage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模板详情查询")
    public ApiResult<TemplateDetailVO> detail(@PathVariable Long id) {
        return ApiResult.success(msgTemplateService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增模板")
    public ApiResult<TemplateDetailVO> create(@RequestBody @Valid TemplateCreateDTO request) {
        return ApiResult.success(msgTemplateService.create(request));
    }

    @PostMapping("/preview")
    @Operation(summary = "预览模板正文")
    public ApiResult<TemplatePreviewVO> preview(@RequestBody @Valid TemplatePreviewDTO request) {
        return ApiResult.success(msgTemplateService.preview(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑模板基础信息")
    public ApiResult<TemplateDetailVO> update(@PathVariable Long id,
                                               @RequestBody @Valid TemplateUpdateDTO request) {
        return ApiResult.success(msgTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public ApiResult<Void> delete(@PathVariable Long id) {
        msgTemplateService.delete(id);
        return ApiResult.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "模板启停切换")
    public ApiResult<TemplateDetailVO> toggle(@PathVariable Long id) {
        return ApiResult.success(msgTemplateService.toggle(id));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制模板")
    public ApiResult<TemplateCopyVO> copy(@PathVariable Long id,
                                          @RequestBody @Valid TemplateCopyDTO request) {
        return ApiResult.success(msgTemplateService.copy(id, request));
    }

    @PutMapping("/{id}/content")
    @Operation(summary = "保存模板Blockly内容")
    public ApiResult<TemplateContentVO> saveContent(@PathVariable Long id,
                                                     @RequestBody @Valid TemplateContentSaveDTO request) {
        return ApiResult.success(msgTemplateService.saveContent(id, request));
    }

    @GetMapping("/{id}/toolbox")
    @Operation(summary = "获取模板场景参数工具箱")
    public ApiResult<TemplateToolboxVO> toolbox(@PathVariable Long id) {
        return ApiResult.success(msgTemplateService.toolbox(id));
    }

    @GetMapping("/{id}/references")
    @Operation(summary = "查询参考模板")
    public ApiResult<List<TemplateReferenceVO>> references(@PathVariable Long id) {
        return ApiResult.success(msgTemplateService.references(id));
    }

    @GetMapping("/{id}/references/{referenceId}")
    @Operation(summary = "加载参考模板内容")
    public ApiResult<TemplateReferenceDetailVO> referenceDetail(@PathVariable Long id,
                                                                 @PathVariable Long referenceId) {
        return ApiResult.success(msgTemplateService.referenceDetail(id, referenceId));
    }
}
