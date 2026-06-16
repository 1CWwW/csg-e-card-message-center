package com.csg.ecard.messagecenter.module.scene.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景分页查询条件。
 */
@Getter
@Setter
@Schema(description = "场景分页查询条件")
public class ScenePageQueryDTO {

    @NotNull(message = "pageNum不能为空")
    @Min(value = 1, message = "pageNum必须从1开始")
    @Schema(description = "页码，从1开始", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    @Min(value = 1, message = "pageSize必须大于0")
    @Max(value = 100, message = "pageSize不能超过100")
    @Schema(description = "每页条数，默认20，最大100", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageSize = 20;

    @Schema(description = "场景编码")
    private String sceneCode;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "启用状态，1启用，0停用")
    private Integer status;
}
