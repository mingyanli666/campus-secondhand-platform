-- ============================================================
-- 二手数码产品信息发布与交流平台 - 数据库初始化脚本
-- 环境：MySQL 8.0+
-- 字符集：utf8mb4（支持 emoji）
-- 说明：本脚本可重复执行（先删库再建库），生产环境请去掉 DROP 语句
-- ============================================================

DROP DATABASE IF EXISTS `used_digital_platform`;
CREATE DATABASE `used_digital_platform`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
USE `used_digital_platform`;

-- ------------------------------------------------------------
-- 1. 用户表
-- 说明：用户不做逻辑删除，用 status 控制（1 正常 / 0 禁用），
--       避免逻辑删除与 username 唯一索引冲突的经典问题。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    `password`        VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt 密文)',
    `nickname`        VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像地址',
    `phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `email`           VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `gender`          TINYINT      NOT NULL DEFAULT 0      COMMENT '性别 0未知 1男 2女',
    `campus`          VARCHAR(100) DEFAULT NULL            COMMENT '所在学校/区域',
    `intro`           VARCHAR(255) DEFAULT NULL            COMMENT '个人简介',
    `role`            VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER普通用户 / ADMIN管理员',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1正常 0禁用',
    `credit_score`    INT          NOT NULL DEFAULT 100    COMMENT '信誉分，初始100',
    `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 商品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_category`;
CREATE TABLE `sys_category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50) NOT NULL                COMMENT '分类名称',
    `icon`        VARCHAR(255) DEFAULT NULL           COMMENT '分类图标',
    `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序值，越小越靠前',
    `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态 1启用 0停用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

