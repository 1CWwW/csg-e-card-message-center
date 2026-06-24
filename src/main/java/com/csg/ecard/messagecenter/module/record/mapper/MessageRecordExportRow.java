package com.csg.ecard.messagecenter.module.record.mapper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录导出查询结果。
 */
@Getter
@Setter
public class MessageRecordExportRow {

    private String msgId;
    private String bizId;
    private String sceneCode;
    private String sceneName;
    private String templateName;
    private String channelType;
    private String channelName;
    private String userId;
    private String userOrgId;
    private String priority;
    private String callType;
    private String messageContent;
    private String sendStatus;
    private String errorMsg;
    private LocalDateTime sendTime;
    private LocalDateTime createdAt;
    private String sceneParams;
}
