/**
 * 用户 API
 * 对应后端: /v1/users (UserController)
 *
 * 用户状态（Integer 类型，与角色保持一致）：
 * - 1: 正常/启用（前端语义：active）
 * - 0: 禁用（前端语义：inactive）
 *
 * 注意：用户 ID 在后端为 Long，前端使用 number 类型（与 role.ts 保持一致）。
 *       用户名/邮箱/手机号唯一性检查为公开接口，无需登录即可调用。
 */
import { get, post, put, del } from '../request'

/** 后端返回的用户 VO */
export interface UserItem {
  /** 用户ID */
  id: number
  /** 用户名（登录名） */
  username: string
  /** 用户姓名 */
  fullName: string
  /** 用户邮箱 */
  email: string
  /** 用户手机号 */
  phone: string
  /** 用户状态：1 启用 / 0 禁用 */
  status: number
  /** 角色ID列表（JSON 字符串，如 ["1","2"]） */
  roles: string
  /** 角色名称列表（JSON 字符串，如 ["系统管理员","操作员"]，仅用于前端显示） */
  roleNames: string
  /** 所属部门名称（仅用于前端显示） */
  department: string
  /** 所属部门ID */
  departmentId: string
  /** 所属门店ID */
  storeId: string
  /** 所属门店名称（仅用于前端显示） */
  storeName: string
  /** 员工编号 */
  employeeCode: string
  /** 是否被锁定 */
  isLocked: boolean
  /** 最后登录时间（ISO 8601 字符串） */
  lastLoginTime: string
  /** 最后登录IP */
  lastLoginIp: string
  /** 头像URL */
  avatar: string
  /** 创建时间（ISO 8601 字符串） */
  createdTime: string
  /** 更新时间（ISO 8601 字符串） */
  updatedTime: string
  /** 创建人 */
  createdBy: string
  /** 更新人 */
  updatedBy: string
}

/** 创建用户 DTO */
export interface UserCreateDTO {
  /** 用户名（3-50字符，必填） */
  username: string
  /** 密码（6-100字符，必填） */
  password: string
  /** 用户姓名（必填） */
  fullName: string
  /** 用户邮箱 */
  email?: string
  /** 用户手机号 */
  phone?: string
  /** 用户状态：1 启用 / 0 禁用 */
  status?: number
  /** 所属部门ID */
  departmentId?: string
  /** 所属门店ID */
  storeId?: string
  /** 员工编号 */
  employeeCode?: string
  /** 头像URL */
  avatar?: string
}

/** 更新用户 DTO（id 通过 URL 路径传递，username 和 password 不可通过此接口修改） */
export interface UserUpdateDTO extends Partial<Omit<UserCreateDTO, 'username' | 'password'>> {
}

/** 用户查询参数 */
export interface UserQueryParams {
  /** 页码（默认 1） */
  page?: number
  /** 每页条数（默认 10） */
  pageSize?: number
  /** 用户名（模糊匹配） */
  username?: string
  /** 姓名（模糊匹配） */
  fullName?: string
  /** 邮箱（模糊匹配） */
  email?: string
  /** 手机号（模糊匹配） */
  phone?: string
  /** 角色（角色编码或ID） */
  role?: string
  /** 状态：1 启用 / 0 禁用（HTTP 查询参数为字符串） */
  status?: string | ''
  /** 部门 */
  department?: string
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
}

/** 分页响应 */
export interface UserPageResponse {
  records: UserItem[]
  total: number
  current: number
  size: number
  pages: number
}

/** 门店选项（用于分配门店下拉，对应后端 Store 实体） */
export interface StoreOption {
  /** 门店ID */
  storeId: string
  /** 门店名称 */
  storeName: string
  /** 门店编码（唯一） */
  storeCode: string
  /** 门店状态 */
  status: string
}

