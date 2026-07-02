package com.csg.ecard.messagecenter.module.push.service;

import com.csg.ecard.messagecenter.module.push.vo.ScheduledDispatchResultVO;

/**
 * 到期待发送消息调度服务。
 */
public interface ScheduledMessageDispatchService {

    /**
     * 扫描并发送已到预计发送时间的消息。
     *
     * @return 本轮调度结果
     */
    ScheduledDispatchResultVO dispatchDueMessages();
}
