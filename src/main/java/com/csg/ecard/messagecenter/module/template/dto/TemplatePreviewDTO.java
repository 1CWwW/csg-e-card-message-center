package com.csg.ecard.messagecenter.module.template.dto;

import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息模板正文预览请求。
 */
@Getter
@Setter
@Schema(description = "消息模板正文预览请求")
public class TemplatePreviewDTO {

    @NotBlank(message = "templateId不能为空")
    @Schema(description = "模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateId;

    @Schema(description = "Blockly工作区草稿，非空时仅用于本次预览")
    private JsonNode workspace;

    @Schema(description = "Blockly结构版本，workspace非空时默认1")
    private Integer schemaVersion = BlocklyJsonValidator.SUPPORTED_SCHEMA_VERSION;

    @Schema(description = "以场景参数名为key的JSON示例值；BOOLEAN参数必须传true或false，不能传字符串",
            example = "{\"isPark\":true}")
    private Map<String, JsonNode> values = new LinkedHashMap<>();
    @Schema(description = "编辑模式：BLOCKLY或RULE_VERSIONS；历史请求默认BLOCKLY")
    private String editorType;

    @Schema(description = "条件模板完整规则文档，editorType=RULE_VERSIONS时必填")
    private JsonNode ruleTemplate;

}
