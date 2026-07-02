package com.csg.ecard.messagecenter.module.push.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 定时消息派发结果。
 */
@Getter
@Setter
public class ScheduledDispatchResultVO {

    private int scannedCount;
    private int sentCount;
    private int failedCount;
    private int skippedCount;
}
