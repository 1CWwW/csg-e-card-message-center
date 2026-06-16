package com.csg.ecard.messagecenter.module.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 场景编码可用性结果。
 */
@Getter
@AllArgsConstructor
@Schema(description = "场景编码可用性结果")
public class SceneCodeCheckVO {

    @Schema(description = "场景编码")
    private String sceneCode;

    @Schema(description = "是否可用")
    private Boolean available;
}
