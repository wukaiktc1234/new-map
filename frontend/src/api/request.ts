/**
 * API 请求实例
 *
 * 功能：
 * 1. 默认使用真实后端 API（H2/PostgreSQL）
 * 2. 仅当 VITE_USE_MOCK=true 时启用 Mock 模式（后端不可用自动回退）
 * 3. 提供统一的错误处理和日志记录
 *
 * 设计原则（mock 与真实数据库分离）：
 * - 开发环境默认走真实数据库（profile=h2，PostgreSQL 兼容模式）
 * - mock 数据作为「显式 opt-in」功能，避免污染真实业务数据
 * - 生产环境（PROD）永不启用 mock 模式
 */

import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import logger from '@/utils/logger'
import { getRefreshToken, setToken, getTokenRemainingTime } from '@/utils/auth'

// ============================================================
// 配置常量
// ============================================================

/** API 基础路径 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/** 请求超时时间（毫秒） */
const REQUEST_TIMEOUT = 15000

/**
 * 是否允许启用 Mock 模式
 * - 生产环境（PROD）始终为 false
 * - 开发环境根据 VITE_USE_MOCK 环境变量决定（默认 false）
 *
 * 此变量是「总开关」，控制 enableMockMode() 是否真正生效。
 * 即便请求失败，也不会自动切换到 mock 模式（除非显式开启）。
 */
const MOCK_ALLOWED: boolean = !import.meta.env.PROD && import.meta.env.VITE_USE_MOCK === 'true'

/** 是否启用 Mock 模式（运行时状态，仅当 MOCK_ALLOWED=true 时才可能为 true） */
let mockModeEnabled = false

/**
 * Mock fallback 触发标记（独立于 MOCK_ALLOWED 总开关）
 *
 * 用途：operations/* 等模块的 catch 块通过 silentGet/silentPost 静默回退到本地 Mock 数据时，
 * 主动调用 markMockFallback() 标记，使 UI 层可通过 isMockFallbackTriggered() 感知。
 *
 * 与 mockModeEnabled 的区别：
 * - mockModeEnabled 受 MOCK_ALLOWED 总开关限制，仅在 VITE_USE_MOCK=true 时才可触发；
 * - mockFallbackTriggered 不受总开关限制，任何 catch 回退都可主动标记，
 *   用于在 UI 上提示"演示模式"横幅。
 *
 * 一旦标记，在当前会话内保持为 true，直到调用 resetMockFallback() 或页面刷新。
 */
let mockFallbackTriggered = false

/** 标记已发生 Mock 回退（catch 块主动调用） */
export function markMockFallback(): void {
  if (!mockFallbackTriggered) {
    mockFallbackTriggered = true
    logger.warnMessage(
      '%c[API] 检测到 Mock fallback，部分接口使用本地演示数据',
      'color: #E6A23C; font-weight: bold; padding: 2px 8px; background: #FDF6EC; border-radius: 4px;'
    )
  }
}

/** 检查是否已发生 Mock 回退（UI 用于显示"演示模式"横幅） */
export function isMockFallbackTriggered(): boolean {
  return mockFallbackTriggered
}

/** 重置 Mock fallback 标记（用于手动重新连接后端场景） */
export function resetMockFallback(): void {
  mockFallbackTriggered = false
}

/**
 * 清除所有认证状态并跳转登录页
 * 在 Token 过期/刷新失败/401 响应时统一调用
 */
function clearAuthAndRedirect(): void {
  // 清除所有认证相关 localStorage
  const authKeys = [
    'token',
    'refresh_token',
    'user_id',
    'username',
    'token_expiry',
    'permissions',
    'roles',
    'menu-overrides',
    'menu-template',
    'tab-bar-tabs',
  ]
  for (const key of authKeys) {
    localStorage.removeItem(key)
  }
  // 跳转到登录页（非 /home，避免已失效的 session 继续渲染业务页面）
  window.location.href = '/login'
}

// ============================================================
// Token 静默续期
// ============================================================

/** 剩余多少秒时提前静默刷新（5 分钟） */
const TOKEN_PREFRESH_SECONDS = 300

/** 并发去重：同一时刻只发起一次刷新请求 */
let refreshPromise: Promise<string> | null = null

/** 弹框去重：同一会话只弹一次"登录已过期"确认框 */
let sessionExpiredPrompting = false

/** 是否为登录接口请求（其业务 401 = 密码错误，非会话过期） */
function isLoginRequest(url: string | undefined): boolean {
  if (!url) return false
  return url.includes('/v1/auth/login') || url.includes('/auth/login')
}

/**
 * 用 refresh_token 静默换取新 token（使用原生 axios，避免递归走本实例拦截器）
 * 成功后自动更新 localStorage 中的 token 与过期时间
 */
