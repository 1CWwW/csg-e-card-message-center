package com.csg.ecard.messagecenter.module.channel.service;

import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;

import java.util.List;
import java.util.Optional;

/**
 * 渠道匹配器。
 */
public interface ChannelMatcher {

    /**
     * 按单位路径匹配可用渠道。
     *
     * @param channelType 渠道类型
     * @param unitPath    单位路径，顺序为当前单位到根单位
     * @return 命中的渠道
     */
    Optional<MsgChannel> match(String channelType, List<String> unitPath);
}
