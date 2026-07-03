package com.csg.ecard.messagecenter.module.record.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 消息记录概览。
 */
@Getter
@Setter
@Schema(description = "消息记录概览")
public class MessageRecordOverviewVO {

    @Schema(description = "今日创建记录数")
    private long todayTotal;

    @Schema(description = "今日成功记录数")
    private long todaySuccess;

    @Schema(description = "今日失败记录数")
    private long todayFailed;

    @Schema(description = "今日待处理记录数，包含PENDING和ACCEPTED")
    private long todayPending;

    @Schema(description = "今日成功率，百分比，保留1位小数")
    private BigDecimal successRate;

    @Schema(description = "昨日创建记录数")
    private long yesterdayTotal;

    @Schema(description = "今日总量较昨日环比，百分比，保留1位小数")
    private BigDecimal dayOverDayRate;
}
