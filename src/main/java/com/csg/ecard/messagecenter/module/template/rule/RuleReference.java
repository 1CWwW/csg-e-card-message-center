package com.csg.ecard.messagecenter.module.template.rule;

import lombok.Getter;
import lombok.Setter;

/** 条件模板参数或当前列表项字段引用。 */
@Getter
@Setter
public class RuleReference {
    private String source;
    private String key;
    private String type;
}
