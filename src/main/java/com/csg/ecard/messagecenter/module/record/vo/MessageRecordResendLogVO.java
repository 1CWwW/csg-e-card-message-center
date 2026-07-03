package com.csg.ecard.messagecenter.module.record.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录手动重发日志。
 */
@Getter
@Setter
@Schema(description = "消息记录手动重发日志")
public class MessageRecordResendLogVO {

    @JsonLongId
    @Schema(description = "重发日志ID", type = "string")
    private Long id;

    @JsonLongId
    @Schema(description = "消息记录ID", type = "string")
    private Long recordId;

    private Integer resendNo;
    private String sendStatus;
    private String sendStatusDesc;
    private String errorMsg;

    @Schema(description = "技术异常堆栈，仅用于排障")
    private String errorStack;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operatorId;
    private LocalDateTime createdAt;
}
