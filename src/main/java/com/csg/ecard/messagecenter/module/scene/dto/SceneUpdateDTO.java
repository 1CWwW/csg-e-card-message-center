package com.csg.ecard.messagecenter.module.scene.dto;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 编辑场景请求。
 */
@Getter
@Setter
@Schema(description = "编辑场景请求")
public class SceneUpdateDTO {

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

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "启用状态，1启用，0停用", example = "1")
    private Integer status;
}
