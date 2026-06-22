package com.csg.ecard.messagecenter.module.template.blockly;

import java.math.BigDecimal;

/**
 * Blockly 表达式运行时值。
 *
 * @param type  静态值类型
 * @param value 已校验的运行时值，非必填参数缺失时可为 null
 */
record BlocklyRenderValue(BlocklyValueType type, Object value) {

    String asText() {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }
}
