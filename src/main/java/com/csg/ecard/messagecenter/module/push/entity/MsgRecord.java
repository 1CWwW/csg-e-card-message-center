package com.csg.ecard.messagecenter.module.push.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import com.csg.ecard.messagecenter.common.enums.MessagePriority;
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

    private String pcId;
    private String msgId;
    private String bizId;
    private String sceneCode;
    private String registerCode;
    private String registerName;
    private String registerXtbs;
    private String msgType;
    private String noticeType;
    private String title;
    private String url;
    private LocalDateTime scheduleTime;
    private Long templateId;
    private Long channelId;
    private String sceneParams;
    private String messageContent;
    private String userId;
    private String userName;
    private String userOrgId;
    private String receiveUserId;
    private String receiveCorpId;
    private String receivePhone;
    private String receiveEmail;
    private String emailId;
    private String senderEmail;
    private String senderEmailPassword;
    private String senderEmailUrl;
    private String copyEmail;
    private String file;
    private String senderUserId;
    private String elinkUserId;
    private MessagePriority priority;
    private MessageCallType callType;
    private String sendStatus;
    private Integer resendCount;
    private Integer maxResendCount;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMsg;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorStack;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime sendTime;
}
