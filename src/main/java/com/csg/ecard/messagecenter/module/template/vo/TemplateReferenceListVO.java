package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 参考模板分页列表项。
 */
@Getter
@Setter
public class TemplateReferenceListVO {

    @JsonLongId
    @Schema(type = "string")
    private Long id;

    private String templateName;

    @JsonLongId
    @Schema(type = "string")
    private Long sceneId;

    private String sceneName;

    private String channelType;

    private String channelTypeDesc;

    private Boolean hasContent;

    @Schema(type = "integer", format = "int64")
    private Long unitCount;

    @Schema(type = "integer", format = "int32")
    private Integer status;

    private LocalDateTime updatedAt;
}
