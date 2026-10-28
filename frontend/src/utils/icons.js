/**
 * 项目实际用到的 Element Plus 图标登记表。
 *
 * 为什么需要这个文件？
 * Element Plus 官方文档给的注册方式是「全量注册」：
 *
 *     import * as ElementPlusIconsVue from '@element-plus/icons-vue'
 *     for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
 *       app.component(name, component)
 *     }
 *
 * 这样会把全部 293 个图标都打进产物。而 unplugin-vue-components 的
 * 自动按需引入只能处理「模板里写死的标签名」，例如 <Search />；
 * 本项目有几个地方的图标名是运行时才知道的：
 *
 *   - AdminLayout 左侧菜单：icon 字段来自菜单配置数组
 *   - Home / CategoryManage：图标名来自数据库 sys_category.icon
 *   - Dashboard 统计卡片：icon 字段来自卡片配置
 *
 * 这些是 <component :is="xxx" /> 的用法，构建期静态分析不出来，
 * 所以必须在这里显式登记。
 *
 * 维护约定：
 *   - 模板里新增了 <el-icon><Xxx /></el-icon>  → 把 Xxx 加进来
 *   - 新增了数据库可配置的图标（如分类图标）    → 同时加到这里和
 *     CategoryManage.vue 的 ICON_OPTIONS，并同步 sql/init.sql 的注释
 */

import {
  Aim,
  ArrowDown,
  ArrowRight,
  Basketball,
  Bell,
  Bicycle,
  Box,
  Camera,
  Cellphone,
  ChatDotRound,
  Clock,
  Compass,
  Cpu,
  DataLine,
  Discount,
  Document,
  DocumentCopy,
  Expand,
  Files,
  Film,
  Fold,
  Football,
  Goods,
  Grid,
  Headset,
  Iphone,
  Lightning,
  Location,
  Lock,
  MagicStick,
  Microphone,
  Monitor,
  MoreFilled,
  Mouse,
  Notebook,
  Phone,
  Picture,
  Plus,
  Postcard,
  Present,
  Printer,
  Reading,
  Search,
  Setting,
  ShoppingCart,
  Star,
  StarFilled,
  Suitcase,
  Trophy,
  User,
  VideoPlay,
  View,
  Wallet,
  Warning,
  Watch
} from '@element-plus/icons-vue'

/**
 * 图标名 → 组件。
 * 只能用「具名导入 + 对象字面量」的写法，不能用 import *，
 * 否则打包器无法判断哪些被用到，tree-shaking 会失效。
 */
export const icons = {
  Aim,
  ArrowDown,
  ArrowRight,
  Basketball,
  Bell,
  Bicycle,
  Box,
  Camera,
  Cellphone,
  ChatDotRound,
  Clock,
  Compass,
  Cpu,
  DataLine,
  Discount,
  Document,
  DocumentCopy,
  Expand,
  Files,
  Film,
  Fold,
  Football,
  Goods,
  Grid,
  Headset,
  Iphone,
  Lightning,
  Location,
  Lock,
  MagicStick,
  Microphone,
  Monitor,
  MoreFilled,
  Mouse,
  Notebook,
  Phone,
  Picture,
  Plus,
  Postcard,
  Present,
  Printer,
  Reading,
  Search,
  Setting,
  ShoppingCart,
  Star,
  StarFilled,
  Suitcase,
  Trophy,
  User,
  VideoPlay,
  View,
  Wallet,
  Warning,
  Watch
}

/** 默认图标：名字非法或图标不存在时用它兜底，避免页面上出现空白格 */
export const FALLBACK_ICON = 'Goods'

/** 判断图标名是否已登记 */
export function hasIcon(name) {
  return Boolean(name && icons[name])
}

/**
 * 图标名兜底。
 * 数据库里的 sys_category.icon 是历史数据，可能存着不存在的图标名，
 * 这时退回默认图标，而不是渲染出一个空白格。
 */
export function resolveIcon(name) {
  return hasIcon(name) ? name : FALLBACK_ICON
}
