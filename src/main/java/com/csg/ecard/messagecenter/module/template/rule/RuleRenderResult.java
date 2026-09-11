package com.csg.ecard.messagecenter.module.template.rule;

import java.util.List;

/** 条件模板预览结果；错误时不返回半成品正文。 */
public record RuleRenderResult(String matchedId, String matchedName, String content,
                               List<Trace> trace, List<String> errors) {
    /** 分支执行轨迹，原因只包含规则位置和判定，不包含业务值。 */
    public record Trace(String id, String name, String state, List<String> reasons) { }
}
