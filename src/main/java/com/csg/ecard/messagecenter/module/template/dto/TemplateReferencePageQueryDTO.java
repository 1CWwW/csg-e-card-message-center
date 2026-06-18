package com.csg.ecard.messagecenter.module.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 参考模板分页查询条件。
 */
@Getter
@Setter
@Schema(description = "参考模板分页查询条件")
public class TemplateReferencePageQueryDTO {

    private String channelType;

    @Schema(type = "string")
    private Long sceneId;

    @Schema(description = "内容状态，0全部，1有有效内容，2无有效内容", example = "1")
    private Integer contentStatus = 1;

    @NotNull(message = "pageNum不能为空")
    @Min(value = 1, message = "pageNum必须从1开始")
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    @Min(value = 1, message = "pageSize必须大于0")
    @Max(value = 100, message = "pageSize不能超过100")
    private Integer pageSize = 20;

    @Schema(type = "string")
    private Long excludeTemplateId;
}
