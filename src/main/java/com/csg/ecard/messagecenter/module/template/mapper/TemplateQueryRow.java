package com.csg.ecard.messagecenter.module.template.mapper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模板关联查询结果。
 */
@Getter
@Setter
public class TemplateQueryRow {

    private Long id;
    private String templateName;
    private Long sceneId;
    private String sceneCode;
    private String sceneName;
    private String channelType;
    private String blocklyJson;
    private Integer status;
    private Long unitCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
