package com.csg.ecard.messagecenter.module.record.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息记录手动重发日志实体。
 */
@Getter
@Setter
@TableName("msg_record_resend_log")
public class MsgRecordResendLog extends BaseEntity {

    private Long recordId;
    private Integer resendNo;
    private String sendStatus;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMsg;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorStack;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operatorId;
}
