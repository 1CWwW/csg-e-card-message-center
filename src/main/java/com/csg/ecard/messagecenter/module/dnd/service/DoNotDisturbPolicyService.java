package com.csg.ecard.messagecenter.module.dnd.service;

import java.time.LocalDateTime;

/**
 * 消息发送免打扰决策服务。
 */
public interface DoNotDisturbPolicyService {

    /**
     * 加载一次处理批次使用的启用规则快照。
     *
     * @return 规则快照
     */
    DoNotDisturbPolicySnapshot loadSnapshot();

    /**
     * 根据用户、单位及原计划时间计算最终发送时间。
     *
     * @param snapshot             规则快照
     * @param elinkUserId           接收人的eLinkId
     * @param unitId                接收单位ID
     * @param requestedScheduleTime 调用方原计划时间，空表示立即发送
     * @return 发送时间决策
     */
    DoNotDisturbDecision evaluate(DoNotDisturbPolicySnapshot snapshot,
                                  String elinkUserId,
                                  String unitId,
                                  LocalDateTime requestedScheduleTime);
}
