package com.csg.ecard.messagecenter.module.push.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息发送记录实体。
 */
@Getter
@Setter
@TableName("msg_record")
public class MsgRecord extends BaseEntity {

    private String msgId;
    private String bizId;
    private String sceneCode;
    private Long templateId;
    private Long channelId;
    private String sceneParams;
    private String messageContent;
    private String userId;
    private String userOrgId;
    private String sendStatus;
    private String errorMsg;
    private LocalDateTime sendTime;
}
