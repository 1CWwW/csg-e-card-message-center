package com.csg.ecard.messagecenter.module.dnd.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量新增免打扰规则请求。
 */
@Getter
@Setter
@Schema(description = "批量新增免打扰规则请求")
public class DoNotDisturbBatchCreateDTO {

    @NotBlank(message = "scopeType不能为空")
    @Schema(description = "作用范围：GLOBAL全局、UNIT单位、USER用户", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"GLOBAL", "UNIT", "USER"})
    private String scopeType;

    @Schema(description = "作用对象ID列表；UNIT传单位ID，USER传eLinkId，GLOBAL不传")
    private List<String> scopeIds;

    @Schema(description = "单位规则是否包含下级单位；仅UNIT有效", defaultValue = "false")
    private Boolean includeSubUnits;

    @Valid
    @NotEmpty(message = "免打扰时间段不能为空")
    @Schema(description = "每日免打扰时间段", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DoNotDisturbTimeRangeDTO> timeRanges;

    @Schema(description = "启停状态，1启用、0停用；未传默认1", allowableValues = {"0", "1"})
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
