package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 按场景聚合统计行。
 */
@Getter
@Setter
public class StatisticsSceneRow extends StatisticsCountRow {

    private Long sceneId;
    private String sceneCode;
    private String sceneName;
}
