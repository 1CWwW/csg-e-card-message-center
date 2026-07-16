package com.csg.ecard.messagecenter.module.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * 场景参数数据访问接口。
 */
public interface MsgSceneParamMapper extends BaseMapper<MsgSceneParam> {

    /**
     * 逻辑删除场景参数，并以主键作为删除标记，支持同名参数再次创建。
     *
     * @param id 参数ID
     * @return 受影响行数
     */
    @Update("UPDATE msg_scene_param SET deleted = id WHERE id = #{id} AND deleted = 0")
    int logicalDeleteById(@Param("id") Long id);

    /**
     * 查询场景下最大排序号。
     *
     * @param sceneId 场景ID
     * @return 最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM msg_scene_param WHERE scene_id = #{sceneId} AND deleted = 0")
    Integer selectMaxSortOrder(@Param("sceneId") Long sceneId);

    /**
     * 按场景批量统计参数数量。
     *
     * @param sceneIds 场景ID集合
     * @return 参数数量统计
     */
    @Select({
            "<script>",
            "SELECT scene_id AS sceneId, COUNT(1) AS paramCount",
            "FROM msg_scene_param",
            "WHERE deleted = 0 AND scene_id IN",
            "<foreach collection='sceneIds' item='sceneId' open='(' separator=',' close=')'>",
            "#{sceneId}",
            "</foreach>",
            "GROUP BY scene_id",
            "</script>"
    })
    List<SceneParamCountResult> selectParamCountsBySceneIds(@Param("sceneIds") Collection<Long> sceneIds);
}
