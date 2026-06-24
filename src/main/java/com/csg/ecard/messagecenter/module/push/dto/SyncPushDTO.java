package com.csg.ecard.messagecenter.module.push.dto;

import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 同步消息推送请求。
 */
@Getter
@Setter
@Schema(description = "同步消息推送请求")
public class SyncPushDTO {

    @NotBlank(message = "sceneCode不能为空")
    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sceneCode;

    @NotNull(message = "sceneParams不能为空")
    @Schema(description = "场景参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> sceneParams;

    @NotBlank(message = "userId不能为空")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "用户姓名")
    private String userName;

    @NotBlank(message = "userOrgId不能为空")
    @Schema(description = "用户单位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userOrgId;

    @Schema(description = "用户单位名称")
    private String userOrgName;

    @Schema(description = "手机号")
    private String userPhone;

    @Schema(description = "邮箱")
    private String userEmail;

    @Schema(description = "消息业务优先级：HIGH高、NORMAL普通、LOW低；未传时后端默认NORMAL。"
            + "该字段与渠道匹配使用的渠道优先级无关",
            allowableValues = {"HIGH", "NORMAL", "LOW"}, defaultValue = "NORMAL")
    private MessagePriority priority;

    @Schema(description = "业务幂等ID")
    private String bizId;
}
