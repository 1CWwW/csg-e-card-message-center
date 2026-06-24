package com.csg.ecard.messagecenter.module.record.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息记录场景参数展示项。
 */
@Getter
@Setter
@Schema(description = "消息记录场景参数展示项")
public class MessageRecordSceneParamItemVO {

    private String paramName;
    private String paramLabel;
    private String paramType;
    private String paramTypeDesc;

    @Schema(description = "动态JSON参数原值")
    private Object value;

    private String valueText;
}
