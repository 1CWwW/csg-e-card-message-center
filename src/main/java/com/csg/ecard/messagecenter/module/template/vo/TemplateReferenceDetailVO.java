package com.csg.ecard.messagecenter.module.template.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 参考模板内容详情。
 */
@Getter
@Setter
public class TemplateReferenceDetailVO extends TemplateReferenceVO {

    private JsonNode blocklyJson;
}
