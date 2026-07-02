package com.csg.ecard.messagecenter.module.push.service;

import com.csg.ecard.messagecenter.module.push.dto.GroupPushDTO;
import com.csg.ecard.messagecenter.module.push.dto.MassPushDTO;
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
     * 执行群发推送，同一内容发送给多个接收人。
     *
     * @param request 群发请求
     * @return 推送结果
     */
    SyncPushVO pushMass(MassPushDTO request);

    /**
     * 执行组发推送，多条消息组成同一批次。
     *
     * @param request 组发请求
     * @return 推送结果
     */
    SyncPushVO pushGroup(GroupPushDTO request);

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
