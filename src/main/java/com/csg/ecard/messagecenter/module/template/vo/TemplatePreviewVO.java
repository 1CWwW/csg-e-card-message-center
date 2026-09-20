package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 消息模板正文预览结果。
 */
@Getter
@Setter
@Schema(description = "消息模板正文预览结果")
public class TemplatePreviewVO {

    @JsonLongId
    @Schema(type = "string")
    private Long templateId;

    private String channelType;

    private String renderedContent = "";

    private List<String> usedParams = Collections.emptyList();

    private List<String> warnings = Collections.emptyList();
    private String matchedId;
    private String matchedName;
    @Schema(description = "本次预览是否按默认分支配置跳过发送")
    private boolean skipSend;
    private String content = "";
    private List<com.csg.ecard.messagecenter.module.template.rule.RuleRenderResult.Trace> trace = Collections.emptyList();
    private List<String> errors = Collections.emptyList();
}
