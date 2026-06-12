package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 消息发送状态。
 */
public enum MessageSendStatus {

    PENDING("PENDING", "待发送"),
    SUCCESS("SUCCESS", "发送成功"),
    FAILED("FAILED", "发送失败"),
    PARTIAL_SUCCESS("PARTIAL_SUCCESS", "部分成功");

    private final String code;
    private final String desc;

    MessageSendStatus(String code, String desc) {
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
     * 根据 code 获取发送状态。
     *
     * @param code 发送状态编码
     * @return 发送状态
     */
    public static MessageSendStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知消息发送状态: " + code));
    }
}
