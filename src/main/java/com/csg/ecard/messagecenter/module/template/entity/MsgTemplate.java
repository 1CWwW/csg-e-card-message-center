package com.csg.ecard.messagecenter.module.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息模板实体。
 */
@Getter
@Setter
@TableName("msg_template")
public class MsgTemplate extends BaseEntity {

    private String templateName;

    private Long sceneId;

    private String channelType;

    private String blocklyJson;

    private Integer status;
}
