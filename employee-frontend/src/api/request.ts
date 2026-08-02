import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ==================== 重复请求取消机制 ====================

/** 进行中的请求控制器映射表（用于取消重复请求） */
const pendingRequests = new Map<string, AbortController>()

/**
 * 生成请求唯一标识
 * 相同 method + url + params + data 的请求被视为重复请求
 */
function generateRequestKey(config: { method?: string; url?: string; params?: unknown; data?: unknown }): string {
  const { method, url, params, data } = config
  // GET 请求只按 url+params 去重（不含 body）；POST/PUT 按 url+params+data 去重
  const isGet = method?.toLowerCase() === 'get'
  return isGet
    ? [method, url, JSON.stringify(params)].join('&')
    : [method, url, JSON.stringify(params), JSON.stringify(data)].join('&')
}

/**
 * 取消所有未完成的请求（用于路由切换时清理）
 */
export function cancelAllPendingRequests(): void {
  pendingRequests.forEach((controller) => controller.abort())
  pendingRequests.clear()
}

// ==================== Token 刷新队列 ====================

/** 是否正在刷新 Token */
let isRefreshing = false

/** 等待 Token 刷新完成的请求队列 */
let refreshSubscribers: Array<(token: string) => void> = []

/** 将请求加入等待队列 */
function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

/** Token 刷新成功后通知所有等待的请求 */
function onTokenRefreshed(newToken: string) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}

/** Token 刷新失败后清空队列并通知等待者 */
function onRefreshFailed() {
  refreshSubscribers.forEach(cb => cb(''))
  refreshSubscribers = []
}

/** 清除所有认证信息 */
function clearAuthInfo() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('tokenExpiresAt')
}

/** 使用 refreshToken 尝试刷新 Token */
async function tryRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  try {
    const response = await axios.post(`${API_BASE_URL}/v1/auth/refresh`, { refreshToken })
    const data = response.data?.data || response.data
    const { token, refreshToken: newRefreshToken, expiresIn } = data

    localStorage.setItem('token', token)
    if (newRefreshToken) localStorage.setItem('refreshToken', newRefreshToken)
    if (expiresIn) {
      const expiresAt = Date.now() + expiresIn * 1000
      localStorage.setItem('tokenExpiresAt', String(expiresAt))
    }

    return token
  } catch {
    clearAuthInfo()
    return null
  }
}

/** 检查 Token 是否即将过期（距过期不足 5 分钟） */
function isTokenExpiringSoon(): boolean {
  const expiresAt = localStorage.getItem('tokenExpiresAt')
  if (!expiresAt) return false
  return Date.now() > Number(expiresAt) - 5 * 60 * 1000
}

/** 跳转到登录页 */
async function redirectToLogin() {
  try {
    const router = (await import('@/router')).default
    await router.replace('/login')
  } catch {
    window.location.href = '/login'
  }
}

// ==================== GET 请求重试配置 ====================

// ==================== CSRF Token 防护 ====================

/** 当前持有的 CSRF token，由后端通过 meta 标签或 cookie 注入 */
let csrfToken: string | null = null

/**
 * 从页面中获取 CSRF token
 * 优先级：meta 标签 > cookie
 * 后端在渲染登录页或首页时注入，前端在初始化/登录成功后刷新
 */
function getCsrfToken(): string | null {
  // 优先从 meta 标签获取（后端 SSR 或模板引擎注入）
  const metaTag = document.querySelector('meta[name="csrf-token"]')
  if (metaTag) return metaTag.getAttribute('content')
  // 其次从 cookie 获取（后端 Set-Cookie 设置）
  const match = document.cookie.match(/(^|;)\s*csrftoken=([^;]*)/)
  return match ? match[2] : null
}

/**
 * 刷新 CSRF token
 * 应在以下时机调用：
 * - 应用初始化时（main.ts）
 * - 登录成功后
 * - 页面重新获取焦点时
 */
export function refreshCsrfToken(): void {
  csrfToken = getCsrfToken()
}

// ==================== XSS 响应数据清理 ====================

