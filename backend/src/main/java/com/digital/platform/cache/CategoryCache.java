package com.digital.platform.cache;

import com.digital.platform.common.constant.CacheKeys;
import com.digital.platform.vo.CategoryVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 分类列表缓存。
 * <p>
 * 分类是典型的<b>读多写极少</b>的字典数据：一个平台可能就十几个分类，
 * 但每一页商品、每一个发布页都要用到它。
 * <p>
 * 这类数据的策略和商品详情不同：
 * <ul>
 *   <li>TTL 可以长（1 小时），因为数据几乎不变</li>
 *   <li>不需要空值缓存 —— 空列表本身就有意义</li>
 *   <li>管理端增删改分类时<b>主动删除</b>，保证立刻生效</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryCache {

    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public List<CategoryVO> get() {
        try {
            String json = redis.opsForValue().get(CacheKeys.categoryList());
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<CategoryVO>>() {
            });
        } catch (Exception e) {
            log.warn("读取分类缓存失败，降级查库: {}", e.getMessage());
            return null;
        }
    }

    public void put(List<CategoryVO> list) {
        try {
            redis.opsForValue().set(CacheKeys.categoryList(), objectMapper.writeValueAsString(list), TTL);
        } catch (Exception e) {
            log.warn("写入分类缓存失败: {}", e.getMessage());
        }
    }

    /** 分类被增删改时调用 */
    public void evict() {
        try {
            redis.delete(CacheKeys.categoryList());
        } catch (Exception e) {
            log.warn("删除分类缓存失败: {}", e.getMessage());
        }
    }
}
