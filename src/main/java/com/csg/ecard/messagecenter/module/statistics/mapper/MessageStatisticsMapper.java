package com.csg.ecard.messagecenter.module.statistics.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

/**
 * 消息统计聚合查询数据访问接口。
 */
public interface MessageStatisticsMapper {

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectSceneFilterOptions")
    List<StatisticsFilterOptionRow> selectSceneFilterOptions();

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectTemplateFilterOptions")
    List<StatisticsFilterOptionRow> selectTemplateFilterOptions();

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectOverview")
    StatisticsOverviewRow selectOverview(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectSummary")
    StatisticsCountRow selectSummary(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectUnitSummary")
    StatisticsCountRow selectUnitSummary(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectTimeDay")
    List<StatisticsTimeRow> selectTimeDay(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectTimeWeek")
    List<StatisticsTimeRow> selectTimeWeek(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectTimeMonth")
    List<StatisticsTimeRow> selectTimeMonth(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectChannel")
    List<StatisticsChannelRow> selectChannel(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectScene")
    List<StatisticsSceneRow> selectScene(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectUnit")
    List<StatisticsUnitRow> selectUnit(@Param("query") MessageStatisticsQueryCriteria query);

    @SelectProvider(type = MessageStatisticsSqlProvider.class, method = "selectTemplate")
    List<StatisticsTemplateRow> selectTemplate(@Param("query") MessageStatisticsQueryCriteria query);
}