export const userApi = {
  /**
   * 分页查询用户列表
   * 对应 GET /v1/users/list
   */
  async getList(params: UserQueryParams = {}): Promise<UserPageResponse> {
    return await get<UserPageResponse>('/v1/users/list', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 10,
      pages: 0,
    }
  },

  /**
   * 根据ID查询用户
   * 对应 GET /v1/users/{id}
   */
  async getById(id: number): Promise<UserItem | null> {
    return await get<UserItem | null>(`/v1/users/${id}`)
  },

  /**
   * 创建用户
   * 对应 POST /v1/users
   */
  async create(data: UserCreateDTO): Promise<UserItem> {
    return await post<UserItem>('/v1/users', data)
  },

  /**
   * 更新用户信息
   * 对应 PUT /v1/users/{id}
   * 注意：username 和 password 不可通过此接口修改
   */
  async update(id: number, data: UserUpdateDTO): Promise<UserItem> {
    return await put<UserItem>(`/v1/users/${id}`, data)
  },

  /**
   * 重置用户密码
   * 对应 PUT /v1/users/{id}/reset-password
   * @param newPassword 新密码（6-100字符）
   */
  async resetPassword(id: number, newPassword: string): Promise<void> {
    await put<void>(`/v1/users/${id}/reset-password`, { newPassword })
  },

  /**
   * 切换用户状态（启用/禁用）
   * 对应 PUT /v1/users/{id}/status
   * @param status 1 启用 / 0 禁用
   */
  async toggleStatus(id: number, status: number): Promise<void> {
    await put<void>(`/v1/users/${id}/status`, { status })
  },

  /**
   * 分配门店给用户
   * 对应 PUT /v1/users/{id}/assign-store
   */
  async assignStore(id: number, storeId: string): Promise<void> {
    await put<void>(`/v1/users/${id}/assign-store`, { storeId })
  },

  /**
   * 取消用户的门店分配
   * 对应 DELETE /v1/users/{id}/assign-store
   */
  async unassignStore(id: number): Promise<void> {
    await del<void>(`/v1/users/${id}/assign-store`)
  },

  /**
   * 分配角色给用户
   * 对应 PUT /v1/users/{id}/assign-roles
   * @param roleIds 角色ID列表
   */
  async assignRoles(id: number, roleIds: string[]): Promise<void> {
    await put<void>(`/v1/users/${id}/assign-roles`, { roleIds })
  },

  /**
   * 取消用户的角色分配
   * 对应 DELETE /v1/users/{id}/assign-roles
   */
  async unassignRoles(id: number): Promise<void> {
    await del<void>(`/v1/users/${id}/assign-roles`)
  },

  /**
   * 获取用户的角色ID列表
   * 对应 GET /v1/users/{id}/roles
   */
  async getUserRoles(id: number): Promise<string[]> {
    return await get<string[]>(`/v1/users/${id}/roles`) || []
  },

  /**
   * 获取所有活跃门店列表（用于分配门店下拉）
   * 对应 GET /v1/users/stores
   */
  async getActiveStores(): Promise<StoreOption[]> {
    return await get<StoreOption[]>('/v1/users/stores') || []
  },

  /**
   * 检查用户名是否可用
   * 对应 GET /v1/users/check-username
   * @returns true 表示可用，false 表示已存在
   */
  async checkUsername(username: string): Promise<boolean> {
    return await get<boolean>('/v1/users/check-username', { username }) ?? false
  },

  /**
   * 检查邮箱是否可用
   * 对应 GET /v1/users/check-email
   * @returns true 表示可用，false 表示已存在
   */
  async checkEmail(email: string): Promise<boolean> {
    return await get<boolean>('/v1/users/check-email', { email }) ?? false
  },

  /**
   * 检查手机号是否可用
   * 对应 GET /v1/users/check-phone
   * @returns true 表示可用，false 表示已存在
   */
  async checkPhone(phone: string): Promise<boolean> {
    return await get<boolean>('/v1/users/check-phone', { phone }) ?? false
  },
}

export default userApi
