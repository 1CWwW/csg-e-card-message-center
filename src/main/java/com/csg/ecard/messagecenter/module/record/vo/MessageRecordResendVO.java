package com.csg.ecard.messagecenter.module.record.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录手工重发结果。
 */
@Getter
@Setter
@Schema(description = "消息记录手工重发结果")
public class MessageRecordResendVO {

    @JsonLongId
    @Schema(description = "记录ID", type = "string")
    private Long id;
    private String msgId;
    private String sendStatus;
    private String sendStatusDesc;
    private Integer resendCount;
    private Integer maxResendCount;
    private String errorMsg;
    @Schema(description = "命中免打扰规则后的预计发送时间")
    private LocalDateTime scheduleTime;
    private LocalDateTime sendTime;
    private boolean success;
}
