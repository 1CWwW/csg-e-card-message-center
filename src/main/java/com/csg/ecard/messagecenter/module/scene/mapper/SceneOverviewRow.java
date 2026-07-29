package com.csg.ecard.messagecenter.module.scene.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 场景概览聚合结果。
 */
@Getter
@Setter
public class SceneOverviewRow {

    private Long total;
    private Long activeCount;
    private Long paramTotal;
    private Long templateTotal;
    private Long associatedSceneCount;
}
