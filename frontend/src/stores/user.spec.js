import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import * as userApi from '@/api/user'

vi.mock('@/api/user', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  getProfile: vi.fn()
}))

describe('user store 登录态', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('没有本地存储时是未登录状态', () => {
    const store = useUserStore()
    expect(store.isLogin).toBe(false)
    expect(store.isAdmin).toBe(false)
    expect(store.nickname).toBe('未登录')
  })

  it('能从 localStorage 恢复登录态（刷新页面不掉登录）', () => {
    localStorage.setItem('udp_token', 'token-abc')
    localStorage.setItem('udp_user', JSON.stringify({ id: 1, username: 'admin', role: 'ADMIN' }))

    const store = useUserStore()
    expect(store.token).toBe('token-abc')
    expect(store.isLogin).toBe(true)
    expect(store.isAdmin).toBe(true)
  })

  it('localStorage 里是坏 JSON 时不崩溃', () => {
    localStorage.setItem('udp_token', 'token-abc')
    localStorage.setItem('udp_user', '{坏掉的json')

    const store = useUserStore()
    // token 还在，所以算已登录；但用户信息解析失败，退回 null
    expect(store.isLogin).toBe(true)
    expect(store.userInfo).toBeNull()
    expect(store.nickname).toBe('未登录')
  })

  it('登录成功后写入 token 与用户信息到内存和 localStorage', async () => {
    userApi.login.mockResolvedValue({
      token: 'token-new',
      userInfo: { id: 2, username: 'chenqi', nickname: '陈七', role: 'USER' }
    })

    const store = useUserStore()
    await store.doLogin({ username: 'chenqi', password: '123456' })

    expect(store.isLogin).toBe(true)
    expect(store.nickname).toBe('陈七')
    expect(store.isAdmin).toBe(false)
    expect(localStorage.getItem('udp_token')).toBe('token-new')
    expect(JSON.parse(localStorage.getItem('udp_user')).username).toBe('chenqi')
  })

  it('nickname 缺失时回落到 username', async () => {
    userApi.login.mockResolvedValue({
      token: 't',
      userInfo: { id: 3, username: 'lisi', role: 'USER' }
    })

    const store = useUserStore()
    await store.doLogin({})
    expect(store.nickname).toBe('lisi')
  })

  it('clearLogin 会同时清掉内存与 localStorage', () => {
    localStorage.setItem('udp_token', 'token-abc')
    localStorage.setItem('udp_user', JSON.stringify({ id: 1, role: 'USER' }))

    const store = useUserStore()
    store.clearLogin()

    expect(store.isLogin).toBe(false)
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('udp_token')).toBeNull()
    expect(localStorage.getItem('udp_user')).toBeNull()
  })

  it('退出接口失败也要清理本地登录态', async () => {
    localStorage.setItem('udp_token', 'token-abc')
    userApi.logout.mockRejectedValue(new Error('网络异常'))

    const store = useUserStore()
    await store.doLogout()

    expect(store.isLogin).toBe(false)
    expect(localStorage.getItem('udp_token')).toBeNull()
  })

  it('setUserInfo 是合并而不是覆盖', () => {
    localStorage.setItem('udp_token', 't')
    localStorage.setItem('udp_user', JSON.stringify({ id: 1, username: 'a', nickname: '旧名' }))

    const store = useUserStore()
    store.setUserInfo({ nickname: '新名' })

    expect(store.userInfo.nickname).toBe('新名')
    expect(store.userInfo.username).toBe('a')
  })
})
