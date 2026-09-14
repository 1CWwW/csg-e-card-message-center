package com.csg.ecard.messagecenter.module.dnd.enums;

import java.util.Arrays;

/**
 * 免打扰规则作用范围。
 */
public enum DoNotDisturbScopeType {

    GLOBAL("全局"),
    UNIT("单位"),
    USER("用户");

    private final String desc;

    DoNotDisturbScopeType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据编码获取作用范围。
     *
     * @param code 作用范围编码
     * @return 作用范围
     */
    public static DoNotDisturbScopeType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知免打扰作用范围: " + code));
    }
}
