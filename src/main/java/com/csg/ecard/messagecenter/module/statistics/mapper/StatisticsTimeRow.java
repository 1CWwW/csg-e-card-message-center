package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 按时间聚合统计行。
 */
@Getter
@Setter
public class StatisticsTimeRow extends StatisticsCountRow {

    private String period;
}
