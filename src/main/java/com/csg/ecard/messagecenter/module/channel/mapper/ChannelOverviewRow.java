package com.csg.ecard.messagecenter.module.channel.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 渠道概览聚合结果。
 */
@Getter
@Setter
public class ChannelOverviewRow {

    private Long smsCount;
    private Long emailCount;
    private Long elinkCount;
    private Long inAppCount;
}
