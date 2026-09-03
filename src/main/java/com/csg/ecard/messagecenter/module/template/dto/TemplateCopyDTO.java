package com.csg.ecard.messagecenter.module.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 复制模板请求。
 */
@Getter
@Setter
@Schema(description = "复制模板请求")
public class TemplateCopyDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 50, message = "模板名称长度不能超过50")
    private String templateName;

    @NotNull(message = "场景ID不能为空")
    @Schema(type = "string")
    private Long sceneId;

    @NotBlank(message = "渠道类型不能为空")
    @Schema(description = "目标渠道类型",
            allowableValues = {"SMS", "EMAIL", "ELINK", "IN_APP"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelType;

    @NotNull(message = "copyContent不能为空")
    private Boolean copyContent;

    @Schema(description = "适用单位ID列表，空数组表示全量适用")
    private List<String> unitIds;
}
