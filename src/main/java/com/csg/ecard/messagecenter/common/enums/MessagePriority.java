package com.csg.ecard.messagecenter.common.enums;

import java.util.Arrays;

/**
 * 消息优先级。
 */
public enum MessagePriority {

    HIGH("HIGH", "高优先级"),
    NORMAL("NORMAL", "普通优先级"),
    LOW("LOW", "低优先级");

    private final String code;
    private final String desc;

    MessagePriority(String code, String desc) {
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
     * 根据 code 获取消息优先级。
     *
     * @param code 优先级编码
     * @return 消息优先级
     */
    public static MessagePriority fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知消息优先级: " + code));
    }
}
