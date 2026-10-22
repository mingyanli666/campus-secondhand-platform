-- ============================================================
-- 演示数据脚本
-- 用途：答辩 / 演示时填充足够的数据，让各页面都有内容
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 used_digital_platform < sql/demo-data.sql
--
-- 说明：
--   1. 所有演示账号的密码都是 123456（复用 admin 的 BCrypt 密文）
--   2. 演示用户 id 从 101 开始，商品 id 从 101 开始，不与已有数据冲突
--   3. 商品图片是 backend/uploads/demo/ 下生成的占位图
--   4. 本脚本可以重复执行（先删掉旧的演示数据再插入）
-- ============================================================

USE `used_digital_platform`;

-- ------------------------------------------------------------
-- 清理旧的演示数据（只删 id >= 100 的记录，不影响你自己创建的数据）
-- ------------------------------------------------------------
DELETE FROM `biz_report`        WHERE `product_id` >= 100;
DELETE FROM `biz_comment`       WHERE `product_id` >= 100;
DELETE FROM `biz_favorite`      WHERE `product_id` >= 100;
DELETE FROM `biz_order`         WHERE `product_id` >= 100;
DELETE FROM `biz_product_image` WHERE `product_id` >= 100;
DELETE FROM `biz_product`       WHERE `id` >= 100;
DELETE FROM `sys_user`          WHERE `id` >= 100;

-- ------------------------------------------------------------
-- 1. 演示用户（密码统一为 123456）
-- ------------------------------------------------------------
INSERT INTO `sys_user`
    (`id`, `username`, `password`, `nickname`, `phone`, `email`, `gender`, `campus`, `intro`, `role`, `status`, `credit_score`, `create_time`, `update_time`)
