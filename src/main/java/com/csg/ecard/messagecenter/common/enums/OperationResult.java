package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 操作执行结果。
 */
public enum OperationResult {

    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    OperationResult(String code, String desc) {
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
     * 根据 code 获取操作结果。
     *
     * @param code 操作结果编码
     * @return 操作结果
     */
    public static OperationResult fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知操作结果: " + code));
    }
}
