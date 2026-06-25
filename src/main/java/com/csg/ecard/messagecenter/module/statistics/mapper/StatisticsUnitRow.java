package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 按单位聚合统计行。
 */
@Getter
@Setter
public class StatisticsUnitRow extends StatisticsCountRow {

    private String unitId;
}
