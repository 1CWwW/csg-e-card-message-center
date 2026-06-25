package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 按时间统计结果。
 */
@Getter
@Setter
@Schema(description = "按时间统计结果")
public class StatisticsTimeVO {

    @Schema(description = "汇总结果")
    private StatisticsSummaryVO summary = new StatisticsSummaryVO();

    @Schema(description = "时间粒度：DAY、WEEK、MONTH")
    private String granularity;

    @Schema(description = "时间统计明细")
    private List<StatisticsTimeItemVO> items = new ArrayList<>();
}
