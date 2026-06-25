package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 统计汇总结果。
 */
@Getter
@Setter
@Schema(description = "统计汇总结果")
public class StatisticsSummaryVO {

    @Schema(description = "完成发送总量")
    private Long totalCount = 0L;

    @Schema(description = "成功数量")
    private Long successCount = 0L;

    @Schema(description = "失败数量")
    private Long failedCount = 0L;

    @Schema(description = "成功率，0到100之间的数字，保留两位小数")
    private BigDecimal successRate = BigDecimal.ZERO;
}
