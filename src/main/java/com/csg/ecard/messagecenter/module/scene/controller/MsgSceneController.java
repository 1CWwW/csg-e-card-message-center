package com.csg.ecard.messagecenter.module.scene.controller;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.csg.ecard.messagecenter.module.scene.dto.SceneCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.ScenePageQueryDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.service.MsgSceneService;
import com.csg.ecard.messagecenter.module.scene.vo.MsgSceneVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneCodeCheckVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

/**
 * 场景管理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "场景管理")
@RequestMapping("/api/msg/scene")
public class MsgSceneController {

    private final MsgSceneService msgSceneService;

    /**
     * 分页查询场景。
     *
     * @param query 查询条件
     * @return 场景分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "场景分页查询")
    public ApiResult<PageResult<MsgSceneVO>> list(@ParameterObject @Valid ScenePageQueryDTO query) {
        return ApiResult.success(msgSceneService.page(query));
    }

    /**
     * 查询场景详情。
     *
     * @param id 场景ID
     * @return 场景详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "场景详情查询")
    public ApiResult<MsgSceneVO> detail(@PathVariable Long id) {
        return ApiResult.success(msgSceneService.detail(id));
    }

    /**
     * 检查场景编码是否可用。
     *
     * @param sceneCode 场景编码
     * @return 编码可用性
     */
    @GetMapping("/check-code")
    @Operation(summary = "场景编码可用性检查")
    public ApiResult<SceneCodeCheckVO> checkCode(@RequestParam @NotBlank(message = "场景编码不能为空") String sceneCode) {
        return ApiResult.success(msgSceneService.checkCode(sceneCode));
    }

    /**
     * 新增场景。
     *
     * @param request 新增请求
     * @return 新增后的场景
     */
    @PostMapping
    @Operation(summary = "新增场景")
    public ApiResult<MsgSceneVO> create(@RequestBody @Valid SceneCreateDTO request) {
        return ApiResult.success(msgSceneService.create(request));
    }

    /**
     * 编辑场景。
     *
     * @param id 场景ID
     * @param request 编辑请求
     * @return 编辑后的场景
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑场景")
    public ApiResult<MsgSceneVO> update(@PathVariable Long id, @RequestBody @Valid SceneUpdateDTO request) {
        return ApiResult.success(msgSceneService.update(id, request));
    }

    /**
     * 删除场景。
     *
     * @param id 场景ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除场景")
    public ApiResult<Void> delete(@PathVariable Long id) {
        msgSceneService.delete(id);
        return ApiResult.success();
    }

    /**
     * 启停切换场景。
     *
     * @param id 场景ID
     * @return 切换后的场景
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "启停切换场景")
    public ApiResult<MsgSceneVO> toggle(@PathVariable Long id) {
        return ApiResult.success(msgSceneService.toggle(id));
    }
}
