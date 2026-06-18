package com.csg.ecard.messagecenter.module.channel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息渠道实体。
 */
@Getter
@Setter
@TableName("msg_channel")
@Schema(description = "消息渠道")
public class MsgChannel extends BaseEntity {

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "渠道类型")
    private String channelType;

    @Schema(description = "渠道类型配置JSON")
    private String typeConfig;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "启停状态")
    private Integer status;
}
