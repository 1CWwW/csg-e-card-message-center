package com.csg.ecard.messagecenter.module.statistics.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 按模板聚合统计行。
 */
@Getter
@Setter
public class StatisticsTemplateRow extends StatisticsCountRow {

    private Long templateId;
    private String templateName;
    private Long sceneId;
    private String sceneCode;
    private String sceneName;
    private String channelType;
}
