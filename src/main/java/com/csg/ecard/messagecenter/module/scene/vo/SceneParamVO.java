package com.csg.ecard.messagecenter.module.scene.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 场景参数响应结果。
 */
@Getter
@Setter
@Schema(description = "场景参数响应结果")
public class SceneParamVO {

    @JsonLongId
    @Schema(description = "参数ID", type = "string")
    private Long id;

    @JsonLongId
    @Schema(description = "场景ID", type = "string")
    private Long sceneId;

    @Schema(description = "参数名")
    private String paramName;

    @Schema(description = "参数标签")
    private String paramLabel;

    @Schema(description = "参数类型")
    private String paramType;

    @Schema(description = "参数类型描述")
    private String paramTypeDesc;

    @Schema(description = "排序号", type = "integer", format = "int32")
    private Integer sortOrder;

    @Schema(description = "是否必填，0否，1是", type = "integer", format = "int32")
    private Integer isRequired;

    @Schema(description = "引用数量", type = "integer", format = "int64")
    private Long usageCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
