import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as userApi from '@/api/user'

const TOKEN_KEY = 'udp_token'
const USER_KEY = 'udp_user'

/**
 * 登录用户状态。
 * token 与用户信息持久化到 localStorage，刷新页面不掉登录态。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(safeParse(localStorage.getItem(USER_KEY)))

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '未登录')

  function safeParse(str) {
    if (!str) return null
    try {
      return JSON.parse(str)
    } catch {
      return null
    }
  }

  /** 登录并写入状态 */
  async function doLogin(form) {
    const data = await userApi.login(form)
    token.value = data.token
    userInfo.value = data.userInfo
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.userInfo))
    return data
  }

  /** 注册 */
  function doRegister(form) {
    return userApi.register(form)
  }

  /** 拉取最新用户信息，保证多端数据一致 */
  async function loadProfile() {
    const data = await userApi.getProfile()
    userInfo.value = data
    localStorage.setItem(USER_KEY, JSON.stringify(data))
    return data
  }

  /** 更新本地用户信息（资料修改成功后调用） */
  function setUserInfo(data) {
    userInfo.value = { ...userInfo.value, ...data }
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
  }

  /** 退出登录 */
  async function doLogout() {
    try {
      await userApi.logout()
    } catch {
      // 退出接口失败不影响本地清理
    }
    clearLogin()
  }

  /** 只清理本地登录态 */
  function clearLogin() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    userInfo,
    isLogin,
    isAdmin,
    nickname,
    doLogin,
    doRegister,
    loadProfile,
    setUserInfo,
    doLogout,
    clearLogin
  }
})
