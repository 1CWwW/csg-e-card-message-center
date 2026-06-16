package com.csg.ecard.messagecenter.module.scene.dto;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增场景请求。
 */
@Getter
@Setter
@Schema(description = "新增场景请求")
public class SceneCreateDTO {

    @NotBlank(message = "场景编码不能为空")
    @Size(max = MessageCenterConstants.SCENE_CODE_MAX_LENGTH, message = "场景编码长度不能超过64")
    @Schema(description = "场景编码", example = "CANTEEN_CONSUME_SUCCESS")
    private String sceneCode;

    @NotBlank(message = "场景名称不能为空")
    @Size(max = MessageCenterConstants.SCENE_NAME_MAX_LENGTH, message = "场景名称长度不能超过50")
    @Schema(description = "场景名称", example = "食堂消费成功")
    private String sceneName;

    @NotBlank(message = "所属模块不能为空")
    @Schema(description = "所属模块", example = "CANTEEN_CONSUME")
    private String module;

    @Size(max = MessageCenterConstants.DESCRIPTION_MAX_LENGTH, message = "场景描述长度不能超过200")
    @Schema(description = "场景描述")
    private String description;

    @Schema(description = "启用状态，1启用，0停用；未传时默认启用", example = "1")
    private Integer status = CommonStatus.ENABLE.getCode();
}
