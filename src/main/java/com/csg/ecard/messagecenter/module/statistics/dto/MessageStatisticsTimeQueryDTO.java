package com.csg.ecard.messagecenter.module.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 按时间统计查询条件。
 */
@Getter
@Setter
@Schema(description = "按时间统计查询条件")
public class MessageStatisticsTimeQueryDTO extends MessageStatisticsQueryDTO {

    @Schema(description = "时间粒度：DAY、WEEK、MONTH，默认DAY")
    private String granularity;
}
