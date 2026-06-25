package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息统计SQL查询条件。
 */
@Getter
@Setter
public class MessageStatisticsQueryCriteria {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> channelTypes;
    private List<Long> sceneIds;
    private List<String> unitIds;
    private List<Long> templateIds;
    private List<String> callTypes;
}
