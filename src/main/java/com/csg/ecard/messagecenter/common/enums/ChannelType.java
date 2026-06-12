package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 消息渠道类型。
 */
public enum ChannelType {

    SMS("SMS", "短信"),
    EMAIL("EMAIL", "邮件"),
    ELINK("ELINK", "eLink 应用消息"),
    IN_APP("IN_APP", "站内信");

    private final String code;
    private final String desc;

    ChannelType(String code, String desc) {
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
     * 根据 code 获取渠道类型。
     *
     * @param code 渠道类型编码
     * @return 渠道类型
     */
    public static ChannelType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知渠道类型: " + code));
    }
}
