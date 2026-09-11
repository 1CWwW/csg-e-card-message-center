package com.csg.ecard.messagecenter.module.template.rule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 条件模板正文绑定定义。 */
@Getter
@Setter
public class RuleBinding {
    private String token;
    private String source;
    private String key;
    private String type;
    private String format;
    private Integer decimals;
    private String fallback;
    private String datePattern;
    private List<RuleCalculation> calculations;
}
