package com.csg.ecard.messagecenter.module.statistics.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 按模板统计明细项。
 */
@Getter
@Setter
@Schema(description = "按模板统计明细项")
public class StatisticsTemplateItemVO {

    @Schema(description = "模板ID，对外按字符串返回")
    @JsonLongId
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "场景ID，对外按字符串返回")
    @JsonLongId
    private Long sceneId;

    @Schema(description = "场景编码")
    private String sceneCode;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "渠道类型")
    private String channelType;

    @Schema(description = "渠道类型描述")
    private String channelTypeDesc;

    @Schema(description = "使用次数")
    private Long usageCount = 0L;

    @Schema(description = "成功次数")
    private Long successCount = 0L;

    @Schema(description = "失败次数")
    private Long failedCount = 0L;

    @Schema(description = "成功率，0到100之间的数字，保留两位小数")
    private BigDecimal successRate = BigDecimal.ZERO;

    @Schema(description = "占比，0到100之间的数字，保留两位小数")
    private BigDecimal percentage = BigDecimal.ZERO;
}
