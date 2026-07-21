package com.csg.ecard.messagecenter.module.push.dto;

import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 群发推送请求，同一消息内容发送给多个接收人。
 */
@Getter
@Setter
@Schema(description = "群发推送请求")
public class MassPushDTO {

    @Schema(description = "消息发送人ID")
    private String userId;

    @NotBlank(message = "registerCode不能为空")
    @Schema(description = "消息注册编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registerCode;

    @Schema(description = "接收消息的单位ID")
    private String receiveCorpId;

    @Schema(description = "预计发送时间；为空表示立即发送")
    private LocalDateTime scheduleTime;

    @Valid
    @Schema(description = "消息内容数据")
    private PushMessageDataDTO data;

    @Schema(description = "场景编码；为空时使用registerCode")
    private String sceneCode;

    @Schema(description = "场景参数；BOOLEAN参数必须传JSON布尔值，不能传字符串；"
            + "兼容旧请求，优先级低于data.sceneParams", example = "{\"isPark\":true}")
    private Map<String, Object> sceneParams;

    @Valid
    @Schema(description = "接收人列表；兼容旧请求")
    private List<PushRecipientDTO> recipients;

    @Schema(description = "消息系统标识（所属模块）")
    private String registerXtbs;

    @Schema(description = "站内信通知类型；兼容旧请求，优先级低于data.type")
    private String type;

    @Schema(description = "邮件/eLink/站内信标题；兼容旧请求，优先级低于data.title")
    private String title;

    @Schema(description = "跳转链接；兼容旧请求，优先级低于data.url")
    private String url;

    @Schema(description = "消息业务优先级", allowableValues = {"HIGH", "NORMAL", "LOW"}, defaultValue = "NORMAL")
    private MessagePriority priority;

    @Schema(description = "业务幂等ID")
    private String bizId;
}
