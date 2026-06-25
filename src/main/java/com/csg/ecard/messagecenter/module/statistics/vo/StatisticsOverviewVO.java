package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息统计总览。
 */
@Getter
@Setter
@Schema(description = "消息统计总览")
public class StatisticsOverviewVO extends StatisticsSummaryVO {

    @Schema(description = "同步调用完成发送数量")
    private Long syncCount = 0L;

    @Schema(description = "异步调用完成发送数量")
    private Long asyncCount = 0L;

    @Schema(description = "实际出现过的渠道类型数量")
    private Long channelTypeCount = 0L;

    @Schema(description = "实际出现过的场景数量")
    private Long sceneCount = 0L;

    @Schema(description = "实际使用过的模板数量")
    private Long templateCount = 0L;

    @Schema(description = "实际出现过的非空单位ID数量")
    private Long unitCount = 0L;
}
