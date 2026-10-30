import axios, { type AxiosRequestConfig, type AxiosError } from 'axios'
import router from '../router'
import { clearCapabilities } from '../composables/usePermission'

/**
 * 扩展请求配置：
 *   silent=true → 该请求的任何失败都**不弹全局提示**，由业务层用内联方式呈现。
 * 登录、后台轮询、权限预检一律 silent，避免"页面弹窗 + 操作点提示"重复。
 */
declare module 'axios' {
  interface AxiosRequestConfig {
    silent?: boolean
  }
}

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

/** 登录类接口：即使调用方忘了 silent，也绝不允许全局提示（错误只回给登录页表单） */
const AUTH_SILENT_PATTERNS = ['/v1/pos/auth/login', '/v1/pos/auth/me']

export function isSilentRequest(config?: AxiosRequestConfig): boolean {
  if (config?.silent) return true
  const url = String(config?.url || '')
  return AUTH_SILENT_PATTERNS.some((pattern) => url.includes(pattern))
}

// 清理历史遗留的演示模式标志，避免旧状态影响新逻辑
if (localStorage.getItem('pos-demo-mode')) {
  localStorage.removeItem('pos-demo-mode')
}

/**
 * 路径前缀重复保护（开发期断言）
 *
 * axios 实例 baseURL 已经是 '/api'；如果调用方再把 '/api/v1/...' 传进来，
 * 实际请求就变成 '/api/api/v1/...'，后端返回 **HTTP 404 且响应体为空**（没有 mapping）。
 * 这种 404 看起来像「接口不存在」，很容易被误判成业务逻辑问题——本仓库真踩过。
 * 所以在 DEV 环境直接抛错，把问题暴露在调用点。
 */
/**
 * 错误响应体形状断言（开发期告警）
 *
 * 约定：后端错误必须带 body（{"code":...,"message":"..."}）。
 * 出现「HTTP 4xx/5xx 但响应体为空」时，先怀疑路径前缀重复（/api/api/...）或该 mapping 不存在，
 * 再怀疑业务逻辑——本仓库真踩过（servlet context-path /api + controller 又写 /api/v1 → /api/api/v1）。
 * 只告警不抛错：错误流已由调用方处理，抛错会掩盖原始错误。
 */
function assertErrorBodyShape(status: number | undefined, url: string, data: unknown): void {
  if (!import.meta.env.DEV) return
  if (!status || status < 400) return
  const isEmpty =
    data === undefined ||
    data === null ||
    (typeof data === 'string' && data.trim() === '') ||
    (typeof data === 'object' && Object.keys(data as Record<string, unknown>).length === 0)
  if (isEmpty) {
    console.warn(
      '[request] 错误响应体为空：HTTP ' + status + ' ' + url +
        ' —— 优先怀疑 /api/api/ 前缀重复或该 mapping 不存在（真实业务错误通常带 body.code/message）',
    )
  }
}

function assertNoDuplicatedApiPrefix(rawUrl: string): void {
  if (!import.meta.env.DEV) return
  if (/(^|\/)api\/api\//i.test(rawUrl) || /^\/api\//i.test(rawUrl)) {
    throw new Error(
      '[request] URL 前缀重复：baseURL 已是 /api，请传 /v1/... 而不是 /api/...（实际会打到 /api/api/... → 404 空响应体）：' + rawUrl,
    )
  }
}

