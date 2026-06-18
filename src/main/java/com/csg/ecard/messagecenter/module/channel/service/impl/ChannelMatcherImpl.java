package com.csg.ecard.messagecenter.module.channel.service.impl;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.channel.service.ChannelMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 默认渠道匹配实现。
 */
@Service
@RequiredArgsConstructor
public class ChannelMatcherImpl implements ChannelMatcher {

    private final MsgChannelMapper msgChannelMapper;

    @Override
    public Optional<MsgChannel> match(String channelType, List<String> unitPath) {
        ChannelType type = ChannelType.fromCode(channelType);
        if (unitPath == null || unitPath.isEmpty()) {
            return Optional.empty();
        }
        for (String unitId : unitPath) {
            if (!StringUtils.hasText(unitId)) {
                continue;
            }
            List<MsgChannel> candidates = msgChannelMapper.selectEnabledCandidates(type.getCode(), unitId.trim());
            if (candidates != null && !candidates.isEmpty()) {
                return Optional.of(candidates.get(0));
            }
        }
        return Optional.empty();
    }
}