/**
 * 判断字符串是否可能包含 Markdown 格式内容
 * 用于决定是否保留 HTML 标签结构（如 <code>、<pre> 等）
 * 注意：无论是否富文本，XSS 清理（script/iframe/事件属性/javascript:）都会完整执行
 */
function isRichTextContent(value: string): boolean {
  return /^#{1,6}\s/m.test(value) ||
    /^\s*[-*+]\s/m.test(value) ||
    /^\s*\d+\.\s/m.test(value) ||
    /```/.test(value)
}

/**
 * 清理单个字符串中的 XSS 攻击向量
 * 所有字符串都执行完整的 XSS 向量清理，不受内容类型影响
 * 富文本内容额外保留合法的 HTML 标签结构
 */
function sanitizeString(value: string): string {
  if (typeof value !== 'string') return value

  // 所有字符串统一执行完整 XSS 清理
  let result = value
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
    // [C1] 统一移除所有 on* 事件属性（无论引号类型、是否有引号、空白字符如何变化）
    // 覆盖：双引号/单引号/无引号/反引号、大小写混合、换行Tab分隔等 XSS 向量
    .replace(/\son\w+\s*(=\s*("[^"]*"|'[^']*'|[^\s>]*))?/gi, '')
    .replace(/javascript:/gi, '')

  // 非富文本内容：额外移除所有 HTML 标签，防止 XSS 注入
  if (!isRichTextContent(value)) {
    result = result.replace(/<[^>]*>/g, '')
  }

  return result
}

/**
 * 递归清理对象中的所有字符串字段
 * 跳过 Date、number、boolean、null 等原始类型
 * 导出供其他模块直接使用（如手动清理用户输入）
 */
export function sanitizeData<T>(data: T): T {
  if (typeof data === 'string') return sanitizeString(data) as unknown as T
  if (Array.isArray(data)) return data.map(item => sanitizeData(item)) as unknown as T
  if (data !== null && typeof data === 'object') {
    const result = {} as Record<string, unknown>
    for (const key of Object.keys(data)) {
      result[key] = sanitizeData((data as Record<string, unknown>)[key])
    }
    return result as T
  }
  return data
}

interface RetryConfig {
  /** 最大重试次数 */
  retries: number
  /** 重试间隔基数（毫秒），实际延迟 = delay x retryCount */
  retryDelay: number
}

/** GET 请求默认重试配置：开发环境1次，生产环境2次 */
const GET_RETRY_CONFIG: RetryConfig = {
  retries: import.meta.env.DEV ? 1 : 2,
  retryDelay: import.meta.env.DEV ? 500 : 1000,
}

/**
 * 判断错误是否可重试（仅网络超时/断连类错误）
 */
function isRetryableError(error: { code?: string; message?: string }): boolean {
  const code = error.code?.toUpperCase()
  const msg = (error.message || '').toLowerCase()
  return (
    code === 'ECONNABORTED' ||
    code === 'ETIMEDOUT' ||
    msg.includes('timeout') ||
    msg.includes('network error') ||
    msg.includes('failed to fetch')
  )
}

// ==================== 请求拦截器 ====================
// eslint-disable-next-line @typescript-eslint/no-explicit any -- axios 拦截器回调返回值需要与原始 config 类型一致
request.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  // ---- 重复请求取消 ----
  const requestKey = generateRequestKey(config)
  if (pendingRequests.has(requestKey)) {
    // 取消上一次未完成的相同请求
    pendingRequests.get(requestKey)?.abort()
  }
  // 创建新的取消控制器并注册
  const controller = new AbortController()
  config.signal = controller.signal
  pendingRequests.set(requestKey, controller)

  // ---- Token 注入与预刷新 ----
  const token = localStorage.getItem('token')
  if (token && config.headers) {
    const isRefreshRequest = config.url?.includes('/auth/refresh')

    if (isTokenExpiringSoon() && !isRefreshRequest) {
      if (!isRefreshing) {
        isRefreshing = true
        const newToken = await tryRefreshToken()
        isRefreshing = false

        if (newToken) {
          onTokenRefreshed(newToken)
          config.headers.Authorization = `Bearer ${newToken}`
        } else {
          config.headers.Authorization = `Bearer ${token}`
        }
      } else {
        // [C11] 增加 reject 回调：Token 刷新失败时 newToken 为空字符串，
        // 此时 reject 请求而非以空 token (Bearer ) 继续执行
        return new Promise<InternalAxiosRequestConfig>((resolve, reject) => {
          subscribeTokenRefresh((newToken) => {
            if (!newToken) {
              reject(new Error('Token 刷新失败'))
              return
            }
            config.headers.Authorization = `Bearer ${newToken}`
            resolve(config)
          })
        })
      }
    } else {
      config.headers.Authorization = `Bearer ${token}`
    }
  }

  // ---- CSRF Token 注入（静默模式：有 token 就带，没有也不报错） ----
  if (csrfToken && config.headers) {
    config.headers['X-CSRF-Token'] = csrfToken
  }

  return config
})

// ==================== 响应拦截器 ====================
request.interceptors.response.use(
  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器：response.data 结构由后端统一响应格式决定
  (response: any) => {
    // 移除已完成的请求记录
    const requestKey = generateRequestKey(response.config)
    pendingRequests.delete(requestKey)

    const res = response.data
    if (res.code !== undefined && res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    // 响应拦截器自动解包：返回 response.data.data（统一响应格式 { code, message, data }）
    // 对解包后的数据执行 XSS 清理，防止恶意内容注入到组件中
    const data = res.data
    if (data !== null && typeof data === 'object') {
      return sanitizeData(data)
    }
    return data
  },
  async (error) => {
    // 移除已失败/取消的请求记录
    if (error.config) {
      const requestKey = generateRequestKey(error.config)
      pendingRequests.delete(requestKey)
    }

    // 被主动取消的请求不弹窗提示
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    const originalRequest = error.config as InternalAxiosRequestConfig & { _retryCount?: number; _retry?: boolean }

    // ---- GET 请求自动重试（仅网络类错误） ----
    if (
      originalRequest.method?.toLowerCase() === 'get' &&
      isRetryableError(error) &&
      (originalRequest._retryCount ?? 0) < GET_RETRY_CONFIG.retries
    ) {
      originalRequest._retryCount = (originalRequest._retryCount || 0) + 1
      const delay = GET_RETRY_CONFIG.retryDelay * originalRequest._retryCount

      await new Promise((resolve) => setTimeout(resolve, delay))

      // 重试时不带 signal（避免被新的同类型请求取消）
      delete originalRequest.signal
      return request(originalRequest)
    }

    // ---- 401 Token 过期处理 ----
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      if (!isRefreshing) {
        isRefreshing = true
        const newToken = await tryRefreshToken()
        isRefreshing = false

        if (newToken) {
          onTokenRefreshed(newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        }
      } else {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken) => {
            // Token 刷新失败时，newToken 为空字符串，reject 等待的请求
            if (!newToken) {
              reject(new Error('登录已过期，请重新登录'))
              return
            }
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          })
        })
      }

      onRefreshFailed()
      clearAuthInfo()
      // [M1] 401 时尝试刷新 CSRF token，为后续重新登录做准备
      refreshCsrfToken()
      await redirectToLogin()
      ElMessage.error('登录已过期，请重新登录')
    } else {
      // 分级错误处理：对5xx服务端错误提供友好提示
      const status = error.response?.status
      if (status && status >= 500) {
        console.error(`[Server Error ${status}]`, error.config?.url)
        ElMessage.error('服务器繁忙，请稍后重试')
      } else {
        const message = error.response?.data?.message || error.message || '网络错误'
        ElMessage.error(message)
      }
    }

    return Promise.reject(error)
  }
)

export default request

// eslint-disable-next-line @typescript-eslint/no-explicit any -- axios 响应类型无法精确推断，运行时由响应拦截器解包
export function get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request({ method: 'GET', url, params }).then((r: any) => r as T)
}

// eslint-disable-next-line @typescript-eslint/no-explicit any
export function post<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request({ method: 'POST', url, data }).then((r: any) => r as T)
}

// eslint-disable-next-line @typescript-eslint/no-explicit any
export function putReq<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request({ method: 'PUT', url, data }).then((r: any) => r as T)
}
