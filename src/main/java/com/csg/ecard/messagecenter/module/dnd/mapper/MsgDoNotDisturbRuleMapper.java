package com.csg.ecard.messagecenter.module.dnd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.module.dnd.entity.MsgDoNotDisturbRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息免打扰规则数据访问接口。
 */
public interface MsgDoNotDisturbRuleMapper extends BaseMapper<MsgDoNotDisturbRule> {

    /**
     * 按作用对象名称、标识或备注分页查询免打扰规则。
     *
     * @param page         分页参数
     * @param scopeType    作用范围
     * @param status       启停状态
     * @param keyword      查询关键字
     * @param unitScopeIds 名称匹配的单位ID
     * @return 规则分页结果
     */
    @Select({
            "<script>",
            "SELECT",
            "d.id, d.scope_type AS scopeType, d.scope_id AS scopeId,",
            "d.include_sub_units AS includeSubUnits, d.time_ranges AS timeRanges,",
            "d.status, d.remark, d.create_by AS createBy, d.create_time AS createTime,",
            "d.update_by AS updateBy, d.update_time AS updateTime, d.deleted",
            "FROM msg_do_not_disturb_rule d",
            "WHERE d.deleted = 0",
            "<if test='scopeType != null'>AND d.scope_type = #{scopeType}</if>",
            "<if test='status != null'>AND d.status = #{status}</if>",
            "<if test='keyword != null'>",
            "AND (",
            "d.remark LIKE '%' || #{keyword} || '%'",
            "<choose>",
            "<when test=\"scopeType == 'UNIT'\">",
            "OR d.scope_id LIKE '%' || #{keyword} || '%'",
            "<if test='unitScopeIds != null and !unitScopeIds.isEmpty()'>",
            "OR d.scope_id IN",
            "<foreach collection='unitScopeIds' item='unitScopeId' open='(' separator=',' close=')'>",
            "#{unitScopeId}",
            "</foreach>",
            "</if>",
            "</when>",
            "<when test=\"scopeType == 'USER'\">",
            "OR d.scope_id LIKE '%' || #{keyword} || '%'",
            "</when>",
            "<when test=\"scopeType == 'GLOBAL'\"></when>",
            "<otherwise>",
            "OR (d.scope_type = 'UNIT' AND (",
            "d.scope_id LIKE '%' || #{keyword} || '%'",
            "<if test='unitScopeIds != null and !unitScopeIds.isEmpty()'>",
            "OR d.scope_id IN",
            "<foreach collection='unitScopeIds' item='unitScopeId' open='(' separator=',' close=')'>",
            "#{unitScopeId}",
            "</foreach>",
            "</if>",
            "))",
            "OR (d.scope_type = 'USER' AND d.scope_id LIKE '%' || #{keyword} || '%')",
            "</otherwise>",
            "</choose>",
            ")",
            "</if>",
            "ORDER BY d.create_time DESC, d.id DESC",
            "</script>"
    })
    Page<MsgDoNotDisturbRule> selectRulePage(
            Page<MsgDoNotDisturbRule> page,
            @Param("scopeType") String scopeType,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("unitScopeIds") List<String> unitScopeIds);
}
