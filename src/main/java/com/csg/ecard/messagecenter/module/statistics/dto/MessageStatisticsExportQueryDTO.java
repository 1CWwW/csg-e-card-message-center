package com.csg.ecard.messagecenter.module.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息统计导出查询条件。
 */
@Getter
@Setter
@Schema(description = "消息统计导出查询条件")
public class MessageStatisticsExportQueryDTO extends MessageStatisticsTimeQueryDTO {

    @NotBlank(message = "导出维度不能为空")
    @Schema(description = "导出维度：TIME、CHANNEL、SCENE、UNIT、TEMPLATE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimension;

    @Schema(description = "导出范围：CURRENT当前筛选条件、ALL忽略维度筛选条件，默认CURRENT")
    private String scope;
}