-- ------------------------------------------------------------
-- 3. 商品表（核心表）
-- 数码专属字段：brand 品牌 / model 型号 / condition_level 成色
--               spec 参数 / warranty 质保 / purchase_date 购入时间
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_product`;
CREATE TABLE `biz_product` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT        NOT NULL                COMMENT '发布者ID',
    `category_id`     BIGINT        NOT NULL                COMMENT '分类ID',
    `title`           VARCHAR(100)  NOT NULL                COMMENT '标题',
    `description`     TEXT                                  COMMENT '详细描述',
    `price`           DECIMAL(10,2) NOT NULL                COMMENT '出售价格',
    `original_price`  DECIMAL(10,2) DEFAULT NULL            COMMENT '原价，用于展示折扣',
    `cover_image`     VARCHAR(255)  DEFAULT NULL            COMMENT '封面图',
    `brand`           VARCHAR(50)   DEFAULT NULL            COMMENT '品牌，如 Apple',
    `model`           VARCHAR(100)  DEFAULT NULL            COMMENT '型号，如 iPhone 14 Pro',
    `condition_level` VARCHAR(20)   DEFAULT NULL            COMMENT '成色：全新/99新/95新/9成新/8成新/7成新及以下',
    `spec`            VARCHAR(1000) DEFAULT NULL            COMMENT '参数配置 JSON，如 {"内存":"8G","存储":"256G"}',
    `warranty`        VARCHAR(50)   DEFAULT NULL            COMMENT '质保情况：在保/过保/无质保',
    `purchase_date`   DATE          DEFAULT NULL            COMMENT '购入时间',
    `trade_location`  VARCHAR(100)  DEFAULT NULL            COMMENT '交易地点',
    `contact`         VARCHAR(100)  DEFAULT NULL            COMMENT '联系方式',
    `status`          TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0待审核 1已上架 2已下架 3已售出 4审核驳回',
    `reject_reason`   VARCHAR(255)  DEFAULT NULL            COMMENT '驳回原因',
    `view_count`      INT           NOT NULL DEFAULT 0      COMMENT '浏览量',
    `favorite_count`  INT           NOT NULL DEFAULT 0      COMMENT '收藏数',
    `publish_time`    DATETIME      DEFAULT NULL            COMMENT '审核通过上架时间',
    `audit_time`      DATETIME      DEFAULT NULL            COMMENT '最近审核时间',
    `auditor_id`      BIGINT        DEFAULT NULL            COMMENT '最近审核人ID',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_brand` (`brand`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_price` (`price`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '二手数码商品表';

-- ------------------------------------------------------------
-- 4. 商品图片表
-- 说明：一对多，不存 deleted，删除商品时物理删除图片记录，
--       但磁盘文件保留（避免误删，空间紧张时再清理）。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_product_image`;
CREATE TABLE `biz_product_image` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`  BIGINT       NOT NULL                COMMENT '商品ID',
    `url`         VARCHAR(255) NOT NULL                COMMENT '图片地址',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品图片表';

-- ------------------------------------------------------------
-- 5. 收藏表
-- 说明：不存 deleted，取消收藏即物理删除，避免
--       (user_id, product_id) 唯一索引与逻辑删除冲突。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_favorite`;
CREATE TABLE `biz_favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `product_id`  BIGINT   NOT NULL                COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_product` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品收藏表';

-- ------------------------------------------------------------
-- 6. 留言咨询表
-- 说明：parent_id = 0 表示对商品的一级留言，否则表示回复某条留言。
--       用 status 控制软删除（保留追溯能力），不用逻辑删除。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_comment`;
CREATE TABLE `biz_comment` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`    BIGINT       NOT NULL                COMMENT '商品ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '留言人ID',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0      COMMENT '父留言ID，0 表示一级留言',
    `reply_user_id` BIGINT       DEFAULT NULL            COMMENT '被回复人ID',
    `content`       VARCHAR(500) NOT NULL                COMMENT '留言内容',
    `is_read`       TINYINT      NOT NULL DEFAULT 0      COMMENT '商品发布者是否已读 0未读 1已读',
    `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1正常 0已屏蔽',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '留言时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品留言咨询表';

-- ------------------------------------------------------------
-- 7. 订单表
-- 状态流转：0待确认 -> 1已确认(待面交) -> 2已完成
--                      \-> 3已取消（买卖任一方取消）
--                      \-> 4已关闭（商品被下架/卖家拒绝）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`        VARCHAR(32)   NOT NULL                COMMENT '订单号',
    `product_id`      BIGINT        NOT NULL                COMMENT '商品ID',
    `product_title`   VARCHAR(100)  NOT NULL                COMMENT '商品标题快照',
    `product_cover`   VARCHAR(255)  DEFAULT NULL            COMMENT '商品封面快照',
    `buyer_id`        BIGINT        NOT NULL                COMMENT '买家ID',
    `seller_id`       BIGINT        NOT NULL                COMMENT '卖家ID',
    `price`           DECIMAL(10,2) NOT NULL                COMMENT '成交价快照',
    `contact`         VARCHAR(100)  DEFAULT NULL            COMMENT '买家联系方式',
    `trade_location`  VARCHAR(100)  DEFAULT NULL            COMMENT '约定交易地点',
    `remark`          VARCHAR(255)  DEFAULT NULL            COMMENT '买家留言',
    `cancel_reason`   VARCHAR(255)  DEFAULT NULL            COMMENT '取消原因',
    `status`          TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0待确认 1已确认 2已完成 3已取消 4已关闭',
    `finish_time`     DATETIME      DEFAULT NULL            COMMENT '完成时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer` (`buyer_id`),
    KEY `idx_seller` (`seller_id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品订单表';

-- ------------------------------------------------------------
-- 8. 举报表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_report`;
CREATE TABLE `biz_report` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`    BIGINT       NOT NULL                COMMENT '被举报商品ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '举报人ID',
    `type`          TINYINT      NOT NULL DEFAULT 1      COMMENT '举报类型 1虚假信息 2违禁物品 3价格欺诈 4其他',
    `reason`        VARCHAR(255) NOT NULL                COMMENT '举报说明',
    `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0待处理 1已处理 2已驳回',
    `handle_result` VARCHAR(255) DEFAULT NULL            COMMENT '处理结果',
    `handler_id`    BIGINT       DEFAULT NULL            COMMENT '处理人ID',
    `handle_time`   DATETIME     DEFAULT NULL            COMMENT '处理时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品举报表';

-- ------------------------------------------------------------
-- 9. 审核记录表（追加型，只增不改，用于全程追溯）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `biz_audit_record`;
CREATE TABLE `biz_audit_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`  BIGINT       NOT NULL                COMMENT '商品ID',
    `product_title` VARCHAR(100) DEFAULT NULL          COMMENT '商品标题快照',
    `admin_id`    BIGINT       NOT NULL                COMMENT '审核人ID',
    `admin_name`  VARCHAR(50)  DEFAULT NULL            COMMENT '审核人账号快照',
    `action`      TINYINT      NOT NULL                COMMENT '审核动作 1通过 2驳回 3强制下架',
    `reason`      VARCHAR(255) DEFAULT NULL            COMMENT '驳回原因',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_admin` (`admin_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品审核记录表';

-- ------------------------------------------------------------
-- 10. 站内通知表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '接收人ID',
    `type`        TINYINT      NOT NULL DEFAULT 1      COMMENT '类型 1审核结果 2订单状态 3留言回复 4系统通知',
    `title`       VARCHAR(100) NOT NULL                COMMENT '标题',
    `content`     VARCHAR(500) DEFAULT NULL            COMMENT '内容',
    `biz_id`      BIGINT       DEFAULT NULL            COMMENT '关联业务ID，如商品ID、订单ID',
    `is_read`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读 0未读 1已读',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内通知表';

-- ------------------------------------------------------------
-- 11. 轮播图表（首页展示）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_banner`;
CREATE TABLE `sys_banner` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(100) DEFAULT NULL            COMMENT '标题',
    `image`       VARCHAR(255) NOT NULL                COMMENT '图片地址',
    `link`        VARCHAR(255) DEFAULT NULL            COMMENT '跳转链接',
    `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1启用 0停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '首页轮播图表';

-- ------------------------------------------------------------
-- 12. 公告表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`        VARCHAR(100) NOT NULL                COMMENT '公告标题',
    `content`      TEXT                                 COMMENT '公告内容',
    `publisher_id` BIGINT       DEFAULT NULL            COMMENT '发布人ID',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1发布 0下架',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台公告表';


-- ============================================================
-- 初始化字典数据
-- 说明：用户数据由后端 DataInitializer 在首次启动时创建
--       （密码需要 BCrypt 加密，放在程序里生成更可靠）
-- ============================================================

INSERT INTO `sys_category` (`name`, `icon`, `sort`, `status`) VALUES
('手机',     'Iphone',       1, 1),
('笔记本电脑', 'Monitor',     2, 1),
('平板电脑',  'Cellphone',   3, 1),
('相机/摄像机', 'Camera',     4, 1),
('耳机/音响',  'Headset',    5, 1),
('智能穿戴',  'Watch',       6, 1),
('游戏设备',  'VideoPlay',   7, 1),
('数码配件',  'Cpu',         8, 1),
('电脑外设',  'Mouse',       9, 1),
('其他数码',  'MoreFilled', 10, 1);

INSERT INTO `sys_banner` (`title`, `image`, `link`, `sort`, `status`) VALUES
('校园二手数码交易平台', '/uploads/banner/banner1.jpg', '/product/list', 1, 1),
('闲置数码，物尽其用',   '/uploads/banner/banner2.jpg', '/product/list', 2, 1),
('数码好物，低价捡漏',   '/uploads/banner/banner3.jpg', '/product/list', 3, 1);

INSERT INTO `sys_notice` (`title`, `content`, `status`) VALUES
('平台使用规范', '请如实描述商品成色与参数，禁止发布虚假信息。一经核实，平台将下架商品并扣除信誉分。', 1),
('交易安全提醒', '建议校内当面交易、当面验货，避免线上转账产生纠纷。平台不对线下交易承担担保责任。', 1);
