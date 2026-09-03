package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 模板 Blockly 内容保存结果。
 */
@Getter
@Setter
@Schema(description = "模板Blockly内容保存结果")
public class TemplateContentVO {

    @JsonLongId
    @Schema(type = "string")
    private Long templateId;

    private JsonNode blocklyJson;

    private Boolean hasContent;

    private Boolean valid;

    @Schema(description = "内容校验错误列表；参数错误包含参数名称或参数标签")
    private List<String> errors = Collections.emptyList();

    private LocalDateTime updatedAt;
}
