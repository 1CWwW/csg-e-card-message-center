package com.csg.ecard.messagecenter.module.template.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 模板概览聚合结果。
 */
@Getter
@Setter
public class TemplateOverviewRow {

    private Long total;
    private Long editedCount;
    private Long enabledCount;
    private Long pendingCount;
}