VALUES
(101, 'chenqi',   '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '陈七', '13800000101', 'chenqi@example.com',   1, '计算机学院',   '数码爱好者，出闲置换新机',   'USER', 1, 100, DATE_SUB(NOW(), INTERVAL 90 DAY), NOW()),
(102, 'wangba',   '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '王八', '13800000102', 'wangba@example.com',   1, '电子信息学院', '摄影爱好者，镜头和相机常换', 'USER', 1,  96, DATE_SUB(NOW(), INTERVAL 85 DAY), NOW()),
(103, 'zhaoliu',  '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '赵六', '13800000103', 'zhaoliu@example.com',  1, '机械工程学院', '毕业清仓，东西都很新',       'USER', 1, 100, DATE_SUB(NOW(), INTERVAL 80 DAY), NOW()),
(104, 'sunjiu',   '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '孙九', '13800000104', 'sunjiu@example.com',   2, '外国语学院',   '喜欢折腾手机，换机频繁',     'USER', 1,  92, DATE_SUB(NOW(), INTERVAL 70 DAY), NOW()),
(105, 'zhouyi',   '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '周怡', '13800000105', 'zhouyi@example.com',   2, '设计学院',     '设计专业，设备更新快',       'USER', 1, 100, DATE_SUB(NOW(), INTERVAL 65 DAY), NOW()),
(106, 'wuxin',    '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '吴欣', '13800000106', 'wuxin@example.com',    2, '数学学院',     '打游戏用的设备，保护得很好', 'USER', 1, 100, DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
(107, 'zhengkai', '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '郑凯', '13800000107', 'zhengkai@example.com', 1, '物理学院',     '偶尔出一些自用设备',         'USER', 1,  74, DATE_SUB(NOW(), INTERVAL 50 DAY), NOW()),
(108, 'liuyang',  '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '刘洋', '13800000108', 'liuyang@example.com',  1, '经管学院',     '喜欢数码产品，诚信交易',     'USER', 1, 100, DATE_SUB(NOW(), INTERVAL 40 DAY), NOW()),
(109, 'huangmin', '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '黄敏', '13800000109', 'huangmin@example.com', 2, '化学学院',     '闲置转让，价格好商量',       'USER', 1,  98, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(110, 'linfeng',  '$2a$10$w/gYB1/D00MDDw0YLg.D6e6HQi/OTVeInb011TFXI7LuYKFKYJqdG', '林峰', '13800000110', 'linfeng@example.com',  1, '土木学院',     '账号因违规被禁用（演示用）', 'USER', 0,  55, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW());


-- ------------------------------------------------------------
-- 2. 演示商品
-- 状态：0待审核 1已上架 2已下架 3已售出 4审核驳回
-- ------------------------------------------------------------
INSERT INTO `biz_product`
    (`id`, `user_id`, `category_id`, `title`, `description`, `price`, `original_price`, `cover_image`,
     `brand`, `model`, `condition_level`, `spec`, `warranty`, `purchase_date`,
     `trade_location`, `contact`, `status`, `reject_reason`, `view_count`, `favorite_count`,
     `publish_time`, `create_time`, `update_time`)
VALUES
-- ---------- 手机 ----------
(101, 101, 1, 'iPhone 13 128G 星光色 国行',
 '2022 年 9 月购入，一直带壳贴膜使用，无拆无修。电池健康 88%，日常使用一天没问题。原装充电线和包装盒都在，支持当面验机。',
 2799.00, 5999.00, '/uploads/demo/demo-01.png',
 'Apple', 'iPhone 13', '9成新', '{"内存":"4GB","存储":"128GB","电池健康":"88%","版本":"国行"}', '过保', '2022-09-20',
 '学校东门', '微信 chenqi_13', 1, NULL, 186, 12,
 DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

(102, 102, 1, '小米13 12+256 远山蓝',
 '自用一年，无磕碰，屏幕无划痕。骁龙8Gen2 日常很流畅，67W 快充半小时充满。原装充电器+包装盒齐全。',
 2199.00, 4599.00, '/uploads/demo/demo-02.png',
 'Xiaomi', '13', '95新', '{"内存":"12GB","存储":"256GB","电池健康":"91%","版本":"国行"}', '在保', '2023-03-10',
 '图书馆门口', 'QQ 100200301', 3, NULL, 254, 18,
 DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),

(103, 103, 1, '华为 Mate50 256G 曜金黑',
 '昆仑玻璃版本，抗摔。使用一年半，功能全部正常，无维修记录。支持鸿蒙 4.0，拍照很能打。',
 2899.00, 5488.00, '/uploads/demo/demo-03.png',
 'Huawei', 'Mate50', '9成新', '{"内存":"8GB","存储":"256GB","电池健康":"86%","版本":"国行"}', '过保', '2022-11-05',
 '三号宿舍楼下', '微信 zhaoliu_m50', 1, NULL, 143, 9,
 DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),

(104, 104, 1, 'OPPO Find X6 16+512 大内存',
 '顶配版本，哈苏影像。只用过半年，几乎全新，出厂膜都还在。因换机出，价格可以小刀。',
 2999.00, 5999.00, '/uploads/demo/demo-04.png',
 'OPPO', 'Find X6', '95新', '{"内存":"16GB","存储":"512GB","电池健康":"95%","版本":"国行"}', '在保', '2023-08-15',
 '学校南门', '微信 sunjiu_fx6', 1, NULL, 97, 7,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),

-- ---------- 笔记本 ----------
(105, 105, 2, 'MacBook Air M2 8+256 午夜色',
 '2023 款，设计课买的，主要用来跑 Figma 和 PS，性能完全够。循环次数 120 次，电池健康 96%。原装 35W 双口充电器。',
 5799.00, 8999.00, '/uploads/demo/demo-05.png',
 'Apple', 'MacBook Air M2', '95新', '{"处理器":"M2","内存":"8GB","硬盘":"256GB","屏幕":"13.6英寸"}', '在保', '2023-07-01',
 '设计学院楼', '微信 zhouyi_mba', 1, NULL, 412, 36,
 DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), NOW()),

(106, 106, 2, '联想拯救者 R9000P 2023 游戏本',
 'R7-7745HX + RTX4060，16G 内存 1T 固态。打游戏很稳，散热好。一直插电用，电池损耗很小。附原装电源适配器（比较大）。',
 6599.00, 9999.00, '/uploads/demo/demo-06.png',
 'Lenovo', 'R9000P 2023', '9成新', '{"处理器":"R7-7745HX","内存":"16GB","硬盘":"1TB","显卡":"RTX4060"}', '过保', '2023-05-12',
 '六号宿舍楼', 'QQ 556677889', 1, NULL, 328, 24,
 DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),

(107, 107, 2, '戴尔 XPS 13 9315 轻薄本',
 'i5-1230U / 16G / 512G，屏幕素质很好。有轻微使用痕迹（A 面一小道划痕，不影响使用），已如实拍出。',
 4299.00, 7999.00, '/uploads/demo/demo-07.png',
 'Dell', 'XPS 13 9315', '8成新', '{"处理器":"i5-1230U","内存":"16GB","硬盘":"512GB","屏幕":"13.4英寸"}', '过保', '2022-10-08',
 '图书馆三楼', '微信 zhengkai_xps', 1, NULL, 158, 11,
 DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY), NOW()),

(108, 108, 2, '华硕天选4 游戏本 4060',
 'i7-13620H + RTX4060，16G/512G。去年双十一买的，还在保修期内。打游戏和做课设都够用。',
 5899.00, 8499.00, '/uploads/demo/demo-08.png',
 'ASUS', '天选4', '9成新', '{"处理器":"i7-13620H","内存":"16GB","硬盘":"512GB","显卡":"RTX4060"}', '在保', '2023-11-11',
 '七号宿舍楼', '微信 liuyang_tx4', 1, NULL, 203, 15,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

-- ---------- 平板 ----------
(109, 109, 3, 'iPad Air 5 64G 紫色 WiFi版',
 '买来记笔记用的，配了笔但笔单独出。机身无磕碰，屏幕完好。续航正常，看视频能撑一天。',
 2899.00, 4399.00, '/uploads/demo/demo-09.png',
 'Apple', 'iPad Air 5', '95新', '{"内存":"8GB","存储":"64GB","屏幕尺寸":"10.9英寸","网络版本":"WiFi"}', '在保', '2023-02-14',
 '教学楼 B 座', '微信 huangmin_ipad', 1, NULL, 267, 21,
 DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 26 DAY), NOW()),

(110, 101, 3, '华为 MatePad 11 2023 柔光版',
 '柔光屏护眼版本，适合看论文。6+128，附原装键盘保护套。用了一年，功能正常。',
 1599.00, 2599.00, '/uploads/demo/demo-10.png',
 'Huawei', 'MatePad 11 2023', '9成新', '{"内存":"6GB","存储":"128GB","屏幕尺寸":"11英寸","网络版本":"WiFi"}', '过保', '2023-04-20',
 '学校东门', '微信 chenqi_mp11', 1, NULL, 128, 6,
 DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),

-- ---------- 相机 ----------
(111, 102, 4, '索尼 A6400 微单 16-50 套机',
 '快门数约 12000，机身有轻微使用痕迹。适合入门拍人像和 vlog。附两块电池和 64G 卡。',
 4299.00, 6999.00, '/uploads/demo/demo-11.png',
 'Sony', 'A6400', '9成新', '{"有效像素":"2420万","快门次数":"12000","镜头":"16-50mm 套机","配件":"双电池+64G卡"}', '过保', '2021-06-18',
 '学校西门', '微信 wangba_a6400', 1, NULL, 342, 28,
 DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),

(112, 103, 4, '佳能 EOS R6 全画幅机身',
 '快门数约 20000，成色很好。全画幅微单，暗光表现优秀。机身、原装电池、充电器齐全，不含镜头。',
 8999.00, 15999.00, '/uploads/demo/demo-12.png',
 'Canon', 'EOS R6', '95新', '{"有效像素":"2010万","快门次数":"20000","镜头":"不含镜头","配件":"原装电池+充电器"}', '过保', '2021-11-30',
 '摄影社团活动室', '微信 zhaoliu_r6', 1, NULL, 456, 39,
 DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), NOW()),

(113, 105, 4, '富士 X-T4 18-55 套机',
 '胶片模拟很好玩，直出好看。快门数 8000 左右，机身无磕碰。整套出，不单卖镜头。',
 7499.00, 11999.00, '/uploads/demo/demo-13.png',
 'Fujifilm', 'X-T4', '9成新', '{"有效像素":"2610万","快门次数":"8000","镜头":"18-55mm 套机","配件":"原装电池两块"}', '过保', '2022-03-25',
 '设计学院楼', '微信 zhouyi_xt4', 1, NULL, 389, 31,
 DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),

-- ---------- 耳机 ----------
(114, 106, 5, 'AirPods Pro 2 USB-C 版本',
 '去年买的，降噪很好用。因为换了头戴式所以出。耳塞已更换全新（附三个尺寸），充电盒无划痕。',
 1199.00, 1899.00, '/uploads/demo/demo-14.png',
 'Apple', 'AirPods Pro 2', '99新', '{"类型":"入耳式","连接方式":"蓝牙","降噪":"主动降噪","续航":"6小时+30小时"}', '在保', '2023-10-01',
 '六号宿舍楼', 'QQ 223344556', 1, NULL, 512, 47,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),

(115, 107, 5, '索尼 WH-1000XM5 头戴降噪',
 '降噪天花板，通勤神器。用了一年，头梁无开裂，耳罩有轻微使用痕迹。附原装收纳盒。',
 1599.00, 2899.00, '/uploads/demo/demo-15.png',
 'Sony', 'WH-1000XM5', '95新', '{"类型":"头戴式","连接方式":"蓝牙","降噪":"主动降噪","续航":"30小时"}', '在保', '2023-01-15',
 '图书馆三楼', '微信 zhengkai_xm5', 1, NULL, 276, 19,
 DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY), NOW()),

(116, 104, 5, '漫步者 W820NB 头戴耳机',
 '入门降噪，性价比高。用了半年，功能正常，耳罩干净。学生党首选。',
 199.00, 399.00, '/uploads/demo/demo-16.png',
 'Edifier', 'W820NB', '9成新', '{"类型":"头戴式","连接方式":"蓝牙","降噪":"主动降噪","续航":"49小时"}', '过保', '2023-06-10',
 '学校南门', '微信 sunjiu_w820', 1, NULL, 88, 4,
 DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),

-- ---------- 智能穿戴 ----------
(117, 108, 6, 'Apple Watch S8 45mm GPS',
 '午夜色铝金属，配运动表带。使用一年多，屏幕无划痕，电池健康 92%。附原装磁吸充电线。',
 1699.00, 3199.00, '/uploads/demo/demo-17.png',
 'Apple', 'Watch S8', '95新', '{"表盘尺寸":"45mm","续航":"18小时","网络版本":"GPS","配件":"原装充电线"}', '在保', '2022-12-20',
 '七号宿舍楼', '微信 liuyang_s8', 1, NULL, 194, 13,
 DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), NOW()),

(118, 109, 6, '华为手环 8 NFC 版',
 '买来跑步用的，戴了两个月。NFC 可以刷门禁和公交。附原装充电线。',
 159.00, 299.00, '/uploads/demo/demo-18.png',
 'Huawei', '手环8', '99新', '{"表盘尺寸":"1.47英寸","续航":"14天","网络版本":"NFC","配件":"原装充电线"}', '在保', '2024-03-01',
 '教学楼 B 座', '微信 huangmin_b8', 1, NULL, 76, 5,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),

-- ---------- 游戏设备 ----------
(119, 101, 7, 'Switch OLED 白色 日版',
 '日版续航加强款，可在家玩也可带出门。买来主要玩塞尔达，通关后闲置。附原装底座和 HDMI 线。',
 1499.00, 2099.00, '/uploads/demo/demo-19.png',
 'Nintendo', 'Switch OLED', '95新', '{"版本":"日版","手柄数量":"2","是否破解":"未破解","配件":"底座+HDMI线"}', '过保', '2023-03-05',
 '学校东门', '微信 chenqi_switch', 1, NULL, 431, 33,
 DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),

(120, 102, 7, 'PS5 原装手柄 白色 DualSense',
 '随机附带的原装手柄，用得不多。按键手感正常，无漂移。',
 299.00, 499.00, '/uploads/demo/demo-20.png',
 'Sony', 'DualSense', '9成新', '{"版本":"国行","手柄数量":"1","是否破解":"否","配件":"原装数据线"}', '过保', '2023-02-10',
 '图书馆门口', 'QQ 100200301', 1, NULL, 112, 8,
 DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),

-- ---------- 数码配件 ----------
(121, 103, 8, '安克 65W 氮化镓充电器',
 '一个头能充笔记本+手机，出差很方便。用了几个月，成色很好。附原装 C to C 线。',
 99.00, 199.00, '/uploads/demo/demo-21.png',
 'Anker', 'Nano 65W', '99新', '{"规格":"65W","接口":"2C1A","是否原装":"原装"}', '在保', '2023-09-10',
 '三号宿舍楼下', '微信 zhaoliu_anker', 1, NULL, 64, 3,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),

(122, 105, 8, '紫米 20000mAh 充电宝',
 '支持 50W 快充，能充笔记本。容量大但略重，出门带包的话没问题。',
 129.00, 249.00, '/uploads/demo/demo-22.png',
 'ZMI', 'QB826', '9成新', '{"规格":"20000mAh","接口":"2C1A","是否原装":"原装"}', '过保', '2023-01-20',
 '设计学院楼', '微信 zhouyi_zmi', 1, NULL, 57, 2,
 DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),

-- ---------- 电脑外设 ----------
(123, 106, 9, '罗技 MX Keys 无线键盘',
 '办公神器，手感好。用了两年，功能全部正常，键帽无明显油光。附原装接收器。',
 469.00, 899.00, '/uploads/demo/demo-23.png',
 'Logitech', 'MX Keys', '95新', '{"类型":"无线键盘","连接方式":"蓝牙+优联","轴体":"剪刀脚","是否原装":"原装"}', '过保', '2022-05-15',
 '六号宿舍楼', 'QQ 556677889', 1, NULL, 148, 10,
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),

-- ---------- 其他数码 ----------
(124, 108, 10, 'Kindle Paperwhite 5 8G',
 '看书神器，墨水屏不伤眼。用了一年多，屏幕完好，附原装保护套。',
 449.00, 998.00, '/uploads/demo/demo-24.png',
 'Amazon', 'Paperwhite 5', '9成新', '{"规格":"8GB","屏幕":"6.8英寸墨水屏","成色说明":"屏幕无划痕"}', '过保', '2022-11-11',
 '七号宿舍楼', '微信 liuyang_kindle', 1, NULL, 203, 16,
 DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY), NOW()),

-- ---------- 其他状态的商品（用于演示审核/下架/驳回） ----------
(125, 109, 1, '一加 12 16+512 岩板绿',
 '刚买一个月，几乎全新，因换机出。骁龙8Gen3，屏幕素质很好。',
 3999.00, 4999.00, '/uploads/demo/demo-01.png',
 'OnePlus', '12', '99新', '{"内存":"16GB","存储":"512GB","电池健康":"100%","版本":"国行"}', '在保', '2024-08-01',
 '教学楼 B 座', '微信 huangmin_1p12', 0, NULL, 0, 0,
 NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

(126, 104, 3, '小米平板 6 Pro 8+256',
 '看剧和记笔记都合适，144Hz 屏幕。用了一年，成色不错。',
 1299.00, 1999.00, '/uploads/demo/demo-10.png',
 'Xiaomi', '平板6 Pro', '9成新', '{"内存":"8GB","存储":"256GB","屏幕尺寸":"11英寸","网络版本":"WiFi"}', '过保', '2023-05-20',
 '学校南门', '微信 sunjiu_pad6', 3, NULL, 176, 14,
 DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), NOW()),

(127, 110, 1, '华为 P60 256G',
 '自用机，成色很好。',
 2499.00, 4488.00, '/uploads/demo/demo-03.png',
 'Huawei', 'P60', '9成新', '{"内存":"8GB","存储":"256GB","电池健康":"90%","版本":"国行"}', '过保', '2023-03-01',
 '土木学院楼', '微信 linfeng_p60', 4, '商品描述过于简单，请补充实物图片、成色细节和配件情况后重新提交。', 0, 0,
 NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

(128, 107, 4, '索尼 A7M3 全画幅机身',
 '快门数 50000，功能正常。',
 6999.00, 12999.00, '/uploads/demo/demo-11.png',
 'Sony', 'A7M3', '8成新', '{"有效像素":"2420万","快门次数":"50000","镜头":"不含镜头","配件":"电池+充电器"}', '过保', '2020-08-15',
 '图书馆三楼', '微信 zhengkai_a7m3', 2, '卖家主动下架，准备重新整理后再上架。', 88, 5,
 DATE_SUB(NOW(), INTERVAL 50 DAY), DATE_SUB(NOW(), INTERVAL 50 DAY), NOW());


-- ------------------------------------------------------------
-- 3. 商品图片（每个商品一张，复用演示图）
-- ------------------------------------------------------------
INSERT INTO `biz_product_image` (`product_id`, `url`, `sort`, `create_time`)
SELECT p.id,
       CONCAT('/uploads/demo/demo-', LPAD(((p.id - 101) % 24) + 1, 2, '0'), '.png'),
       0,
       p.create_time
FROM `biz_product` p
WHERE p.id >= 100;


-- ------------------------------------------------------------
-- 4. 收藏记录
-- ------------------------------------------------------------
INSERT INTO `biz_favorite` (`user_id`, `product_id`, `create_time`) VALUES
(101, 105, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(101, 109, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(101, 114, DATE_SUB(NOW(), INTERVAL  5 DAY)),
(102, 101, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(102, 113, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(103, 106, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(103, 111, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(103, 119, DATE_SUB(NOW(), INTERVAL  8 DAY)),
(104, 112, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(104, 117, DATE_SUB(NOW(), INTERVAL  7 DAY)),
(105, 107, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(106, 108, DATE_SUB(NOW(), INTERVAL  9 DAY)),
(106, 115, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(107, 101, DATE_SUB(NOW(), INTERVAL  4 DAY)),
(108, 104, DATE_SUB(NOW(), INTERVAL  6 DAY)),
(109, 102, DATE_SUB(NOW(), INTERVAL 21 DAY)),
(109, 114, DATE_SUB(NOW(), INTERVAL  3 DAY));


-- ------------------------------------------------------------
-- 5. 留言咨询（一级留言 id 1000+，回复 id 1200+）
-- ------------------------------------------------------------
INSERT INTO `biz_comment` (`id`, `product_id`, `user_id`, `parent_id`, `reply_user_id`, `content`, `is_read`, `status`, `create_time`) VALUES
-- iPhone 13 (101) 下的对话
(1001, 101, 102, 0,    NULL, '电池健康 88% 还能用多久？一天两充吗？', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1002, 101, 101, 1001, 102,  '正常上课、刷手机的话一天一充，重度使用下午需要补一次。88% 还算健康，不用马上换电池。', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1003, 101, 102, 1001, 101,  '好的，那我考虑一下，能小刀到 2600 吗？', 0, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1004, 101, 103, 0,    NULL, '国行还是港版？有没有维修记录？', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1005, 101, 101, 1004, 103,  '国行，无任何维修记录，可以当面查序列号。', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- MacBook Air M2 (105) 下的对话
(1006, 105, 101, 0,    NULL, '电池循环 120 次，那大概用了多久？', 1, 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(1007, 105, 105, 1006, 101,  '不到一年，主要跑设计软件，基本插电用。', 1, 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),

-- iPad Air 5 (109) 下的对话
(1008, 109, 102, 0,    NULL, '支持 Apple Pencil 2 吗？笔一起出不出？', 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1009, 109, 109, 1008, 102,  '支持二代笔，但笔不出，单独留着用。', 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),

-- 索尼 A6400 (111) 下的对话
(1010, 111, 105, 0,    NULL, '快门数 12000 会不会有点多？能看看实拍样张吗？', 1, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1011, 111, 102, 1010, 105,  '12000 算中等，机器状态很好。可以加微信发原图给你看。', 1, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),

-- AirPods Pro 2 (114) 下的对话
(1012, 114, 104, 0,    NULL, '是 USB-C 版本吗？充电盒有划痕吗？', 1, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1013, 114, 106, 1012, 104,  '是 USB-C 版本，充电盒基本无划痕，一直在壳里。', 1, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Switch (119) 下的对话
(1014, 119, 103, 0,    NULL, '日版能玩国服游戏吗？账号能一起给吗？', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1015, 119, 101, 1014, 103,  '日版可以登录任意区服账号。账号里有几个数字版游戏，可以商量一起出。', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY));


-- ------------------------------------------------------------
-- 6. 订单（覆盖已完成 / 已确认 / 待确认 / 已取消 / 已关闭 五种状态）
-- ------------------------------------------------------------
INSERT INTO `biz_order`
    (`order_no`, `product_id`, `product_title`, `product_cover`, `buyer_id`, `seller_id`, `price`,
     `contact`, `trade_location`, `remark`, `cancel_reason`, `status`, `finish_time`, `create_time`, `update_time`)
VALUES
-- 已完成：赵六买了王八的小米13
('OD202609100930001001', 102, '小米13 12+256 远山蓝', '/uploads/demo/demo-02.png', 103, 102, 2199.00,
 '微信 zhaoliu_m50', '图书馆门口', '周六下午方便吗？', NULL, 2,
 DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),

-- 已完成：陈七买了孙九的小米平板 6 Pro（商品本身已是售出状态）
('OD202609151420002002', 126, '小米平板 6 Pro 8+256', '/uploads/demo/demo-10.png', 101, 104, 1299.00,
 '微信 chenqi_13', '学校南门', '想当面验一下屏幕', NULL, 2,
 DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),

-- 已确认：王八买了黄敏的 iPad Air 5，等待线下交易
('OD202610151015003003', 109, 'iPad Air 5 64G 紫色 WiFi版', '/uploads/demo/demo-09.png', 102, 109, 2899.00,
 'QQ 100200301', '教学楼 B 座', '明天中午可以交易', NULL, 1,
 NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- 待确认：孙九想买王八的索尼 A6400，卖家还没处理
('OD202610181830004004', 111, '索尼 A6400 微单 16-50 套机', '/uploads/demo/demo-11.png', 104, 102, 4299.00,
 '微信 sunjiu_fx6', '学校西门', '能不能先看看相机实拍样张？', NULL, 0,
 NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 已取消：吴欣买了刘洋的 Apple Watch，后来取消
('OD202610050910005005', 117, 'Apple Watch S8 45mm GPS', '/uploads/demo/demo-17.png', 106, 108, 1699.00,
 '微信 wuxin_watch', '七号宿舍楼', '周末有空', '已经买到别的了，抱歉。', 3,
 NULL, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),

-- 已关闭：郑凯买周怡的充电宝，商品被平台下架导致订单关闭
('OD202610080830006006', 122, '紫米 20000mAh 充电宝', '/uploads/demo/demo-22.png', 107, 105, 129.00,
 '微信 zhengkai_xm5', '设计学院楼', '随时可以', '商品被平台下架，订单自动关闭。', 4,
 NULL, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));


-- ------------------------------------------------------------
-- 7. 举报记录（2 条待处理 + 1 条已处理 + 1 条已驳回）
-- ------------------------------------------------------------
INSERT INTO `biz_report`
    (`product_id`, `user_id`, `type`, `reason`, `status`, `handle_result`, `handler_id`, `handle_time`, `create_time`)
VALUES
-- 待处理：戴尔 XPS 的成色描述可能有夸大
(107, 101, 1, '卖家的商品描述写的是"屏幕素质很好"，但图片里能看到 A 面有明显划痕，和描述不符，建议核实。', 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 待处理：漫步者耳机的价格疑似引流
(116, 103, 3, '这款耳机标价 199，但点进去看描述要求加微信私聊，疑似低价引流。', 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 已处理：PS5 手柄的描述曾被举报，核实后要求补充
(120, 105, 1, '描述里没有说明手柄是否为原装，担心是翻新货。', 1, '经核实卖家已补充原装说明和购买凭证，举报成立但商品无需下架。', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
-- 已驳回：富士相机的举报不成立
(113, 106, 3, '同样的相机别人都卖 8000 以上，这个 7499 便宜得离谱，怀疑是骗局。', 2, '经核实该价格属于正常二手行情区间（快门数较高），未发现异常。', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));


-- ------------------------------------------------------------
-- 8. 站内通知
-- 通知是业务动作的副作用，没有独立的主键区间，所以整体清空后重建，
-- 保证演示时铃铛里的内容与演示数据一致（而不是残留测试产生的消息）
-- ------------------------------------------------------------
DELETE FROM `sys_notification`;

INSERT INTO `sys_notification` (`user_id`, `type`, `title`, `content`, `biz_id`, `is_read`, `create_time`) VALUES
-- 审核通过
(101, 1, '商品审核通过', '你发布的「iPhone 13 128G 星光色 国行」已通过审核，其他用户现在可以看到它了。', 101, 1, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(102, 1, '商品审核通过', '你发布的「小米13 12+256 远山蓝」已通过审核，其他用户现在可以看到它了。', 102, 1, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(105, 1, '商品审核通过', '你发布的「MacBook Air M2 8+256 午夜色」已通过审核，其他用户现在可以看到它了。', 105, 1, DATE_SUB(NOW(), INTERVAL 35 DAY)),
(109, 1, '商品审核通过', '你发布的「iPad Air 5 64G 紫色 WiFi版」已通过审核，其他用户现在可以看到它了。', 109, 1, DATE_SUB(NOW(), INTERVAL 26 DAY)),

-- 订单流转
(102, 2, '你有一笔新的订单',
 '有买家想买你的「索尼 A6400 微单 16-50 套机」。请尽快确认或取消，避免买家久等。',
 (SELECT id FROM `biz_order` WHERE `order_no` = 'OD202610181830004004'), 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(109, 2, '你有一笔新的订单',
 '有买家想买你的「iPad Air 5 64G 紫色 WiFi版」。请尽快确认或取消，避免买家久等。',
 (SELECT id FROM `biz_order` WHERE `order_no` = 'OD202610151015003003'), 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(102, 2, '卖家已确认订单',
 '你购买「iPad Air 5 64G 紫色 WiFi版」的订单已被卖家确认。请按约定联系卖家完成交易，交易完成后记得点「确认完成」。',
 (SELECT id FROM `biz_order` WHERE `order_no` = 'OD202610151015003003'), 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(102, 2, '订单已完成',
 '「小米13 12+256 远山蓝」的交易已完成，商品已标记为售出。感谢你使用本平台。',
 (SELECT id FROM `biz_order` WHERE `order_no` = 'OD202609100930001001'), 1, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(107, 2, '订单已关闭',
 '「紫米 20000mAh 充电宝」已被平台下架，相关订单已自动关闭。原因：商品不符合发布规范。',
 (SELECT id FROM `biz_order` WHERE `order_no` = 'OD202610080830006006'), 0, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- 留言回复
(102, 3, '你的商品收到新留言',
 '有人咨询了你发布的「索尼 A6400 微单 16-50 套机」：快门数 12000 会不会有点多？能看看实拍样张吗？',
 111, 0, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(101, 3, '你的留言收到回复',
 '王八 回复了你在「iPhone 13 128G 星光色 国行」下的留言：正常上课、刷手机的话一天一充，重度使用下午需要补一次。',
 101, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- 举报处理结果
(105, 4, '你的举报已处理',
 '你举报的「PS5 原装手柄 白色 DualSense」经核实存在违规。处理结论：经核实卖家已补充原装说明和购买凭证，举报成立但商品无需下架。',
 120, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(106, 4, '你的举报未通过',
 '你举报的「富士 X-T4 18-55 套机」经核实未发现违规。处理结论：经核实该价格属于正常二手行情区间（快门数较高），未发现异常。',
 113, 0, DATE_SUB(NOW(), INTERVAL 7 DAY));


-- ------------------------------------------------------------
-- 9. 刷新商品的收藏数（让数字和实际收藏记录一致）
-- ------------------------------------------------------------
UPDATE `biz_product` p
SET p.`favorite_count` = (
    SELECT COUNT(*) FROM `biz_favorite` f WHERE f.`product_id` = p.`id`
)
WHERE p.`id` >= 100;


-- ------------------------------------------------------------
-- 完成
-- ------------------------------------------------------------
SELECT '演示数据导入完成' AS message,
       (SELECT COUNT(*) FROM `sys_user`    WHERE `id` >= 100) AS demo_users,
       (SELECT COUNT(*) FROM `biz_product` WHERE `id` >= 100) AS demo_products,
       (SELECT COUNT(*) FROM `biz_favorite` WHERE `product_id` >= 100) AS demo_favorites,
       (SELECT COUNT(*) FROM `biz_comment`  WHERE `product_id` >= 100) AS demo_comments,
       (SELECT COUNT(*) FROM `biz_order`    WHERE `product_id` >= 100) AS demo_orders,
       (SELECT COUNT(*) FROM `biz_report`   WHERE `product_id` >= 100) AS demo_reports,
       (SELECT COUNT(*) FROM `sys_notification`) AS demo_notifications;
