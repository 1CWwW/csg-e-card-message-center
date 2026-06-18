package com.csg.ecard.messagecenter.module.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplateUnit;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板适用单位数据访问接口。
 */
public interface MsgTemplateUnitMapper extends BaseMapper<MsgTemplateUnit> {

    @Select("SELECT unit_id FROM msg_template_unit WHERE template_id = #{templateId} ORDER BY id ASC")
    List<String> selectUnitIdsByTemplateId(@Param("templateId") Long templateId);

    @Delete("DELETE FROM msg_template_unit WHERE template_id = #{templateId}")
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
