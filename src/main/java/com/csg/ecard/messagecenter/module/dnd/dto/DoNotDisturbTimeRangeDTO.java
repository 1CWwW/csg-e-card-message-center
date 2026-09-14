package com.csg.ecard.messagecenter.module.dnd.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 每日免打扰时间段。
 */
@Getter
@Setter
@Schema(description = "每日免打扰时间段，开始时间包含、结束时间不包含，允许跨天")
public class DoNotDisturbTimeRangeDTO {

    @NotNull(message = "免打扰开始时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "开始时间", example = "22:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime startTime;

    @NotNull(message = "免打扰结束时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "结束时间，早于开始时间表示跨天", example = "08:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime endTime;
}
