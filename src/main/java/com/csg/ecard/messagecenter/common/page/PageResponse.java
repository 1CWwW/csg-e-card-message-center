package com.csg.ecard.messagecenter.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应对象。
 * <p>
 * 用于统一返回分页记录、总数、页码和每页条数，可直接从 MyBatis-Plus 分页对象转换。
 *
 * @param <T> 分页记录类型
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "数据列表")
    private List<T> records = Collections.emptyList();

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "页码")
    private long pageNo;

    @Schema(description = "每页条数")
    private long pageSize;

    /**
     * 根据列表和分页元数据构造分页响应。
     *
     * @param records  当前页记录
     * @param total    总记录数
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param <T>      分页记录类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(List<T> records, long total, long pageNo, long pageSize) {
        return new PageResponse<>(records, total, pageNo, pageSize);
    }

    /**
     * 从 MyBatis-Plus 分页结果构造分页响应。
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  分页记录类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
