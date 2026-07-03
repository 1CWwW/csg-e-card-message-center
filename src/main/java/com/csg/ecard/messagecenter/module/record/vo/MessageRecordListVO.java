package com.csg.ecard.messagecenter.module.record.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录列表项。
 */
@Getter
@Setter
@Schema(description = "消息记录列表项")
public class MessageRecordListVO {

    @JsonLongId
    @Schema(description = "记录ID", type = "string")
    private Long id;
    private String msgId;
    private String bizId;
    private String sceneCode;
    private String sceneName;

    @JsonLongId
    @Schema(description = "模板ID", type = "string")
    private Long templateId;
    private String templateName;

    @JsonLongId
    @Schema(description = "渠道ID", type = "string")
    private Long channelId;
    private String channelName;
    private String channelType;
    private String channelTypeDesc;
    private String userId;
    private String userOrgId;

    @Schema(description = "消息业务优先级：HIGH、NORMAL、LOW；与渠道匹配优先级无关")
    private String priority;

    @Schema(description = "消息业务优先级描述：高、普通、低")
    private String priorityDesc;

    @Schema(description = "消息原始调用方式：SYNC同步调用、ASYNC异步调用")
    private String callType;

    @Schema(description = "消息原始调用方式描述：同步调用、异步调用")
    private String callTypeDesc;

    @Schema(description = "模板渲染后实际提交渠道发送器的完整最终推送内容")
    private String messageContent;

    @Schema(description = "发送状态：SUCCESS、FAILED、PENDING、ACCEPTED")
    private String sendStatus;
    private Integer resendCount;
    private Integer maxResendCount;
    private String sendStatusDesc;
    private String errorMsg;

    @Schema(description = "最近一次实际发送尝试完成时间；异步消息尚未真正发送时可为空")
    private LocalDateTime sendTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "是否允许手工重发，仅FAILED为true")
    private boolean canResend;
}
