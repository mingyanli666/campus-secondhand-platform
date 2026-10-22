/**
 * 展示层格式化工具。后端存的是"事实数据"，怎么显示由前端决定。
 */

/** 价格显示：12 -> 12.00 */
export function formatPrice(value) {
  if (value === null || value === undefined || value === '') return '面议'
  const num = Number(value)
  if (Number.isNaN(num)) return '面议'
  return num.toFixed(2)
}

/** 时间显示：2026-01-01 10:00:00 -> 2026-01-01 10:00 */
export function formatTime(value, withSecond = false) {
  if (!value) return '-'
  const str = String(value).replace('T', ' ')
  return withSecond ? str.slice(0, 19) : str.slice(0, 16)
}

/** 相对时间：刚刚 / 3分钟前 / 2小时前 / 5天前 / 具体日期 */
export function fromNow(value) {
  if (!value) return '-'
  // 兼容 2026-01-01T10:00:00 与 2026-01-01 10:00:00 两种格式
  const time = new Date(String(value).replace('T', ' ').replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return '-'
  const diff = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
  if (diff < day) return `${Math.floor(diff / hour)}小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)}天前`
  return formatTime(value)
}

/** 商品状态 -> 标签样式 */
export const PRODUCT_STATUS = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '在售', type: 'success' },
  2: { text: '已下架', type: 'info' },
  3: { text: '已售出', type: 'danger' },
  4: { text: '审核驳回', type: 'danger' }
}

export function productStatus(code) {
  return PRODUCT_STATUS[code] || { text: '未知', type: 'info' }
}

/** 订单状态 -> 标签样式 */
export const ORDER_STATUS = {
  0: { text: '待确认', type: 'warning' },
  1: { text: '已确认', type: 'primary' },
  2: { text: '已完成', type: 'success' },
  3: { text: '已取消', type: 'info' },
  4: { text: '已关闭', type: 'info' }
}

export function orderStatus(code) {
  return ORDER_STATUS[code] || { text: '未知', type: 'info' }
}

/** 成色选项，统一维护，避免各处手写 */
export const CONDITION_OPTIONS = [
  '全新',
  '99新',
  '95新',
  '9成新',
  '8成新',
  '7成新及以下'
]

/** 质保选项 */
export const WARRANTY_OPTIONS = ['在保', '过保', '无质保']
