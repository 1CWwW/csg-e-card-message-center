package com.csg.ecard.messagecenter.module.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 场景数据访问接口。
 */
public interface MsgSceneMapper extends BaseMapper<MsgScene> {

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
