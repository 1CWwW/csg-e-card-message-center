package com.csg.ecard.messagecenter.module.dnd.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.ecard.messagecenter.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息免打扰规则实体。
 */
@Getter
@Setter
@TableName("msg_do_not_disturb_rule")
public class MsgDoNotDisturbRule extends BaseEntity {

    private String scopeType;
    private String scopeId;
    private Integer includeSubUnits;
    private String timeRanges;
    private Integer status;
    private String remark;
}
