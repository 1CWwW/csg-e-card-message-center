package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 逻辑删除标记。
 */
public enum DeleteFlag {

    NORMAL(0, "正常"),
    DELETED(1, "已删除");

    private final Integer code;
    private final String desc;

    DeleteFlag(Integer code, String desc) {
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
     * 根据 code 获取删除标记。
     *
     * @param code 删除标记编码
     * @return 删除标记
     */
    public static DeleteFlag fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知删除标记: " + code));
    }
}
