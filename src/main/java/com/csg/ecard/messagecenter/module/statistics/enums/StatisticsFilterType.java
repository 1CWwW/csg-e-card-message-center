package com.csg.ecard.messagecenter.module.statistics.enums;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;

import java.util.Arrays;

/**
 * 消息统计动态筛选项类型。
 */
public enum StatisticsFilterType {

    /** 场景。 */
    SCENE("scene"),

    /** 模板。 */
    TEMPLATE("template");

    private final String code;

    StatisticsFilterType(String code) {
        this.code = code;
    }

    /**
     * 根据接口编码获取筛选项类型。
     *
     * @param code 接口编码
     * @return 筛选项类型
     */
    public static StatisticsFilterType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        ErrorCode.PARAM_ERROR, "type仅支持scene或template"));
    }
}
