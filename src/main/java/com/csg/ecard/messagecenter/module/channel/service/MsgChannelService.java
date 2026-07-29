package com.csg.ecard.messagecenter.module.channel.service;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelCreateDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelPageQueryDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelUpdateDTO;
import com.csg.ecard.messagecenter.module.channel.vo.ChannelOverviewVO;
import com.csg.ecard.messagecenter.module.channel.vo.MsgChannelVO;

/**
 * 消息渠道管理服务。
 */
public interface MsgChannelService {

    /**
     * 查询渠道概览。
     *
     * @return 渠道概览
     */
    ChannelOverviewVO overview();

    PageResult<MsgChannelVO> page(ChannelPageQueryDTO query);

    MsgChannelVO detail(Long id);

    MsgChannelVO create(ChannelCreateDTO request);

    MsgChannelVO update(Long id, ChannelUpdateDTO request);

    void delete(Long id);

    MsgChannelVO toggle(Long id);
}
