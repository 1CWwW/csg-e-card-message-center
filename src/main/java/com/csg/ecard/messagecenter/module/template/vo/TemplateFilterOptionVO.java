package com.csg.ecard.messagecenter.module.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 模板管理筛选项。
 */
@Getter
@Setter
@Schema(description = "模板管理筛选项")
public class TemplateFilterOptionVO {

    @Schema(description = "筛选值；场景筛选时为字符串类型的场景ID", type = "string",
            example = "2085629226374467586")
    private String value;

    @Schema(description = "筛选项显示名称；场景筛选格式为“场景编码 - 场景名称”",
            example = "A_01 - qqq")
    private String label;

    @Schema(description = "场景编码，仅场景筛选项返回", example = "A_01")
    private String sceneCode;

    @Schema(description = "场景名称，仅场景筛选项返回", example = "qqq")
    private String sceneName;

    @Schema(description = "启停状态：1启用、0停用", example = "1")
    private Integer status;
}
