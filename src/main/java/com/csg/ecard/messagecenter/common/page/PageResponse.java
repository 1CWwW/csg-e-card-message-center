package com.csg.ecard.messagecenter.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 通用分页响应对象。
 * <p>
 * 保留该类用于兼容已有代码，新代码优先使用 {@link PageResult}。
 *
 * @param <T> 分页记录类型
 */
@Schema(description = "分页响应")
public class PageResponse<T> extends PageResult<T> {

    public PageResponse() {
        super();
    }

    public PageResponse(List<T> list, long total, long pageNo, long pageSize) {
        super(list, total, pageNo, pageSize, pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize);
    }

    /**
     * 根据列表和分页元数据构造分页响应。
     *
     * @param list     当前页记录
     * @param total    总记录数
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param <T>      分页记录类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> list, long total, long pageNo, long pageSize) {
        return new PageResponse<>(list, total, pageNo, pageSize);
    }

    /**
     * 从 MyBatis-Plus 分页对象转换为分页响应。
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  分页记录类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(IPage<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setList(page.getRecords());
        response.setTotal(page.getTotal());
        response.setPageNo(page.getCurrent());
        response.setPageSize(page.getSize());
        response.setPages(page.getPages());
        return response;
    }
}
