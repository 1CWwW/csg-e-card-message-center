package com.csg.ecard.messagecenter.module.push.vo;

import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 单渠道发送结果。
 */
@Getter
@Setter
@Schema(description = "单渠道发送结果")
public class ChannelResultVO {

    private String channelType;
    private String channelName;
    private String templateName;
    private String messageContent;
    private SendStatus status;
    private String errorMsg;
    private LocalDateTime sendTime;
}
