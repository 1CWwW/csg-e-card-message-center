package com.csg.ecard.messagecenter.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息统计筛选项。
 */
@Getter
@Setter
@Schema(description = "消息统计筛选项")
public class StatisticsFilterOptionVO {

    @Schema(description = "筛选值，Long类型ID按字符串返回")
    private String value;

    @Schema(description = "筛选项显示名称")
    private String label;
}
