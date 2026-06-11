package com.csg.ecard.messagecenter.common.page;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用分页请求对象。
 * <p>
 * 用于 Controller 入参承载分页参数，并通过 Jakarta Validation 限制页码和页大小范围。
 */
@Getter
@Setter
@Schema(description = "分页请求")
public class PageRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    @Schema(description = "页码", example = "1")
    private long pageNo = 1;

    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 500, message = "每页条数不能超过500")
    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;

    /**
     * 计算数据库查询偏移量。
     *
     * @return 从 0 开始的分页偏移量
     */
    public long offset() {
        return (pageNo - 1) * pageSize;
    }
}
