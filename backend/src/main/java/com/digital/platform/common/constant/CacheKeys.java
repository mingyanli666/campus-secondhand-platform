package com.digital.platform.common.constant;

/**
 * Redis 缓存 key 的统一管理。
 * <p>
 * 为什么集中定义？三个理由：
 * <ol>
 *   <li><b>避免 key 冲突</b>——所有 key 都带 {@code udp:} 前缀，
 *       同一台 Redis 上跑多个项目也不会互相覆盖</li>
 *   <li><b>避免拼写错误</b>——写操作删缓存时，key 拼错一个字母就会静默失效，
 *       而这种 bug 不会有任何报错，只有"数据不一致"的现象</li>
 *   <li><b>便于排查</b>——线上 {@code SCAN udp:*} 一眼就能看出都缓存了什么</li>
 * </ol>
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 全局前缀，换项目时只改这一处 */
    private static final String PREFIX = "udp:";

    /** 商品详情缓存：udp:product:detail:{id} */
    public static String productDetail(Long productId) {
        return PREFIX + "product:detail:" + productId;
    }

    /** 商品浏览量增量计数器：udp:product:view:{id} */
    public static String productViewDelta(Long productId) {
        return PREFIX + "product:view:" + productId;
    }

    /** 待回写浏览量的商品 ID 集合（避免用 SCAN 遍历全部 key） */
    public static String productViewPending() {
        return PREFIX + "product:view:pending";
    }

    /** 分类列表缓存：udp:category:list */
    public static String categoryList() {
        return PREFIX + "category:list";
    }

    /** 用户状态缓存：udp:user:status:{id}，值为 1 正常 / 0 禁用 */
    public static String userStatus(Long userId) {
        return PREFIX + "user:status:" + userId;
    }
}
