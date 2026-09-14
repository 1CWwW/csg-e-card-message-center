package com.csg.ecard.messagecenter.module.dnd.vo;

import com.csg.ecard.messagecenter.common.serialization.JsonLongId;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbTimeRangeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 免打扰规则响应。
 */
@Getter
@Setter
@Schema(description = "免打扰规则响应")
public class DoNotDisturbRuleVO {

    @JsonLongId
    @Schema(description = "规则ID", type = "string")
    private Long id;
    private String scopeType;
    private String scopeTypeDesc;
    private String scopeId;
    private Boolean includeSubUnits;
    private List<DoNotDisturbTimeRangeDTO> timeRanges;
    private Integer status;
    private String statusDesc;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
