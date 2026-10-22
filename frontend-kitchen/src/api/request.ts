import axios from 'axios'

// 请求参数类型
interface RequestParams {
  [key: string]: string | number | boolean | undefined | null | object
}

// 请求配置类型
export interface RequestConfig {
  headers?: Record<string, string>
  responseType?: 'blob' | 'json' | 'text' | 'arraybuffer'
  [key: string]: unknown
}

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true
})

/**
 * 2026-10-23 修复：KDS 设备凭证接线。
 *
 * 后厨端（/v1/kitchen/**、/v1/kitchen-order/**、/v1/tray/**）走后端的 KdsTokenFilter，
 * 凭证格式为 `Bearer kds_{storeId}_{secret}`（见后端 KdsAuthProperties / application.yml 的 app.kds.tokens）。
 * 原实现只挂 localStorage 里的用户 JWT —— 后厨页从未登录过，于是请求不带任何 Authorization，
 * 后端返回 401「未认证，请先登录」，表现为托盘绑不上、完成制作点不动。
 *
 * 取值优先级：localStorage('kds_token')（现场可覆盖） > 构建期 VITE_KDS_TOKEN（.env）。
 */
const KDS_ENDPOINT_PREFIXES = ['/v1/kitchen', '/v1/kitchen-order', '/v1/tray']

function resolveKdsToken(): string {
  const fromStorage = localStorage.getItem('kds_token')
  if (fromStorage) return fromStorage.trim()
  const fromEnv = import.meta.env.VITE_KDS_TOKEN as string | undefined
  return fromEnv ? fromEnv.trim() : ''
}

// 请求拦截器：KDS 接口用设备凭证，其余接口沿用用户 JWT
service.interceptors.request.use(
  (config) => {
    const url = config.url || ''
    if (KDS_ENDPOINT_PREFIXES.some((prefix) => url.startsWith(prefix))) {
      const kdsToken = resolveKdsToken()
      if (kdsToken) {
        config.headers.Authorization = `Bearer ${kdsToken}`
      } else {
        console.warn(
          '[KDS] 未配置设备凭证：请在 frontend-kitchen/.env 设置 VITE_KDS_TOKEN=kds_{storeId}_{secret}，' +
          '或执行 localStorage.setItem("kds_token", "kds_{storeId}_{secret}")；否则后厨接口会返回 401。'
        )
      }
      return config
    }

    const token = localStorage.getItem('token') || localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一提取data，处理错误码
service.interceptors.response.use(
  (response) => {
    const { data } = response

    // blob类型直接返回
    if (response.config.responseType === 'blob') {
      return data
    }

    // 统一响应格式：code=200/0时返回data.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 200 || data.code === 0) {
        return data.data !== undefined ? data.data : data
      }
      return Promise.reject({ message: data.message || '请求失败', status: data.code })
    }

    return data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject({ message, originalError: error })
  }
)

// 导出封装后的请求方法（与frontend项目保持一致）
export default {
  get<T = unknown>(url: string, params?: RequestParams, config?: RequestConfig): Promise<T> {
    return service.get(url, { params, ...config })
  },

  post<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = unknown>(url: string, params?: RequestParams): Promise<T> {
    return service.delete(url, { params })
  }
}
