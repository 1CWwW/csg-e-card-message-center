package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息统计总览聚合行。
 */
@Getter
@Setter
public class StatisticsOverviewRow extends StatisticsCountRow {

    private Long syncCount;
    private Long asyncCount;
    private Long channelTypeCount;
    private Long sceneCount;
    private Long templateCount;
    private Long unitCount;
}
