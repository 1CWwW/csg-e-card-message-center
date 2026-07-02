package com.csg.ecard.messagecenter.module.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 组发推送明细，每条消息可带不同场景参数。
 */
@Getter
@Setter
@Schema(description = "组发推送明细")
public class GroupPushItemDTO {

    @NotBlank(message = "sceneCode不能为空")
    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sceneCode;

    @NotNull(message = "sceneParams不能为空")
    @Schema(description = "场景参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> sceneParams;

    @Valid
    @NotNull(message = "recipient不能为空")
    @Schema(description = "接收人", requiredMode = Schema.RequiredMode.REQUIRED)
    private PushRecipientDTO recipient;

    @Schema(description = "邮件/eLink/站内信标题；为空时使用组发请求标题")
    private String title;

    @Schema(description = "跳转链接；为空时使用组发请求链接")
    private String url;
}
