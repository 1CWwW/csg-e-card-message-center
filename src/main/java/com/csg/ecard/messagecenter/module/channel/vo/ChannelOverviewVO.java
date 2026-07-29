package com.csg.ecard.messagecenter.module.channel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 渠道概览响应。
 */
@Getter
@Setter
@Schema(description = "渠道概览")
public class ChannelOverviewVO {

    @Schema(description = "短信渠道数")
    private long smsCount;

    @Schema(description = "邮件渠道数")
    private long emailCount;

    @Schema(description = "eLink渠道数")
    private long elinkCount;

    @Schema(description = "站内信渠道数")
    private long inAppCount;
}
