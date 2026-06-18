package com.csg.ecard.messagecenter.module.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 保存模板 Blockly 内容请求。
 */
@Getter
@Setter
@Schema(description = "保存模板Blockly内容请求")
public class TemplateContentSaveDTO {

    @NotNull(message = "schemaVersion不能为空")
    private Integer schemaVersion;

    @NotNull(message = "workspace不能为空")
    private JsonNode workspace;
}