request.interceptors.request.use(
  (config) => {
    // dev 断言：拦掉 /api/api/... 这类前缀重复（见上）
    assertNoDuplicatedApiPrefix(String(config.url || ''))

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
  // 权限只在内存：会话失效时一并清空，避免旧快照继续渲染按钮
  clearCapabilities()
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

      // 反模式：HTTP 200 却用 body.code=401/403 表达鉴权失败。
      // 这会绕开所有基于 HTTP 状态码的全局处理（刷新 token、路由跳转、埋点），必须当作缺陷上报。
      if (data.code === 401 || data.code === 403) {
        const message = String(data.message || '')
        // eslint-disable-next-line no-console
        console.error(
          '[request] 接口以 200 + body.code=' + data.code + ' 返回鉴权失败（反模式）：' +
          String(response.config?.url || '') +
          ' —— 后端必须改为真实 HTTP ' + data.code + '，否则权限判定与刷新链路都会失效'
        )
        return Promise.reject({
          message: message || (data.code === 403 ? '无权限执行该操作（403）' : '登录已过期，请重新登录'),
          status: data.code,
          code: data.code,
          /** 标记：这是 body 里的伪状态码，不是真实 HTTP 状态 */
          fromBodyCode: true,
          originalError: response
        })
      }

      return Promise.reject({ message: data.message || '请求失败', status: data.code })
    }

    return data
  },
  async (error: AxiosError) => {
    const status = error.response?.status
    assertErrorBodyShape(status, String(error.config?.url || ''), error.response?.data)
    const originalRequest = error.config as (AxiosRequestConfig & { _retry?: boolean }) | undefined

    // ★ 登录接口返回 401 = 凭据错误：只把服务端原文抛给登录页，绝不触发刷新/跳登录
    const isLoginCall = String(error.config?.url || '').includes('/v1/pos/auth/login')
    /** silent：登录、权限预检、后台轮询 —— 失败一律交给业务层内联呈现，不弹全局提示 */
    const silent = isSilentRequest(originalRequest)

    // B3 修复：401 时尝试用 refresh token 刷新，刷新失败再跳登录
    if (status === 401 && !isLoginCall && originalRequest && !originalRequest._retry) {
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

      // 刷新失败：silent 请求（登录/预检/轮询）只 reject，不跳转、不弹窗
      if (!silent) clearAuthAndRedirect()
      return Promise.reject({ message: '登录已过期，请重新登录', status: 401, silent })
    }

    // 限流 429：服务端 message 优先；无 message 给「可重试」兜底文案，有 Retry-After 就补等待时长
    if (status === 429) {
      const headerRetry = Number(error.response?.headers?.['retry-after'])
      const rateData = error.response?.data as { message?: string; retryAfter?: unknown } | undefined
      const bodyRetry = Number(rateData?.retryAfter)
      const retryAfter = Number.isFinite(headerRetry) && headerRetry > 0 ? headerRetry : bodyRetry
      const waitHint = Number.isFinite(retryAfter) && retryAfter > 0 ? `（约 ${retryAfter} 秒后可重试）` : ''
      return Promise.reject({
        message: (rateData?.message || '请求过于频繁，请稍后重试') + waitHint,
        status: 429,
        /** 供登录页做"XX 秒后可重试"的倒计时（秒） */
        retryAfter: Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined,
        silent,
        originalError: error,
      })
    }

    // AxiosError.response.data 类型为 unknown，需类型断言访问 message
    const errData = error.response?.data as { message?: string; code?: number | string } | undefined
    const httpStatus = error.response?.status
    // 403 是"越权"信号：调用方需要据此隐藏入口/提示，因此把 status 一并抛出去
    if (httpStatus === 403) {
      return Promise.reject({
        message: errData?.message || '无权限执行该操作（403）',
        status: 403,
        code: errData?.code ?? 403,
        silent,
        originalError: error
      })
    }
    // 网络层错误汉化（R8 P1-2）：axios 的英文原文绝不直出到 UI
    const rawMessage = errData?.message || error.message || ''
    const message = toChineseNetworkMessage(rawMessage, httpStatus, error.code)
    return Promise.reject({ message, status: httpStatus, silent, originalError: error })
  }
)

/**
 * 把网络层/框架层的英文错误翻译成中文（R8 P1-2）
 *
 * 覆盖：
 *   Network Error / Failed to fetch            → 网络连接失败，请检查网络后重试
 *   ECONNABORTED / timeout / ETIMEDOUT         → 请求超时，请检查网络后重试
 *   5xx 无 message                              → 服务暂时不可用，请稍后重试
 *   其它英文（无中文）                            → 兜底「请求失败，请稍后重试」
 * 已经是中文的 message 原样返回（后端中文提示优先级最高）。
 */
export function toChineseNetworkMessage(rawMessage: string, httpStatus?: number, code?: string): string {
  const msg = String(rawMessage || '').trim()
  const isChinese = /[\u4e00-\u9fa5]/.test(msg)
  if (isChinese) return msg

  const lower = msg.toLowerCase()
  if (
    lower.includes('network error') ||
    lower.includes('failed to fetch') ||
    lower.includes('err_network') ||
    lower.includes('networkerror')
  ) {
    return '网络连接失败，请检查网络后重试'
  }
  if (
    lower.includes('econnaborted') ||
    lower.includes('timeout') ||
    lower.includes('etimedout') ||
    code === 'ECONNABORTED'
  ) {
    return '请求超时，请检查网络后重试'
  }
  if (typeof httpStatus === 'number' && httpStatus >= 500) {
    return '服务暂时不可用，请稍后重试'
  }
  if (!msg) return '请求失败，请稍后重试'
  // 仍是非中文（例如 "Request failed with status code 500"）→ 统一兜底
  return /[a-zA-Z]/.test(msg) ? '请求失败，请稍后重试' : msg
}

export default request
