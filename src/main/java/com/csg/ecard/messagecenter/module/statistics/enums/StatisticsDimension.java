package com.csg.ecard.messagecenter.module.statistics.enums;

import java.util.Arrays;

/**
 * 统计导出维度。
 */
public enum StatisticsDimension {

    TIME,
    CHANNEL,
    SCENE,
    UNIT,
    TEMPLATE;

    /**
     * 解析统计导出维度。
     */
    public static StatisticsDimension parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("导出维度不能为空");
        }
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("导出维度仅支持TIME、CHANNEL、SCENE、UNIT、TEMPLATE"));
    }
}
