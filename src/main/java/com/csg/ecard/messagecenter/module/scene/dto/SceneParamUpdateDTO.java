package com.csg.ecard.messagecenter.module.scene.dto;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 编辑场景参数请求。
 */
@Getter
@Setter
@Schema(description = "编辑场景参数请求")
public class SceneParamUpdateDTO {

    @NotBlank(message = "参数名不能为空")
    @Size(max = MessageCenterConstants.PARAM_NAME_MAX_LENGTH, message = "参数名长度不能超过64")
    @Schema(description = "参数名", example = "merchantName")
    private String paramName;

    @NotBlank(message = "参数标签不能为空")
    @Size(max = 20, message = "参数标签长度不能超过20")
    @Schema(description = "参数标签", example = "商户名称")
    private String paramLabel;

    @NotBlank(message = "参数类型不能为空")
    @Schema(description = "参数类型", example = "STRING")
    private String paramType;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否必填，0否，1是", example = "0")
    private Integer isRequired;
}
