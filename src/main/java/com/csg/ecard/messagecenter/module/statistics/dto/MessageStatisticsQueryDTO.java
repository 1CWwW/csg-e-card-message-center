package com.csg.ecard.messagecenter.module.statistics.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息统计公共查询条件。
 */
@Getter
@Setter
@Schema(description = "消息统计公共查询条件")
public class MessageStatisticsQueryDTO {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际发送时间起点，格式 yyyy-MM-dd HH:mm:ss，包含边界；为空表示不限制起点")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际发送时间终点，格式 yyyy-MM-dd HH:mm:ss，包含边界；为空表示不限制终点，仅传startTime时默认到当前时间")
    private LocalDateTime endTime;

    @Schema(description = "渠道类型：SMS、EMAIL、ELINK、IN_APP；为空表示不筛选")
    private List<String> channelTypes;

    @Schema(description = "场景ID，按字符串传递；为空表示不筛选")
    private List<Long> sceneIds;

    @Schema(description = "单位ID，精确匹配msg_record.user_org_id；为空表示不筛选")
    private List<String> unitIds;

    @Schema(description = "是否包含所选单位的全部下级单位；默认false")
    private Boolean includeSubUnits;

    @ArraySchema(arraySchema = @Schema(description = "模板ID数组；为空表示不筛选"),
            schema = @Schema(type = "string", description = "模板ID，按字符串传递"))
    private List<Long> templateIds;

    @Schema(description = "消息原始调用方式：SYNC、ASYNC；为空表示不筛选")
    private List<String> callTypes;
}
