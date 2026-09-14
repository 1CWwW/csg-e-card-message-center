package com.csg.ecard.messagecenter.module.dnd.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 免打扰规则分页查询条件。
 */
@Getter
@Setter
@Schema(description = "免打扰规则分页查询条件")
public class DoNotDisturbPageQueryDTO {

    @NotNull(message = "pageNum不能为空")
    @Min(value = 1, message = "pageNum必须从1开始")
    private Integer pageNum = 1;

    @NotNull(message = "pageSize不能为空")
    @Min(value = 1, message = "pageSize必须大于0")
    @Max(value = 100, message = "pageSize不能超过100")
    private Integer pageSize = 20;

    @Schema(description = "作用范围：GLOBAL、UNIT、USER")
    private String scopeType;

    @Schema(description = "单位名称、单位ID、eLinkId或备注关键字")
    private String keyword;

    @Schema(description = "启停状态，1启用、0停用")
    private Integer status;
}
