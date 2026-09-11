package com.csg.ecard.messagecenter.module.template.blockly;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Blockly 数学运算统一精度规则。
 */
public final class BlocklyMathRules {

    /** 除法保留最多 16 位小数并使用 HALF_UP 舍入，输出时移除无意义的末尾零。 */
    static final int DIVISION_SCALE = 16;
    static final RoundingMode DIVISION_ROUNDING = RoundingMode.HALF_UP;

    private BlocklyMathRules() {
    }

    /** 按模板统一精度执行除法。 */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, DIVISION_SCALE, DIVISION_ROUNDING).stripTrailingZeros();
    }
}