async function refreshTokenSilently(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('无 refresh_token，无法续期')
  }
  if (!refreshPromise) {
    refreshPromise = axios
      .post<{ code?: number; message?: string; data?: string }>(
        `${API_BASE_URL}/v1/auth/refresh`,
        {},
        { headers: { Authorization: `Bearer ${refreshToken}` } },
      )
      .then((res) => {
        const body = res.data
        if (body?.code !== undefined && body.code !== 0) {
          throw new Error(body.message || '刷新令牌失败')
        }
        const token = body?.data
        if (!token) {
          throw new Error('刷新令牌响应为空')
        }
        setToken(token)
        return token
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

/**
 * 处理会话过期（401）：
 * 1. 先尝试静默刷新并重放原请求（用户无感，不丢数据）
 * 2. 刷新失败则提示并自动登出（P1-SEC-004）：
 *    - 取消「留在本页」选项：死会话不再允许继续交互，避免用户输入丢失且毫不知情
 *    - 弹框关闭（X/遮罩）同样强制登出（fail-closed）
 *    - sessionExpiredPrompting 防重：连续 401 只弹一次
 */
async function handleSessionExpired(
  originalConfig?: InternalAxiosRequestConfig & { _retried?: boolean },
): Promise<never> {
  if (originalConfig && !originalConfig._retried) {
    try {
      await refreshTokenSilently()
      originalConfig._retried = true
      return request(originalConfig) as Promise<never>
    } catch {
      // 刷新失败，走登出提示
    }
  }
  if (sessionExpiredPrompting) {
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }
  sessionExpiredPrompting = true
  try {
    await ElMessageBox.confirm(
      '登录已过期，请重新登录。当前页面未保存的修改可能丢失。',
      '登录已过期',
      {
        confirmButtonText: '重新登录',
        showCancelButton: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
      },
    )
  } catch {
    // 用户关闭弹框：同样强制登出，不允许停留在死会话
  } finally {
    sessionExpiredPrompting = false
  }
  clearAuthAndRedirect()
  return Promise.reject(new Error('登录已过期，请重新登录'))
}
// ============================================================
// 创建 Axios 实例
// ============================================================

const request: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ============================================================
// 请求拦截器
// ============================================================

request.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // 添加认证 Token；token 即将过期时先静默续期，避免保存/提交时突然 401
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      try {
        if (getTokenRemainingTime(token) < TOKEN_PREFRESH_SECONDS && getRefreshToken()) {
          const fresh = await refreshTokenSilently()
          config.headers.Authorization = `Bearer ${fresh}`
        } else {
          config.headers.Authorization = `Bearer ${token}`
        }
      } catch {
        config.headers.Authorization = `Bearer ${token}`
      }
    }

    // 添加请求日志
    logger.api('Request', `${config.method?.toUpperCase()} ${config.url}`, config.params || config.data)

    return config
  },
  (error) => {
    logger.error('Request', '请求配置错误', error)
    return Promise.reject(error)
  }
)

// ============================================================
// 响应拦截器
// ============================================================

