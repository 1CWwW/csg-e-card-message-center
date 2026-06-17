package com.csg.ecard.messagecenter.module.scene.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景参数实体。
 */
@Getter
@Setter
@TableName("msg_scene_param")
@Schema(description = "场景参数")
public class MsgSceneParam extends BaseEntity {

    @Schema(description = "场景ID")
    private Long sceneId;

    @Schema(description = "参数名")
    private String paramName;

    @Schema(description = "参数标签")
    private String paramLabel;

    @Schema(description = "参数类型")
    private String paramType;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "是否必填，0否，1是")
    private Integer isRequired;
}
