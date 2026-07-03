package com.csg.ecard.messagecenter.module.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.ecard.messagecenter.module.record.entity.MsgRecordResendLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息记录手动重发日志数据访问接口。
 */
public interface MsgRecordResendLogMapper extends BaseMapper<MsgRecordResendLog> {

    /**
     * 查询指定消息记录的手动重发日志。
     *
     * @param recordId 消息记录ID
     * @return 重发日志列表
     */
    @Select({
            "SELECT",
            "id, record_id AS recordId, resend_no AS resendNo,",
            "send_status AS sendStatus, error_msg AS errorMsg, error_stack AS errorStack,",
            "start_time AS startTime, end_time AS endTime, operator_id AS operatorId,",
            "create_time AS createTime, update_time AS updateTime, create_by AS createBy,",
            "update_by AS updateBy, deleted",
            "FROM msg_record_resend_log",
            "WHERE record_id = #{recordId} AND deleted = 0",
            "ORDER BY resend_no ASC, id ASC"
    })
    List<MsgRecordResendLog> selectByRecordId(@Param("recordId") Long recordId);
}
