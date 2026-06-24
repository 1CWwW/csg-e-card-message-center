package com.csg.ecard.messagecenter.module.record.service;

import com.csg.ecard.messagecenter.module.record.dto.MessageRecordFilterDTO;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordPageQueryDTO;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordExportRow;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordDetailVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordOverviewVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordPageResult;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendVO;

import java.util.List;

/**
 * 消息记录查询与手工重发服务。
 */
public interface MessageRecordService {

    MessageRecordOverviewVO overview();

    MessageRecordPageResult page(MessageRecordPageQueryDTO query);

    MessageRecordDetailVO detail(Long id);

    MessageRecordResendVO resend(Long id);

    List<MessageRecordExportRow> exportRows(MessageRecordFilterDTO query);
}
