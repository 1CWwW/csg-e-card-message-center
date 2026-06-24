package com.csg.ecard.messagecenter.module.record.mapper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录详情关联查询结果。
 */
@Getter
@Setter
public class MessageRecordDetailRow {

    private Long id;
    private String msgId;
    private String bizId;
    private String sceneCode;
    private String sceneName;
    private Long sceneId;
    private Long templateId;
    private String templateName;
    private String templateChannelType;
    private Long channelId;
    private String channelName;
    private String channelType;
    private String channelTypeConfig;
    private String userId;
    private String userOrgId;
    private String messageContent;
    private String sceneParams;
    private String priority;
    private String callType;
    private String sendStatus;
    private String errorMsg;
    private String errorStack;
    private LocalDateTime sendTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
