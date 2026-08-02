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

// 请求拦截器：自动添加Token
service.interceptors.request.use(
  (config) => {
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
