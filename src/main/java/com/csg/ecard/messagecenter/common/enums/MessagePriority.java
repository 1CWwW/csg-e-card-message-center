package com.csg.ecard.messagecenter.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.Arrays;

/**
 * 消息业务优先级，与渠道匹配使用的整数优先级相互独立。
 */
public enum MessagePriority {

    HIGH("HIGH", "高", 9),
    NORMAL("NORMAL", "普通", 5),
    LOW("LOW", "低", 1);

    @EnumValue
    private final String code;
    private final String desc;
    private final int mqPriority;

    MessagePriority(String code, String desc, int mqPriority) {
        this.code = code;
        this.desc = desc;
        this.mqPriority = mqPriority;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public int getMqPriority() {
        return mqPriority;
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
