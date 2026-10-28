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

/**
 * 在途请求登记表：requestKey -> AbortController
 *
 * 解决什么问题？
 * 商品列表页连续点两个分类会并发两个请求。如果第一个的响应后到，
 * 它会把第二个的结果覆盖掉 —— 界面变成「分类选中 B，列表却是 A 的数据」。
 * 网络越慢越容易复现，而且控制台不报错，用户只会觉得「点了没反应」。
 *
 * 用法：请求配置里带 requestKey，同一个 key 只保留最后一个请求，
 * 新的发起时把旧的取消掉。
 * 不带 requestKey 的请求不受影响 —— 写操作、详情查询都不应该被取消。
 */
const inflight = new Map()

/**
 * 请求结束后注销登记。
 *
 * 必须校验「登记的控制器就是本次请求的控制器」再删：
 * A 被 B 取代后，A 的响应/异常仍会走到这里，如果无条件删除，
 * 删掉的是 B 的登记项，之后 B 就再也取消不掉了。
 */
function releaseRequestKey(config) {
  const key = config?.requestKey
  if (!key) return
  const controller = inflight.get(key)
  if (controller && controller.signal === config.signal) {
    inflight.delete(key)
  }
}

/* ---------------- 请求拦截：带上 token + 取消同 key 的旧请求 ---------------- */
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    if (config.requestKey) {
      const previous = inflight.get(config.requestKey)
      if (previous) previous.abort()

      const controller = new AbortController()
      config.signal = controller.signal
      inflight.set(config.requestKey, controller)
    }

    return config
  },
  (error) => Promise.reject(error)
)

/* ---------------- 响应拦截：剥壳 + 错误处理 ---------------- */
service.interceptors.response.use(
  (response) => {
    releaseRequestKey(response.config)

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
    // 被同 key 的新请求取消：这不是错误，是预期行为。
    // 调用方普遍写成 try / finally（没有 catch），直接 reject 会变成
    // 未处理的 Promise 异常。所以返回一个永不落定的 Promise，
    // 让这次 await 就此停住，界面交给新请求去更新。
    if (axios.isCancel(error)) {
      return new Promise(() => {})
    }

    releaseRequestKey(error.config)

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
