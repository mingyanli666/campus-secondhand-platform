package com.digital.platform.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.request.PageQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页参数边界处理的测试。
 * <p>
 * 这类"看起来只是几个 if"的代码，恰恰是安全防线：
 * <ul>
 *   <li>{@code size=99999} 不拦住，一次请求就把整张表读进 JVM → OOM</li>
 *   <li>{@code current=-3} 不修正，会生成 {@code LIMIT -30, 10} 这种非法 SQL</li>
 * </ul>
 * 不需要 Spring 上下文，纯逻辑测试，跑得极快。
 */
@DisplayName("分页参数 - 边界处理")
class PageQueryTest {

    @Test
    @DisplayName("size 被强制限制在 100，防止一次拉取整张表")
    void toPage_caps_size_and_fixes_invalid_page_number() {
        PageQuery query = new PageQuery();

        // 恶意/手滑的超大分页
        query.setSize(99999L);
        query.setCurrent(-3L);
        Page<Object> page = query.toPage();
        assertThat(page.getSize())
                .as("size 上限")
                .isEqualTo(100L);
        assertThat(page.getCurrent())
                .as("非法页码应归到第 1 页")
                .isEqualTo(1L);

        // 正常值应当原样保留，不能"一刀切"全改成默认值
        query.setSize(20L);
        query.setCurrent(3L);
        Page<Object> normal = query.toPage();
        assertThat(normal.getSize()).isEqualTo(20L);
        assertThat(normal.getCurrent()).isEqualTo(3L);
    }
}
