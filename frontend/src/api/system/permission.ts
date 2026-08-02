/**
 * 权限管理 API
 * 对应后端: /v1/permissions (PermissionController)
 *
 * 字段约定：
 * - permissionType: 1-菜单 / 2-按钮 / 3-接口
 * - status: 1-启用 / 0-禁用（与后端数字编码对齐）
 * - id / parentId: 后端 Long 经 ToStringSerializer 序列化为字符串
 */
import { get, post, put, del } from '../request'

/** 权限类型枚举（数字编码，与后端对齐） */
export type PermissionType = 1 | 2 | 3

/** 权限状态枚举（数字编码，与后端对齐） */
export type PermissionStatus = 1 | 0

/** 后端返回的权限 VO（对应 Permission 实体） */
export interface PermissionVO {
  id: string
  permissionCode: string
  permissionName: string
  permissionType: PermissionType | number
  module: string | null
  parentId: string | null
  level: number | null
  sortOrder: number | null
  status: PermissionStatus | number
  icon: string | null
  path: string | null
  component: string | null
  redirect: string | null
  isHidden: number | null
  isCache: number | null
  createdAt: string | null
  updatedAt: string | null
  children?: PermissionVO[]
}

/** 创建权限 DTO */
export interface PermissionCreateDTO {
  permissionCode: string
  permissionName: string
  permissionType: number
  module?: string | null
  parentId?: string | null
  level?: number
  sortOrder?: number
  status?: number
  icon?: string
  path?: string
  component?: string
  redirect?: string
  isHidden?: number
  isCache?: number
}

/** 更新权限 DTO */
export interface PermissionUpdateDTO extends PermissionCreateDTO {
  id: string
}

/** 查询参数（对应 PermissionSearchDTO） */
export interface PermissionQueryParams {
  page?: number
  size?: number
  permissionCode?: string
  permissionName?: string
  permissionType?: string
  status?: string
  module?: string
}

/** 分页响应 */
export interface PermissionPageResponse {
  records: PermissionVO[]
  total: number
  current: number
  size: number
  pages: number
}

/** 状态统计 */
export interface PermissionStatusCount {
  status: number
  count: number
}

/** 权限树节点（对应 PermissionTreeNode DTO） */
export interface PermissionTreeNodeVO {
  id: string
  permissionCode: string
  permissionName: string
  permissionType: number
  module: string | null
  parentId: string | null
  sortOrder: number | null
  checked?: boolean
  disabled?: boolean
  children?: PermissionTreeNodeVO[]
}

/** 同步结果（对应 PermissionSyncResult DTO） */
export interface PermissionSyncResultVO {
  added: number
  updated: number
  deleted: number
  unchanged: number
  addedPermissions: string[]
  updatedPermissions: string[]
  deletedPermissions: string[]
}

/** 权限类型选项 */
export const PERMISSION_TYPE_OPTIONS: Array<{ value: number; label: string }> = [
  { value: 1, label: '菜单' },
  { value: 2, label: '按钮' },
  { value: 3, label: '接口' },
]

/** 状态选项 */
export const PERMISSION_STATUS_OPTIONS: Array<{ value: number; label: string }> = [
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
]

/**
 * 后端数字状态 → StatusTag 字符串
 * 状态字段映射统一在 API 边界完成，禁止在组件中直接做映射
 */
export function permissionStatusToTagStatus(status: number | null | undefined): 'active' | 'inactive' {
  return status === 1 ? 'active' : 'inactive'
}

/** 后端数字状态 → 中文标签 */
export function permissionStatusToLabel(status: number | null | undefined): string {
  return status === 1 ? '启用' : '禁用'
}

/** 权限类型 → 中文标签 */
export function permissionTypeToLabel(type: number | null | undefined): string {
  const item = PERMISSION_TYPE_OPTIONS.find(o => o.value === type)
  return item?.label ?? '-'
}

/** 权限类型 → StatusTag 字符串（仅用于颜色分类展示） */
export function permissionTypeToTagStatus(type: number | null | undefined): 'success' | 'warning' | 'info' {
  if (type === 1) return 'success'
  if (type === 2) return 'warning'
  return 'info'
}

export const permissionApi = {
  /** 获取所有启用的权限 */
  async getAll(): Promise<PermissionVO[]> {
    return await get<PermissionVO[]>('/v1/permissions') || []
  },

  /** 统计各状态的权限数量 */
  async countByStatus(): Promise<PermissionStatusCount[]> {
    return await get<PermissionStatusCount[]>('/v1/permissions/count-by-status') || []
  },

  /** 分页查询权限 */
  async getPage(params: { page?: number; size?: number } = {}): Promise<PermissionPageResponse> {
    return await get<PermissionPageResponse>('/v1/permissions/page', params as Record<string, unknown>) || {
      records: [], total: 0, current: 1, size: 10, pages: 0,
    }
  },

  /** 条件分页查询权限（GET 方式） */
  async search(params: PermissionQueryParams = {}): Promise<PermissionPageResponse> {
    return await get<PermissionPageResponse>('/v1/permissions/search', params as Record<string, unknown>) || {
      records: [], total: 0, current: 1, size: 10, pages: 0,
    }
  },

  /** 获取权限树结构（含 children 嵌套） */
  async getTree(): Promise<PermissionVO[]> {
    return await get<PermissionVO[]>('/v1/permissions/tree') || []
  },

  /** 获取权限树节点（用于父级权限选择） */
  async getTreeNodes(): Promise<PermissionTreeNodeVO[]> {
    return await get<PermissionTreeNodeVO[]>('/v1/permissions/tree-nodes') || []
  },

  /** 创建权限 */
  async create(data: PermissionCreateDTO): Promise<PermissionVO> {
    return await post<PermissionVO>('/v1/permissions', data)
  },

  /** 更新权限 */
  async update(permissionId: string, data: PermissionUpdateDTO): Promise<PermissionVO> {
    return await put<PermissionVO>(`/v1/permissions/${permissionId}`, data)
  },

  /** 删除权限 */
  async delete(permissionId: string): Promise<void> {
    await del<void>(`/v1/permissions/${permissionId}`)
  },

  /** 更新权限状态（status 作为 query 参数传递） */
  async updateStatus(permissionId: string, status: number): Promise<void> {
    await put<void>(`/v1/permissions/${permissionId}/status`, undefined, {
      params: { status },
    } as Record<string, unknown>)
  },

  /** 更新权限排序（sortOrder 作为 body 传递） */
  async updateSortOrder(permissionId: string, sortOrder: number): Promise<void> {
    await put<void>(`/v1/permissions/${permissionId}/sort-order`, { sortOrder })
  },

  /** 初始化权限数据（仅 admin） */
  async init(): Promise<void> {
    await post<void>('/v1/permissions/init')
  },

  /** 同步权限数据（仅 admin） */
  async sync(): Promise<PermissionSyncResultVO> {
    return await post<PermissionSyncResultVO>('/v1/permissions/sync')
  },

  /** 检查权限完整性 */
  async checkIntegrity(): Promise<boolean> {
    return await get<boolean>('/v1/permissions/check-integrity') ?? false
  },
}

export default permissionApi
