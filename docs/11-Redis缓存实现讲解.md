# Redis 缓存实现讲解

> 系列第十篇。前篇见 [02-地基原理](02-地基原理讲解.md) 至 [09-演示数据与系统自检](09-演示数据与系统自检.md)、
> [10-卖家主页](10-卖家主页实现讲解.md)。
>
> 这一篇是本项目唯一的**性能优化专项**。难点不在"会用 Redis"，
> 而在**缓存引入之后必然带来的一致性问题**——加缓存容易，保证数据不错乱很难。

## 目录

- [第一部分：先量化问题，再谈优化](#第一部分先量化问题再谈优化)
- [第二部分：缓存设计总览](#第二部分缓存设计总览)
- [第三部分：缓存穿透 —— 空值哨兵](#第三部分缓存穿透--空值哨兵)
- [第四部分：缓存雪崩 —— TTL 随机抖动](#第四部分缓存雪崩--ttl-随机抖动)
- [第五部分：缓存一致性 —— 写时删除](#第五部分缓存一致性--写时删除)
- [第六部分：缓存击穿 —— 我为什么没做](#第六部分缓存击穿--我为什么没做)
- [第七部分：浏览量 —— 从同步写库到计数回写](#第七部分浏览量--从同步写库到计数回写)
- [第八部分：降级 —— 缓存不能成为单点故障](#第八部分降级--缓存不能成为单点故障)
- [第九部分：什么该缓存，什么绝对不能](#第九部分什么该缓存什么绝对不能)
- [第十部分：压测方法与真实数据](#第十部分压测方法与真实数据)
- [第十一部分：面试怎么讲](#第十一部分面试怎么讲)
- [附录：本功能文件索引](#附录本功能文件索引)

---

# 第一部分：先量化问题，再谈优化

**优化之前必须先回答一个问题：慢在哪里？**

商品详情接口原本要做这些事：

| 步骤 | 操作 | 说明 |
| --- | --- | --- |
| 1 | `SELECT 商品` | 主键查询 |
| 2 | `SELECT 图片列表` | 按 product_id 索引 |
| 3 | `SELECT 分类名` | 主键查询 |
| 4 | `SELECT 卖家信息` | 主键查询 |
| 5 | `SELECT 收藏状态` | user_id + product_id 唯一索引 |
| 6 | **`UPDATE view_count + 1`** | **写操作** |

**一件热门商品被 1000 人浏览 = 5000 次查询 + 1000 次更新。**

而且第 6 步是**写操作**：MySQL 会对同一行加行锁，1000 次更新被串行化。**并发越高，这个接口越慢，而它恰恰在整站最热的读路径上。**

## 1.1 优化思路

| 问题 | 手段 |
| --- | --- |
| 4 次重复查询 | 缓存组装好的 VO |
| 每次浏览都 UPDATE | Redis 计数 + 定时批量回写 |
| 引入缓存后的数据一致性 | 写操作主动删除 |

---

# 第二部分：缓存设计总览

## 2.1 缓存了什么

| 缓存 | Key | TTL | 读写时机 |
| --- | --- | --- | --- |
| 商品详情 | `udp:product:detail:{id}` | 30 分钟 + 随机 0~5 分钟 | 读时回填，写时删除 |
| 空值哨兵 | 同上 | 2 分钟 | 商品不存在时写入 |
| 分类列表 | `udp:category:list` | 1 小时 | 读时回填，管理端写时删除 |
| 浏览量增量 | `udp:product:view:{id}` | 无（回写后删除） | 每次浏览自增 |
| 待回写集合 | `udp:product:view:pending` | 无 | 记录有待回写增量的商品 |

## 2.2 Key 为什么要集中管理

```java
/**
 * Redis 缓存 key 的统一管理。
 * <p>
 * 为什么集中定义？三个理由：
 * - 避免 key 冲突：所有 key 都带 udp: 前缀
 * - 避免拼写错误：写操作删缓存时，key 拼错一个字母就会静默失效
 * - 便于排查：线上 SCAN udp:* 一眼就能看出都缓存了什么
 */
public final class CacheKeys {
    private static final String PREFIX = "udp:";

    public static String productDetail(Long productId) {
        return PREFIX + "product:detail:" + productId;
    }
    // ...
}
```

**第二点最要命。** 假设"编辑商品"里写成了 `udp:products:detail:{id}`（多一个 s），删除缓存时会删一个不存在的 key——**Redis 不会报错，业务也不会报错**，只有用户发现"我改了价格怎么没生效"。

**这类 bug 在测试环境极难复现，因为缓存刚好过期了就看不出来。**

## 2.3 为什么用 `StringRedisTemplate` + 手动 JSON

没有用 `RedisTemplate<String, Object>` + `GenericJackson2JsonRedisSerializer`，原因很实际：

`ProductDetailVO` 里有 `LocalDateTime` 和 `LocalDate`。而 `GenericJackson2JsonRedisSerializer` 的默认 `ObjectMapper` **没有注册 JavaTimeModule**，序列化时会直接抛 `InvalidDefinitionException`。

要修就得手动构造一个有 JavaTimeModule 的 ObjectMapper 并开启 default typing——**配置链一长，出错点就多。**

而项目里的 Spring 已经自动配置好了一个带 JavaTimeModule（还有我自定义的日期格式）的 `ObjectMapper`。直接复用它：

```java
private final StringRedisTemplate redis;
private final ObjectMapper objectMapper;   // ← Spring 已配置好的那个

public ProductDetailVO getDetail(Long productId) {
    String json = redis.opsForValue().get(CacheKeys.productDetail(productId));
    return objectMapper.readValue(json, ProductDetailVO.class);
}
```

**收益：序列化规则和接口响应完全一致，不会出现"接口返回的日期格式和缓存里的不一样"这种诡异问题。**

---

# 第三部分：缓存穿透 —— 空值哨兵

## 3.1 问题

```
攻击者循环请求 /api/product/999999（不存在的 ID）
   ↓
缓存永远不命中（因为从来不会缓存"不存在"）
   ↓
每个请求都打到数据库
   ↓
数据库被打穿
```

**这不是"性能问题"，是"可用性问题"**——一个脚本就能让数据库 CPU 打满。

## 3.2 解法：把"不存在"也缓存起来

```java
ProductDetailVO vo = productCache.getDetail(productId);
if (vo == null) {
    vo = buildDetail(productId);
    if (vo == null) {
        // 商品确实不存在：写入空值哨兵（TTL 2 分钟），
        // 防止有人用同一个不存在的 ID 反复请求把数据库打穿
        productCache.putEmpty(productId);
        throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
    }
    productCache.putDetail(productId, vo);
}
```

哨兵值是一个特殊字符串：

```java
private static final String EMPTY_SENTINEL = "__EMPTY__";
private static final Duration EMPTY_TTL = Duration.ofMinutes(2);

public void putEmpty(Long productId) {
    redis.opsForValue().set(CacheKeys.productDetail(productId), EMPTY_SENTINEL, EMPTY_TTL);
}
```

读取时：

```java
public ProductDetailVO getDetail(Long productId) {
    String json = redis.opsForValue().get(...);
    if (json == null || EMPTY_SENTINEL.equals(json)) {
        return null;   // 未命中 or 已知不存在，都交给上层处理
    }
    return objectMapper.readValue(json, ProductDetailVO.class);
}
```

上层再单独判断一次"是不是已知不存在"：

```java
// ① 缓存穿透防护：已经确认过不存在的 ID，直接返回，不再打数据库
if (productCache.isKnownMissing(productId)) {
    throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
}
```

## 3.3 为什么空值的 TTL 要短

| 数据类型 | TTL | 理由 |
| --- | --- | --- |
| 正常商品详情 | 30~35 分钟 | 数据稳定，代价小 |
| **空值哨兵** | **2 分钟** | **商品可能是刚创建的** |

**如果空值也缓存 30 分钟，会出现这种情况：**

```
10:00  用户 A 请求商品 999 → 不存在 → 写入空值哨兵（假设 TTL 30 分钟）
10:01  卖家创建了商品 999
10:30  用户 B 请求商品 999 → 命中空值哨兵 → 还是"不存在"
       ↑ 商品明明已经创建了，29 分钟查不到
```

**空值缓存的 TTL 必须明显短于正常数据**，这是"防穿透"和"及时可见"之间的平衡点。

## 3.4 实测

```
请求 /api/product/999999  →  code=3001  商品不存在或已被删除
redis: udp:product:detail:999999 = __EMPTY__
       TTL = 120 秒
第二次请求 → code=3001（直接命中哨兵，不再查库）
```

---

# 第四部分：缓存雪崩 —— TTL 随机抖动

## 4.1 问题

如果所有商品详情的 TTL 都是固定的 30 分钟，而它们在系统启动或批量预热时被同时写入，那么 **30 分钟后会同时过期**：

```
10:00:00  批量预热 500 个商品详情（TTL 都是 30 分钟）
10:30:00  500 个 key 同时失效
          ↓
          500 个请求同时打到数据库
          ↓
          数据库瞬时压力是平时的 N 倍 → 可能直接挂掉
```

## 4.2 解法：在 TTL 上加随机量

```java
/** 基础 TTL */
private static final Duration DETAIL_TTL = Duration.ofMinutes(30);

/** 抖动上限：在基础 TTL 上再随机加 0~5 分钟 */
private static final long JITTER_MAX_SECONDS = Duration.ofMinutes(5).getSeconds();

private Duration ttlWithJitter() {
    return DETAIL_TTL.plusSeconds(ThreadLocalRandom.current().nextLong(JITTER_MAX_SECONDS));
}
```

**实际 TTL 在 30~35 分钟之间随机分布**，500 个 key 会分散在 5 分钟的窗口里陆续过期，峰值压力被摊平。

> 用 `ThreadLocalRandom` 而不是 `Random`，因为它在多线程下没有竞争，性能更好。
> 这是一个很小的细节，但写出来说明你知道为什么。

## 4.3 实测

```
设定 TTL = 30 分钟，实际观测到 ttl 返回 1997 秒（≈ 33 分钟）
```

**不是整数 1800 秒**，说明抖动生效了。

## 4.4 雪崩还有别的成因

TTL 抖动只解决了"同时过期"这一种。另外两种：

| 成因 | 处理方式 |
| --- | --- |
| Redis 整体宕机 | 本项目的**降级**（见第八部分）：回落到数据库 |
| 大量 key 因为内存淘汰被驱逐 | 配置 `maxmemory-policy`，对缓存数据用 `allkeys-lru` |

**第二点属于运维配置，本项目没有专门做，但知道有这个可能。**

---

# 第五部分：缓存一致性 —— 写时删除

**这是缓存最难的部分，也是最容易被面试官追问的地方。**

## 5.1 三种常见策略

| 策略 | 做法 | 问题 |
| --- | --- | --- |
| 先更新库，再更新缓存 | 写完 DB 后 `SET` 缓存 | 并发下两个线程可能**用旧值覆盖新值** |
| ⭐ **先更新库，再删除缓存** | 写完 DB 后 `DEL` 缓存 | 有极短窗口读到旧值，但不会存错值 |
| 先删缓存，再更新库 | `DEL` 后写 DB | 并发读会把旧值重新写回缓存，**错误会长期存在** |

本项目用第二种：**Cache-Aside（旁路缓存）的"写时删除"**。

```java
productMapper.updateById(exist);
// 内容变了，缓存立刻作废 —— 否则下一个访客看到的还是旧价格
productCache.evict(productId);
```

**为什么"删除"比"更新"好？**

- **删除是幂等的**：删两次和删一次结果一样
- **更新不是**：如果 A 线程把新值写进缓存，B 线程随后把更旧的值写进去，缓存就永久错了
- **删除后下次读会从数据库重建**，拿到的一定是最新值

**为什么不是"先删缓存再更新库"？**

```
时刻 T1: 线程A 删除缓存
时刻 T2: 线程B 读缓存 → 未命中 → 查库拿到【旧值】
时刻 T3: 线程A 更新数据库为【新值】
时刻 T4: 线程B 把【旧值】写回缓存     ← 缓存里是旧值，而且会存在很久
```

**这个错误会持续到下一次缓存过期**，比"读一次旧值"严重得多。

## 5.2 完整清单：7 个必须删缓存的写入点

**加缓存最容易犯的错是"只记得主流程，忘了边角操作"。**

我把所有会改变商品可见内容的操作都梳理了一遍：

| # | 操作 | 位置 |
| --- | --- | --- |
| 1 | 发布商品 | `ProductServiceImpl.publish` |
| 2 | 编辑商品 | `ProductServiceImpl.update` |
| 3 | 删除商品 | `ProductServiceImpl.delete` |
| 4 | 卖家下架 | `ProductServiceImpl.offShelf` |
| 5 | 重新提交审核 | `ProductServiceImpl.resubmit` |
| 6 | 管理员审核通过/驳回 | `AdminProductServiceImpl.audit` |
| 7 | 管理员强制下架 / 删除 | `AdminProductServiceImpl.forceOffShelf` / `delete` |
| 8 | **收藏 / 取消收藏** | `FavoriteServiceImpl.add` / `remove` |

**第 8 个最容易漏。**

因为直觉上"收藏"和"商品详情"没关系。但详情 VO 里有 `favoriteCount` 字段——**不删缓存的话，用户收藏完刷新页面，收藏数还是旧的。**

```java
// 详情缓存里存着 favoriteCount，收藏数变了必须让它失效
productCache.evict(productId);
```

> **判断"哪些写操作要删缓存"的标准只有一个：**
> **这个操作改动的字段，有没有出现在被缓存的 VO 里？**
> 有就删，没有就不用。

## 5.3 实测验证（这是最有说服力的一组测试）

**怎么证明缓存真的生效、又真的会失效？** 用"直接改数据库"来判断：

```powershell
# ① 先请求一次，让缓存建立
GET /api/product/101  →  title = "iPhone 13 128G 星光色 国行"

# ② 绕过应用，直接改数据库
UPDATE biz_product SET title='CACHE_HIT_TEST' WHERE id=101

# ③ 再请求一次
GET /api/product/101  →  title 还是 "iPhone 13 128G 星光色 国行"
```

**数据库已经是 `CACHE_HIT_TEST`，接口却返回旧标题——这就是"走了缓存"的铁证。**

然后验证失效：

```powershell
# ④ 再改一次数据库
UPDATE biz_product SET title='AFTER_EDIT' WHERE id=101

# ⑤ 调用一个会删缓存的业务接口（比如收藏）
POST /api/favorite/101   →  evict

# ⑥ 再请求
GET /api/product/101  →  title = "AFTER_EDIT"   ✓ 重新查库了
```

**这两步合起来，同时证明了"缓存命中"和"写时失效"都工作正常。**

## 5.4 一个绕不开的问题：删除失败了怎么办

```java
public void evict(Long productId) {
    try {
        redis.delete(CacheKeys.productDetail(productId));
    } catch (Exception e) {
        log.warn("删除商品详情缓存失败 id={}: {}", productId, e.getMessage());
    }
}
```

**Redis 删失败时，只记日志、不抛异常**——因为抛异常会把整个业务事务回滚，用户改价失败。而删缓存失败最多是"数据旧到 TTL 过期"。

**这是一个有意的取舍：宁可短暂不一致，也不让主业务失败。**

**更严格的做法**是"延迟双删"或"用消息队列保证最终删除"，但那需要引入 MQ，对本项目属于过度设计。

---

# 第六部分：缓存击穿 —— 我为什么没做

## 6.1 什么是击穿

**缓存击穿 ≠ 缓存穿透**，两者经常被搞混：

| | 缓存穿透 | 缓存击穿 |
| --- | --- | --- |
| 数据在不在 | **不存在** | **存在** |
| 现象 | 每次请求都查库 | 某个**热点 key 过期瞬间**，大量请求同时查库 |
| 典型场景 | 恶意构造不存在的 ID | 秒杀商品详情突然过期 |

## 6.2 两种常规解法

**方案一：互斥锁**

```java
// 只让一个线程去查库，其他线程等待
if (redis.setIfAbsent(lockKey, "1", 10, SECONDS)) {
    try { 查库并回填; } finally { redis.delete(lockKey); }
} else {
    休眠 50ms 后重试读缓存;
}
```

**方案二：逻辑过期**

缓存永不物理过期，而是存一个 `expireAt` 字段。发现逻辑过期时，**一个线程异步重建**，其他线程先返回旧数据。

## 6.3 我为什么没做

**因为本项目的场景不满足击穿的前提。**

| 条件 | 本项目 |
| --- | --- |
| 有明确的"秒杀级"热点 key 吗？ | 没有。二手商品流量分散 |
| key 集中的过期瞬间会造成明显抖动吗？ | 不会。TTL 已经加了随机抖动 |
| 加锁带来的复杂度 vs 收益 | 不划算。互斥锁会引入阻塞、锁超时、重试等一堆边界情况 |

**如果我为了"简历上好看"硬加一个互斥锁，面试官一问"你这个锁的超时设多少、为什么、锁抛异常了怎么办"，就会露馅。**

> **能说清楚"我评估过但情况不满足"，比"我全都实现了"更可信。**
> 面试时如果被问到击穿，回答：
> "我考虑过，但本项目流量分散、TTL 已加抖动，击穿的概率很低；
> 更重要的是商品详情不是秒杀场景，短暂多打几次库完全可以接受。
> 如果真要做，我会用逻辑过期而不是互斥锁，因为前者不阻塞请求。"

---

# 第七部分：浏览量 —— 从同步写库到计数回写

**这是本次优化里技术含量最高的一部分。**

## 7.1 原来为什么慢

```java
// 旧实现：每次浏览都执行
productMapper.update(null, Wrappers.<Product>lambdaUpdate()
        .setSql("view_count = view_count + 1")
        .eq(Product::getId, productId));
```

**这是典型的热点行竞争（hot row contention）：**

```
1000 个并发请求看同一件商品
   ↓
1000 条 UPDATE 全部要锁同一行
   ↓
MySQL 行锁把它们串行化
   ↓
第 1000 个请求要等前面 999 个执行完
```

**而在秒杀式流量下，这还会带来死锁和锁等待超时。**

## 7.2 改成"内存计数 + 批量回写"

### 浏览时：只碰 Redis

```java
public long increment(Long productId) {
    try {
        Long delta = redis.opsForValue().increment(CacheKeys.productViewDelta(productId));
        // 同时登记到待回写集合
        redis.opsForSet().add(CacheKeys.productViewPending(), String.valueOf(productId));
        return delta == null ? -1L : delta;
    } catch (Exception e) {
        log.warn("浏览量计数失败，降级为直接更新数据库 id={}: {}", productId, e.getMessage());
        return -1L;   // -1 作为特殊值，告诉调用方"Redis 不可用"
    }
}
```

**`INCR` 是 Redis 的单线程内存操作，微秒级，没有锁竞争。**

### 展示时：缓存基线 + 增量

```java
long delta = viewCountCache.increment(productId);
if (delta < 0) {
    // Redis 不可用：降级为直接改库
    productMapper.addViewCount(productId, 1L);
    vo.setViewCount(vo.getViewCount() + 1);
} else {
    // 展示值 = 缓存基线 + 自缓存建立以来的增量
    vo.setViewCount(vo.getViewCount() + (int) delta);
}
```

**为什么不能直接用缓存里的 `viewCount`？**

因为缓存里那个值是"缓存建立那一刻的数据库值"，30 分钟内都不会变——**用户会看到浏览量一直不涨**。

所以拆成两部分：**基线（缓存）+ 增量（Redis 计数器）**。

### 定时回写

```java
@Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
public void flushViewCounts() {
    try {
        Set<String> pendingIds = redis.opsForSet().members(CacheKeys.productViewPending());
        if (pendingIds == null || pendingIds.isEmpty()) return;

        for (String idStr : pendingIds) {
            Long productId = Long.valueOf(idStr);
            long delta = 读出增量;
            if (delta > 0) {
                productMapper.addViewCount(productId, delta);   // 一条 UPDATE 累加
            }
            redis.delete(CacheKeys.productViewDelta(productId));
            redis.opsForSet().remove(CacheKeys.productViewPending(), idStr);
            productCache.evict(productId);   // 基线变了，缓存要重建
        }
    } catch (Exception e) {
        // 定时任务绝不能因为一次异常就停掉
        log.error("浏览量回写失败，本轮跳过", e);
    }
}
```

SQL 侧：

```java
@Update("UPDATE biz_product SET view_count = view_count + #{delta} WHERE id = #{id} AND deleted = 0")
int addViewCount(@Param("id") Long id, @Param("delta") long delta);
```

**1000 次浏览 → 1 条 UPDATE。**

## 7.3 四个关键设计点

### ① 用 SET 记录待回写商品，不用 SCAN

```
❌ SCAN udp:product:view:*     → 遍历整个 keyspace，key 多时阻塞 Redis 主线程
✅ SADD udp:product:view:pending {id}   → O(1)，只处理真正有增量的商品
```

**`SCAN` 虽然在生产上比 `KEYS` 安全，但在大 key 空间下仍然会拖慢 Redis。** 用 SET 精确记录是更好的做法。

### ② 回写后必须删详情缓存

**这是最容易漏的一步，也是最容易出错的地方。**

```
缓存里 viewCount = 186（建立时的基线）
Redis 增量 = 9
展示值 = 186 + 9 = 195  ✓

定时任务回写：DB = 186 + 9 = 195，然后删除增量 key
   ↓
如果【不删详情缓存】：
   下次请求 → 缓存基线还是 186，增量是 0
   展示值 = 186 + 0 = 186    ← 数字倒退了！
```

**所以回写时必须 `productCache.evict(productId)`**，让下一次请求重新从数据库读取基线。

### ③ 用 `fixedDelay` 而不是 `fixedRate`

```java
@Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
```

| 注解属性 | 含义 |
| --- | --- |
| `fixedRate` | 每隔 5 分钟**开始**一次（不管上一轮有没有跑完） |
| **`fixedDelay`** | 上一轮**跑完后**再等 5 分钟 |

**用 `fixedRate` 的话，如果某一轮因为数据库慢跑了 6 分钟，会立刻再叠一轮，任务越积越多。** `fixedDelay` 天然避免这个问题。

`initialDelay = 60s` 是为了让应用先完成启动，别一上来就抢资源。

### ④ 整个任务包在 try-catch 里

**定时任务因为一次异常就永久停掉，是最常见的线上事故。**

Spring 的 `@Scheduled` 方法抛异常后，后续调度**不会继续**（默认只记录错误）。所以必须自己兜住：

```java
try {
    ...整个回写逻辑...
} catch (Exception e) {
    log.error("浏览量回写失败，本轮跳过", e);   // 不抛出去，下一轮继续
}
```

## 7.4 实测全过程

```
① 初始：数据库 view_count = 186，Redis 清空

② 浏览 8 次
   redis delta  = 8
   pending set  = {101}
   数据库        = 186（未变）

③ 再请求一次（第 9 次）
   redis delta  = 9
   API 返回      = 195   ← 186 + 9，展示值实时正确

④ 等定时任务执行（≤ 5 分钟）
   pending set  = 空
   delta key    = 已删除
   detail cache = 已失效
   数据库        = 195   ← 186 + 9，一条 UPDATE 完成
```

## 7.5 代价是什么

**Redis 在这 5 分钟内挂掉，最多丢 5 分钟的浏览量。**

**这是一个有意的取舍：**

| 数据类型 | 能不能这么做 |
| --- | --- |
| 浏览量 | ✅ 统计数字，不影响交易 |
| 点赞数、播放量 | ✅ 同理 |
| **订单金额** | ❌ **绝对不行**，必须同步落库 + 事务 |
| **库存扣减** | ❌ 同上 |

> **面试时这样说**：
> "我知道这个方案会丢数据，所以我只对浏览量这类非关键统计使用。
> 订单金额和库存仍然是强一致的同步写库，因为它们丢了就是资损。"

---

# 第八部分：降级 —— 缓存不能成为单点故障

## 8.1 问题

很多项目加缓存时只写：

```java
public ProductDetailVO get(Long id) {
    ProductDetailVO cached = redis.get(key);   // ❌ Redis 挂了这里就抛异常
    ...
}
```

**Redis 一挂，整个商品详情接口 500。** 本来只是"优化性能"，结果变成了"引入新的单点故障"。

## 8.2 解法：每个缓存方法都兜住异常

```java
public ProductDetailVO getDetail(Long productId) {
    try {
        String json = redis.opsForValue().get(CacheKeys.productDetail(productId));
        if (json == null || EMPTY_SENTINEL.equals(json)) {
            return null;
        }
        return objectMapper.readValue(json, ProductDetailVO.class);
    } catch (Exception e) {
        // 缓存层出任何问题都不能影响业务，降级为查库
        log.warn("读取商品详情缓存失败，降级查库 id={}: {}", productId, e.getMessage());
        return null;   // 返回 null = 未命中 → 走查库路径
    }
}
```

**核心思想：把"缓存不可用"伪装成"缓存未命中"，业务代码完全不需要知道 Redis 挂了。**

写缓存失败也一样处理——**写不进去就写不进去，最多下次还得查库**：

```java
public void putDetail(Long productId, ProductDetailVO vo) {
    try {
        redis.opsForValue().set(..., ttlWithJitter());
    } catch (Exception e) {
        log.warn("写入商品详情缓存失败 id={}: {}", productId, e.getMessage());
        // 不抛异常
    }
}
```

## 8.3 浏览量的降级更细

浏览量的降级不只是"别报错"，还要**保证计数不丢**：

```java
long delta = viewCountCache.increment(productId);
if (delta < 0) {
    // Redis 不可用：降级为直接改库，保证计数不丢
    productMapper.addViewCount(productId, 1L);
    vo.setViewCount(vo.getViewCount() + 1);
}
```

`increment()` 用 `-1` 作为"我失败了"的约定值，而不是抛异常——**这样调用方可以自己决定怎么降级**。

## 8.4 效果

| Redis 状态 | 商品详情接口 |
| --- | --- |
| 正常 | 走缓存，8.79ms |
| **挂掉** | **自动查库，38ms，功能完全可用** |

**性能下降，但业务不中断。** 这才叫"缓存"，而不是"把数据库换成 Redis"。

---

# 第九部分：什么该缓存，什么绝对不能

**这一节是加缓存时最容易埋雷的地方。**

## 9.1 陷阱一：用户相关字段不能进共享缓存

`ProductDetailVO` 里有两个字段是**因人而异**的：

```java
private Boolean favorite;   // 当前用户是否收藏了这件商品
private Boolean owner;      // 当前用户是不是发布者
```

**如果把整个 VO（含这两个字段）直接缓存**：

```
用户 A 收藏了商品 1 → 缓存里 favorite = true
   ↓
用户 B 打开商品 1 → 从缓存读到 favorite = true
   ↓
B 看到"已收藏"，实际上他从没收藏过
```

**这是数据串号，比"数据旧"严重得多。**

解法：**这两个字段在缓存之外实时计算。**

```java
// buildDetail() 里只填"与用户无关"的字段，owner/favorite 全部不设

// 从缓存拿到 VO 之后，再按当前用户覆盖
boolean owner = currentUserId != null && currentUserId.equals(vo.getSellerId());
vo.setOwner(owner);
vo.setFavorite(currentUserId != null && favoriteService.isFavorite(currentUserId, productId));
```

`buildDetail()` 的注释写得很清楚：

```java
/**
 * 只填<b>与当前用户无关</b>的字段 —— owner / favorite 这类用户相关字段
 * 绝不能进缓存，否则张三打开同一个商品会看到李四的收藏状态。
 */
```

## 9.2 陷阱二：权限校验必须基于缓存里的字段

**这里有一个更危险的问题：**

未上架（待审核/已下架/驳回）的商品，**只有发布者本人和管理员能看**。如果把"缓存"放在权限校验之前，可能出现：

```
① 管理员打开一个待审核商品 → 详情被缓存
② 普通用户拿到这个 ID 请求详情 → 命中缓存 → 直接看到未过审内容
```

**解法：权限校验放在缓存之后，但校验依据来自缓存里的字段。**

```java
// ② 读缓存（可能是刚被管理员带出来的待审核商品）
ProductDetailVO vo = productCache.getDetail(productId);
if (vo == null) { vo = buildDetail(...); ... }

// ③ 权限校验。直接用缓存里的 status / sellerId，不需要额外查库
boolean owner = currentUserId != null && currentUserId.equals(vo.getSellerId());
if (!ProductStatus.ON_SALE.getCode().equals(vo.getStatus()) && !owner) {
    if (currentUserId == null || !isAdmin(currentUserId)) {
        throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
    }
}
```

**关键点：缓存里必须带上 `status` 和 `sellerId`**，否则校验无从下手。

**顺序是"先取缓存，再校验"，而不是"先校验，再取缓存"**——因为校验需要的数据就在缓存里。

## 9.3 判断标准

> **一个字段能不能进缓存，看它会不会"因人而异"。**
>
> - 商品标题、价格、图片、卖家昵称 → ✅ 所有人看到一样
> - 是否收藏、是否本人、未读标记 → ❌ 每个人不同

---

# 第十部分：压测方法与真实数据

## 10.1 怎么测才可信

**不能用 curl 循环测**——`curl` 每次启动一个进程、建立新连接，**进程启动开销（10~30ms）会把缓存带来的差异完全淹没**。

正确做法：**复用连接的 HTTP 客户端**。

```powershell
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient

# 预热：建立连接 + JIT 编译
$null = $client.GetStringAsync($url).Result
Start-Sleep -Milliseconds 500

# 测缓存命中
for ($i = 0; $i -lt 40; $i++) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $null = $client.GetStringAsync($url).Result
    $sw.Stop()
    $warm += $sw.Elapsed.TotalMilliseconds
}

# 测缓存未命中：每次请求前删缓存
for ($i = 0; $i -lt 40; $i++) {
    docker exec udp-redis redis-cli del udp:product:detail:101 | Out-Null
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $null = $client.GetStringAsync($url).Result
    $sw.Stop()
    $cold += $sw.Elapsed.TotalMilliseconds
}
```

**三个细节：**

1. **预热**：第一次请求包含 TCP 握手、类加载、JIT 编译，必须排除
2. **删除缓存的操作在计时之外**：`docker exec` 本身要 100ms，会污染结果
3. **取 P95 而不只是平均值**：平均值会被少数极慢的请求拉偏，P95 更能反映真实体验

## 10.2 结果

```
                       avg        p95        max
 缓存未命中(查库)     38.08 ms   70.56 ms   75.57 ms
 缓存命中(Redis)       8.79 ms   12.74 ms   19.68 ms
 -------------------------------------------------
 平均降低 76.9%（约 4.3 倍）      P95 降低 81.9%
```

**为什么 P95 降得比平均值更多（81.9% > 76.9%）？**

因为**数据库查询的耗时波动更大**——连接池等待、缓冲池命中率、锁等待都会造成长尾。而 Redis 是内存操作，耗时有上限。

**缓存最擅长的正是"削掉长尾"，而不只是降低平均值。** 这一点在面试里讲出来，说明你理解 P95 的意义。

## 10.3 这组数据的边界

**要诚实说明测量条件：**

| 条件 | 说明 |
| --- | --- |
| 数据量 | 28 件商品，单机 MySQL |
| 并发度 | 串行请求，没有真实并发压力 |
| 网络 | 全部在 localhost |
| 缓存类型 | 命中率 100%（因为只有一个商品反复请求） |

**真实场景下的收益会低于这个数字**（缓存命中率不可能 100%，网络延迟更大）。

> **面试时主动说明测量条件，比只报一个漂亮数字更可信。**
> "这是本地单机、串行请求下的数据，真实环境命中率没这么高——但趋势是一致的。"

---

# 第十一部分：面试怎么讲

## 11.1 30 秒版本

> 我给商品详情加了 Redis 缓存。它原本要做 4 次关联查询 + 1 次浏览量 UPDATE，
> 压测显示命中缓存后平均响应从 38ms 降到 8.8ms，P95 从 70ms 降到 12.7ms。
> 加了缓存之后主要处理三类问题：穿透用空值哨兵、雪崩用 TTL 抖动、
> 一致性靠写操作主动删除，我把 8 个写入点都梳理了一遍。
> 另外我把浏览量从"每次 UPDATE"改成了 Redis 计数 + 5 分钟批量回写，
> 解决了热点行的锁竞争。所有缓存操作都做了降级，Redis 挂了不影响业务。

## 11.2 常见追问

| 问题 | 怎么答 |
| --- | --- |
| **怎么保证缓存和数据库一致？** | 先更新库再删缓存，不用"更新缓存"（并发下会被旧值覆盖），也不用"先删再更新"（并发读会把旧值写回）。删失败只记日志不抛异常，宁可短暂不一致也不让主业务失败。 |
| **为什么不用延迟双删？** | 那是为了处理"删缓存后仍有并发读把旧值写回"的更极端场景，需要延迟 + 重试，本项目写少读多，概率低于收益。 |
| **缓存击穿怎么办？** | 我评估过但没做：本项目没有秒杀级热点 key，TTL 已加抖动，击穿概率低。真要做我会用逻辑过期而不是互斥锁，因为前者不阻塞请求。 |
| **Redis 挂了怎么办？** | 所有缓存方法都吞异常返回"未命中"，自动回落到数据库。功能可用，性能退化到优化前。 |
| **为什么浏览量不用同步写库？** | 热点行锁竞争。1000 次浏览原本要 1000 条 UPDATE，现在 1 条。代价是最多丢 5 分钟浏览量——它是统计数字，不是资损数据。 |
| **为什么不用 Redis 事务 / Lua？** | 浏览量用 `INCR` 本身就是原子的；缓存读写是单 key 操作，不需要事务。引入 Lua 只会增加复杂度。 |

## 11.3 简历句式

> 引入 Redis 缓存商品详情与分类列表，命中时省去 4 次关联查询，
> 压测显示平均响应从 38.08ms 降至 8.79ms（**-76.9%**），P95 从 70.56ms 降至 12.74ms（**-81.9%**）；
> 通过空值哨兵防穿透、TTL 随机抖动防雪崩、写操作主动失效保证一致性；
> 将浏览量从同步 UPDATE 改造为 Redis 计数 + 定时批量回写，消除热点行锁竞争；
> 实现 Redis 不可用时的自动降级。

---

# 附录：本功能文件索引

## 新增文件

| 文件 | 作用 |
| --- | --- |
| `cache/ProductCache.java` | 商品详情缓存：空值哨兵、TTL 抖动、降级 |
| `cache/CategoryCache.java` | 分类列表缓存（1 小时 TTL） |
| `cache/ViewCountCache.java` | 浏览量计数器（Redis INCR） |
| `task/ViewCountFlushTask.java` | 定时批量回写（每 5 分钟） |
| `common/constant/CacheKeys.java` | Key 统一管理（`udp:` 前缀） |

## 修改文件

| 文件 | 改动 |
| --- | --- |
| `ProductServiceImpl` | `detail()` 加缓存读写与权限校验；`buildDetail()` 抽出组装逻辑；5 个写操作加 `evict` |
| `FavoriteServiceImpl` | 收藏/取消收藏后 `evict`（favoriteCount 变了） |
| `AdminProductServiceImpl` | 审核、强制下架、删除后 `evict` |
| `AdminCategoryServiceImpl` | 分类增删改后 `evict` 分类缓存 |
| `CategoryServiceImpl` | 读分类走缓存 |
| `ProductMapper` | 新增 `addViewCount(id, delta)` 批量累加 |
| `DigitalPlatformApplication` | 加 `@EnableScheduling` |
| `application.yml` | Redis 连接配置（支持环境变量覆盖） |
| `pom.xml` | `spring-boot-starter-data-redis` |

## 本地启动 Redis

```bash
docker run -d --name udp-redis -p 6379:6379 redis:7-alpine
```

## 验证缓存是否生效

```bash
# 1. 看 key 是否存在
docker exec udp-redis redis-cli exists udp:product:detail:101

# 2. 看剩余 TTL（应该是 30~35 分钟之间，不是整数）
docker exec udp-redis redis-cli ttl udp:product:detail:101

# 3. 看浏览量增量
docker exec udp-redis redis-cli get udp:product:view:101

# 4. 看所有缓存 key
docker exec udp-redis redis-cli keys 'udp:*'
```
