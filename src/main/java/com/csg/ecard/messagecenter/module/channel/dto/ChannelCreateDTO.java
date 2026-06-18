package com.csg.ecard.messagecenter.module.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 新增渠道请求。
 */
@Getter
@Setter
@Schema(description = "新增渠道请求")
public class ChannelCreateDTO {

    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 50, message = "渠道名称长度不能超过50")
    @Schema(description = "渠道名称")
    private String channelName;

    @NotBlank(message = "渠道类型不能为空")
    @Schema(description = "渠道类型")
    private String channelType;

    @Valid
    @Schema(description = "渠道类型配置")
    private ChannelTypeConfigDTO typeConfig;

    @NotNull(message = "优先级不能为空")
    @Positive(message = "优先级必须为正整数")
    @Schema(description = "优先级，数字越小优先级越高", type = "integer", format = "int32")
    private Integer priority;

    @Schema(description = "启停状态，1启用，0停用，未传默认启用", type = "integer", format = "int32")
    private Integer status;

    @Schema(description = "适用单位ID列表")
    private List<String> unitIds;
}
