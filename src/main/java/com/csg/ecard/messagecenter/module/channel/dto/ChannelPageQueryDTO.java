package com.csg.ecard.messagecenter.module.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 渠道分页查询条件。
 */
@Getter
@Setter
@Schema(description = "渠道分页查询条件")
public class ChannelPageQueryDTO {

    @NotNull(message = "pageNum不能为空")
    @Min(value = 1, message = "pageNum必须从1开始")
    @Schema(description = "页码，从1开始", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageNum;

    @NotNull(message = "pageSize不能为空")
    @Min(value = 1, message = "pageSize必须大于0")
    @Max(value = 100, message = "pageSize不能超过100")
    @Schema(description = "每页条数，默认20，最大100", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageSize = 20;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "渠道类型")
    private String channelType;

    @Schema(description = "启停状态，1启用，0停用", type = "integer", format = "int32")
    private Integer status;

    @Schema(description = "适用单位ID")
    private String unitId;
}
