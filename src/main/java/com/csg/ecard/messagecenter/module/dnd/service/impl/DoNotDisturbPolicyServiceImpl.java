package com.csg.ecard.messagecenter.module.dnd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbTimeRangeDTO;
import com.csg.ecard.messagecenter.module.dnd.entity.MsgDoNotDisturbRule;
import com.csg.ecard.messagecenter.module.dnd.enums.DoNotDisturbScopeType;
import com.csg.ecard.messagecenter.module.dnd.mapper.MsgDoNotDisturbRuleMapper;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbDecision;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbPolicyService;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbPolicySnapshot;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbTimeRangeCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息发送免打扰决策服务实现。
 */
@Service
@RequiredArgsConstructor
public class DoNotDisturbPolicyServiceImpl implements DoNotDisturbPolicyService {

    private static final TypeReference<List<DoNotDisturbTimeRangeDTO>> TIME_RANGE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final MsgDoNotDisturbRuleMapper ruleMapper;
    private final OrganizationProvider organizationProvider;
    private final ObjectMapper objectMapper;
    private final DoNotDisturbTimeRangeCalculator timeRangeCalculator;

    @Override
    public DoNotDisturbPolicySnapshot loadSnapshot() {
        List<MsgDoNotDisturbRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<MsgDoNotDisturbRule>()
                .eq(MsgDoNotDisturbRule::getStatus, CommonStatus.ENABLE.getCode())
                .orderByAsc(MsgDoNotDisturbRule::getId));
        Map<String, DoNotDisturbPolicySnapshot.RuleDefinition> userRules = new LinkedHashMap<>();
        Map<String, DoNotDisturbPolicySnapshot.RuleDefinition> unitRules = new LinkedHashMap<>();
        DoNotDisturbPolicySnapshot.RuleDefinition globalRule = null;
        for (MsgDoNotDisturbRule rule : rules) {
            DoNotDisturbScopeType type;
            try {
                type = DoNotDisturbScopeType.fromCode(rule.getScopeType());
            } catch (IllegalArgumentException ex) {
                throw invalidRule(rule.getId(), ex);
            }
            DoNotDisturbPolicySnapshot.RuleDefinition definition = toDefinition(rule);
            switch (type) {
                case USER -> putUnique(userRules, rule.getScopeId(), definition, rule.getId());
                case UNIT -> putUnique(unitRules, rule.getScopeId(), definition, rule.getId());
                case GLOBAL -> {
                    if (globalRule != null) {
                        throw invalidRule(rule.getId(), null);
                    }
                    globalRule = definition;
                }
            }
        }
        return new DoNotDisturbPolicySnapshot(Map.copyOf(userRules), Map.copyOf(unitRules), globalRule);
    }

    @Override
    public DoNotDisturbDecision evaluate(DoNotDisturbPolicySnapshot snapshot,
                                         String elinkUserId,
                                         String unitId,
                                         LocalDateTime requestedScheduleTime) {
        DoNotDisturbPolicySnapshot safeSnapshot = snapshot == null ? loadSnapshot() : snapshot;
        DoNotDisturbPolicySnapshot.RuleDefinition rule = matchRule(safeSnapshot, elinkUserId, unitId);
        if (rule == null) {
            return new DoNotDisturbDecision(false, requestedScheduleTime, null);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = requestedScheduleTime != null && requestedScheduleTime.isAfter(now)
                ? requestedScheduleTime : now;
        LocalDateTime nextAllowed = timeRangeCalculator.nextAllowed(candidate, rule.timeRanges());
        if (nextAllowed.equals(candidate)) {
            return new DoNotDisturbDecision(false, requestedScheduleTime, rule.id());
        }
        return new DoNotDisturbDecision(true, nextAllowed, rule.id());
    }

    private DoNotDisturbPolicySnapshot.RuleDefinition matchRule(DoNotDisturbPolicySnapshot snapshot,
                                                                 String elinkUserId,
                                                                 String unitId) {
        String normalizedUserId = trimToNull(elinkUserId);
        if (normalizedUserId != null) {
            DoNotDisturbPolicySnapshot.RuleDefinition userRule = snapshot.userRules().get(normalizedUserId);
            if (userRule != null) {
                return userRule;
            }
        }
        String normalizedUnitId = trimToNull(unitId);
        if (normalizedUnitId != null && !snapshot.unitRules().isEmpty()) {
            DoNotDisturbPolicySnapshot.RuleDefinition exactUnitRule = snapshot.unitRules().get(normalizedUnitId);
            if (exactUnitRule != null) {
                return exactUnitRule;
            }
            List<String> unitPath = organizationProvider.resolveUnitPath(normalizedUnitId);
            List<String> safePath = unitPath == null || unitPath.isEmpty() ? List.of(normalizedUnitId) : unitPath;
            for (String pathUnitId : safePath) {
                DoNotDisturbPolicySnapshot.RuleDefinition unitRule = snapshot.unitRules().get(pathUnitId);
                if (unitRule != null && unitRule.includeSubUnits()) {
                    return unitRule;
                }
            }
        }
        return snapshot.globalRule();
    }

    private DoNotDisturbPolicySnapshot.RuleDefinition toDefinition(MsgDoNotDisturbRule rule) {
        try {
            List<DoNotDisturbTimeRangeDTO> ranges = objectMapper.readValue(rule.getTimeRanges(), TIME_RANGE_LIST_TYPE);
            return new DoNotDisturbPolicySnapshot.RuleDefinition(
                    rule.getId(), rule.getScopeId(), Integer.valueOf(1).equals(rule.getIncludeSubUnits()),
                    timeRangeCalculator.validateAndCopy(ranges));
        } catch (Exception ex) {
            throw invalidRule(rule.getId(), ex);
        }
    }

    private void putUnique(Map<String, DoNotDisturbPolicySnapshot.RuleDefinition> target,
                           String scopeId,
                           DoNotDisturbPolicySnapshot.RuleDefinition definition,
                           Long ruleId) {
        String normalizedScopeId = trimToNull(scopeId);
        if (normalizedScopeId == null || target.putIfAbsent(normalizedScopeId, definition) != null) {
            throw invalidRule(ruleId, null);
        }
    }

    private BizException invalidRule(Long ruleId, Exception cause) {
        String message = "免打扰规则配置异常，ruleId=" + ruleId;
        return new BizException(ErrorCode.BUSINESS_ERROR, message);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
