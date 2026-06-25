package com.csg.ecard.messagecenter.module.statistics.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 按场景统计明细项。
 */
@Getter
@Setter
@Schema(description = "按场景统计明细项")
public class StatisticsSceneItemVO extends StatisticsSummaryVO {

    @Schema(description = "场景ID，对外按字符串返回")
    @JsonLongId
    private Long sceneId;

    @Schema(description = "场景编码")
    private String sceneCode;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "占比，0到100之间的数字，保留两位小数")
    private BigDecimal percentage = BigDecimal.ZERO;
}
