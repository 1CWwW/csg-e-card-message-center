package com.csg.ecard.messagecenter.common.page;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;

/**
 * 通用分页请求对象。
 * <p>
 * 统一承载分页、关键字和排序参数，读取页码和页大小时会自动修正非法值。
 */
@Setter
@Schema(description = "分页请求")
public class PageRequest {

    @Schema(description = "页码", example = "1")
    private Long pageNo = MessageCenterConstants.DEFAULT_PAGE_NO;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = MessageCenterConstants.DEFAULT_PAGE_SIZE;

    @Schema(description = "查询关键字")
    private String keyword;

    @Schema(description = "排序字段")
    private String orderBy;

    @Schema(description = "是否升序")
    private Boolean asc = Boolean.TRUE;

    public Long getPageNo() {
        if (pageNo == null || pageNo < 1) {
            return MessageCenterConstants.DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    public Long getPageSize() {
        if (pageSize == null || pageSize < 1 || pageSize > MessageCenterConstants.MAX_PAGE_SIZE) {
            return MessageCenterConstants.DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public Boolean getAsc() {
        return asc == null ? Boolean.TRUE : asc;
    }

    /**
     * 修正当前对象中的分页参数，便于后续业务复用同一个实例。
     *
     * @return 当前分页请求对象
     */
    public PageRequest normalize() {
        this.pageNo = getPageNo();
        this.pageSize = getPageSize();
        this.asc = getAsc();
        return this;
    }

    /**
     * 计算数据库查询偏移量。
     *
     * @return 从 0 开始的分页偏移量
     */
    public long offset() {
        return (getPageNo() - 1) * getPageSize();
    }
}
