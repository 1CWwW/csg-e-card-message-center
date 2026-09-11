package com.csg.ecard.messagecenter.module.template.rule;

import lombok.Getter;
import lombok.Setter;

/** 数值运算右操作数，沿用规则比较右值的 literal/reference 结构。 */
@Getter
@Setter
public class RuleCalculationOperand {
    private String source;
    private String value;
    private RuleReference reference;
}
