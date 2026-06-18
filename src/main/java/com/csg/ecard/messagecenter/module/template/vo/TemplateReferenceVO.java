package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 参考模板列表项。
 */
@Getter
@Setter
public class TemplateReferenceVO {

    @JsonLongId
    @Schema(type = "string")
    private Long templateId;

    private String templateName;

    @JsonLongId
    @Schema(type = "string")
    private Long sceneId;

    private String sceneName;

    private String channelType;

    private String channelTypeDesc;

    private Integer status;

    private String statusDesc;

    private Boolean hasContent;

    private LocalDateTime updatedAt;
}
