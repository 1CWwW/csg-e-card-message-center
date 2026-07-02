package com.csg.ecard.messagecenter.module.push.dto;

import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组发推送请求，多组消息内容组成同一批次。
 */
@Getter
@Setter
@Schema(description = "组发推送请求")
public class GroupPushDTO {

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
    @Schema(description = "消息内容数据列表")
    private List<PushMessageDataDTO> dataList;

    @Valid
    @Schema(description = "组发消息明细；兼容旧请求")
    private List<GroupPushItemDTO> messages;

    @Schema(description = "消息系统标识（所属模块）")
    private String registerXtbs;

    @Schema(description = "站内信通知类型；兼容旧请求，优先级低于dataList.type")
    private String type;

    @Schema(description = "邮件/eLink/站内信标题；兼容旧请求，优先级低于dataList.title")
    private String title;

    @Schema(description = "跳转链接；兼容旧请求，优先级低于dataList.url")
    private String url;

    @Schema(description = "消息业务优先级", allowableValues = {"HIGH", "NORMAL", "LOW"}, defaultValue = "NORMAL")
    private MessagePriority priority;

    @Schema(description = "业务幂等ID")
    private String bizId;
}