request.interceptors.response.use(
  (response: AxiosResponse) => {
    // Blob 响应（文件下载）：直接返回 Blob，不提取 code/data
    if (response.config.responseType === 'blob') {
      return response.data
    }

    // 提取响应数据
    const res = response.data

    // 检查业务状态码（code=0 表示成功）
    if (res.code !== undefined && res.code !== 0) {
      const errorMessage = res.message || '请求失败'

      // 静默错误：当请求标记了 _silentError 时不弹出提示（由调用方自行处理）
      // 修复Bug：原代码仅在HTTP错误拦截器检查_silentError，业务错误（如461）未检查
      // 导致silentPost对业务错误码（如461需要验证码）不静默，用户看到"登录失败次数过多"提示
      const silentError = (response.config as InternalAxiosRequestConfig & { _silentError?: boolean })?._silentError

      // 退款归属确认（contract: fund-500 v2）：400「已确认过」/403「无权限」等错误
      // 需要由「待确认归属」面板带上下文展示（哪笔退款、哪个班次），
      // 拦截器不再 toast，避免同一错误弹两次。
      const attrPath = String(response.config?.url || '')
      const isAttributionConfirm = attrPath.includes('/confirm-attribution')

      // 特殊错误码处理
      switch (res.code) {
        case 401:
          // 登录接口的业务 401 = 用户名或密码错误（后端返回 HTTP 200 + code 401），
          // 不是会话过期：直接透出后端 message，避免误报"登录已过期"
          if (isLoginRequest(response.config.url)) {
            // 登录页自己会弹一次（用它 catch 到的 message），这里再弹就重复了 —— 实测双弹
            if (!silentError) ElMessage.error(errorMessage)
            return Promise.reject(new Error(errorMessage))
          }
          if (import.meta.env.PROD) {
            // 先尝试静默刷新并重放请求；失败则弹确认框后再跳转，避免用户数据丢失
            return handleSessionExpired(
              response.config as InternalAxiosRequestConfig & { _retried?: boolean },
            )
          }
          // 开发环境：不跳转、不清空标签页，让 silentGet 走 mock 回退
          // 原因：开发模式无真实 token，后端必然返回 401，若跳转会清空 tab-bar-tabs 导致标签页丢失
          break
        case 400:
          // 业务 400：优先服务端原文（如「角色名不能为空」），不再用通用文案
          if (!silentError && !isAttributionConfirm) ElMessage.error(errorMessage)
          break
        case 403:
          // ★ 业务 403：服务端原文优先（如「权限不足: system:role:create」），兜底才是通用文案
          if (!silentError && !isAttributionConfirm) ElMessage.error(errorMessage)
          break
        case 404:
          logger.warn('Response', `接口不存在: ${res.message || ''}`)
          return Promise.reject(new Error(res.message || '请求的资源不存在'))
        case 500:
          // 显示后端返回的实际错误信息（如"用户名或密码错误"），而非通用提示
          if (!silentError && !isAttributionConfirm) ElMessage.error(errorMessage)
          break
        case 461:
          // 461表示"需要验证码"，不是真正的错误，返回data让调用方处理
          // 登录接口返回461时，data包含requireCaptcha/captchaId/captchaImage
          logger.info('Response', '需要验证码，返回验证码数据')
          return res.data
        default:
          // 461（需要验证码）等其他业务错误：静默模式下不弹提示
          // 归属确认（400 缺少参数 / 已确认过）由面板展示，不在此 toast
          if (!silentError && !isAttributionConfirm) ElMessage.error(errorMessage)
      }

      // silentGet/silentPost 的契约就是「调用方自行处理」，这里不能再打 error 级日志，
      // 否则财务详情里的权限降级探测会刷出一串 403 控制台噪音（E2E 按控制台 error 输出判定会判失败）
      if (silentError) {
        logger.info('Response', `业务错误 [${res.code}]（silent，由调用方处理）`, errorMessage)
      } else {
        logger.error('Response', `业务错误 [${res.code}]`, errorMessage)
      }
      return Promise.reject(new Error(errorMessage))
    }

    // 返回 data 字段（统一响应格式：{ code, message, data }）
    return res.data
  },
  async (error) => {
    if (axios.isCancel(error)) {
      logger.warn('Request', '请求已取消', error.message)
      return Promise.reject(error)
    }

    const errorResponse = error.response
    let errorMessage = '网络错误，请稍后重试'

    if (errorResponse) {
      const status = errorResponse.status
      const url = error.config?.url || 'unknown'

      switch (status) {
        case 400:
          // ★（Lead 2026-10-30 X-2a）：优先用服务端原文，避免再次吞掉 message
          errorMessage = errorResponse.data?.message || '请求参数错误'
          break
        case 401:
          // ★ 登录失败（密码错误/账号禁用/锁定）可能仍是 401：服务端原文优先，避免误报「登录已过期」
          errorMessage = errorResponse.data?.message || '登录已过期，请重新登录'
          // ★ 登录接口自身返回 401 = 凭据错误，绝不能走「会话过期」流程（会弹确认框/跳转，掩盖真正原因）
          if (isLoginRequest(error.config?.url)) break
          if (import.meta.env.PROD) {
            // 先尝试静默刷新并重放请求；失败则弹确认框后再跳转，避免用户数据丢失
            return handleSessionExpired(error.config)
          }
          // 开发环境：不跳转、不清空标签页，让 silentGet 走 mock 回退
          // 原因：开发模式无真实 token，后端必然返回 401，若跳转会清空 tab-bar-tabs 导致标签页丢失
          break
        case 403:
          // ★ 服务端原文优先（如「权限不足: system:role:create」「账号已被禁用，请联系管理员」）
          errorMessage = errorResponse.data?.message || '没有权限执行此操作'
          break
        case 404:
          // ★ 业务 404（如「角色不存在」）与「接口不存在」必须用服务端 message 区分
          errorMessage = errorResponse.data?.message || `接口不存在: ${url}`
          // 仅当显式允许 mock 模式时才启用，否则暴露真实错误
          if (MOCK_ALLOWED) enableMockMode()
          break
        case 408:
          errorMessage = '请求超时'
          break
        case 500:
          // ★ 服务端原文优先（很多业务异常仍以 500 返回并带精确 message）
          errorMessage = errorResponse.data?.message || '服务器内部错误'
          break
        case 502:
        case 503:
        case 504:
          errorMessage = '服务暂时不可用'
          // 仅当显式允许 mock 模式时才启用，否则暴露真实错误
          if (MOCK_ALLOWED) enableMockMode()
          break
        default:
          errorMessage = `请求失败 (${status})`
      }

      logger.error(
        'Response',
        `[${status}] ${error.config?.method?.toUpperCase()} ${url}`,
        errorResponse.data?.message || errorMessage
      )
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '请求超时，请检查网络连接'
    } else if (error.code === 'ERR_NETWORK') {
      errorMessage = '网络连接失败，请检查网络设置'
      // 仅当显式允许 mock 模式时才启用，否则暴露真实错误
      if (MOCK_ALLOWED) enableMockMode()
    }

    // 静默错误：当请求标记了 _silentError 时不弹出提示（由调用方自行处理）
    const silentError = error.config?.['_silentError']
    if (!silentError) {
      ElMessage.error(errorMessage)
    }
    return Promise.reject(new Error(errorMessage))
  }
)

