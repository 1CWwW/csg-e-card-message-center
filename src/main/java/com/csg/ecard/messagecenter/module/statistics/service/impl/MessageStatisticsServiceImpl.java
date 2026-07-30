package com.csg.ecard.messagecenter.module.statistics.service.impl;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationNode;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsExportQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsTimeQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsDimension;
import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsExportScope;
import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsFilterType;
import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsGranularity;
import com.csg.ecard.messagecenter.module.statistics.mapper.MessageStatisticsMapper;
import com.csg.ecard.messagecenter.module.statistics.mapper.MessageStatisticsQueryCriteria;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsChannelRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsCountRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsFilterOptionRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsOverviewRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsSceneRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsTemplateRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsTimeRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsUnitRow;
import com.csg.ecard.messagecenter.module.statistics.service.MessageStatisticsService;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsChannelItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsChannelVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsFilterOptionVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsOverviewVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsSceneItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsSceneVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsSummaryVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTemplateItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTemplateVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTimeItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTimeVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsUnitItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsUnitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息统计报表服务实现。
 */
@Service
@RequiredArgsConstructor
public class MessageStatisticsServiceImpl implements MessageStatisticsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String UNKNOWN_TEXT = "-";

    private final MessageStatisticsMapper messageStatisticsMapper;
    private final OrganizationProvider organizationProvider;

    @Override
    public List<StatisticsFilterOptionVO> filterOptions(StatisticsFilterType type) {
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "type不能为空");
        }
        List<StatisticsFilterOptionRow> rows = switch (type) {
            case SCENE -> messageStatisticsMapper.selectSceneFilterOptions();
            case TEMPLATE -> messageStatisticsMapper.selectTemplateFilterOptions();
        };
        return rows.stream().map(this::toFilterOption).toList();
    }

    @Override
    public StatisticsOverviewVO overview(MessageStatisticsQueryDTO query) {
        MessageStatisticsQueryCriteria criteria = normalize(query, false);
        StatisticsOverviewRow row = messageStatisticsMapper.selectOverview(criteria);
        StatisticsOverviewVO vo = new StatisticsOverviewVO();
        fillSummary(vo, row);
        vo.setSyncCount(safe(row == null ? null : row.getSyncCount()));
        vo.setAsyncCount(safe(row == null ? null : row.getAsyncCount()));
        vo.setChannelTypeCount(safe(row == null ? null : row.getChannelTypeCount()));
        vo.setSceneCount(safe(row == null ? null : row.getSceneCount()));
        vo.setTemplateCount(safe(row == null ? null : row.getTemplateCount()));
        vo.setUnitCount(safe(row == null ? null : row.getUnitCount()));
        return vo;
    }

    @Override
    public StatisticsTimeVO time(MessageStatisticsTimeQueryDTO query) {
        StatisticsGranularity granularity = parseGranularity(query.getGranularity());
        MessageStatisticsQueryCriteria criteria = normalize(query, false);
        List<StatisticsTimeRow> rows = switch (granularity) {
            case DAY -> messageStatisticsMapper.selectTimeDay(criteria);
            case WEEK -> messageStatisticsMapper.selectTimeWeek(criteria);
            case MONTH -> messageStatisticsMapper.selectTimeMonth(criteria);
        };
        StatisticsTimeVO vo = new StatisticsTimeVO();
        vo.setGranularity(granularity.name());
        vo.setSummary(summary(messageStatisticsMapper.selectSummary(criteria)));
        vo.setItems(fillTimeItems(criteria, granularity, rows));
        return vo;
    }

    @Override
    public StatisticsChannelVO channel(MessageStatisticsQueryDTO query) {
        MessageStatisticsQueryCriteria criteria = normalize(query, false);
        StatisticsSummaryVO summary = summary(messageStatisticsMapper.selectSummary(criteria));
        List<StatisticsChannelItemVO> items = messageStatisticsMapper.selectChannel(criteria).stream()
                .map(row -> toChannelItem(row, summary.getTotalCount()))
                .toList();
        StatisticsChannelVO vo = new StatisticsChannelVO();
        vo.setSummary(summary);
        vo.setItems(items);
        return vo;
    }

    @Override
    public StatisticsSceneVO scene(MessageStatisticsQueryDTO query) {
        MessageStatisticsQueryCriteria criteria = normalize(query, false);
        StatisticsSummaryVO summary = summary(messageStatisticsMapper.selectSummary(criteria));
        List<StatisticsSceneItemVO> items = messageStatisticsMapper.selectScene(criteria).stream()
                .map(row -> toSceneItem(row, summary.getTotalCount()))
                .toList();
        StatisticsSceneVO vo = new StatisticsSceneVO();
        vo.setSummary(summary);
        vo.setItems(items);
        return vo;
    }

    @Override
    public StatisticsUnitVO unit(MessageStatisticsQueryDTO query) {
        MessageStatisticsQueryCriteria criteria = normalize(query, false);
        StatisticsSummaryVO summary = summary(messageStatisticsMapper.selectUnitSummary(criteria));
        List<StatisticsUnitItemVO> items = messageStatisticsMapper.selectUnit(criteria).stream()
                .map(row -> toUnitItem(row, summary.getTotalCount()))
                .toList();
        StatisticsUnitVO vo = new StatisticsUnitVO();
        vo.setSummary(summary);
        vo.setItems(items);
        return vo;
    }

    @Override
    public StatisticsTemplateVO template(MessageStatisticsQueryDTO query) {
        MessageStatisticsQueryCriteria criteria = normalize(query, false);
        StatisticsSummaryVO summary = summary(messageStatisticsMapper.selectSummary(criteria));
        List<StatisticsTemplateItemVO> items = messageStatisticsMapper.selectTemplate(criteria).stream()
                .map(row -> toTemplateItem(row, summary.getTotalCount()))
                .toList();
        StatisticsTemplateVO vo = new StatisticsTemplateVO();
        vo.setSummary(summary);
        vo.setItems(items);
        return vo;
    }

    @Override
    public StatisticsDimension exportDimension(MessageStatisticsExportQueryDTO query) {
        validateBaseTime(query);
        try {
            return StatisticsDimension.parse(query.getDimension());
        } catch (IllegalArgumentException ex) {
            throw paramError(ex.getMessage());
        }
    }

    @Override
    public MessageStatisticsTimeQueryDTO exportTimeQuery(MessageStatisticsExportQueryDTO query) {
        MessageStatisticsTimeQueryDTO target = new MessageStatisticsTimeQueryDTO();
        copyCommon(query, target, exportAll(query));
        target.setGranularity(query.getGranularity());
        return target;
    }

    @Override
    public MessageStatisticsQueryDTO exportCommonQuery(MessageStatisticsExportQueryDTO query) {
        MessageStatisticsQueryDTO target = new MessageStatisticsQueryDTO();
        copyCommon(query, target, exportAll(query));
        return target;
    }

    private MessageStatisticsQueryCriteria normalize(MessageStatisticsQueryDTO query, boolean ignoreFilters) {
        validateBaseTime(query);
        MessageStatisticsQueryCriteria criteria = new MessageStatisticsQueryCriteria();
        criteria.setStartTime(query.getStartTime());
        criteria.setEndTime(effectiveEndTime(query));
        if (ignoreFilters) {
            return criteria;
        }
        criteria.setChannelTypes(normalizeChannelTypes(query.getChannelTypes()));
        criteria.setSceneIds(normalizeLongs(query.getSceneIds()));
        criteria.setUnitIds(normalizeUnitIds(query.getUnitIds(), query.getIncludeSubUnits()));
        criteria.setTemplateIds(normalizeLongs(query.getTemplateIds()));
        criteria.setCallTypes(normalizeCallTypes(query.getCallTypes()));
        return criteria;
    }

    private void validateBaseTime(MessageStatisticsQueryDTO query) {
        if (query.getStartTime() != null
                && query.getEndTime() != null
                && query.getStartTime().isAfter(query.getEndTime())) {
            throw paramError("开始时间不能晚于结束时间");
        }
    }

    private LocalDateTime effectiveEndTime(MessageStatisticsQueryDTO query) {
        if (query.getStartTime() != null && query.getEndTime() == null) {
            return LocalDateTime.now();
        }
        return query.getEndTime();
    }

    private StatisticsGranularity parseGranularity(String value) {
        try {
            return StatisticsGranularity.parse(value);
        } catch (IllegalArgumentException ex) {
            throw paramError(ex.getMessage());
        }
    }

    private List<String> normalizeChannelTypes(List<String> values) {
        return normalizeTexts(values).stream()
                .map(value -> {
                    try {
                        return ChannelType.fromCode(value).getCode();
                    } catch (IllegalArgumentException ex) {
                        throw paramError("渠道类型仅支持SMS、EMAIL、ELINK、IN_APP");
                    }
                })
                .toList();
    }

    private List<String> normalizeCallTypes(List<String> values) {
        return normalizeTexts(values).stream()
                .map(value -> {
                    try {
                        return MessageCallType.fromCode(value).getCode();
                    } catch (IllegalArgumentException ex) {
                        throw paramError("调用方式仅支持SYNC、ASYNC");
                    }
                })
                .toList();
    }

    private List<String> normalizeTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(value.trim());
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> normalizeUnitIds(List<String> values, Boolean includeSubUnits) {
        List<String> selectedUnitIds = normalizeTexts(values);
        if (selectedUnitIds.isEmpty() || !Boolean.TRUE.equals(includeSubUnits)) {
            return selectedUnitIds;
        }
        Set<String> selected = new LinkedHashSet<>(selectedUnitIds);
        List<OrganizationNode> organizationTree = organizationProvider.tree();
        if (containsTopLevelUnit(organizationTree, selected)) {
            return Collections.emptyList();
        }
        Set<String> result = new LinkedHashSet<>(selectedUnitIds);
        collectSubUnitIds(organizationTree, selected, result, false);
        return new ArrayList<>(result);
    }

    private boolean containsTopLevelUnit(List<OrganizationNode> nodes, Set<String> selected) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        return nodes.stream()
                .filter(Objects::nonNull)
                .map(OrganizationNode::getOrgId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(selected::contains);
    }

    private void collectSubUnitIds(List<OrganizationNode> nodes,
                                   Set<String> selected,
                                   Set<String> result,
                                   boolean parentSelected) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (OrganizationNode node : nodes) {
            if (node == null) {
                continue;
            }
            String orgId = StringUtils.hasText(node.getOrgId()) ? node.getOrgId().trim() : null;
            boolean currentSelected = parentSelected || selected.contains(orgId);
            if (currentSelected && orgId != null) {
                result.add(orgId);
            }
            collectSubUnitIds(node.getChildren(), selected, result, currentSelected);
        }
    }

    private List<Long> normalizeLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private StatisticsSummaryVO summary(StatisticsCountRow row) {
        StatisticsSummaryVO vo = new StatisticsSummaryVO();
        fillSummary(vo, row);
        return vo;
    }

    private void fillSummary(StatisticsSummaryVO vo, StatisticsCountRow row) {
        long total = safe(row == null ? null : row.getTotalCount());
        long success = safe(row == null ? null : row.getSuccessCount());
        long failed = safe(row == null ? null : row.getFailedCount());
        vo.setTotalCount(total);
        vo.setSuccessCount(success);
        vo.setFailedCount(failed);
        vo.setSuccessRate(rate(success, total));
    }

    private List<StatisticsTimeItemVO> fillTimeItems(MessageStatisticsQueryCriteria criteria,
                                                    StatisticsGranularity granularity,
                                                    List<StatisticsTimeRow> rows) {
        Map<String, StatisticsTimeRow> rowMap = rows == null ? Collections.emptyMap()
                : rows.stream().collect(Collectors.toMap(StatisticsTimeRow::getPeriod,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<StatisticsTimeItemVO> items = new ArrayList<>();
        LocalDate current = fillStartDate(criteria, granularity, rows);
        LocalDate end = fillEndDate(criteria, granularity, rows);
        if (current == null || end == null || current.isAfter(end)) {
            return items;
        }
        while (!current.isAfter(end)) {
            String period = periodKey(current, granularity);
            StatisticsTimeRow row = rowMap.get(period);
            StatisticsTimeItemVO item = new StatisticsTimeItemVO();
            item.setPeriod(period);
            item.setPeriodLabel(periodLabel(current, granularity));
            fillSummary(item, row);
            items.add(item);
            current = nextPeriod(current, granularity);
        }
        return items;
    }

    private LocalDate fillStartDate(MessageStatisticsQueryCriteria criteria,
                                    StatisticsGranularity granularity,
                                    List<StatisticsTimeRow> rows) {
        if (criteria.getStartTime() != null) {
            return alignStart(criteria.getStartTime().toLocalDate(), granularity);
        }
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return parsePeriodDate(rows.get(0).getPeriod(), granularity);
    }

    private LocalDate fillEndDate(MessageStatisticsQueryCriteria criteria,
                                  StatisticsGranularity granularity,
                                  List<StatisticsTimeRow> rows) {
        if (criteria.getEndTime() != null) {
            return alignStart(criteria.getEndTime().toLocalDate(), granularity);
        }
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return parsePeriodDate(rows.get(rows.size() - 1).getPeriod(), granularity);
    }

    private LocalDate parsePeriodDate(String period, StatisticsGranularity granularity) {
        if (!StringUtils.hasText(period)) {
            return null;
        }
        return switch (granularity) {
            case DAY, WEEK -> LocalDate.parse(period);
            case MONTH -> LocalDate.parse(period + "-01");
        };
    }

    private LocalDate alignStart(LocalDate date, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate nextPeriod(LocalDate date, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }

    private String periodKey(LocalDate date, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAY -> date.toString();
            case WEEK -> date.toString();
            case MONTH -> date.getYear() + "-" + twoDigits(date.getMonthValue());
        };
    }

    private String periodLabel(LocalDate date, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAY -> date.toString();
            case WEEK -> date + "~" + date.plusDays(6);
            case MONTH -> date.getYear() + "-" + twoDigits(date.getMonthValue());
        };
    }

    private StatisticsChannelItemVO toChannelItem(StatisticsChannelRow row, long totalCount) {
        StatisticsChannelItemVO item = new StatisticsChannelItemVO();
        fillSummary(item, row);
        item.setChannelType(row.getChannelType());
        item.setChannelTypeDesc(channelTypeDesc(row.getChannelType()));
        item.setPercentage(rate(item.getTotalCount(), totalCount));
        return item;
    }

    private StatisticsFilterOptionVO toFilterOption(StatisticsFilterOptionRow row) {
        StatisticsFilterOptionVO option = new StatisticsFilterOptionVO();
        option.setValue(row.getOptionValue());
        option.setLabel(row.getOptionLabel());
        return option;
    }

    private StatisticsSceneItemVO toSceneItem(StatisticsSceneRow row, long totalCount) {
        StatisticsSceneItemVO item = new StatisticsSceneItemVO();
        fillSummary(item, row);
        item.setSceneId(row.getSceneId());
        item.setSceneCode(row.getSceneCode());
        item.setSceneName(StringUtils.hasText(row.getSceneName()) ? row.getSceneName() : fallback(row.getSceneCode()));
        item.setPercentage(rate(item.getTotalCount(), totalCount));
        return item;
    }

    private StatisticsUnitItemVO toUnitItem(StatisticsUnitRow row, long totalCount) {
        StatisticsUnitItemVO item = new StatisticsUnitItemVO();
        fillSummary(item, row);
        item.setUnitId(row.getUnitId());
        item.setUnitName(UNKNOWN_TEXT);
        item.setPercentage(rate(item.getTotalCount(), totalCount));
        return item;
    }

    private StatisticsTemplateItemVO toTemplateItem(StatisticsTemplateRow row, long totalCount) {
        StatisticsTemplateItemVO item = new StatisticsTemplateItemVO();
        long usageCount = safe(row.getTotalCount());
        item.setTemplateId(row.getTemplateId());
        item.setTemplateName(StringUtils.hasText(row.getTemplateName()) ? row.getTemplateName() : UNKNOWN_TEXT);
        item.setSceneId(row.getSceneId());
        item.setSceneCode(row.getSceneCode());
        item.setSceneName(StringUtils.hasText(row.getSceneName()) ? row.getSceneName() : fallback(row.getSceneCode()));
        item.setChannelType(row.getChannelType());
        item.setChannelTypeDesc(channelTypeDesc(row.getChannelType()));
        item.setUsageCount(usageCount);
        item.setSuccessCount(safe(row.getSuccessCount()));
        item.setFailedCount(safe(row.getFailedCount()));
        item.setSuccessRate(rate(item.getSuccessCount(), usageCount));
        item.setPercentage(rate(usageCount, totalCount));
        return item;
    }

    private String channelTypeDesc(String channelType) {
        if (!StringUtils.hasText(channelType)) {
            return UNKNOWN_TEXT;
        }
        try {
            return ChannelType.fromCode(channelType).getDesc();
        } catch (IllegalArgumentException ex) {
            return channelType;
        }
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN_TEXT;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0L || numerator <= 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal value = BigDecimal.valueOf(numerator)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        if (value.compareTo(HUNDRED) > 0) {
            return HUNDRED.setScale(2, RoundingMode.HALF_UP);
        }
        return value;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private boolean exportAll(MessageStatisticsExportQueryDTO query) {
        try {
            return StatisticsExportScope.parse(query.getScope()) == StatisticsExportScope.ALL;
        } catch (IllegalArgumentException ex) {
            throw paramError(ex.getMessage());
        }
    }

    private void copyCommon(MessageStatisticsExportQueryDTO source,
                            MessageStatisticsQueryDTO target,
                            boolean ignoreFilters) {
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        if (!ignoreFilters) {
            target.setChannelTypes(source.getChannelTypes());
            target.setSceneIds(source.getSceneIds());
            target.setUnitIds(source.getUnitIds());
            target.setIncludeSubUnits(source.getIncludeSubUnits());
            target.setTemplateIds(source.getTemplateIds());
            target.setCallTypes(source.getCallTypes());
        }
    }

    private BizException paramError(String message) {
        return new BizException(ErrorCode.PARAM_ERROR, message);
    }
}
