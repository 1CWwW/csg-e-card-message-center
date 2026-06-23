package com.csg.ecard.messagecenter.module.push.vo;

import com.csg.ecard.messagecenter.module.push.enums.PushStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步消息推送响应。
 */
@Getter
@Setter
@Schema(description = "同步消息推送响应")
public class SyncPushVO {

    private String msgId;
    private PushStatus status;
    private List<ChannelResultVO> channelResults = new ArrayList<>();
}
