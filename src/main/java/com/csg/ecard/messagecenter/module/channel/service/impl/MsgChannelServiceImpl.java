package com.csg.ecard.messagecenter.module.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.DeleteFlag;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelCreateDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelPageQueryDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelTypeConfigDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelUpdateDTO;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannelUnit;
import com.csg.ecard.messagecenter.module.channel.mapper.ChannelUnitCountResult;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelUnitMapper;
import com.csg.ecard.messagecenter.module.channel.service.MsgChannelService;
import com.csg.ecard.messagecenter.module.channel.validator.ChannelTypeConfigValidator;
import com.csg.ecard.messagecenter.module.channel.vo.MsgChannelVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息渠道管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class MsgChannelServiceImpl implements MsgChannelService {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "渠道不存在";
    private static final String CHANNEL_NAME_DUPLICATE_MESSAGE = "渠道名称已存在";

    private final MsgChannelMapper msgChannelMapper;
    private final MsgChannelUnitMapper msgChannelUnitMapper;
    private final ChannelTypeConfigValidator channelTypeConfigValidator;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<MsgChannelVO> page(ChannelPageQueryDTO query) {
        validatePageQuery(query);
        validateOptionalChannelType(query.getChannelType());
        validateOptionalStatus(query.getStatus());
        trimQuery(query);

        Page<MsgChannel> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<MsgChannel> result = msgChannelMapper.selectChannelPage(page, query);
        List<Long> channelIds = result.getRecords().stream().map(MsgChannel::getId).toList();
        Map<Long, Long> unitCountMap = countUnitsByChannelIds(channelIds);
        List<MsgChannelVO> list = result.getRecords().stream()
                .map(channel -> toVO(channel,
                        unitCountMap.getOrDefault(channel.getId(), 0L),
                        null,
                        null,
                        null))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MsgChannelVO detail(Long id) {
        MsgChannel channel = requireChannel(id);
        List<String> unitIds = msgChannelUnitMapper.selectUnitIdsByChannelId(id);
        Long unitCount = unitIds == null ? 0L : (long) unitIds.size();
        Long uniqueUnitCount = resolveUniqueUnitCount(channel);
        return toVO(channel, unitCount, unitIds, uniqueUnitCount, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgChannelVO create(ChannelCreateDTO request) {
        validateChannelName(request.getChannelName());
        ChannelType channelType = validateChannelType(request.getChannelType());
        validatePriority(request.getPriority());
        Integer status = resolveCreateStatus(request.getStatus());
        List<String> unitIds = normalizeUnitIds(request.getUnitIds());
        ensureChannelNameUnique(request.getChannelName(), null);
        String typeConfig = serializeTypeConfig(channelType, request.getTypeConfig());

        MsgChannel channel = new MsgChannel();
        channel.setChannelName(request.getChannelName());
        channel.setChannelType(channelType.getCode());
        channel.setTypeConfig(typeConfig);
        channel.setPriority(request.getPriority());
        channel.setStatus(status);
        msgChannelMapper.insert(channel);
        saveChannelUnits(channel.getId(), unitIds);
        return toVO(channel, (long) unitIds.size(), unitIds, resolveUniqueUnitCount(channel), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgChannelVO update(Long id, ChannelUpdateDTO request) {
        MsgChannel existed = requireChannel(id);
        validateChannelName(request.getChannelName());
        validatePriority(request.getPriority());
        validateStatus(request.getStatus());
        List<String> unitIds = normalizeUnitIds(request.getUnitIds());
        ensureChannelNameUnique(request.getChannelName(), id);
        ChannelType channelType = ChannelType.fromCode(existed.getChannelType());
        String typeConfig = serializeTypeConfig(channelType, request.getTypeConfig());

        MsgChannel channel = new MsgChannel();
        channel.setId(id);
        channel.setChannelName(request.getChannelName());
        channel.setTypeConfig(typeConfig);
        channel.setPriority(request.getPriority());
        channel.setStatus(request.getStatus());
        msgChannelMapper.updateById(channel);

        msgChannelUnitMapper.deleteByChannelId(id);
        saveChannelUnits(id, unitIds);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireChannel(id);
        msgChannelUnitMapper.deleteByChannelId(id);
        msgChannelMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgChannelVO toggle(Long id) {
        MsgChannel existed = requireChannel(id);
        Integer nextStatus = CommonStatus.ENABLE.getCode().equals(existed.getStatus())
                ? CommonStatus.DISABLE.getCode()
                : CommonStatus.ENABLE.getCode();

        MsgChannel update = new MsgChannel();
        update.setId(id);
        update.setStatus(nextStatus);
        msgChannelMapper.updateById(update);
        existed.setStatus(nextStatus);
        List<String> unitIds = msgChannelUnitMapper.selectUnitIdsByChannelId(id);
        return toVO(existed,
                unitIds == null ? 0L : (long) unitIds.size(),
                unitIds,
                resolveUniqueUnitCount(existed),
                null);
    }

    private MsgChannel requireChannel(Long id) {
        MsgChannel channel = msgChannelMapper.selectById(id);
        if (channel == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, CHANNEL_NOT_FOUND_MESSAGE);
        }
        return channel;
    }

    private void saveChannelUnits(Long channelId, List<String> unitIds) {
        for (String unitId : unitIds) {
            MsgChannelUnit channelUnit = new MsgChannelUnit();
            channelUnit.setChannelId(channelId);
            channelUnit.setUnitId(unitId);
            msgChannelUnitMapper.insert(channelUnit);
        }
    }

    private void ensureChannelNameUnique(String channelName, Long excludeId) {
        LambdaQueryWrapper<MsgChannel> wrapper = new LambdaQueryWrapper<MsgChannel>()
                .eq(MsgChannel::getChannelName, channelName)
                .eq(MsgChannel::getDeleted, DeleteFlag.NORMAL.getCode())
                .ne(excludeId != null, MsgChannel::getId, excludeId);
        Long count = msgChannelMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, CHANNEL_NAME_DUPLICATE_MESSAGE);
        }
    }

    private ChannelType validateChannelType(String channelType) {
        if (!StringUtils.hasText(channelType)) {
            throw new BizException(ErrorCode.CHANNEL_TYPE_INVALID, "渠道类型不能为空");
        }
        try {
            return ChannelType.fromCode(channelType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.CHANNEL_TYPE_INVALID, "渠道类型不合法");
        }
    }

    private void validateOptionalChannelType(String channelType) {
        if (StringUtils.hasText(channelType)) {
            validateChannelType(channelType);
        }
    }

    private void validateChannelName(String channelName) {
        if (!StringUtils.hasText(channelName) || channelName.length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR, "渠道名称不能为空且长度不能超过50");
        }
    }

    private void validatePriority(Integer priority) {
        if (priority == null || priority < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "优先级必须为正整数");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "启停状态不能为空");
        }
        try {
            CommonStatus.fromCode(status);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "启停状态不合法");
        }
    }

    private void validateOptionalStatus(Integer status) {
        if (status != null) {
            validateStatus(status);
        }
    }

    private Integer resolveCreateStatus(Integer status) {
        if (status == null) {
            return CommonStatus.ENABLE.getCode();
        }
        validateStatus(status);
        return status;
    }

    private List<String> normalizeUnitIds(List<String> unitIds) {
        if (unitIds == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "适用单位不能为空");
        }
        Set<String> normalized = unitIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "适用单位不能为空");
        }
        return List.copyOf(normalized);
    }

    private void validatePageQuery(ChannelPageQueryDTO query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageNum不能为空且必须从1开始");
        }
        if (query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize不能为空且不能超过100");
        }
    }

    private void trimQuery(ChannelPageQueryDTO query) {
        query.setChannelName(trimToNull(query.getChannelName()));
        query.setChannelType(trimToNull(query.getChannelType()));
        query.setUnitId(trimToNull(query.getUnitId()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String serializeTypeConfig(ChannelType channelType, ChannelTypeConfigDTO typeConfig) {
        Map<String, String> normalized = channelTypeConfigValidator.validateAndNormalize(channelType, typeConfig);
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "渠道类型配置序列化失败");
        }
    }

    private ChannelTypeConfigDTO parseTypeConfig(String typeConfig) {
        if (!StringUtils.hasText(typeConfig)) {
            return new ChannelTypeConfigDTO();
        }
        try {
            Map<String, String> configMap = objectMapper.readValue(typeConfig, new TypeReference<>() {
            });
            ChannelTypeConfigDTO config = new ChannelTypeConfigDTO();
            config.setSenderNumber(configMap.get("senderNumber"));
            config.setSenderEmail(configMap.get("senderEmail"));
            config.setAppId(configMap.get("appId"));
            return config;
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "渠道类型配置反序列化失败");
        }
    }

    private Map<Long, Long> countUnitsByChannelIds(List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return msgChannelUnitMapper.selectUnitCountsByChannelIds(channelIds).stream()
                .collect(Collectors.toMap(ChannelUnitCountResult::getChannelId,
                        ChannelUnitCountResult::getUnitCount,
                        Long::sum));
    }

    private Long resolveUniqueUnitCount(MsgChannel channel) {
        if (channel == null || channel.getId() == null) {
            return 0L;
        }
        Long count = msgChannelUnitMapper.countUniqueEnabledUnits(channel.getId(), channel.getChannelType());
        return count == null ? 0L : count;
    }

    private MsgChannelVO toVO(MsgChannel channel,
                              Long unitCount,
                              List<String> unitIds,
                              Long uniqueUnitCount,
                              ChannelTypeConfigDTO parsedTypeConfig) {
        MsgChannelVO vo = new MsgChannelVO();
        vo.setId(channel.getId());
        vo.setChannelName(channel.getChannelName());
        vo.setChannelType(channel.getChannelType());
        vo.setChannelTypeDesc(resolveChannelTypeDesc(channel.getChannelType()));
        ChannelTypeConfigDTO typeConfig = parsedTypeConfig == null ? parseTypeConfig(channel.getTypeConfig()) : parsedTypeConfig;
        vo.setTypeConfig(typeConfig);
        vo.setTypeConfigSummary(resolveTypeConfigSummary(channel.getChannelType(), typeConfig));
        vo.setUnitCount(unitCount == null ? 0L : unitCount);
        vo.setUniqueUnitCount(uniqueUnitCount);
        vo.setPriority(channel.getPriority());
        vo.setStatus(channel.getStatus());
        vo.setStatusDesc(resolveStatusDesc(channel.getStatus()));
        vo.setUnitIds(unitIds);
        vo.setCreatedAt(channel.getCreateTime());
        vo.setUpdatedAt(channel.getUpdateTime());
        return vo;
    }

    private String resolveChannelTypeDesc(String channelType) {
        return StringUtils.hasText(channelType) ? ChannelType.fromCode(channelType).getDesc() : null;
    }

    private String resolveStatusDesc(Integer status) {
        return status == null ? null : CommonStatus.fromCode(status).getDesc();
    }

    private String resolveTypeConfigSummary(String channelType, ChannelTypeConfigDTO config) {
        ChannelType type = ChannelType.fromCode(channelType);
        return switch (type) {
            case SMS -> config.getSenderNumber();
            case EMAIL -> config.getSenderEmail();
            case ELINK -> config.getAppId();
            case IN_APP -> "无额外配置";
        };
    }
}
