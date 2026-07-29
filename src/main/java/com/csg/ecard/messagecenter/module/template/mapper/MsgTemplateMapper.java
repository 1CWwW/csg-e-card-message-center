package com.csg.ecard.messagecenter.module.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateReferencePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息模板数据访问接口。
 */
public interface MsgTemplateMapper extends BaseMapper<MsgTemplate> {

    /**
     * 聚合查询未删除模板的概览数据。
     *
     * @return 模板概览聚合结果
     */
    @Select({
            "SELECT",
            "COUNT(1) AS total,",
            "COALESCE(SUM(CASE WHEN t.blockly_json IS NOT NULL",
            "  AND LENGTH(TRIM(t.blockly_json)) > 0 THEN 1 ELSE 0 END), 0) AS editedCount,",
            "COALESCE(SUM(CASE WHEN t.status = 1 THEN 1 ELSE 0 END), 0) AS enabledCount,",
            "COALESCE(SUM(CASE WHEN t.blockly_json IS NULL",
            "  OR LENGTH(TRIM(t.blockly_json)) = 0 THEN 1 ELSE 0 END), 0) AS pendingCount",
            "FROM msg_template t",
            "JOIN msg_scene s ON s.id = t.scene_id AND s.deleted = 0",
            "WHERE t.deleted = 0"
    })
    TemplateOverviewRow selectOverview();

    /**
     * 分页查询模板及关联场景、单位数量。
     *
     * @param page  分页对象
     * @param query 查询条件
     * @return 模板分页结果
     */
    @Select({
            "<script>",
            "SELECT t.id, t.template_name, t.scene_id, s.scene_code, s.scene_name,",
            "t.channel_type, t.blockly_json, t.status,",
            "COALESCE(unit_count.unit_count, 0) AS unit_count,",
            "t.create_time, t.update_time",
            "FROM msg_template t",
            "JOIN msg_scene s ON s.id = t.scene_id AND s.deleted = 0",
            "LEFT JOIN (",
            "  SELECT template_id, COUNT(1) AS unit_count",
            "  FROM msg_template_unit",
            "  GROUP BY template_id",
            ") unit_count ON unit_count.template_id = t.id",
            "WHERE t.deleted = 0",
            "<if test='query.templateName != null and query.templateName != \"\"'>",
            "AND t.template_name LIKE '%' || #{query.templateName} || '%'",
            "</if>",
            "<if test='query.sceneId != null'>",
            "AND t.scene_id = #{query.sceneId}",
            "</if>",
            "<if test='query.channelType != null and query.channelType != \"\"'>",
            "AND t.channel_type = #{query.channelType}",
            "</if>",
            "<if test='query.status != null'>",
            "AND t.status = #{query.status}",
            "</if>",
            "<if test='query.unitId != null and query.unitId != \"\"'>",
            "AND EXISTS (",
            "  SELECT 1 FROM msg_template_unit tu",
            "  WHERE tu.template_id = t.id AND tu.unit_id = #{query.unitId}",
            ")",
            "</if>",
            "<if test='query.contentStatus != null and query.contentStatus == 1'>",
            "AND t.blockly_json IS NOT NULL AND LENGTH(TRIM(t.blockly_json)) &gt; 0",
            "</if>",
            "<if test='query.contentStatus != null and query.contentStatus == 2'>",
            "AND (t.blockly_json IS NULL OR LENGTH(TRIM(t.blockly_json)) = 0)",
            "</if>",
            "ORDER BY t.create_time ASC, t.id DESC",
            "</script>"
    })
    Page<TemplateQueryRow> selectTemplatePage(Page<TemplateQueryRow> page,
                                               @Param("query") TemplatePageQueryDTO query);

    /**
     * 查询模板详情关联数据。
     *
     * @param id 模板ID
     * @return 模板详情行
     */
    @Select({
            "SELECT t.id, t.template_name, t.scene_id, s.scene_code, s.scene_name,",
            "t.channel_type, t.blockly_json, t.status,",
            "COALESCE(unit_count.unit_count, 0) AS unit_count,",
            "t.create_time, t.update_time",
            "FROM msg_template t",
            "JOIN msg_scene s ON s.id = t.scene_id AND s.deleted = 0",
            "LEFT JOIN (",
            "  SELECT template_id, COUNT(1) AS unit_count",
            "  FROM msg_template_unit",
            "  GROUP BY template_id",
            ") unit_count ON unit_count.template_id = t.id",
            "WHERE t.id = #{id} AND t.deleted = 0"
    })
    TemplateQueryRow selectTemplateDetail(@Param("id") Long id);

    /**
     * 查询场景下所有包含内容的未删除模板。
     *
     * @param sceneId 场景ID
     * @return 模板列表
     */
    @Select({
            "SELECT id, template_name, scene_id, channel_type, blockly_json, status, create_time, update_time",
            "FROM msg_template",
            "WHERE scene_id = #{sceneId}",
            "AND deleted = 0",
            "AND blockly_json IS NOT NULL",
            "AND LENGTH(TRIM(blockly_json)) > 0",
            "ORDER BY update_time DESC, id DESC"
    })
    List<MsgTemplate> selectContentTemplatesBySceneId(@Param("sceneId") Long sceneId);

