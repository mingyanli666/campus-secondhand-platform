import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  formatPrice,
  formatTime,
  fromNow,
  productStatus,
  orderStatus,
  CONDITION_OPTIONS,
  WARRANTY_OPTIONS
} from '@/utils/format'

describe('formatPrice 价格展示', () => {
  it('数字保留两位小数', () => {
    expect(formatPrice(12)).toBe('12.00')
    expect(formatPrice(12.5)).toBe('12.50')
    expect(formatPrice(0)).toBe('0.00')
  })

  it('null / undefined / 空串表示"面议"', () => {
    expect(formatPrice(null)).toBe('面议')
    expect(formatPrice(undefined)).toBe('面议')
    expect(formatPrice('')).toBe('面议')
  })

  it('非数字字符串不会显示成 NaN', () => {
    expect(formatPrice('abc')).toBe('面议')
  })
})

describe('formatTime 时间展示', () => {
  it('默认精确到分钟', () => {
    expect(formatTime('2026-01-01 10:00:00')).toBe('2026-01-01 10:00')
  })

  it('兼容后端可能返回的 ISO 格式（带 T）', () => {
    expect(formatTime('2026-01-01T10:00:00')).toBe('2026-01-01 10:00')
  })

  it('withSecond 时精确到秒', () => {
    expect(formatTime('2026-01-01 10:00:00', true)).toBe('2026-01-01 10:00:00')
  })

  it('空值显示为 -', () => {
    expect(formatTime('')).toBe('-')
    expect(formatTime(null)).toBe('-')
  })
})

describe('fromNow 相对时间', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-10 12:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('一分钟内显示"刚刚"', () => {
    expect(fromNow('2026-01-10 11:59:30')).toBe('刚刚')
  })

  it('一小时内显示分钟', () => {
    expect(fromNow('2026-01-10 11:30:00')).toBe('30分钟前')
  })

  it('一天内显示小时', () => {
    expect(fromNow('2026-01-10 09:00:00')).toBe('3小时前')
  })

  it('一周内显示天数', () => {
    expect(fromNow('2026-01-05 12:00:00')).toBe('5天前')
  })

  it('超过一周回落成具体日期', () => {
    expect(fromNow('2025-12-01 12:00:00')).toBe('2025-12-01 12:00')
  })

  it('非法时间不抛异常', () => {
    expect(fromNow('不是时间')).toBe('-')
    expect(fromNow(null)).toBe('-')
  })
})

describe('状态码映射', () => {
  it('商品五种状态都有文案', () => {
    expect(productStatus(0).text).toBe('待审核')
    expect(productStatus(1).text).toBe('在售')
    expect(productStatus(2).text).toBe('已下架')
    expect(productStatus(3).text).toBe('已售出')
    expect(productStatus(4).text).toBe('审核驳回')
  })

  it('未知状态回落到"未知"而不是抛异常', () => {
    expect(productStatus(99).text).toBe('未知')
    expect(productStatus(null).text).toBe('未知')
  })

  it('订单五种状态都有文案', () => {
    expect(orderStatus(0).text).toBe('待确认')
    expect(orderStatus(4).text).toBe('已关闭')
    expect(orderStatus(99).text).toBe('未知')
  })
})

describe('选项常量', () => {
  it('成色只有六档，不会前后端对不上', () => {
    expect(CONDITION_OPTIONS).toHaveLength(6)
    expect(CONDITION_OPTIONS).toContain('全新')
  })

  it('质保三档', () => {
    expect(WARRANTY_OPTIONS).toEqual(['在保', '过保', '无质保'])
  })
})
