package com.csg.ecard.messagecenter.module.record.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelTypeConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 消息记录详情。
 */
@Getter
@Setter
@Schema(description = "消息记录详情")
public class MessageRecordDetailVO {

    @JsonLongId
    @Schema(description = "记录ID", type = "string")
    private Long id;
    private String msgId;
    private String bizId;
    private String sceneCode;
    private String sceneName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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

    @JsonLongId
    @Schema(description = "渠道ID", type = "string")
    private Long channelId;
    private String channelName;
    private String channelType;
    private String channelTypeDesc;

    @Schema(description = "渠道安全配置摘要，仅含senderNumber、senderEmail或appId")
    private ChannelTypeConfigDTO channelConfigSummary;

    @JsonLongId
    @Schema(description = "模板ID", type = "string")
    private Long templateId;
    private String templateName;
    private String templateChannelType;
    private String templateChannelTypeDesc;
    private String sendStatus;
    private String sendStatusDesc;
    private String errorMsg;

    @Schema(description = "最近一次技术异常堆栈，仅用于技术排障，可能为空且仅详情接口返回")
    private String errorStack;

    @Schema(description = "最近一次实际发送尝试完成时间；异步消息尚未真正发送时可为空")
    private LocalDateTime sendTime;
    private boolean canResend;

    @Schema(description = "历史记录中模板渲染后的完整最终推送内容")
    private String messageContent;

    @Schema(description = "完整推送内容，与messageContent同源且不重新渲染")
    private String fullMessageContent;

    @Schema(description = "解析后的场景参数")
    private Map<String, Object> sceneParams = Collections.emptyMap();

    @Schema(description = "数据库保存的原始场景参数JSON")
    private String sceneParamsRaw;

    private List<MessageRecordSceneParamItemVO> sceneParamItems = Collections.emptyList();
}
