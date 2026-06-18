package com.csg.ecard.messagecenter.module.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * 渠道类型配置请求。
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "渠道类型配置")
public class ChannelTypeConfigDTO {

    @Schema(description = "短信发送号码")
    private String senderNumber;

    @Schema(description = "邮件发送邮箱")
    private String senderEmail;

    @Schema(description = "eLink 应用ID")
    private String appId;
}
