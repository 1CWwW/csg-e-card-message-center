package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 按渠道类型统计明细项。
 */
@Getter
@Setter
@Schema(description = "按渠道类型统计明细项")
public class StatisticsChannelItemVO extends StatisticsSummaryVO {

    @Schema(description = "渠道类型")
    private String channelType;

    @Schema(description = "渠道类型描述")
    private String channelTypeDesc;

    @Schema(description = "占比，0到100之间的数字，保留两位小数")
    private BigDecimal percentage = BigDecimal.ZERO;
}
