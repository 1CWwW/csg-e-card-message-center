package com.csg.ecard.messagecenter.module.scene.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景参数排序项。
 */
@Getter
@Setter
@Schema(description = "场景参数排序项")
public class SceneParamSortItemDTO {

    @NotNull(message = "paramId不能为空")
    @Schema(description = "参数ID", example = "1")
    private Long paramId;

    @NotNull(message = "sortOrder不能为空")
    @Min(value = 1, message = "sortOrder必须为正整数")
    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;
}
