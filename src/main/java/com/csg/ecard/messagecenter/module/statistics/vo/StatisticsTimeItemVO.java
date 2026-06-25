package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 按时间统计明细项。
 */
@Getter
@Setter
@Schema(description = "按时间统计明细项")
public class StatisticsTimeItemVO extends StatisticsSummaryVO {

    @Schema(description = "时间段标识")
    private String period;

    @Schema(description = "时间段展示名称")
    private String periodLabel;
}
