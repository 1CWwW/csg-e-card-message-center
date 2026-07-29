package com.csg.ecard.messagecenter.module.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景概览响应。
 */
@Getter
@Setter
@Schema(description = "场景概览")
public class SceneOverviewVO {

    @Schema(description = "场景总数")
    private long total;

    @Schema(description = "启用场景数")
    private long activeCount;

    @Schema(description = "参数总数")
    private long paramTotal;

    @Schema(description = "关联模板总数")
    private long templateTotal;

    @Schema(description = "已关联模板的场景数")
    private long associatedSceneCount;
}
