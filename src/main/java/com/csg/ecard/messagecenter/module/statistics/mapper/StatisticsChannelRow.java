package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 按渠道类型聚合统计行。
 */
@Getter
@Setter
public class StatisticsChannelRow extends StatisticsCountRow {

    private String channelType;
}
