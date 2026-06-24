package com.csg.ecard.messagecenter.module.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息记录分页查询条件。
 */
@Getter
@Setter
@Schema(description = "消息记录分页查询条件")
public class MessageRecordPageQueryDTO extends MessageRecordFilterDTO {

    @Min(value = 1, message = "pageNum必须从1开始")
    @Schema(description = "页码，从1开始", defaultValue = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize必须大于0")
    @Max(value = 100, message = "pageSize不能超过100")
    @Schema(description = "每页条数，默认10，最大100", defaultValue = "10")
    private Integer pageSize = 10;
}
