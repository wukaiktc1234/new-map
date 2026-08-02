/**
 * 门店档案管理类型定义
 *
 * 对应后端 StoreNew 实体（stores_new 表）
 */

/** 门店类型（前端语义化） */
export type StoreType = 'direct' | 'franchise' | 'cooperation'

/** 门店状态（前端语义化） */
export type StoreStatus = 'running' | 'renovating' | 'paused' | 'closed'

/** 门店档案（前端展示对象） */
export interface StoreArchive {
  storeId: number
  storeCode: string
  storeName: string
  storeType: StoreType
  address: string
  phone: string
  businessHoursStart: string
  businessHoursEnd: string
  areaSize: number
  /** 所属区域标识（前端下拉选项 value，如 beijing/shanghai） */
  area: string
  managerId: number | null
  openDate: string
  status: StoreStatus
  licenseNo: string
  licenseExpiry: string
  configJson: string
  remark: string
  createTime: string
  updateTime: string
}

/** 门店档案列表查询参数 */
export interface StoreArchiveQuery {
  current: number
  size: number
  storeName?: string
  storeType?: StoreType | null
  status?: StoreStatus | null
}

/** 门店档案分页响应 */
export interface StoreArchivePage {
  records: StoreArchive[]
  total: number
  current: number
  size: number
  pages: number
}

/** 新建门店表单数据 */
export interface StoreArchiveCreateForm {
  storeCode: string
  storeName: string
  storeType: StoreType
  address: string
  phone: string
  businessHoursStart: string
  businessHoursEnd: string
  areaSize: number
  /** 所属区域标识（可选，空字符串表示未指定） */
  area: string
  managerId: number | null
  openDate: string
  status: StoreStatus
  licenseNo: string
  licenseExpiry: string
  configJson: string
  remark: string
}

/** 更新门店表单数据（所有字段可选） */
export interface StoreArchiveUpdateForm {
  storeName?: string
  storeType?: StoreType
  address?: string
  phone?: string
  businessHoursStart?: string
  businessHoursEnd?: string
  areaSize?: number
  /** 所属区域标识（可选） */
  area?: string
  managerId?: number | null
  openDate?: string
  status?: StoreStatus
  licenseNo?: string
  licenseExpiry?: string
  configJson?: string
  remark?: string
}

/** 门店统计信息 */
export interface StoreArchiveStats {
  total: number
  byStatus: {
    running: number
    renovating: number
    paused: number
    closed: number
  }
  byType: {
    direct: number
    franchise: number
    cooperation: number
  }
  licenseExpiringSoon: number
}

/** 编码可用性检查响应 */
export interface StoreCodeAvailability {
  available: boolean
  storeCode: string
}

/** 门店选项（下拉选项） */
export interface StoreOption {
  storeId: number
  storeCode: string
  storeName: string
}

/** 门店类型选项配置 */
export const STORE_TYPE_OPTIONS: { value: StoreType; label: string }[] = [
  { value: 'direct', label: '直营' },
  { value: 'franchise', label: '加盟' },
  { value: 'cooperation', label: '合作' },
]

/** 门店状态选项配置 */
export const STORE_STATUS_OPTIONS: { value: StoreStatus; label: string }[] = [
  { value: 'running', label: '营业中' },
  { value: 'renovating', label: '装修中' },
  { value: 'paused', label: '暂停营业' },
  { value: 'closed', label: '已关闭' },
]

/** 门店类型映射：前端 ↔ 后端数字编码 */
export const STORE_TYPE_TO_BACKEND: Record<StoreType, number> = {
  direct: 1,
  franchise: 2,
  cooperation: 3,
}

export const STORE_TYPE_FROM_BACKEND: Record<number, StoreType> = {
  1: 'direct',
  2: 'franchise',
  3: 'cooperation',
}

/** 门店状态映射：前端 ↔ 后端数字编码 */
export const STORE_STATUS_TO_BACKEND: Record<StoreStatus, number> = {
  running: 1,
  renovating: 2,
  paused: 3,
  closed: 4,
}

export const STORE_STATUS_FROM_BACKEND: Record<number, StoreStatus> = {
  1: 'running',
  2: 'renovating',
  3: 'paused',
  4: 'closed',
}

/** 获取门店类型标签 */
export function getStoreTypeLabel(type: StoreType): string {
  return STORE_TYPE_OPTIONS.find(o => o.value === type)?.label || type
}

/** 获取门店状态标签 */
export function getStoreStatusLabel(status: StoreStatus): string {
  return STORE_STATUS_OPTIONS.find(o => o.value === status)?.label || status
}
