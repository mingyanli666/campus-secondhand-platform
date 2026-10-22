package com.digital.platform.common.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.constant.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基类。
 * <p>
 * 所有需要分页的查询都继承它，好处是：
 * 1. 参数名统一（前端永远传 current / size）
 * 2. 页码、条数的边界处理只写一次
 */
@Data
@Schema(description = "分页参数")
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码，从 1 开始")
    private Long current = Constants.DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数")
    private Long size = Constants.DEFAULT_PAGE_SIZE;

    /**
     * 转换成 MyBatis-Plus 的分页对象。
     * <p>
     * 这里做边界修正非常重要：如果不限制 size，前端传个 size=100000
     * 就能一次性把整张表拉出来，数据库直接被打爆。这叫"分页攻击"。
     */
    public <T> Page<T> toPage() {
        long pageNum = (current == null || current < 1) ? Constants.DEFAULT_PAGE_NUM : current;
        long pageSize = (size == null || size < 1) ? Constants.DEFAULT_PAGE_SIZE : size;
        pageSize = Math.min(pageSize, Constants.MAX_PAGE_SIZE);
        return new Page<>(pageNum, pageSize);
    }
}
