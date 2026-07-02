package com.csg.ecard.messagecenter.module.push.sender;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;

/**
 * 渠道发送使用的消息快照。
 */
@Getter
@Builder
public class MessageSendInfo {

    private final String pcId;
    private final String msgInfoId;
    private final String msgId;
    private final String registerCode;
    private final String registerName;
    private final String registerXtbs;
    private final String msgType;
    private final String type;
    private final String content;
    private final String url;
    private final LocalDateTime sendTime;
    private final String sendUserId;
    private final String receiveUserId;
    private final String receiveCorpId;
    private final String receivePhone;
    private final String receiveEmail;
    private final String emailId;
    private final String senderEmail;
    private final String senderEmailPassword;
    private final String senderEmailUrl;
    private final List<String> copyEmails;
    private final List<EmailFileDTO> files;
    private final String title;
    private final String elinkUserid;
}
