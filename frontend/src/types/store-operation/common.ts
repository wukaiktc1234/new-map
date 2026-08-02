/** 分页请求参数 */
export interface PageParams {
  page: number
  size: number
}

/** 分页响应 */
export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 通用筛选表单 */
export interface SearchFormBase {
  keyword?: string
  dateRange?: [string, string] | null
}

/** 导出参数 */
export interface ExportParams {
  format: 'csv' | 'excel'
  dateRange?: [string, string]
  [key: string]: unknown
}

/** 操作结果 */
export interface OperationResult {
  success: boolean
  message: string
  data?: unknown
}
