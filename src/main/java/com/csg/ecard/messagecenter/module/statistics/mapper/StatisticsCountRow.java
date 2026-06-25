package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 成功、失败和总量统计行。
 */
@Getter
@Setter
public class StatisticsCountRow {

    private Long totalCount;
    private Long successCount;
    private Long failedCount;
}
