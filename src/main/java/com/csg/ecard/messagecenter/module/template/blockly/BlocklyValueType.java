package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ParamType;

/**
 * Blockly 表达式静态值类型。
 */
public enum BlocklyValueType {

    STRING,
    NUMBER,
    BOOLEAN,
    TIME,
    STRING_ARRAY,
    NUMBER_ARRAY,
    STATEMENT;

    /**
     * 将场景参数类型映射为 Blockly 表达式类型。
     *
     * @param paramType 场景参数类型
     * @return Blockly 值类型
     */
    public static BlocklyValueType fromParamType(String paramType) {
        return switch (ParamType.fromCode(paramType)) {
            case STRING -> STRING;
            case NUMBER -> NUMBER;
            case TIME -> TIME;
            case STRING_ARRAY -> STRING_ARRAY;
            case NUMBER_ARRAY -> NUMBER_ARRAY;
        };
    }

    public boolean canRenderAsText() {
        return this == STRING || this == NUMBER || this == TIME;
    }
}
