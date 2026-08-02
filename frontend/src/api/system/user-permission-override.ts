/**
 * 用户权限覆盖 API
 * 对应后端: /v1/user-permission-overrides (UserPermissionOverrideController)
 *
 * 覆盖类型（OverrideType）：
 * - ADD: 为用户添加权限
 * - REMOVE: 为用户移除权限
 *
 * 覆盖状态（OverrideStatus）：
 * - ACTIVE: 生效中（审批通过）
 * - PENDING: 待审批
 * - EXPIRED: 已过期
 * - REVOKED: 已撤销
 * - REJECTED: 已拒绝
 */
import { get, post, put, del } from '../request'
import type { OverrideType, OverrideStatus } from '@/types/permission'

/** 后端返回的用户权限覆盖 VO */
export interface UserPermissionOverrideVO {
  /** 主键ID */
  id: number
  /** 被覆盖的用户ID */
  userId: string
  /** 用户姓名 */
  userName: string | null
  /** 权限码（如 "product:*"） */
  permissionCode: string
  /** 权限名称 */
  permissionName: string | null
  /** 业务域（如 "product"） */
  domainCode: string | null
  /** 覆盖类型：ADD / REMOVE */
  overrideType: OverrideType
  /** 操作原因 */
  reason: string | null
  /** 过期时间（ISO 8601 字符串，空表示永久） */
  expireTime: string | null
  /** 状态 */
  status: OverrideStatus
  /** 审批人ID */
  approveBy: string | null
  /** 审批人姓名 */
  approveName: string | null
  /** 审批时间（ISO 8601 字符串） */
  approveTime: string | null
  /** 创建人ID */
  createdBy: string | null
  /** 创建人姓名 */
  createdByName: string | null
  /** 创建时间（ISO 8601 字符串） */
  createdAt: string
  /** 更新时间（ISO 8601 字符串） */
  updatedAt: string
}

/** 创建用户权限覆盖 DTO */
export interface UserPermissionOverrideCreateDTO {
  userId: string
  userName?: string
  permissionCode: string
  permissionName?: string
  domainCode?: string
  overrideType: OverrideType
  reason?: string
  /** ISO 8601 字符串，不填则永久有效 */
  expireTime?: string
  createdBy?: string
  createdByName?: string
}

/** 更新用户权限覆盖 DTO（id 通过 URL 路径传递，无需在 body 中重复） */
export interface UserPermissionOverrideUpdateDTO {
  userId?: string
  userName?: string
  permissionCode?: string
  permissionName?: string
  domainCode?: string
  overrideType?: OverrideType
  reason?: string
  /** ISO 8601 字符串，不填则永久有效 */
  expireTime?: string | null
}

/** 用户权限覆盖查询参数 */
export interface UserPermissionOverrideQueryParams {
  /** 页码（默认 1） */
  page?: number
  /** 每页条数（默认 10） */
  size?: number
  /** 用户ID */
  userId?: string
  /** 用户姓名（模糊匹配） */
  userName?: string
  /** 权限码（模糊匹配） */
  permissionCode?: string
  /** 业务域 */
  domainCode?: string
  /** 覆盖类型：ADD / REMOVE */
  overrideType?: OverrideType | ''
  /** 状态 */
  status?: OverrideStatus | ''
}

/** 分页响应 */
export interface UserPermissionOverridePageResponse {
  records: UserPermissionOverrideVO[]
  total: number
  current: number
  size: number
  pages: number
}

export const userPermissionOverrideApi = {
  /**
   * 分页查询用户权限覆盖列表
   * 对应 GET /v1/user-permission-overrides
   */
  async getPage(params: UserPermissionOverrideQueryParams = {}): Promise<UserPermissionOverridePageResponse> {
    return await get<UserPermissionOverridePageResponse>('/v1/user-permission-overrides', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 10,
      pages: 0,
    }
  },

  /**
   * 根据ID查询用户权限覆盖
   * 对应 GET /v1/user-permission-overrides/{id}
   */
  async getById(id: number): Promise<UserPermissionOverrideVO | null> {
    return await get<UserPermissionOverrideVO | null>(`/v1/user-permission-overrides/${id}`)
  },

  /**
   * 根据用户ID查询所有覆盖
   * 对应 GET /v1/user-permission-overrides/user/{userId}
   */
  async getByUserId(userId: string): Promise<UserPermissionOverrideVO[]> {
    return await get<UserPermissionOverrideVO[]>(`/v1/user-permission-overrides/user/${userId}`) || []
  },

  /**
   * 创建用户权限覆盖
   * 对应 POST /v1/user-permission-overrides
   * 创建后默认进入 PENDING 待审批状态
   */
  async create(data: UserPermissionOverrideCreateDTO): Promise<UserPermissionOverrideVO> {
    return await post<UserPermissionOverrideVO>('/v1/user-permission-overrides', data)
  },

  /**
   * 更新用户权限覆盖
   * 对应 PUT /v1/user-permission-overrides/{id}
   */
  async update(id: number, data: UserPermissionOverrideUpdateDTO): Promise<UserPermissionOverrideVO> {
    return await put<UserPermissionOverrideVO>(`/v1/user-permission-overrides/${id}`, data)
  },

  /**
   * 删除用户权限覆盖（逻辑删除）
   * 对应 DELETE /v1/user-permission-overrides/{id}
   */
  async remove(id: number): Promise<void> {
    await del<void>(`/v1/user-permission-overrides/${id}`)
  },

  /**
   * 审批用户权限覆盖
   * 对应 POST /v1/user-permission-overrides/{id}/approve
   * @param approved true-通过（ACTIVE），false-拒绝（REJECTED）
   */
  async approve(id: number, approverId: string, approverName: string, approved: boolean): Promise<UserPermissionOverrideVO> {
    return await post<UserPermissionOverrideVO>(
      `/v1/user-permission-overrides/${id}/approve`,
      undefined,
      { params: { approverId, approverName, approved } }
    )
  },

  /**
   * 撤销用户权限覆盖（仅 ACTIVE 状态可撤销）
   * 对应 POST /v1/user-permission-overrides/{id}/revoke
   */
  async revoke(id: number): Promise<UserPermissionOverrideVO> {
    return await post<UserPermissionOverrideVO>(`/v1/user-permission-overrides/${id}/revoke`)
  },

  /**
   * 刷新过期状态（admin only）
   * 将所有已过期但状态仍为 ACTIVE 的记录改为 EXPIRED
   * 对应 POST /v1/user-permission-overrides/refresh-expired
   */
  async refreshExpired(): Promise<boolean> {
    return await post<boolean>('/v1/user-permission-overrides/refresh-expired')
  },
}

export default userPermissionOverrideApi
