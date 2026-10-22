package com.digital.platform.task;

import com.digital.platform.cache.ProductCache;
import com.digital.platform.common.constant.CacheKeys;
import com.digital.platform.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 浏览量回写任务。
 * <p>
 * 商品详情每次被浏览时，只在 Redis 里自增一个计数器，
 * 数据库的 {@code view_count} 由这个任务<b>每 5 分钟批量更新一次</b>。
 * <p>
 * 效果：一个商品 5 分钟内被看 1000 次，只产生 <b>1 条 UPDATE</b>，
 * 而不是 1000 条。热点行的锁竞争消失了。
 * <p>
 * 三个设计细节：
 * <ol>
 *   <li>用 {@code SET} 记录"有哪些商品有待回写的增量"，
 *       而不是 {@code SCAN udp:product:view:*} —— SCAN 在大 key 空间下会拖慢 Redis</li>
 *   <li>回写完删除增量 key，<b>同时删除商品详情缓存</b> ——
 *       缓存里的 viewCount 是旧基线，不删的话展示值会停在旧数字上</li>
 *   <li>整个任务包在 try-catch 里，Redis 或数据库抖动时只跳过这一轮，不影响下一轮</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountFlushTask {

    private final StringRedisTemplate redis;
    private final ProductMapper productMapper;
    private final ProductCache productCache;

    /**
     * fixedDelay：上一轮执行完 5 分钟后才开始下一轮。
     * 用 fixedDelay 而不是 fixedRate，是为了避免上一轮还没跑完就叠加下一轮。
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void flushViewCounts() {
        try {
            Set<String> pendingIds = redis.opsForSet().members(CacheKeys.productViewPending());
            if (pendingIds == null || pendingIds.isEmpty()) {
                return;
            }

            int flushed = 0;
            for (String idStr : pendingIds) {
                Long productId = Long.valueOf(idStr);
                String deltaStr = redis.opsForValue().get(CacheKeys.productViewDelta(productId));
                long delta = deltaStr == null ? 0L : Long.parseLong(deltaStr);

                if (delta > 0) {
                    productMapper.addViewCount(productId, delta);
                    flushed++;
                }

                // 增量已落库，清掉计数器；详情缓存里的 viewCount 也需要重建
                redis.delete(CacheKeys.productViewDelta(productId));
                redis.opsForSet().remove(CacheKeys.productViewPending(), idStr);
                productCache.evict(productId);
            }

            log.info("浏览量回写完成：{} 个商品，共处理 {} 条待回写记录", flushed, pendingIds.size());
        } catch (Exception e) {
            // 定时任务绝不能因为一次异常就停掉；下一轮会继续处理
            log.error("浏览量回写失败，本轮跳过", e);
        }
    }
}
