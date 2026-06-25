package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 按单位统计明细项。
 */
@Getter
@Setter
@Schema(description = "按单位统计明细项")
public class StatisticsUnitItemVO extends StatisticsSummaryVO {

    @Schema(description = "单位ID，来自msg_record.user_org_id")
    private String unitId;

    @Schema(description = "单位名称，当前未接入员工中心，固定返回-")
    private String unitName = "-";

    @Schema(description = "占比，0到100之间的数字，保留两位小数")
    private BigDecimal percentage = BigDecimal.ZERO;
}
