package com.csg.ecard.messagecenter.module.record.service;

import com.csg.ecard.messagecenter.module.record.dto.MessageRecordFilterDTO;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordPageQueryDTO;
import com.csg.ecard.messagecenter.module.record.enums.MessageRecordFilterType;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordExportRow;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordDetailVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordFilterOptionVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordOverviewVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordPageResult;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendLogVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendVO;

import java.util.List;

/**
 * 消息记录查询与手工重发服务。
 */
public interface MessageRecordService {

    MessageRecordOverviewVO overview();

    /**
     * 查询记录页按需加载的动态筛选项。
     *
     * @param type        筛选项类型
     * @param channelType 渠道类型，仅查询渠道选项时生效
     * @return 筛选项列表
     */
    List<MessageRecordFilterOptionVO> filterOptions(MessageRecordFilterType type, String channelType);

    MessageRecordPageResult page(MessageRecordPageQueryDTO query);

    MessageRecordDetailVO detail(Long id);

    MessageRecordResendVO resend(Long id);

    List<MessageRecordResendLogVO> resendLogs(Long id);

    List<MessageRecordExportRow> exportRows(MessageRecordFilterDTO query);
}
