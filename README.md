# 二手数码产品信息发布与交流平台

基于 **Spring Boot 3 + Vue 3** 的前后端分离项目，面向校园/社区的二手数码产品（手机、电脑、平板、相机、耳机等）信息发布与交流场景。

## 技术栈

| 层次 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 3.2.5 |
| JDK | OpenJDK | 17 |
| 持久层 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 鉴权 | JWT (jjwt) | 0.12.5 |
| 接口文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 工具库 | Hutool | 5.8.27 |
| 前端框架 | Vue | 3.4 |
| 构建工具 | Vite | 5.x |
| UI 组件 | Element Plus | 2.x |
| 状态管理 | Pinia | 2.x |

## 界面预览

> 以下截图均来自 `sql/demo-data.sql` 导入的演示数据。

**首页与商品列表**

<p>
  <img src="docs/screenshots/01-home.png" width="49%" alt="首页" />
  <img src="docs/screenshots/02-product-list.png" width="49%" alt="商品列表" />
</p>

**商品详情与卖家主页**

<p>
  <img src="docs/screenshots/03-product-detail.png" width="49%" alt="商品详情" />
  <img src="docs/screenshots/04-seller-home.png" width="49%" alt="卖家主页" />
</p>

**发布商品与我的发布**

<p>
  <img src="docs/screenshots/06-publish.png" width="49%" alt="发布商品" />
  <img src="docs/screenshots/07-my-products.png" width="49%" alt="我的发布" />
</p>

**我的订单与登录页**

<p>
  <img src="docs/screenshots/08-my-orders.png" width="49%" alt="我的订单" />
  <img src="docs/screenshots/05-login.png" width="49%" alt="登录页" />
</p>

**管理后台：数据看板与商品审核**

<p>
  <img src="docs/screenshots/09-admin-dashboard.png" width="49%" alt="数据看板" />
  <img src="docs/screenshots/10-admin-product.png" width="49%" alt="商品审核" />
</p>

**管理后台：举报处理**

<p>
  <img src="docs/screenshots/11-admin-report.png" width="70%" alt="举报处理" />
</p>

## 目录结构

```
used-digital-platform/
├── docs/                  # 开发文档
├── sql/                   # 数据库脚本
├── backend/               # Spring Boot 后端服务
└── frontend/              # Vue 3 前端应用
```

## 开发文档

| 文档 | 内容 |
| --- | --- |
| [01-开发路线图](docs/01-开发路线图.md) | 五个阶段的任务清单、进度与关键设计约定 |
| [02-地基原理讲解](docs/02-地基原理讲解.md) | 后端骨架、数据库连接、前端骨架的原理剖析 |
| [03-商品模块实现讲解](docs/03-商品模块实现讲解.md) | 分类、文件上传、商品发布/检索/详情的完整实现与安全设计 |
| [04-管理后台与商品审核实现讲解](docs/04-管理后台与商品审核实现讲解.md) | 权限隔离、审核事务、审核留痕、站内通知，含一次完整的 bug 排查实录 |
| [05-管理后台用户与分类模块实现讲解](docs/05-管理后台用户与分类模块实现讲解.md) | 管理操作的防呆设计、引用检查，以及 JWT 无状态特性的安全盲区修复 |
| [06-收藏与留言模块实现讲解](docs/06-收藏与留言模块实现讲解.md) | 计数一致性、两级留言树的归并设计、N+1 优化，含异常分类补丁 |
| [07-订单模块实现讲解](docs/07-订单模块实现讲解.md) | 订单快照、状态机与双视角权限、平台联动关闭订单 |
| [08-举报模块实现讲解](docs/08-举报模块实现讲解.md) | 分类驱动的处理流程、能力复用及其副作用、一次真实的 bug 修复 |
| [09-演示数据与系统自检](docs/09-演示数据与系统自检.md) | 演示数据设计、38 项功能自检清单、答辩演示脚本 |
| [10-卖家主页实现讲解](docs/10-卖家主页实现讲解.md) | 公开资料与私有资料的边界、查询复用与参数覆盖、事件冒泡 |
| [11-Redis缓存实现讲解](docs/11-Redis缓存实现讲解.md) | 缓存穿透/雪崩/一致性、浏览量计数回写、降级设计、压测方法与真实数据 |

## 快速开始

### 1. 准备数据库

```bash
mysql -uroot -p < sql/init.sql
```

脚本会创建数据库 `used_digital_platform`、全部数据表以及分类等字典数据。

**（可选，推荐）导入演示数据**：

```bash
mysql -uroot -p --default-character-set=utf8mb4 used_digital_platform < sql/demo-data.sql
```

会填充 10 个演示用户、28 件商品、17 条收藏、15 条留言、6 个订单、4 条举报和 13 条站内通知，
覆盖在售 / 待审核 / 已售出 / 已下架 / 已驳回、订单五种状态、举报三种状态等所有场景，
答辩演示时各页面都有内容。所有演示账号密码均为 `123456`。

> 演示商品的图片由 `sql/demo-data.sql` 配套使用 `backend/uploads/demo/` 下的占位图，
> 重新导入时会自动覆盖旧的演示数据（只清理 id ≥ 100 的记录，不影响你手动创建的数据）。

### 2. 启动 Redis

商品详情、分类列表和浏览量都依赖 Redis（未启动时会自动降级为直接查库，功能不受影响，只是没有缓存收益）。

```bash
docker run -d --name udp-redis -p 6379:6379 redis:7-alpine
```

没有 Docker 的话，装一个 Memurai 或 Redis for Windows 也可以，默认连 `localhost:6379`。
连接信息在 `application.yml` 的 `spring.data.redis` 下，支持 `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` 环境变量覆盖。

### 3. 修改后端配置

打开 `backend/src/main/resources/application.yml`，把数据库账号密码改成你本机的：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

启动后访问接口文档：http://localhost:8080/doc.html

首次启动会自动创建默认账号（见 `DataInitializer`）。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173

## 测试

```bash
cd backend
mvn test
```

15 个 JUnit 5 单元测试，使用 Mockito 隔离数据库依赖，不启动 Spring 上下文，整体执行约 2 秒。

| 测试类 | 数量 | 覆盖内容 |
| --- | --- | --- |
| `OrderServiceImplTest` | 7 | 订单状态机与买卖双方权限：不能买自己的商品、一物不二卖、买家不能替卖家确认、卖家不能单方面标记完成、终态不可流转 |
| `AuthInterceptorTest` | 3 | 鉴权：无 token 返回 401、**禁用用户的旧 token 立即失效**、越权访问返回 403 |
| `FavoriteServiceImplTest` | 2 | 不能收藏自己的商品、唯一键冲突翻译为友好提示 |
| `CommentServiceImplTest` | 2 | 发布者不能自问自答、**回复的回复归并到同一条一级留言下** |
| `PageQueryTest` | 1 | 分页 size 强制上限 100、非法页码归到第 1 页 |

## 默认账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | admin | 123456 |
| 普通用户 | zhangsan | 123456 |
| 普通用户 | lisi | 123456 |

导入演示数据后还会多出 10 个账号（`chenqi` / `wangba` / `zhaoliu` / `sunjiu` / `zhouyi` /
`wuxin` / `zhengkai` / `liuyang` / `huangmin` / `linfeng`），密码同样是 `123456`，
其中 `linfeng` 是**被禁用**状态，可以用来演示「账号禁用」的效果。

> 密码使用 BCrypt 加密存储，数据库中是密文，看不到明文。
