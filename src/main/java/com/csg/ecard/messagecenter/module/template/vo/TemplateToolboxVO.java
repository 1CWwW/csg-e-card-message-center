package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 模板场景参数工具箱数据。
 */
@Getter
@Setter
@Schema(description = "模板场景参数工具箱数据")
public class TemplateToolboxVO {

    @JsonLongId
    @Schema(type = "string")
    private Long templateId;

    @JsonLongId
    @Schema(type = "string")
    private Long sceneId;

    private List<TemplateToolboxParamVO> params = Collections.emptyList();
}
