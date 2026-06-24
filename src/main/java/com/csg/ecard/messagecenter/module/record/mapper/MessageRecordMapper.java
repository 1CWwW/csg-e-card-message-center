package com.csg.ecard.messagecenter.module.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.module.push.entity.MsgRecord;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordListVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 消息记录查询数据访问接口。
 */
public interface MessageRecordMapper extends BaseMapper<MsgRecord> {

    /**
     * 聚合查询今日和昨日记录数量。
     */
    @Select({
            "SELECT",
            "COALESCE(SUM(CASE WHEN r.create_time >= #{todayStart}",
            "  AND r.create_time < #{tomorrowStart} THEN 1 ELSE 0 END), 0) AS todayTotal,",
            "COALESCE(SUM(CASE WHEN r.create_time >= #{todayStart}",
            "  AND r.create_time < #{tomorrowStart}",
            "  AND r.send_status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS todaySuccess,",
            "COALESCE(SUM(CASE WHEN r.create_time >= #{todayStart}",
            "  AND r.create_time < #{tomorrowStart}",
            "  AND r.send_status = 'FAILED' THEN 1 ELSE 0 END), 0) AS todayFailed,",
            "COALESCE(SUM(CASE WHEN r.create_time >= #{yesterdayStart}",
            "  AND r.create_time < #{todayStart} THEN 1 ELSE 0 END), 0) AS yesterdayTotal",
            "FROM msg_record r",
            "WHERE r.deleted = 0",
            "AND r.create_time >= #{yesterdayStart}",
            "AND r.create_time < #{tomorrowStart}"
    })
    MessageRecordOverviewRow selectOverview(@Param("yesterdayStart") LocalDateTime yesterdayStart,
                                            @Param("todayStart") LocalDateTime todayStart,
                                            @Param("tomorrowStart") LocalDateTime tomorrowStart);

    /**
     * 分页查询消息记录及关联名称。
     */
    @Select({
            "<script>",
            "SELECT",
            "r.id, r.msg_id AS msgId, r.biz_id AS bizId,",
            "r.scene_code AS sceneCode, s.scene_name AS sceneName,",
            "r.template_id AS templateId, t.template_name AS templateName,",
            "r.channel_id AS channelId, c.channel_name AS channelName, c.channel_type AS channelType,",
            "r.user_id AS userId, r.user_org_id AS userOrgId,",
            "r.priority AS priority, r.call_type AS callType,",
            "r.message_content AS messageContent,",
            "r.send_status AS sendStatus, r.error_msg AS errorMsg, r.send_time AS sendTime,",
            "r.create_time AS createdAt, r.update_time AS updatedAt",
            "FROM msg_record r",
            "LEFT JOIN msg_scene s ON s.scene_code = r.scene_code",
            "LEFT JOIN msg_template t ON t.id = r.template_id",
            "LEFT JOIN msg_channel c ON c.id = r.channel_id",
            "WHERE r.deleted = 0",
            "<if test='query.msgId != null'>AND r.msg_id = #{query.msgId}</if>",
            "<if test='query.bizId != null'>AND r.biz_id = #{query.bizId}</if>",
            "<if test='query.sceneCode != null'>AND r.scene_code = #{query.sceneCode}</if>",
            "<if test='query.channelType != null'>AND c.channel_type = #{query.channelType}</if>",
            "<if test='query.channelId != null'>AND r.channel_id = #{query.channelId}</if>",
            "<if test='query.channelName != null'>",
            "AND c.channel_name LIKE '%' || #{query.channelName} || '%'",
            "</if>",
            "<if test='query.templateId != null'>AND r.template_id = #{query.templateId}</if>",
            "<if test='query.templateName != null'>",
            "AND t.template_name LIKE '%' || #{query.templateName} || '%'",
            "</if>",
            "<if test='query.sendStatus != null'>AND r.send_status = #{query.sendStatus}</if>",
            "<if test='query.priority != null'>AND r.priority = #{query.priority}</if>",
            "<if test='query.callType != null'>AND r.call_type = #{query.callType.code}</if>",
            "<if test='query.userId != null'>AND r.user_id = #{query.userId}</if>",
            "<if test='query.userOrgId != null'>AND r.user_org_id = #{query.userOrgId}</if>",
            "<if test='query.startTime != null'>AND r.send_time &gt;= #{query.startTime}</if>",
            "<if test='query.endTime != null'>AND r.send_time &lt;= #{query.endTime}</if>",
            "ORDER BY CASE WHEN r.send_time IS NULL THEN 1 ELSE 0 END ASC,",
            "r.send_time DESC,",
            "CASE WHEN r.send_time IS NULL THEN r.create_time ELSE NULL END DESC,",
            "r.id DESC",
            "</script>"
    })
    Page<MessageRecordListVO> selectRecordPage(Page<MessageRecordListVO> page,
                                               @Param("query") MessageRecordQueryCriteria query);

