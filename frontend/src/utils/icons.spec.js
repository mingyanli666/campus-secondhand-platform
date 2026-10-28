import { describe, it, expect } from 'vitest'
import { readdirSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { icons, hasIcon, resolveIcon, FALLBACK_ICON } from '@/utils/icons'

// vitest 的工作目录就是前端工程根目录
const SRC = resolve(process.cwd(), 'src')

/** 递归收集 src 下所有 .vue 文件 */
function collectVueFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) return collectVueFiles(full)
    return entry.name.endsWith('.vue') ? [full] : []
  })
}

/**
 * 从模板里抽出 <el-icon><Xxx /></el-icon> 这种写死标签名的图标。
 * 只认这一种写法 —— :is="xxx" 的名字来自配置或数据库，静态分析不出来，
 * 那部分靠 utils/icons.js 里的人工登记。
 */
function extractStaticIconNames(source) {
  const names = []
  const re = /<el-icon[^>]*>\s*<([A-Z][A-Za-z0-9]*)\s*\/>/g
  let m
  while ((m = re.exec(source)) !== null) names.push(m[1])
  return names
}

describe('图标登记表', () => {
  it('已知图标原样返回', () => {
    expect(hasIcon('Search')).toBe(true)
    expect(resolveIcon('Search')).toBe('Search')
  })

  it('不存在的图标兜底成默认图标（曾经踩过 Gamepad 的坑：构建不报错，页面一个空白格）', () => {
    expect(hasIcon('Gamepad')).toBe(false)
    expect(resolveIcon('Gamepad')).toBe(FALLBACK_ICON)
  })

  it('空值兜底', () => {
    expect(resolveIcon(null)).toBe(FALLBACK_ICON)
    expect(resolveIcon('')).toBe(FALLBACK_ICON)
    expect(resolveIcon(undefined)).toBe(FALLBACK_ICON)
  })

  it('登记表里必须包含 Goods —— 它是兜底图标，缺了就没法兜底', () => {
    expect(icons[FALLBACK_ICON]).toBeTruthy()
  })
})

describe('图标登记表 vs 模板实际用法（契约测试）', () => {
  it('模板里写死的图标必须都已登记，否则按需引入后会渲染成空白', () => {
    const missing = new Set()

    for (const file of collectVueFiles(SRC)) {
      const source = readFileSync(file, 'utf8')
      for (const name of extractStaticIconNames(source)) {
        if (!hasIcon(name)) missing.add(`${name}  (${file.replace(SRC, '')})`)
      }
    }

    expect([...missing]).toEqual([])
  })
})
