package com.csg.ecard.messagecenter.module.scene.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 场景响应结果。
 */
@Getter
@Setter
@Schema(description = "场景响应结果")
public class MsgSceneVO {

    @JsonLongId
    @Schema(description = "主键ID", type = "string")
    private Long id;

    @Schema(description = "场景编码")
    private String sceneCode;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "所属模块名称")
    private String moduleDesc;

    @Schema(description = "场景描述")
    private String description;

    @Schema(description = "启用状态", type = "integer", format = "int32")
    private Integer status;

    @Schema(description = "启用状态名称")
    private String statusDesc;

    @Schema(description = "参数数量", type = "integer", format = "int64")
    private Long paramCount;

    @Schema(description = "关联模板数量", type = "integer", format = "int64")
    private Long templateCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
