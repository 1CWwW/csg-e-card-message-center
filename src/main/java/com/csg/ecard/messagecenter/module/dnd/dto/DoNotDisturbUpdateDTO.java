package com.csg.ecard.messagecenter.module.dnd.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 编辑免打扰规则请求。
 */
@Getter
@Setter
@Schema(description = "编辑免打扰规则请求")
public class DoNotDisturbUpdateDTO {

    @Schema(description = "单位规则是否包含下级单位；仅UNIT有效")
    private Boolean includeSubUnits;

    @Valid
    @NotEmpty(message = "免打扰时间段不能为空")
    private List<DoNotDisturbTimeRangeDTO> timeRanges;

    @NotNull(message = "status不能为空")
    @Schema(description = "启停状态，1启用、0停用", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"0", "1"})
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
