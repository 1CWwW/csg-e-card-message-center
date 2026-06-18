package com.csg.ecard.messagecenter.module.channel.service;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.channel.service.impl.ChannelMatcherImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelMatcherImplTest {

    private MsgChannelMapper msgChannelMapper;
    private ChannelMatcherImpl channelMatcher;

    @BeforeEach
    void setUp() {
        msgChannelMapper = mock(MsgChannelMapper.class);
        channelMatcher = new ChannelMatcherImpl(msgChannelMapper);
    }

    @Test
    void shouldMatchCurrentUnitFirst() {
        MsgChannel current = channel(1L);
        MsgChannel parent = channel(2L);
        when(msgChannelMapper.selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_CURRENT"))
                .thenReturn(List.of(current));
        when(msgChannelMapper.selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_PARENT"))
                .thenReturn(List.of(parent));

        Optional<MsgChannel> result = channelMatcher.match(ChannelType.SMS.getCode(),
                List.of("UNIT_CURRENT", "UNIT_PARENT"));

        assertThat(result).contains(current);
        verify(msgChannelMapper, never()).selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_PARENT");
    }

    @Test
    void shouldMatchParentWhenCurrentUnitHasNoChannel() {
        MsgChannel parent = channel(2L);
        when(msgChannelMapper.selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_CURRENT"))
                .thenReturn(List.of());
        when(msgChannelMapper.selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_PARENT"))
                .thenReturn(List.of(parent));

        Optional<MsgChannel> result = channelMatcher.match(ChannelType.SMS.getCode(),
                List.of("UNIT_CURRENT", "UNIT_PARENT"));

        assertThat(result).contains(parent);
    }

    @Test
    void shouldReturnFirstCandidateOrderedByMapper() {
        MsgChannel highPriority = channel(1L);
        MsgChannel lowPriority = channel(2L);
        when(msgChannelMapper.selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_CURRENT"))
                .thenReturn(List.of(highPriority, lowPriority));

        Optional<MsgChannel> result = channelMatcher.match(ChannelType.SMS.getCode(), List.of("UNIT_CURRENT"));

        assertThat(result).contains(highPriority);
    }

    @Test
    void shouldReturnEmptyWhenNoChannelAvailable() {
        when(msgChannelMapper.selectEnabledCandidates(ChannelType.SMS.getCode(), "UNIT_CURRENT"))
                .thenReturn(List.of());

        Optional<MsgChannel> result = channelMatcher.match(ChannelType.SMS.getCode(), List.of("UNIT_CURRENT"));

        assertThat(result).isEmpty();
    }

    private MsgChannel channel(Long id) {
        MsgChannel channel = new MsgChannel();
        channel.setId(id);
        channel.setChannelType(ChannelType.SMS.getCode());
        return channel;
    }
}
