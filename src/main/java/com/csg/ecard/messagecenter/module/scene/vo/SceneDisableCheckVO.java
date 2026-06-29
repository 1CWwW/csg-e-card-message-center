package com.csg.ecard.messagecenter.module.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 场景停用检查结果。
 */
@Getter
@AllArgsConstructor
@Schema(description = "场景停用检查结果")
public class SceneDisableCheckVO {

    @Schema(description = "当前场景下启用模板数量")
    private Long enabledTemplateCount;
}
