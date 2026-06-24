package com.csg.ecard.messagecenter.module.record.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息记录概览聚合结果。
 */
@Getter
@Setter
public class MessageRecordOverviewRow {

    private Long todayTotal;
    private Long todaySuccess;
    private Long todayFailed;
    private Long yesterdayTotal;
}
