package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模板列表响应。
 */
@Getter
@Setter
@Schema(description = "模板列表响应")
public class TemplateListVO {

    @JsonLongId
    @Schema(type = "string")
    private Long id;

    private String templateName;

    @JsonLongId
    @Schema(type = "string")
    private Long sceneId;

    private String sceneCode;

    private String sceneName;

    private String channelType;

    private String channelTypeDesc;

    @Schema(type = "integer", format = "int64")
    private Long unitCount;

    private Boolean hasContent;

    private String contentStatusDesc;

    @Schema(type = "integer", format = "int32")
    private Integer status;

    private String statusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
