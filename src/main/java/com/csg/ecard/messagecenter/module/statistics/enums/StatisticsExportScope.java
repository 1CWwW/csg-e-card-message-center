package com.csg.ecard.messagecenter.module.statistics.enums;

import java.util.Arrays;

/**
 * 统计导出范围。
 */
public enum StatisticsExportScope {

    CURRENT,
    ALL;

    /**
     * 解析统计导出范围，空值默认当前筛选条件。
     */
    public static StatisticsExportScope parse(String value) {
        if (value == null || value.isBlank()) {
            return CURRENT;
        }
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("导出范围仅支持CURRENT、ALL"));
    }
}
