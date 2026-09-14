package com.csg.ecard.messagecenter.module.dnd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbBatchCreateDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbPageQueryDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbTimeRangeDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbUpdateDTO;
import com.csg.ecard.messagecenter.module.dnd.entity.MsgDoNotDisturbRule;
import com.csg.ecard.messagecenter.module.dnd.enums.DoNotDisturbScopeType;
import com.csg.ecard.messagecenter.module.dnd.mapper.MsgDoNotDisturbRuleMapper;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbRuleService;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbTimeRangeCalculator;
import com.csg.ecard.messagecenter.module.dnd.vo.DoNotDisturbRuleVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 免打扰规则管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class DoNotDisturbRuleServiceImpl implements DoNotDisturbRuleService {

    private static final String GLOBAL_SCOPE_ID = "GLOBAL";
    private static final TypeReference<List<DoNotDisturbTimeRangeDTO>> TIME_RANGE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final MsgDoNotDisturbRuleMapper ruleMapper;
    private final OrganizationProvider organizationProvider;
    private final ObjectMapper objectMapper;
    private final DoNotDisturbTimeRangeCalculator timeRangeCalculator;

    @Override
    public PageResult<DoNotDisturbRuleVO> page(DoNotDisturbPageQueryDTO query) {
        DoNotDisturbPageQueryDTO safeQuery = query == null ? new DoNotDisturbPageQueryDTO() : query;
        String scopeType = normalizeOptionalScopeType(safeQuery.getScopeType());
        Integer status = validateOptionalStatus(safeQuery.getStatus());
        String keyword = trimToNull(safeQuery.getKeyword());
        Page<MsgDoNotDisturbRule> page = new Page<>(safeQuery.getPageNum(), safeQuery.getPageSize());
        List<String> unitScopeIds = resolveUnitScopeIds(scopeType, keyword);
        Page<MsgDoNotDisturbRule> result = ruleMapper.selectRulePage(
                page, scopeType, status, keyword, unitScopeIds);
        return PageResult.of(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private List<String> resolveUnitScopeIds(String scopeType, String keyword) {
        if (keyword == null || DoNotDisturbScopeType.USER.name().equals(scopeType)
                || DoNotDisturbScopeType.GLOBAL.name().equals(scopeType)) {
            return List.of();
        }
        return organizationProvider.search(keyword, Integer.MAX_VALUE, null).stream()
                .map(path -> path.node().getOrgId())
                .distinct()
                .toList();
    }

    @Override
    public DoNotDisturbRuleVO detail(Long id) {
        return toVO(requireRule(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DoNotDisturbRuleVO> createBatch(DoNotDisturbBatchCreateDTO request) {
        DoNotDisturbScopeType scopeType = validateScopeType(request == null ? null : request.getScopeType());
        List<String> scopeIds = normalizeScopeIds(scopeType, request.getScopeIds());
        ensureRulesAbsent(scopeType, scopeIds);
        String serializedRanges = serializeRanges(request.getTimeRanges());
        int status = request.getStatus() == null ? CommonStatus.ENABLE.getCode() : validateStatus(request.getStatus());
        int includeSubUnits = scopeType == DoNotDisturbScopeType.UNIT
                && Boolean.TRUE.equals(request.getIncludeSubUnits()) ? 1 : 0;
        String remark = trimToNull(request.getRemark());
        return scopeIds.stream().map(scopeId -> {
            MsgDoNotDisturbRule rule = new MsgDoNotDisturbRule();
            rule.setScopeType(scopeType.name());
            rule.setScopeId(scopeId);
            rule.setIncludeSubUnits(includeSubUnits);
            rule.setTimeRanges(serializedRanges);
            rule.setStatus(status);
            rule.setRemark(remark);
            ruleMapper.insert(rule);
            return toVO(rule);
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoNotDisturbRuleVO update(Long id, DoNotDisturbUpdateDTO request) {
        MsgDoNotDisturbRule existed = requireRule(id);
        DoNotDisturbScopeType scopeType = DoNotDisturbScopeType.fromCode(existed.getScopeType());
        existed.setIncludeSubUnits(scopeType == DoNotDisturbScopeType.UNIT
                && Boolean.TRUE.equals(request.getIncludeSubUnits()) ? 1 : 0);
        existed.setTimeRanges(serializeRanges(request.getTimeRanges()));
        existed.setStatus(validateStatus(request.getStatus()));
        existed.setRemark(trimToNull(request.getRemark()));
        ruleMapper.updateById(existed);
        return toVO(existed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireRule(id);
        ruleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoNotDisturbRuleVO toggle(Long id) {
        MsgDoNotDisturbRule rule = requireRule(id);
        rule.setStatus(CommonStatus.ENABLE.getCode().equals(rule.getStatus())
                ? CommonStatus.DISABLE.getCode() : CommonStatus.ENABLE.getCode());
        ruleMapper.updateById(rule);
        return toVO(rule);
    }

    private MsgDoNotDisturbRule requireRule(Long id) {
        MsgDoNotDisturbRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "免打扰规则不存在");
        }
        return rule;
    }

    private void ensureRulesAbsent(DoNotDisturbScopeType scopeType, List<String> scopeIds) {
        List<MsgDoNotDisturbRule> existing = ruleMapper.selectList(
                new LambdaQueryWrapper<MsgDoNotDisturbRule>()
                        .eq(MsgDoNotDisturbRule::getScopeType, scopeType.name())
                        .in(MsgDoNotDisturbRule::getScopeId, scopeIds));
        if (!existing.isEmpty()) {
            String duplicated = existing.stream().map(MsgDoNotDisturbRule::getScopeId).distinct()
                    .reduce((left, right) -> left + "、" + right).orElse("");
            throw new BizException(ErrorCode.DATA_DUPLICATE, "以下对象已存在免打扰规则：" + duplicated);
        }
    }

    private List<String> normalizeScopeIds(DoNotDisturbScopeType scopeType, List<String> scopeIds) {
        if (scopeType == DoNotDisturbScopeType.GLOBAL) {
            return List.of(GLOBAL_SCOPE_ID);
        }
        Set<String> normalized = new LinkedHashSet<>();
        if (scopeIds != null) {
            scopeIds.stream().map(this::trimToNull).filter(java.util.Objects::nonNull).forEach(value -> {
                if (value.length() > 128) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "单位ID或eLinkId长度不能超过128");
                }
                normalized.add(value);
            });
        }
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单位或用户作用对象不能为空");
        }
        return List.copyOf(normalized);
    }

    private String serializeRanges(List<DoNotDisturbTimeRangeDTO> ranges) {
        try {
            return objectMapper.writeValueAsString(timeRangeCalculator.validateAndCopy(ranges));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "免打扰时间段序列化失败");
        }
    }

    private List<DoNotDisturbTimeRangeDTO> parseRanges(String ranges) {
        try {
            return timeRangeCalculator.validateAndCopy(objectMapper.readValue(ranges, TIME_RANGE_LIST_TYPE));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "免打扰时间段配置解析失败");
        }
    }

    private DoNotDisturbRuleVO toVO(MsgDoNotDisturbRule rule) {
        DoNotDisturbScopeType scopeType = DoNotDisturbScopeType.fromCode(rule.getScopeType());
        DoNotDisturbRuleVO vo = new DoNotDisturbRuleVO();
        vo.setId(rule.getId());
        vo.setScopeType(scopeType.name());
        vo.setScopeTypeDesc(scopeType.getDesc());
        vo.setScopeId(scopeType == DoNotDisturbScopeType.GLOBAL ? null : rule.getScopeId());
        vo.setIncludeSubUnits(Integer.valueOf(1).equals(rule.getIncludeSubUnits()));
        vo.setTimeRanges(parseRanges(rule.getTimeRanges()));
        vo.setStatus(rule.getStatus());
        vo.setStatusDesc(CommonStatus.fromCode(rule.getStatus()).getDesc());
        vo.setRemark(rule.getRemark());
        vo.setCreatedAt(rule.getCreateTime());
        vo.setUpdatedAt(rule.getUpdateTime());
        return vo;
    }

    private DoNotDisturbScopeType validateScopeType(String scopeType) {
        if (!StringUtils.hasText(scopeType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "scopeType不能为空");
        }
        try {
            return DoNotDisturbScopeType.fromCode(scopeType.trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "scopeType仅支持GLOBAL、UNIT或USER");
        }
    }

    private String normalizeOptionalScopeType(String scopeType) {
        return StringUtils.hasText(scopeType) ? validateScopeType(scopeType).name() : null;
    }

    private int validateStatus(Integer status) {
        if (status == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status不能为空");
        }
        try {
            return CommonStatus.fromCode(status).getCode();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status仅支持1或0");
        }
    }

    private Integer validateOptionalStatus(Integer status) {
        return status == null ? null : validateStatus(status);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
