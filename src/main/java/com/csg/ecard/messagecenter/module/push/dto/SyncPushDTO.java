package com.csg.ecard.messagecenter.module.push.dto;

import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 同步消息推送请求。
 */
@Getter
@Setter
@Schema(description = "同步消息推送请求")
public class SyncPushDTO {

    @NotBlank(message = "sceneCode不能为空")
    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sceneCode;

    @Schema(description = "消息注册编码；为空时使用sceneCode")
    private String registerCode;

    @Schema(description = "接收消息的单位ID；为空时使用userOrgId")
    private String receiveCorpId;

    @NotNull(message = "sceneParams不能为空")
    @Schema(description = "场景参数；BOOLEAN参数必须传JSON布尔值，不能传字符串",
            example = "{\"isPark\":true}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> sceneParams;

    @Schema(description = "业务用户ID；与elinkUserId至少传一个")
    private String userId;

    @Schema(description = "用户姓名")
    private String userName;

    @NotBlank(message = "userOrgId不能为空")
    @Schema(description = "用户单位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userOrgId;

    @Schema(description = "用户单位名称")
    private String userOrgName;

    @Schema(description = "消息系统标识（所属模块）")
    private String registerXtbs;

    @Schema(description = "站内信通知类型")
    private String type;

    @Schema(description = "邮件/eLink/站内信标题")
    private String title;

    @Schema(description = "跳转链接")
    private String url;

    @Schema(description = "直接消息内容；有值时优先使用该内容，不再渲染模板正文")
    private String content;

    @Schema(description = "预计发送时间；为空表示立即发送")
    private LocalDateTime scheduleTime;

    @Schema(description = "消息发送人ID")
    private String senderUserId;

    @Schema(description = "eLink平台用户ID；与userId至少传一个，userId为空时后端使用该值补齐")
    private String elinkUserId;

    @Schema(description = "手机号")
    private String userPhone;

    @Schema(description = "邮箱")
    private String userEmail;

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

    @Schema(description = "消息业务优先级：HIGH高、NORMAL普通、LOW低；未传时后端默认NORMAL。"
            + "该字段与渠道匹配使用的渠道优先级无关",
            allowableValues = {"HIGH", "NORMAL", "LOW"}, defaultValue = "NORMAL")
    private MessagePriority priority;

    @Schema(description = "业务幂等ID")
    private String bizId;
}
