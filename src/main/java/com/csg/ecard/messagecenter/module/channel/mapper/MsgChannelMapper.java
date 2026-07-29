package com.csg.ecard.messagecenter.module.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelPageQueryDTO;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息渠道数据访问接口。
 */
public interface MsgChannelMapper extends BaseMapper<MsgChannel> {

    /**
     * 按渠道类型聚合未删除渠道数量。
     *
     * @return 渠道概览聚合结果
     */
    @Select({
            "SELECT",
            "COALESCE(SUM(CASE WHEN c.channel_type = 'SMS' THEN 1 ELSE 0 END), 0) AS smsCount,",
            "COALESCE(SUM(CASE WHEN c.channel_type = 'EMAIL' THEN 1 ELSE 0 END), 0) AS emailCount,",
            "COALESCE(SUM(CASE WHEN c.channel_type = 'ELINK' THEN 1 ELSE 0 END), 0) AS elinkCount,",
            "COALESCE(SUM(CASE WHEN c.channel_type = 'IN_APP' THEN 1 ELSE 0 END), 0) AS inAppCount",
            "FROM msg_channel c",
            "WHERE c.deleted = 0"
    })
    ChannelOverviewRow selectOverview();

    /**
     * 分页查询渠道，支持单位关联筛选。
     *
     * @param page  分页对象
     * @param query 查询条件
     * @return 渠道分页结果
     */
    @Select({
            "<script>",
            "SELECT c.*",
            "FROM msg_channel c",
            "WHERE c.deleted = 0",
            "<if test='query.channelName != null and query.channelName != \"\"'>",
            "AND c.channel_name LIKE '%' || #{query.channelName} || '%'",
            "</if>",
            "<if test='query.channelType != null and query.channelType != \"\"'>",
            "AND c.channel_type = #{query.channelType}",
            "</if>",
            "<if test='query.status != null'>",
            "AND c.status = #{query.status}",
            "</if>",
            "<if test='query.unitId != null and query.unitId != \"\"'>",
            "AND EXISTS (",
            "  SELECT 1 FROM msg_channel_unit cu",
            "  WHERE cu.channel_id = c.id AND cu.unit_id = #{query.unitId}",
            ")",
            "</if>",
            "ORDER BY c.channel_type ASC, c.priority ASC, c.create_time ASC",
            "</script>"
    })
    Page<MsgChannel> selectChannelPage(Page<MsgChannel> page, @Param("query") ChannelPageQueryDTO query);

    /**
     * 查询指定单位可用渠道候选。
     *
     * @param channelType 渠道类型
     * @param unitId      单位ID
     * @return 可用渠道候选
     */
    @Select({
            "SELECT c.*",
            "FROM msg_channel c",
            "JOIN msg_channel_unit cu ON cu.channel_id = c.id",
            "WHERE c.deleted = 0",
            "AND c.status = 1",
            "AND c.channel_type = #{channelType}",
            "AND cu.unit_id = #{unitId}",
            "ORDER BY c.priority ASC, c.create_time ASC, c.id ASC"
    })
    List<MsgChannel> selectEnabledCandidates(@Param("channelType") String channelType,
                                             @Param("unitId") String unitId);

    /**
     * 查询未配置适用单位的默认启用渠道。
     *
     * @param channelType 渠道类型
     * @return 默认渠道候选
     */
    @Select({
            "SELECT c.*",
            "FROM msg_channel c",
            "WHERE c.deleted = 0",
            "AND c.status = 1",
            "AND c.channel_type = #{channelType}",
            "AND NOT EXISTS (",
            "  SELECT 1 FROM msg_channel_unit cu",
            "  WHERE cu.channel_id = c.id",
            ")",
            "ORDER BY c.priority ASC, c.create_time ASC, c.id ASC"
    })
    List<MsgChannel> selectEnabledDefaultCandidates(@Param("channelType") String channelType);

    /**
     * 查询指定类型的任意启用渠道，用于默认模板兜底发送。
     *
     * @param channelType 渠道类型
     * @return 启用渠道候选
     */
    @Select({
            "SELECT c.*",
            "FROM msg_channel c",
            "WHERE c.deleted = 0",
            "AND c.status = 1",
            "AND c.channel_type = #{channelType}",
            "ORDER BY c.priority ASC, c.create_time ASC, c.id ASC"
    })
    List<MsgChannel> selectEnabledAnyCandidates(@Param("channelType") String channelType);
}
