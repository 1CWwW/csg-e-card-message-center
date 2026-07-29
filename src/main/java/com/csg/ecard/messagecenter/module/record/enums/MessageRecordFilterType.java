package com.csg.ecard.messagecenter.module.record.enums;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;

import java.util.Arrays;

/**
 * 消息记录动态筛选项类型。
 */
public enum MessageRecordFilterType {

    /** 场景。 */
    SCENE("scene"),

    /** 渠道。 */
    CHANNEL("channel"),

    /** 模板。 */
    TEMPLATE("template");

    private final String code;

    MessageRecordFilterType(String code) {
        this.code = code;
    }

    /**
     * 根据接口编码获取筛选项类型。
     *
     * @param code 接口编码
     * @return 筛选项类型
     */
    public static MessageRecordFilterType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        ErrorCode.PARAM_ERROR, "type仅支持scene、channel或template"));
    }
}
