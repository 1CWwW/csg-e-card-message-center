package com.csg.ecard.messagecenter.module.statistics.service;

import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsExportQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsTimeQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsDimension;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsChannelVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsOverviewVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsSceneVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTemplateVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTimeVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsUnitVO;

/**
 * 消息统计报表服务。
 */
public interface MessageStatisticsService {

    StatisticsOverviewVO overview(MessageStatisticsQueryDTO query);

    StatisticsTimeVO time(MessageStatisticsTimeQueryDTO query);

    StatisticsChannelVO channel(MessageStatisticsQueryDTO query);

    StatisticsSceneVO scene(MessageStatisticsQueryDTO query);

    StatisticsUnitVO unit(MessageStatisticsQueryDTO query);

    StatisticsTemplateVO template(MessageStatisticsQueryDTO query);

    StatisticsDimension exportDimension(MessageStatisticsExportQueryDTO query);

    MessageStatisticsTimeQueryDTO exportTimeQuery(MessageStatisticsExportQueryDTO query);

    MessageStatisticsQueryDTO exportCommonQuery(MessageStatisticsExportQueryDTO query);
}
