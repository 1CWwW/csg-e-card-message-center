package com.csg.ecard.messagecenter.module.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 推送消息内容数据。
 */
@Getter
@Setter
@Schema(description = "推送消息内容数据")
public class PushMessageDataDTO {

    @Schema(description = "接收人用户ID列表")
    private List<String> receiveUserIds;

    @Schema(description = "用户ID到eLink用户ID的映射")
    private Map<String, String> elinkIdMap;

    @Schema(description = "接收手机号列表，短信特殊场景使用")
    private List<String> receivePhones;

    @Schema(description = "模板填充数据")
    private Map<String, Object> sceneParams;

    @Schema(description = "跳转链接")
    private String url;

    @Schema(description = "站内信通知类型")
    private String type;

    @Schema(description = "直接消息内容；有值时优先使用该内容，不再渲染模板正文")
    private String content;

    @Schema(description = "邮件/eLink/站内信标题")
    private String title;

    @Schema(description = "邮件ID")
    private String emailId;

    @Schema(description = "发送人邮箱")
    private String senderEmail;

    @Schema(description = "发送邮箱密码")
    private String senderEmailPassword;

    @Schema(description = "发送邮箱服务器")
    private String senderEmailUrl;

    @Schema(description = "邮件抄送用户ID集合")
    private List<String> copyToUsers;

    @Schema(description = "邮件抄送邮箱集合")
    private List<String> copyEmails;

    @Schema(description = "邮件附件")
    private List<EmailFileDTO> file;
}
