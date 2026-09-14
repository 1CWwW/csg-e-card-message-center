package com.csg.ecard.messagecenter.module.dnd.service;

import java.time.LocalDateTime;

/**
 * 免打扰发送时间决策。
 *
 * @param delayed              是否因免打扰延后
 * @param effectiveScheduleTime 最终计划发送时间，立即发送时为空
 * @param ruleId               命中的规则ID
 */
public record DoNotDisturbDecision(boolean delayed,
                                   LocalDateTime effectiveScheduleTime,
                                   Long ruleId) {
}
