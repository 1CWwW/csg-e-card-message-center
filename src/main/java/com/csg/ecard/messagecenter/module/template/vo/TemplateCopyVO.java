package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模板复制结果。
 */
@Getter
@AllArgsConstructor
@Schema(description = "模板复制结果")
public class TemplateCopyVO {

    @JsonLongId
    @Schema(type = "string")
    private Long newTemplateId;

    private String templateName;

    private Boolean hasContent;
}
