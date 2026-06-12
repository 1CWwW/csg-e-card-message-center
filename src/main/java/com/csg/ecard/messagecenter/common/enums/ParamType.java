package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 模板参数类型。
 */
public enum ParamType {

    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    TIME("TIME", "时间"),
    STRING_ARRAY("STRING_ARRAY", "字符串数组"),
    NUMBER_ARRAY("NUMBER_ARRAY", "数字数组");

    private final String code;
    private final String desc;

    ParamType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取参数类型。
     *
     * @param code 参数类型编码
     * @return 参数类型
     */
    public static ParamType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知参数类型: " + code));
    }
}
