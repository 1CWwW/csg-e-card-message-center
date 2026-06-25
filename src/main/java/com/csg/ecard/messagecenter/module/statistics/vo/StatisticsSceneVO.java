package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 按场景统计结果。
 */
@Getter
@Setter
@Schema(description = "按场景统计结果")
public class StatisticsSceneVO {

    @Schema(description = "汇总结果")
    private StatisticsSummaryVO summary = new StatisticsSummaryVO();

    @Schema(description = "场景统计明细")
    private List<StatisticsSceneItemVO> items = new ArrayList<>();
}
