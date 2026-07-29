package com.csg.ecard.messagecenter.module.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * 场景数据访问接口。
 */
public interface MsgSceneMapper extends BaseMapper<MsgScene> {

    /**
     * 聚合查询未删除场景的概览数据。
     *
     * @return 场景概览聚合结果
     */
    @Select({
            "SELECT",
            "COUNT(1) AS total,",
            "COALESCE(SUM(CASE WHEN s.status = 1 THEN 1 ELSE 0 END), 0) AS activeCount,",
            "(SELECT COUNT(1)",
            " FROM msg_scene_param p",
            " JOIN msg_scene ps ON ps.id = p.scene_id AND ps.deleted = 0",
            " WHERE p.deleted = 0) AS paramTotal,",
            "(SELECT COUNT(1)",
            " FROM msg_template t",
            " JOIN msg_scene ts ON ts.id = t.scene_id AND ts.deleted = 0",
            " WHERE t.deleted = 0) AS templateTotal,",
            "(SELECT COUNT(1)",
            " FROM msg_scene associated_scene",
            " WHERE associated_scene.deleted = 0",
            " AND EXISTS (",
            "   SELECT 1 FROM msg_template associated_template",
            "   WHERE associated_template.scene_id = associated_scene.id",
            "   AND associated_template.deleted = 0",
            " )) AS associatedSceneCount",
            "FROM msg_scene s",
            "WHERE s.deleted = 0"
    })
    SceneOverviewRow selectOverview();

    /**
     * 逻辑删除场景，并以主键作为删除标记，支持同一场景编码再次创建。
     *
     * @param id 场景ID
     * @return 受影响行数
     */
    @Update("UPDATE msg_scene SET deleted = id WHERE id = #{id} AND deleted = 0")
    int logicalDeleteById(@Param("id") Long id);

    /**
     * 批量统计场景下未删除模板数量。
     *
     * @param sceneIds 场景ID集合
     * @return 模板数量统计
     */
    @Select({
            "<script>",
            "SELECT scene_id AS sceneId, COUNT(1) AS templateCount",
            "FROM msg_template",
            "WHERE deleted = 0 AND scene_id IN",
            "<foreach collection='sceneIds' item='sceneId' open='(' separator=',' close=')'>",
            "#{sceneId}",
            "</foreach>",
            "GROUP BY scene_id",
            "</script>"
    })
    List<SceneTemplateCountResult> selectTemplateCountsBySceneIds(@Param("sceneIds") Collection<Long> sceneIds);

    /**
     * 统计指定场景下未删除模板数量。
     *
     * @param sceneId 场景ID
     * @return 模板数量
     */
    @Select("SELECT COUNT(1) FROM msg_template WHERE scene_id = #{sceneId} AND deleted = 0")
    Long selectTemplateCountBySceneId(@Param("sceneId") Long sceneId);

    /**
     * 统计指定场景下未删除且启用的模板数量。
     *
     * @param sceneId 场景ID
     * @return 启用模板数量
     */
    @Select("SELECT COUNT(1) FROM msg_template WHERE scene_id = #{sceneId} AND status = 1 AND deleted = 0")
    Long selectEnabledTemplateCountBySceneId(@Param("sceneId") Long sceneId);
}
