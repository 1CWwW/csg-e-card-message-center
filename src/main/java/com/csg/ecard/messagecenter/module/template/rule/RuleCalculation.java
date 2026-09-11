package com.csg.ecard.messagecenter.module.template.rule;

import lombok.Getter;
import lombok.Setter;

/** 条件或正文绑定上的单步数值运算。 */
@Getter
@Setter
public class RuleCalculation {
    private String id;
    private String operator;
    private String currentSide;
    private RuleCalculationOperand right;
}
