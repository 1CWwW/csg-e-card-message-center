package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 模板内容编辑状态。
 */
public enum TemplateContentStatus {

    EMPTY("EMPTY", "未编辑"),
    EDITED("EDITED", "已编辑");

    private final String code;
    private final String desc;

    TemplateContentStatus(String code, String desc) {
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
     * 根据 code 获取模板内容状态。
     *
     * @param code 模板内容状态编码
     * @return 模板内容状态
     */
    public static TemplateContentStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知模板内容状态: " + code));
    }
}
