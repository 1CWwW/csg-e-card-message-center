package com.csg.ecard.messagecenter.module.dnd.service;

import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbTimeRangeDTO;

import java.util.List;
import java.util.Map;

/**
 * 一次消息处理批次使用的免打扰规则快照。
 *
 * @param userRules   用户规则，键为eLinkId
 * @param unitRules   单位规则，键为单位ID
 * @param globalRule  全局规则
 */
public record DoNotDisturbPolicySnapshot(Map<String, RuleDefinition> userRules,
                                         Map<String, RuleDefinition> unitRules,
                                         RuleDefinition globalRule) {

    /**
     * 已解析的规则定义。
     */
    public record RuleDefinition(Long id,
                                 String scopeId,
                                 boolean includeSubUnits,
                                 List<DoNotDisturbTimeRangeDTO> timeRanges) {
    }
}
