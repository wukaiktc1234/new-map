export interface PaginationParams {
  page: number
  size: number
}

export interface PaginationData<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface SelectOption<T = string | number> {
  label: string
  value: T
  disabled?: boolean
}

export interface QuickActionItem {
  icon: string
  label: string
  path: string
  color: string
  badge?: string | number
}

export interface DashboardStat {
  label: string
  value: string | number
  subText?: string
  trend?: 'up' | 'down' | 'stable'
  color: string
}