    /**
     * 查询单条消息记录详情。
     */
    @Select({
            "SELECT",
            "r.id, r.msg_id AS msgId, r.biz_id AS bizId,",
            "r.scene_code AS sceneCode, s.scene_name AS sceneName, s.id AS sceneId,",
            "r.template_id AS templateId, t.template_name AS templateName,",
            "t.channel_type AS templateChannelType,",
            "r.channel_id AS channelId, c.channel_name AS channelName, c.channel_type AS channelType,",
            "c.type_config AS channelTypeConfig,",
            "r.user_id AS userId, r.user_org_id AS userOrgId,",
            "r.priority AS priority, r.call_type AS callType,",
            "r.message_content AS messageContent, r.scene_params AS sceneParams,",
            "r.send_status AS sendStatus, r.error_msg AS errorMsg,",
            "r.error_stack AS errorStack, r.send_time AS sendTime,",
            "r.create_time AS createdAt, r.update_time AS updatedAt",
            "FROM msg_record r",
            "LEFT JOIN msg_scene s ON s.scene_code = r.scene_code",
            "LEFT JOIN msg_template t ON t.id = r.template_id",
            "LEFT JOIN msg_channel c ON c.id = r.channel_id",
            "WHERE r.id = #{id} AND r.deleted = 0"
    })
    MessageRecordDetailRow selectRecordDetail(@Param("id") Long id);

    /**
     * 按列表条件查询导出数据，由分页插件限制最大读取数量。
     */
    @Select({
            "<script>",
            "SELECT",
            "r.msg_id AS msgId, r.biz_id AS bizId,",
            "r.scene_code AS sceneCode, s.scene_name AS sceneName,",
            "t.template_name AS templateName, c.channel_type AS channelType,",
            "c.channel_name AS channelName, r.user_id AS userId, r.user_org_id AS userOrgId,",
            "r.priority AS priority, r.call_type AS callType,",
            "r.message_content AS messageContent, r.send_status AS sendStatus,",
            "r.error_msg AS errorMsg, r.send_time AS sendTime,",
            "r.create_time AS createdAt, r.scene_params AS sceneParams",
            "FROM msg_record r",
            "LEFT JOIN msg_scene s ON s.scene_code = r.scene_code",
            "LEFT JOIN msg_template t ON t.id = r.template_id",
            "LEFT JOIN msg_channel c ON c.id = r.channel_id",
            "WHERE r.deleted = 0",
            "<if test='query.msgId != null'>AND r.msg_id = #{query.msgId}</if>",
            "<if test='query.bizId != null'>AND r.biz_id = #{query.bizId}</if>",
            "<if test='query.sceneCode != null'>AND r.scene_code = #{query.sceneCode}</if>",
            "<if test='query.channelType != null'>AND c.channel_type = #{query.channelType}</if>",
            "<if test='query.channelId != null'>AND r.channel_id = #{query.channelId}</if>",
            "<if test='query.channelName != null'>",
            "AND c.channel_name LIKE '%' || #{query.channelName} || '%'",
            "</if>",
            "<if test='query.templateId != null'>AND r.template_id = #{query.templateId}</if>",
            "<if test='query.templateName != null'>",
            "AND t.template_name LIKE '%' || #{query.templateName} || '%'",
            "</if>",
            "<if test='query.sendStatus != null'>AND r.send_status = #{query.sendStatus}</if>",
            "<if test='query.priority != null'>AND r.priority = #{query.priority}</if>",
            "<if test='query.callType != null'>AND r.call_type = #{query.callType.code}</if>",
            "<if test='query.userId != null'>AND r.user_id = #{query.userId}</if>",
            "<if test='query.userOrgId != null'>AND r.user_org_id = #{query.userOrgId}</if>",
            "<if test='query.startTime != null'>AND r.send_time &gt;= #{query.startTime}</if>",
            "<if test='query.endTime != null'>AND r.send_time &lt;= #{query.endTime}</if>",
            "ORDER BY CASE WHEN r.send_time IS NULL THEN 1 ELSE 0 END ASC,",
            "r.send_time DESC,",
            "CASE WHEN r.send_time IS NULL THEN r.create_time ELSE NULL END DESC,",
            "r.id DESC",
            "</script>"
    })
    Page<MessageRecordExportRow> selectExportPage(Page<MessageRecordExportRow> page,
                                                  @Param("query") MessageRecordQueryCriteria query);
}
