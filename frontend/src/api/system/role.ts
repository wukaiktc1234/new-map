/**
 * 角色 API
 * 对应后端: /v1/roles (RoleController)
 *
 * 角色类型：
 * - 1: 系统角色
 * - 2: 自定义角色
 *
 * 角色状态：
 * - 1: 启用（前端语义：active）
 * - 0: 禁用（前端语义：inactive）
 *
 * 数据范围（后端小写值）：
 * - all: 全部数据
 * - company: 本公司
 * - department: 本部门
 * - store: 本门店
 * - self: 仅本人
 */
import { get, post, put, del } from '../request'

/** 后端返回的角色 VO */
export interface RoleVO {
  /** 角色ID */
  id: number
  /** 角色编码（唯一） */
  roleCode: string
  /** 角色名称 */
  roleName: string
  /** 角色描述 */
  roleDescription: string
  /** 角色级别（数字越大级别越高） */
  roleLevel: number
  /** 角色状态：1启用 / 0禁用 */
  status: number
  /** 角色权限列表（JSON 字符串） */
  permissions: string
  /** 角色类型：1系统 / 2自定义 */
  roleType: number
  /** 是否系统内置角色 */
  systemBuilt: boolean
  /** 父角色ID（用于角色继承） */
  parentId: number | null
  /** 数据权限范围 */
  dataScope: string
  /** 可访问门店ID列表（JSON 字符串） */
  accessibleStores: string
  /** 可访问部门ID列表（JSON 字符串） */
  accessibleDepartments: string
  /** 创建时间（ISO 8601 字符串） */
  createdTime: string
  /** 更新时间（ISO 8601 字符串） */
  updatedTime: string
  /** 创建人ID */
  createdBy: number | null
  /** 更新人ID */
  updatedBy: number | null
}

/** 创建角色 DTO */
export interface RoleCreateDTO {
  roleCode: string
  roleName: string
  roleDescription?: string
  roleLevel?: number
  status?: number
  permissions?: string
  roleType?: number
  systemBuilt?: boolean
  parentId?: number | null
  dataScope?: string
  accessibleStores?: string
  accessibleDepartments?: string
}

/** 更新角色 DTO（id 通过 URL 路径传递，无需在 body 中重复） */
export interface RoleUpdateDTO extends Partial<RoleCreateDTO> {
}

/** 角色查询参数 */
export interface RoleQueryParams {
  /** 页码（默认 1） */
  page?: number
  /** 每页条数（默认 10） */
  pageSize?: number
  /** 角色编码（模糊匹配） */
  roleCode?: string
  /** 角色名称（模糊匹配） */
  roleName?: string
  /** 状态：1启用 / 0禁用 */
  status?: number | ''
  /** 角色类型：1系统 / 2自定义 */
  roleType?: number | ''
}

/** 分页响应 */
export interface RolePageResponse {
  records: RoleVO[]
  total: number
  current: number
  size: number
  pages: number
}

/** 角色统计信息 */
export interface RoleStatistics {
  total: number
  byStatus: Array<{ status: number; count: number }>
  byRoleType: Array<{ roleType: number; count: number }>
}

export const roleApi = {
  /**
   * 分页查询角色列表
   * 对应 GET /v1/roles/list
   */
  async getPage(params: RoleQueryParams = {}): Promise<RolePageResponse> {
    return await get<RolePageResponse>('/v1/roles/list', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 10,
      pages: 0,
    }
  },

  /**
   * 获取所有角色列表
   * 对应 GET /v1/roles/all
   */
  async getAll(): Promise<RoleVO[]> {
    return await get<RoleVO[]>('/v1/roles/all') || []
  },

  /**
   * 获取启用状态角色
   * 对应 GET /v1/roles/active
   */
  async getActive(): Promise<RoleVO[]> {
    return await get<RoleVO[]>('/v1/roles/active') || []
  },

  /**
   * 获取系统内置角色
   * 对应 GET /v1/roles/system
   */
  async getSystem(): Promise<RoleVO[]> {
    return await get<RoleVO[]>('/v1/roles/system') || []
  },

  /**
   * 获取自定义角色
   * 对应 GET /v1/roles/custom
   */
  async getCustom(): Promise<RoleVO[]> {
    return await get<RoleVO[]>('/v1/roles/custom') || []
  },

  /**
   * 根据ID查询角色
   * 对应 GET /v1/roles/{id}
   */
  async getById(id: number): Promise<RoleVO | null> {
    return await get<RoleVO | null>(`/v1/roles/${id}`)
  },

  /**
   * 根据编码查询角色
   * 对应 GET /v1/roles/code/{code}
   */
  async getByCode(code: string): Promise<RoleVO | null> {
    return await get<RoleVO | null>(`/v1/roles/code/${code}`)
  },

  /**
   * 创建角色
   * 对应 POST /v1/roles
   */
  async create(data: RoleCreateDTO): Promise<RoleVO> {
    return await post<RoleVO>('/v1/roles', data)
  },

  /**
   * 更新角色
   * 对应 PUT /v1/roles/{id}
   */
  async update(id: number, data: RoleUpdateDTO): Promise<RoleVO> {
    return await put<RoleVO>(`/v1/roles/${id}`, data)
  },

  /**
   * 删除角色
   * 对应 DELETE /v1/roles/{id}
   */
  async remove(id: number): Promise<void> {
    await del<void>(`/v1/roles/${id}`)
  },

  /**
   * 切换角色状态
   * 对应 PUT /v1/roles/{id}/status
   * @param status 1启用 / 0禁用（也兼容 'active'/'inactive' 字符串）
   */
  async toggleStatus(id: number, status: number | string): Promise<void> {
    await put<void>(`/v1/roles/${id}/status`, { status })
  },

  /**
   * 获取角色权限列表
   * 对应 GET /v1/roles/{id}/permissions
   * @returns 权限码字符串数组
   */
  async getPermissions(id: number): Promise<string[]> {
    return await get<string[]>(`/v1/roles/${id}/permissions`) || []
  },

  /**
   * 分配角色权限
   * 对应 PUT /v1/roles/{id}/permissions
   */
  async assignPermissions(id: number, permissions: string[]): Promise<void> {
    await put<void>(`/v1/roles/${id}/permissions`, { permissions })
  },

  /**
   * 获取角色统计信息
   * 对应 GET /v1/roles/statistics
   */
  async getStatistics(): Promise<RoleStatistics> {
    return await get<RoleStatistics>('/v1/roles/statistics') || {
      total: 0,
      byStatus: [],
      byRoleType: [],
    }
  },

  /**
   * 检查角色编码是否可用
   * 对应 GET /v1/roles/check-code
   * @returns true 表示可用，false 表示已存在
   */
  async checkCode(roleCode: string): Promise<boolean> {
    return await get<boolean>('/v1/roles/check-code', { roleCode }) ?? false
  },

  /**
   * 检查角色名称是否可用
   * 对应 GET /v1/roles/check-name
   * @returns true 表示可用，false 表示已存在
   */
  async checkName(roleName: string): Promise<boolean> {
    return await get<boolean>('/v1/roles/check-name', { roleName }) ?? false
  },
}

export default roleApi
