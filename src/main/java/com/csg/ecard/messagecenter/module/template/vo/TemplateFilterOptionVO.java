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

    @Schema(description = "筛选值，Long类型ID按字符串返回")
    private String value;

    @Schema(description = "筛选项显示名称")
    private String label;

    @Schema(description = "启停状态：1启用、0停用")
    private Integer status;
}