    /**
     * 查询指定单位专属的可用模板。
     *
     * @param sceneId     场景ID
     * @param channelType 渠道类型
     * @param unitId      单位ID
     * @return 单位专属模板
     */
    @Select({
            "SELECT t.id, t.template_name, t.scene_id, t.channel_type, t.blockly_json,",
            "t.status, t.create_time, t.update_time",
            "FROM msg_template t",
            "JOIN msg_template_unit tu ON tu.template_id = t.id",
            "WHERE t.scene_id = #{sceneId}",
            "AND t.deleted = 0",
            "AND t.status = 1",
            "AND t.channel_type = #{channelType}",
            "AND t.blockly_json IS NOT NULL",
            "AND LENGTH(TRIM(t.blockly_json)) > 0",
            "AND tu.unit_id = #{unitId}",
            "ORDER BY t.create_time ASC, t.id ASC"
    })
    List<MsgTemplate> selectEnabledUnitTemplates(@Param("sceneId") Long sceneId,
                                                 @Param("channelType") String channelType,
                                                 @Param("unitId") String unitId);

    /**
     * 查询场景和渠道类型下全部单位模板。
     *
     * @param sceneId     场景ID
     * @param channelType 渠道类型
     * @return 单位模板
     */
    @Select({
            "SELECT DISTINCT t.id, t.template_name, t.scene_id, t.channel_type, t.blockly_json,",
            "t.status, t.create_time, t.update_time",
            "FROM msg_template t",
            "JOIN msg_template_unit tu ON tu.template_id = t.id",
            "WHERE t.scene_id = #{sceneId}",
            "AND t.deleted = 0",
            "AND t.status = 1",
            "AND t.channel_type = #{channelType}",
            "AND t.blockly_json IS NOT NULL",
            "AND LENGTH(TRIM(t.blockly_json)) > 0",
            "ORDER BY t.create_time ASC, t.id ASC"
    })
    List<MsgTemplate> selectEnabledUnitTemplatesByChannelType(@Param("sceneId") Long sceneId,
                                                              @Param("channelType") String channelType);

    /**
     * 查询未配置适用单位的默认可用模板。
     *
     * @param sceneId     场景ID
     * @param channelType 渠道类型
     * @return 默认模板
     */
    @Select({
            "SELECT t.id, t.template_name, t.scene_id, t.channel_type, t.blockly_json,",
            "t.status, t.create_time, t.update_time",
            "FROM msg_template t",
            "WHERE t.scene_id = #{sceneId}",
            "AND t.deleted = 0",
            "AND t.status = 1",
            "AND t.channel_type = #{channelType}",
            "AND t.blockly_json IS NOT NULL",
            "AND LENGTH(TRIM(t.blockly_json)) > 0",
            "AND NOT EXISTS (",
            "  SELECT 1 FROM msg_template_unit tu",
            "  WHERE tu.template_id = t.id",
            ")",
            "ORDER BY t.create_time ASC, t.id ASC"
    })
    List<MsgTemplate> selectEnabledDefaultTemplates(@Param("sceneId") Long sceneId,
                                                    @Param("channelType") String channelType);

    /**
     * 查询参考模板候选，场景和单位数量在同一查询中返回。
     *
     * @param query 查询条件
     * @return 参考模板候选
     */
    @Select({
            "<script>",
            "SELECT t.id, t.template_name, t.scene_id, s.scene_name,",
            "t.channel_type, t.blockly_json, t.status,",
            "COALESCE(unit_count.unit_count, 0) AS unit_count,",
            "t.create_time, t.update_time",
            "FROM msg_template t",
            "JOIN msg_scene s ON s.id = t.scene_id AND s.deleted = 0",
            "LEFT JOIN (",
            "  SELECT template_id, COUNT(1) AS unit_count",
            "  FROM msg_template_unit",
            "  GROUP BY template_id",
            ") unit_count ON unit_count.template_id = t.id",
            "WHERE 1 = 1",
            "AND t.deleted = 0",
            "<if test='query.sceneId != null'>",
            "AND t.scene_id = #{query.sceneId}",
            "</if>",
            "<if test='query.channelType != null and query.channelType != \"\"'>",
            "AND t.channel_type = #{query.channelType}",
            "</if>",
            "<if test='query.excludeTemplateId != null'>",
            "AND t.id &lt;&gt; #{query.excludeTemplateId}",
            "</if>",
            "ORDER BY t.update_time DESC, t.id DESC",
            "</script>"
    })
    List<TemplateQueryRow> selectReferenceCandidates(@Param("query") TemplateReferencePageQueryDTO query);
}
