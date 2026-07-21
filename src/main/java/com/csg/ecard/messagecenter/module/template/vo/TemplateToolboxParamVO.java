package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 模板工具箱场景参数。
 */
@Getter
@Setter
public class TemplateToolboxParamVO {

    @JsonLongId
    @Schema(type = "string")
    private Long paramId;

    private String paramName;

    private String paramLabel;

    @Schema(description = "参数类型",
            allowableValues = {"STRING", "NUMBER", "BOOLEAN", "TIME",
                    "STRING_ARRAY", "NUMBER_ARRAY", "OBJECT_ARRAY"})
    private String paramType;

    private String paramTypeDesc;

    private Integer isRequired;

    private Integer sortOrder;
}
