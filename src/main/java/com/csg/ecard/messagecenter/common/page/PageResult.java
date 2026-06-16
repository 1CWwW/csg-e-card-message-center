package com.csg.ecard.messagecenter.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果对象。
 *
 * @param <T> 分页记录类型
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> list = Collections.emptyList();

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "页码")
    @JsonIgnore
    private long pageNo;

    @Schema(description = "每页条数")
    @JsonIgnore
    private long pageSize;

    @Schema(description = "总页数")
    @JsonIgnore
    private long pages;

    /**
     * 根据列表和分页元数据构造分页结果。
     *
     * @param list     当前页记录
     * @param total    总记录数
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param <T>      分页记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, long pageNo, long pageSize) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(list == null ? Collections.emptyList() : list, total, pageNo, pageSize, pages);
    }

    /**
     * 从 MyBatis-Plus 分页对象转换为统一分页结果。
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  分页记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }
}
