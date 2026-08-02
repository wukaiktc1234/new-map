import axios, { type AxiosRequestConfig, type AxiosError } from 'axios'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 清理历史遗留的演示模式标志，避免旧状态影响新逻辑
if (localStorage.getItem('pos-demo-mode')) {
  localStorage.removeItem('pos-demo-mode')
}

request.interceptors.request.use(
  (config) => {
    // POS 端只使用 pos-token，避免与管理端 token 串用
    const token = localStorage.getItem('pos-token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * Token 刷新状态（B3 修复）
 * <p>
 * 并发请求遇到 401 时，只触发一次刷新，其他请求排队等待刷新完成后重试。
 * 避免多个请求同时刷新导致 token 互相覆盖。
 * </p>
 */
let isRefreshing = false
let refreshSubscribers: Array<(token: string | null) => void> = []

function subscribeTokenRefresh(cb: (token: string | null) => void) {
  refreshSubscribers.push(cb)
}

function notifyTokenRefresh(token: string | null) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

/**
 * 尝试用当前 token 调用刷新接口获取新 token
 * 后端 /v1/auth/refresh 接口允许在 access token 过期后刷新
 */
async function tryRefreshToken(): Promise<string | null> {
  const currentToken = localStorage.getItem('pos-token')
  if (!currentToken) {
    return null
  }
  try {
    // 直接用 axios 避免 circular import，绕过 request 拦截器
    const res = await axios.post(
      '/api/v1/auth/refresh',
      null,
      { headers: { Authorization: `Bearer ${currentToken}` }, timeout: 10000 }
    )
    const data = res.data
    if (data && (data.code === 0 || data.code === 200) && data.data) {
      const newToken = data.data as string
      localStorage.setItem('pos-token', newToken)
      return newToken
    }
    return null
  } catch {
    return null
  }
}

function clearAuthAndRedirect() {
  localStorage.removeItem('pos-token')
  localStorage.removeItem('pos-user')
  const currentPath = router.currentRoute.value.path
  if (currentPath !== '/login') {
    router.push('/login')
  }
}

request.interceptors.response.use(
  (response) => {
    const { data } = response

    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) {
        return data.data !== undefined ? data.data : data
      }
      // 兼容历史接口返回 code=200，但项目规范要求 code=0
      if (data.code === 200) {
        return data.data !== undefined ? data.data : data
      }
      return Promise.reject({ message: data.message || '请求失败', status: data.code })
    }

    return data
  },
  async (error: AxiosError) => {
    const status = error.response?.status
    const originalRequest = error.config as (AxiosRequestConfig & { _retry?: boolean }) | undefined

    // B3 修复：401 时尝试用 refresh token 刷新，刷新失败再跳登录
    if (status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true

      if (isRefreshing) {
        // 已有刷新请求进行中，排队等待新 token
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken: string | null) => {
            if (newToken) {
              originalRequest.headers = originalRequest.headers || {}
              ;(originalRequest.headers as Record<string, string>).Authorization = `Bearer ${newToken}`
              resolve(request(originalRequest))
            } else {
              reject({ message: '登录已过期，请重新登录', status: 401 })
            }
          })
        })
      }

      isRefreshing = true
      try {
        const newToken = await tryRefreshToken()
        isRefreshing = false
        notifyTokenRefresh(newToken)

        if (newToken) {
          // 刷新成功，重试原请求
          originalRequest.headers = originalRequest.headers || {}
          ;(originalRequest.headers as Record<string, string>).Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        }
      } catch {
        isRefreshing = false
        notifyTokenRefresh(null)
      }

      // 刷新失败，清理凭证并跳转登录
      clearAuthAndRedirect()
      return Promise.reject({ message: '登录已过期，请重新登录', status: 401 })
    }

    // AxiosError.response.data 类型为 unknown，需类型断言访问 message
    const errData = error.response?.data as { message?: string } | undefined
    const message = errData?.message || error.message || '请求失败'
    return Promise.reject({ message, originalError: error })
  }
)

export default request
