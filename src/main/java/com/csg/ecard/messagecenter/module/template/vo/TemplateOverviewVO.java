package com.csg.ecard.messagecenter.module.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 模板概览响应。
 */
@Getter
@Setter
@Schema(description = "模板概览")
public class TemplateOverviewVO {

    @Schema(description = "模板总数")
    private long total;

    @Schema(description = "已编辑模板数")
    private long editedCount;

    @Schema(description = "启用模板数")
    private long enabledCount;

    @Schema(description = "待编辑模板数")
    private long pendingCount;
}
