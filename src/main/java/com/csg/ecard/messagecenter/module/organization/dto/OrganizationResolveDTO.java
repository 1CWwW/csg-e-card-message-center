package com.csg.ecard.messagecenter.module.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量解析已选组织请求。
 */
@Getter
@Setter
@Schema(description = "批量解析已选组织请求")
public class OrganizationResolveDTO {

    @NotEmpty(message = "orgIds不能为空")
    @Schema(description = "组织ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotBlank(message = "组织ID不能为空") String> orgIds;
}
