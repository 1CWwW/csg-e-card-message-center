package com.csg.ecard.messagecenter.module.scene.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息场景实体。
 */
@Getter
@Setter
@TableName("msg_scene")
@Schema(description = "消息场景")
public class MsgScene extends BaseEntity {

    @Schema(description = "场景编码")
    private String sceneCode;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "场景描述")
    private String description;

    @Schema(description = "启用状态")
    private Integer status;
}
