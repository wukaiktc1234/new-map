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
import { ElMessage } from 'element-plus'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import logger from '@/utils/logger'

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
    console.warn(
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
  (config: InternalAxiosRequestConfig) => {
    // 添加认证 Token
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
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

      // 特殊错误码处理
      switch (res.code) {
        case 401:
          if (import.meta.env.PROD) {
            // 生产环境：清除认证状态并跳转登录页
            ElMessage.error('登录已过期，请重新登录')
            clearAuthAndRedirect()
          }
          // 开发环境：不跳转、不清空标签页，让 silentGet 走 mock 回退
          // 原因：开发模式无真实 token，后端必然返回 401，若跳转会清空 tab-bar-tabs 导致标签页丢失
          break
        case 403:
          if (!silentError) ElMessage.error('没有权限执行此操作')
          break
        case 404:
          logger.warn('Response', `接口不存在: ${res.message || ''}`)
          return Promise.reject(new Error(res.message || '请求的资源不存在'))
        case 500:
          // 显示后端返回的实际错误信息（如"用户名或密码错误"），而非通用提示
          if (!silentError) ElMessage.error(errorMessage)
          break
        case 461:
          // 461表示"需要验证码"，不是真正的错误，返回data让调用方处理
          // 登录接口返回461时，data包含requireCaptcha/captchaId/captchaImage
          logger.info('Response', '需要验证码，返回验证码数据')
          return res.data
        default:
          // 461（需要验证码）等其他业务错误：静默模式下不弹提示
          if (!silentError) ElMessage.error(errorMessage)
      }

      logger.error('Response', `业务错误 [${res.code}]`, errorMessage)
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
          errorMessage = '请求参数错误'
          break
        case 401:
          errorMessage = '登录已过期，请重新登录'
          if (import.meta.env.PROD) {
            // 生产环境：清除认证状态并跳转登录页
            clearAuthAndRedirect()
          }
          // 开发环境：不跳转、不清空标签页，让 silentGet 走 mock 回退
          // 原因：开发模式无真实 token，后端必然返回 401，若跳转会清空 tab-bar-tabs 导致标签页丢失
          break
        case 403:
          errorMessage = '没有权限执行此操作'
          break
        case 404:
          errorMessage = `接口不存在: ${url}`
          // 仅当显式允许 mock 模式时才启用，否则暴露真实错误
          if (MOCK_ALLOWED) enableMockMode()
          break
        case 408:
          errorMessage = '请求超时'
          break
        case 500:
          errorMessage = '服务器内部错误'
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
    console.warn(
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
