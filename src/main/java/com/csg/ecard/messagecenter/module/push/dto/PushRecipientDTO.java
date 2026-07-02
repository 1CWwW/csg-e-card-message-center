package com.csg.ecard.messagecenter.module.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 推送接收人。
 */
@Getter
@Setter
@Schema(description = "推送接收人")
public class PushRecipientDTO {

    @NotBlank(message = "userId不能为空")
    @Schema(description = "接收人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "接收人姓名")
    private String userName;

    @NotBlank(message = "userOrgId不能为空")
    @Schema(description = "接收人单位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userOrgId;

    @Schema(description = "接收人单位名称")
    private String userOrgName;

    @Schema(description = "接收人手机号")
    private String userPhone;

    @Schema(description = "接收人邮箱")
    private String userEmail;
}
