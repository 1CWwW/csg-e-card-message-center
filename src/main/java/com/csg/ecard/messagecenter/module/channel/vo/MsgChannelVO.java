package com.csg.ecard.messagecenter.module.channel.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelTypeConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道响应结果。
 */
@Getter
@Setter
@Schema(description = "渠道响应结果")
public class MsgChannelVO {

    @JsonLongId
    @Schema(description = "主键ID", type = "string")
    private Long id;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "渠道类型")
    private String channelType;

    @Schema(description = "渠道类型名称")
    private String channelTypeDesc;

    @Schema(description = "渠道类型配置")
    private ChannelTypeConfigDTO typeConfig;

    @Schema(description = "渠道类型配置摘要")
    private String typeConfigSummary;

    @Schema(description = "适用单位数量", type = "integer", format = "int64")
    private Long unitCount;

    @Schema(description = "唯一渠道影响单位数量", type = "integer", format = "int64")
    private Long uniqueUnitCount;

    @Schema(description = "优先级", type = "integer", format = "int32")
    private Integer priority;

    @Schema(description = "启停状态", type = "integer", format = "int32")
    private Integer status;

    @Schema(description = "启停状态名称")
    private String statusDesc;

    @Schema(description = "适用单位ID列表")
    private List<String> unitIds;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
