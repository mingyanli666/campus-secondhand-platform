import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * 全局请求实例。
 * 统一处理三件事：带上 token、剥掉 Result 外壳、集中处理错误提示。
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 是否已经弹过登录失效提示，避免并发请求时弹一堆
let isLoggingOut = false

/* ---------------- 请求拦截：带上 token ---------------- */
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/* ---------------- 响应拦截：剥壳 + 错误处理 ---------------- */
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 后端统一返回 Result，code === 200 才算成功
    if (res.code === 200) {
      return res.data
    }

    // 登录失效：清空登录态并跳转登录页
    if (res.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '登录已失效'))
    }

    // 其他业务错误：给出后端返回的具体原因，而不是笼统的"请求失败"
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  (error) => {
    // HTTP 层面的错误（网络断开、超时、5xx）
    if (error.response?.status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }

    let message = '网络异常，请稍后重试'
    if (error.code === 'ECONNABORTED') {
      message = '请求超时，请检查网络后重试'
    } else if (error.response?.status === 403) {
      message = '没有操作权限'
    } else if (error.response?.status === 404) {
      message = '请求的接口不存在'
    } else if (error.response?.status >= 500) {
      message = '服务器开小差了，请稍后重试'
    } else if (error.response?.data?.message) {
      message = error.response.data.message
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  if (isLoggingOut) return
  isLoggingOut = true

  const userStore = useUserStore()
  userStore.clearLogin()

  ElMessageBox.confirm('登录状态已失效，请重新登录', '提示', {
    confirmButtonText: '去登录',
    showCancelButton: false,
    type: 'warning'
  })
    .catch(() => {})
    .finally(() => {
      isLoggingOut = false
      const current = router.currentRoute.value
      router.push({
        path: '/login',
        query: current.fullPath.startsWith('/login') ? {} : { redirect: current.fullPath }
      })
    })
}

export default service
