package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 通用启停状态。
 */
public enum CommonStatus {

    ENABLE(1, "启用"),
    DISABLE(0, "停用");

    private final Integer code;
    private final String desc;

    CommonStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取通用状态。
     *
     * @param code 状态编码
     * @return 通用状态
     */
    public static CommonStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知通用状态: " + code));
    }
}