// ============================================================
// Mock 模式管理
// ============================================================

/**
 * 启用 Mock 模式
 * - 仅当 MOCK_ALLOWED=true 时才真正启用（环境变量 VITE_USE_MOCK 控制）
 * - 调用方无需关心当前是否允许启用，函数内部会判断
 */
function enableMockMode(): void {
  // 总开关关闭时直接返回（生产环境或 VITE_USE_MOCK=false）
  if (!MOCK_ALLOWED) return
  if (!mockModeEnabled) {
    mockModeEnabled = true
    logger.warnMessage(
      '%c[API] 已自动切换到 Mock 数据模式',
      'color: #E6A23C; font-weight: bold; padding: 2px 8px; background: #FDF6EC; border-radius: 4px;'
    )
  }
}

/**
 * 检查是否处于 Mock 模式
 */
export function isMockMode(): boolean {
  return mockModeEnabled
}

/**
 * 手动禁用 Mock 模式（用于测试）
 */
export function disableMockMode(): void {
  mockModeEnabled = false
}

// ============================================================
// 封装请求方法（兼容原有调用方式）
// ============================================================

interface RequestOptions<T = unknown> extends AxiosRequestConfig {
  /** 是否显示加载状态 */
  loading?: boolean
  /** 是否显示错误消息（默认true） */
  showError?: boolean
  /** 自定义错误消息 */
  customErrorMessage?: string
}

export type { RequestOptions }

async function requestWrapper<T = unknown>(
  config: RequestOptions<T>
): Promise<T> {
  try {
    const response = await request(config)
    return response as T
  } catch (error) {
    throw error
  }
}

/** GET 请求 */
export function get<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>({
    method: 'GET',
    url,
    params,
    ...options,
  })
}

/** POST 请求 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>({
    method: 'POST',
    url,
    data,
    ...options,
  })
}

/** PUT 请求 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>({
    method: 'PUT',
    url,
    data,
    ...options,
  })
}

/** DELETE 请求 */
export function del<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>({
    method: 'DELETE',
    url,
    params,
    ...options,
  })
}

/** PATCH 请求 */
export function patch<T = unknown>(
  url: string,
  data?: unknown,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>({
    method: 'PATCH',
    url,
    data,
    ...options,
  })
}

/**
 * 静默请求封装
 * 用于有 Mock 数据回退的 API 调用，请求失败时不弹出错误提示
 * 使用方式：silentGet / silentPost / silentPut / silentDelete
 */
function addSilentFlag(config: AxiosRequestConfig): AxiosRequestConfig {
  return { ...config, _silentError: true } as AxiosRequestConfig & { _silentError: boolean }
}

/** 静默 GET 请求（失败不弹提示，由调用方自行处理） */
export function silentGet<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>(addSilentFlag({
    method: 'GET',
    url,
    params,
    ...options,
  }))
}

/** 静默 POST 请求（失败不弹提示，由调用方自行处理） */
export function silentPost<T = unknown>(
  url: string,
  data?: unknown,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>(addSilentFlag({
    method: 'POST',
    url,
    data,
    ...options,
  }))
}

/** 静默 PUT 请求（失败不弹提示，由调用方自行处理） */
export function silentPut<T = unknown>(
  url: string,
  data?: unknown,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>(addSilentFlag({
    method: 'PUT',
    url,
    data,
    ...options,
  }))
}

/** 静默 PATCH 请求（失败不弹提示，由调用方自行处理） */
export function silentPatch<T = unknown>(
  url: string,
  data?: unknown,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>(addSilentFlag({
    method: 'PATCH',
    url,
    data,
    ...options,
  }))
}

/** 静默 DELETE 请求（失败不弹提示，由调用方自行处理） */
export function silentDel<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  options?: Partial<RequestOptions>
): Promise<T> {
  return requestWrapper<T>(addSilentFlag({
    method: 'DELETE',
    url,
    params,
    ...options,
  }))
}

export default request
