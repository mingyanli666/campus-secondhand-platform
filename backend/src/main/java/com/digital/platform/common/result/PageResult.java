package com.digital.platform.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页结果包装。与 MyBatis-Plus 的 IPage 解耦，避免前端拿到一堆无关字段。
 *
 * @param <T> 列表元素类型
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页条数")
    private Long size;

    @Schema(description = "总页数")
    private Long pages;

    public PageResult() {
        this.records = Collections.emptyList();
        this.total = 0L;
        this.current = 1L;
        this.size = 10L;
        this.pages = 0L;
    }

    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total == null ? 0L : total;
        this.current = current == null ? 1L : current;
        this.size = size == null || size == 0 ? 10L : size;
        this.pages = (this.total + this.size - 1) / this.size;
    }

    /**
     * 直接由 MyBatis-Plus 分页对象构建。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 由分页对象构建，并把实体转换成 VO。
     *
     * @param page      分页对象
     * @param converter 实体 -> VO 的转换函数
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> list = page.getRecords().stream().map(converter).toList();
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }
}
