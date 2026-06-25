package com.csg.ecard.messagecenter.module.statistics.enums;

import java.util.Arrays;

/**
 * 统计时间粒度。
 */
public enum StatisticsGranularity {

    DAY,
    WEEK,
    MONTH;

    /**
     * 解析统计时间粒度，空值默认按日统计。
     */
    public static StatisticsGranularity parse(String value) {
        if (value == null || value.isBlank()) {
            return DAY;
        }
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("统计时间粒度仅支持DAY、WEEK、MONTH"));
    }
}
