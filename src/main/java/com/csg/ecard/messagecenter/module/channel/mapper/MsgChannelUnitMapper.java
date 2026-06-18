package com.csg.ecard.messagecenter.module.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannelUnit;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 渠道适用单位数据访问接口。
 */
public interface MsgChannelUnitMapper extends BaseMapper<MsgChannelUnit> {

    /**
     * 批量统计渠道关联单位数量。
     *
     * @param channelIds 渠道ID集合
     * @return 统计结果
     */
    @Select({
            "<script>",
            "SELECT channel_id AS channelId, COUNT(1) AS unitCount",
            "FROM msg_channel_unit",
            "WHERE channel_id IN",
            "<foreach collection='channelIds' item='channelId' open='(' separator=',' close=')'>",
            "#{channelId}",
            "</foreach>",
            "GROUP BY channel_id",
            "</script>"
    })
    List<ChannelUnitCountResult> selectUnitCountsByChannelIds(@Param("channelIds") Collection<Long> channelIds);

    /**
     * 查询渠道关联单位ID。
     *
     * @param channelId 渠道ID
     * @return 单位ID列表
     */
    @Select("SELECT unit_id FROM msg_channel_unit WHERE channel_id = #{channelId} ORDER BY id ASC")
    List<String> selectUnitIdsByChannelId(@Param("channelId") Long channelId);

    /**
     * 物理删除渠道单位关联。
     *
     * @param channelId 渠道ID
     * @return 删除行数
     */
    @Delete("DELETE FROM msg_channel_unit WHERE channel_id = #{channelId}")
    int deleteByChannelId(@Param("channelId") Long channelId);

    /**
     * 统计当前渠道停用或删除后会失去同类型启用渠道的单位数量。
     *
     * @param channelId   当前渠道ID
     * @param channelType 渠道类型
     * @return 唯一影响单位数量
     */
    @Select({
            "SELECT COUNT(1)",
            "FROM msg_channel_unit cu",
            "WHERE cu.channel_id = #{channelId}",
            "AND NOT EXISTS (",
            "  SELECT 1",
            "  FROM msg_channel other_channel",
            "  JOIN msg_channel_unit other_unit ON other_unit.channel_id = other_channel.id",
            "  WHERE other_channel.deleted = 0",
            "  AND other_channel.status = 1",
            "  AND other_channel.channel_type = #{channelType}",
            "  AND other_channel.id <> #{channelId}",
            "  AND other_unit.unit_id = cu.unit_id",
            ")"
    })
    Long countUniqueEnabledUnits(@Param("channelId") Long channelId,
                                 @Param("channelType") String channelType);
}
