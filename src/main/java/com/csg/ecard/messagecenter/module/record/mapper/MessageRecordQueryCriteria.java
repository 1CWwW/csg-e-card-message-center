package com.csg.ecard.messagecenter.module.record.mapper;

import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录数据库查询条件。
 */
@Getter
@Setter
public class MessageRecordQueryCriteria {

    private String msgId;
    private String bizId;
    private String sceneCode;
    private String channelType;
    private Long channelId;
    private String channelName;
    private Long templateId;
    private String templateName;
    private String sendStatus;
    private String priority;
    private MessageCallType callType;
    private String userId;
    private String userOrgId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
