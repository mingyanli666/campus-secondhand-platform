import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { fullPath: '/product/list' } },
    push: vi.fn()
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) }
}))

import service from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/** 记录每次请求的 config，便于断言"旧请求是否被取消" */
let seen = []

const tick = () => new Promise((r) => setTimeout(r, 0))
const ok = (data) => ({ code: 200, data })

function canceled(config) {
  const err = new axios.CanceledError('canceled')
  err.config = config
  return err
}

/** 构造一个可控的 axios adapter：可以指定响应体与延迟 */
function useAdapter(bodyFactory, delay = 0) {
  service.defaults.adapter = (config) =>
    new Promise((resolve, reject) => {
      seen.push(config)

      // 真实 adapter 也会先看一眼 signal —— 请求可能在进入 adapter 之前就被取消了
      if (config.signal?.aborted) {
        reject(canceled(config))
        return
      }

      const timer = setTimeout(() => {
        resolve({
          data: typeof bodyFactory === 'function' ? bodyFactory(config) : bodyFactory,
          status: 200,
          statusText: 'OK',
          headers: {},
          config
        })
      }, delay)

      config.signal?.addEventListener('abort', () => {
        clearTimeout(timer)
        reject(canceled(config))
      })
    })
}

describe('request 响应拦截', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    seen = []
  })

  it('成功时剥掉 Result 外壳，直接拿到 data', async () => {
    useAdapter(ok({ id: 1, title: 'iPhone 13' }))
    await expect(service.get('/product/1')).resolves.toEqual({ id: 1, title: 'iPhone 13' })
  })

  it('业务错误码会弹出后端给的具体原因并 reject', async () => {
    useAdapter({ code: 3001, message: '商品不存在或已被删除' })
    await expect(service.get('/product/999')).rejects.toThrow('商品不存在或已被删除')
    expect(ElMessage.error).toHaveBeenCalledWith('商品不存在或已被删除')
  })

  it('后端没给 message 时用兜底文案', async () => {
    useAdapter({ code: 5000 })
    await expect(service.get('/x')).rejects.toThrow('操作失败')
    expect(ElMessage.error).toHaveBeenCalledWith('操作失败')
  })

  it('401 会清空登录态并跳转登录页', async () => {
    localStorage.setItem('udp_token', 'expired-token')
    const store = useUserStore()
    expect(store.isLogin).toBe(true)

    useAdapter({ code: 401, message: '登录已失效' })
    await expect(service.get('/user/profile')).rejects.toThrow()

    expect(store.isLogin).toBe(false)
    expect(localStorage.getItem('udp_token')).toBeNull()

    // handleUnauthorized 在 finally 里跳转，等微任务跑完
    await tick()
    expect(router.push).toHaveBeenCalled()
  })
})

describe('request 竞态治理（同一 requestKey 只保留最后一个请求）', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    seen = []
  })

  it('新请求会取消同 key 的旧请求', async () => {
    useAdapter(ok('ok'), 30)

    // 先让第一个请求真正进入 adapter，再发第二个
    const first = service.get('/product/page', { requestKey: 'product:list' })
    await tick()

    const second = service.get('/product/page', { requestKey: 'product:list' })
    await tick()

    expect(seen).toHaveLength(2)
    expect(seen[0].signal.aborted).toBe(true)
    expect(seen[1].signal.aborted).toBe(false)

    await expect(second).resolves.toBe('ok')
    // first 被取消后不再落定，避免用旧结果覆盖新结果
    void first
  })

  it('被取消的请求不会 reject 给调用方（调用方只有 try/finally，没有 catch）', async () => {
    useAdapter(ok('ok'), 30)

    let settled = false
    const first = service.get('/a', { requestKey: 'k' })
    first.then(
      () => { settled = true },
      () => { settled = true }
    )

    await tick()
    service.get('/b', { requestKey: 'k' })

    await new Promise((r) => setTimeout(r, 80))
    expect(settled).toBe(false)
  })

  it('不同 requestKey 互不影响（列表查询与各状态计数是并发的）', async () => {
    useAdapter((config) => ok(config.url), 10)

    const [list, countA, countB] = await Promise.all([
      service.get('/list', { requestKey: 'list' }),
      service.get('/count-a'),
      service.get('/count-b')
    ])

    expect(list).toBe('/list')
    expect(countA).toBe('/count-a')
    expect(countB).toBe('/count-b')

    // 只有带 requestKey 的请求才会挂 signal；没带的不该被任何机制碰
    const withKey = seen.filter((c) => c.requestKey)
    const withoutKey = seen.filter((c) => !c.requestKey)
    expect(withKey).toHaveLength(1)
    expect(withKey[0].signal.aborted).toBe(false)
    expect(withoutKey).toHaveLength(2)
    withoutKey.forEach((c) => expect(c.signal).toBeUndefined())
  })

  it('上一个请求正常结束后，同 key 可以再次发起且不会被误取消', async () => {
    useAdapter(ok('done'), 5)

    await service.get('/a', { requestKey: 'k' })

    const again = service.get('/b', { requestKey: 'k' })
    await tick()

    expect(seen).toHaveLength(2)
    expect(seen[1].signal.aborted).toBe(false)
    await expect(again).resolves.toBe('done')
  })
})
