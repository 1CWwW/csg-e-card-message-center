package com.csg.ecard.messagecenter.module.record.dto;

import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 消息记录通用筛选条件。
 */
@Getter
@Setter
@Schema(description = "消息记录筛选条件")
public class MessageRecordFilterDTO {

    @Schema(description = "消息ID，精确匹配")
    private String msgId;

    @Schema(description = "业务单据ID，精确匹配")
    private String bizId;

    @Schema(description = "场景编码，精确匹配")
    private String sceneCode;

    @Schema(description = "渠道类型：SMS、EMAIL、ELINK、IN_APP")
    private String channelType;

    @Schema(description = "渠道ID，按字符串传递")
    private String channelId;

    @Schema(description = "渠道名称，模糊匹配")
    private String channelName;

    @Schema(description = "模板ID，按字符串传递")
    private String templateId;

    @Schema(description = "模板名称，模糊匹配")
    private String templateName;

    @Schema(description = "发送状态：SUCCESS、FAILED")
    private String sendStatus;

    @Schema(description = "消息业务优先级：HIGH、NORMAL、LOW；与渠道匹配优先级无关",
            allowableValues = {"HIGH", "NORMAL", "LOW"})
    private String priority;

    @Schema(description = "消息原始调用方式：SYNC同步调用、ASYNC异步调用；为空表示不筛选",
            allowableValues = {"SYNC", "ASYNC"})
    private MessageCallType callType;

    @Schema(description = "用户ID，精确匹配；用户姓名尚未接入员工中心")
    private String userId;

    @Schema(description = "用户单位ID，精确匹配；单位名称尚未接入员工中心")
    private String userOrgId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际发送时间起点，格式 yyyy-MM-dd HH:mm:ss，包含边界")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际发送时间终点，格式 yyyy-MM-dd HH:mm:ss，包含边界")
    private LocalDateTime endTime;
}
