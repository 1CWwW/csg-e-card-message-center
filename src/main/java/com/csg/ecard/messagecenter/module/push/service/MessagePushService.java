package com.csg.ecard.messagecenter.module.push.service;

import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.mq.AsyncPushExecutionResult;
import com.csg.ecard.messagecenter.module.push.mq.AsyncPushMessage;
import com.csg.ecard.messagecenter.module.push.vo.AsyncPushVO;
import com.csg.ecard.messagecenter.module.push.vo.SyncPushVO;

/**
 * 消息推送服务。
 */
public interface MessagePushService {

    /**
     * 执行同步消息推送。
     *
     * @param request 推送请求
     * @return 推送结果
     */
    SyncPushVO pushSync(SyncPushDTO request);

    /**
     * 受理异步消息推送。
     */
    AsyncPushVO pushAsync(SyncPushDTO request);

    /**
     * 执行异步推送消息的一次消费。
     */
    AsyncPushExecutionResult consumeAsync(AsyncPushMessage message);

    /**
     * 记录异步消费过程中未被单渠道逻辑收敛的技术异常。
     *
     * @param message 异步消息
     * @param cause   技术异常
     */
    void recordAsyncTechnicalFailure(AsyncPushMessage message, Throwable cause);
}
