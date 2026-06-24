package com.csg.ecard.messagecenter.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.Arrays;

/**
 * 消息原始调用方式，表示消息最初进入系统的入口。
 */
public enum MessageCallType {

    SYNC("SYNC", "同步调用"),
    ASYNC("ASYNC", "异步调用");

    @EnumValue
    private final String code;
    private final String description;

    MessageCallType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取消息原始调用方式。
     *
     * @param code 调用方式编码
     * @return 消息原始调用方式
     */
    public static MessageCallType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知消息原始调用方式: " + code));
    }
}
