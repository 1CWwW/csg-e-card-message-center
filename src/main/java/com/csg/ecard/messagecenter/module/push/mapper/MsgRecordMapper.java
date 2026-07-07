package com.csg.ecard.messagecenter.module.push.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.push.entity.MsgRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 消息发送记录数据访问接口。
 */
public interface MsgRecordMapper extends BaseMapper<MsgRecord> {

    /**
     * 查询指定日期已生成消息ID中的最大序号。
     *
     * @param prefix 消息ID日期前缀，例如 MSG_20260706_
     * @return 最大序号
     */
    @Select({
            "SELECT COALESCE(MAX(CAST(SUBSTR(msg_id, LENGTH(#{prefix}) + 1) AS INT)), 0)",
            "FROM msg_record",
            "WHERE deleted = 0",
            "AND msg_id LIKE #{prefix} || '%'"
    })
    Long selectMaxMsgIdSequence(@Param("prefix") String prefix);
}
