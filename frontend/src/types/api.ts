/**
 * 统一API类型定义


/** 统一成功响应
 */
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

/** 分页响应数据（与后端IPage保持一致） */
export interface PageData<T = unknown> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

/** 分页请求参数
 */
export interface PageQueryParams {
  current?: number;
  size?: number;
  /** 允许扩展查询条件
 */
  [key: string]: unknown;
}

export interface SelectOption<T = string> {
  label: string;
  value: T;
  disabled?: boolean;
}

export interface TreeNode<T = unknown> {
  id: string | number;
  label: string;
  children?: TreeNode<T>[];
  [key: string]: unknown;
}

/** 操作结果（用于批量操作） */
export interface BatchResult {
  successCount: number;
  failCount: number;
  failures?: Array<{ id: string | number
  reason: string }>
}

/** 统计概览数据
 */
export interface StatsOverview {
  total: number;
  [key: string]: number | string;
}

/** 日期范围查询参数
 */
export interface DateRangeParams {
  startDate?: string;
  endDate?: string;
  [key: string]: unknown;
}
