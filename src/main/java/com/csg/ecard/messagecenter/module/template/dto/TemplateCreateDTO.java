package com.csg.ecard.messagecenter.module.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 新增模板请求。
 */
@Getter
@Setter
@Schema(description = "新增模板请求")
public class TemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 50, message = "模板名称长度不能超过50")
    private String templateName;

    @NotNull(message = "场景ID不能为空")
    @Schema(description = "场景ID，传递场景筛选项的value，不传递sceneCode",
            type = "string", example = "2085629226374467586")
    private Long sceneId;

    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    @Schema(description = "适用单位ID列表，空数组表示全量适用")
    private List<String> unitIds;
}
