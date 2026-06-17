package com.csg.ecard.messagecenter.module.scene.controller;

import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamSortDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.service.SceneParamService;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 场景参数管理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "场景参数管理")
@RequestMapping("/api/msg/scene/{id}/params")
public class SceneParamController {

    private final SceneParamService sceneParamService;

    /**
     * 查询场景参数列表。
     *
     * @param id 场景ID
     * @return 参数列表
     */
    @GetMapping
    @Operation(summary = "参数列表查询")
    public ApiResult<List<SceneParamVO>> list(@PathVariable Long id) {
        return ApiResult.success(sceneParamService.list(id));
    }

    /**
     * 新增场景参数。
     *
     * @param id 场景ID
     * @param request 新增请求
     * @return 新增后的参数
     */
    @PostMapping
    @Operation(summary = "新增参数")
    public ApiResult<SceneParamVO> create(@PathVariable Long id, @RequestBody @Valid SceneParamCreateDTO request) {
        return ApiResult.success(sceneParamService.create(id, request));
    }

    /**
     * 编辑场景参数。
     *
     * @param id 场景ID
     * @param paramId 参数ID
     * @param request 编辑请求
     * @return 编辑后的参数
     */
    @PutMapping("/{paramId}")
    @Operation(summary = "编辑参数")
    public ApiResult<SceneParamVO> update(@PathVariable Long id,
                                          @PathVariable Long paramId,
                                          @RequestBody @Valid SceneParamUpdateDTO request) {
        return ApiResult.success(sceneParamService.update(id, paramId, request));
    }

    /**
     * 删除场景参数。
     *
     * @param id 场景ID
     * @param paramId 参数ID
     * @return 空响应
     */
    @DeleteMapping("/{paramId}")
    @Operation(summary = "删除参数")
    public ApiResult<Void> delete(@PathVariable Long id, @PathVariable Long paramId) {
        sceneParamService.delete(id, paramId);
        return ApiResult.success();
    }

    /**
     * 参数排序。
     *
     * @param id 场景ID
     * @param request 排序请求
     * @return 空响应
     */
    @PutMapping("/sort")
    @Operation(summary = "参数排序")
    public ApiResult<Void> sort(@PathVariable Long id, @RequestBody @Valid SceneParamSortDTO request) {
        sceneParamService.sort(id, request);
        return ApiResult.success();
    }

    /**
     * 查询参数引用。
     *
     * @param id 场景ID
     * @param paramId 参数ID
     * @return 引用结果
     */
    @GetMapping("/{paramId}/usage")
    @Operation(summary = "参数引用查询")
    public ApiResult<SceneParamUsageVO> usage(@PathVariable Long id, @PathVariable Long paramId) {
        return ApiResult.success(sceneParamService.usage(id, paramId));
    }
}
