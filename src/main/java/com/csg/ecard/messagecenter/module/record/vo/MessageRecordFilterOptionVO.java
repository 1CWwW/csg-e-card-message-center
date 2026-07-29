package com.csg.ecard.messagecenter.module.record.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息记录筛选项。
 */
@Getter
@Setter
@Schema(description = "消息记录筛选项")
public class MessageRecordFilterOptionVO {

    @Schema(description = "筛选值；场景为场景编码，渠道和模板为字符串类型ID")
    private String value;

    @Schema(description = "筛选项显示名称")
    private String label;
}
