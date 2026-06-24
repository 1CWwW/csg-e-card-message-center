package com.csg.ecard.messagecenter.module.record.vo;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 消息记录分页结果，补充本接口契约要求的页码和页大小。
 */
@Schema(description = "消息记录分页结果")
public class MessageRecordPageResult extends PageResult<MessageRecordListVO> {

    public MessageRecordPageResult(List<MessageRecordListVO> list,
                                   long total,
                                   long pageNum,
                                   long pageSize) {
        super(list, total, pageNum, pageSize,
                pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize);
    }

    @Override
    @JsonIgnore(false)
    @JsonProperty("pageNum")
    @Schema(description = "页码", type = "integer", format = "int64")
    public long getPageNo() {
        return super.getPageNo();
    }

    @Override
    @JsonIgnore(false)
    @JsonProperty("pageSize")
    @Schema(description = "每页条数", type = "integer", format = "int64")
    public long getPageSize() {
        return super.getPageSize();
    }
}
