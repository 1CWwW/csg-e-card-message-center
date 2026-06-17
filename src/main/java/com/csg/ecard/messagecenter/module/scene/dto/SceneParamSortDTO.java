package com.csg.ecard.messagecenter.module.scene.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 场景参数排序请求。
 */
@Getter
@Setter
@Schema(description = "场景参数排序请求")
public class SceneParamSortDTO {

    @Valid
    @NotEmpty(message = "排序列表不能为空")
    @Schema(description = "排序列表")
    private List<SceneParamSortItemDTO> items;
}
