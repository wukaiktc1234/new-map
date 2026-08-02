/**
 * 系统设置 API（分层 scope）
 * 对应后端: /v1/settings (SysSettingController)
 *
 * 支持三层作用域：global（全局）/ store（门店）/ user（用户）
 * 查询时按 user → store → global 顺序回退查找
 */
import { get, post } from '../request'

/** 作用域类型 */
export type SettingScopeType = 'global' | 'store' | 'user'

/** 系统设置项 */
export interface SysSettingItem {
  settingId: number
  settingGroup: string
  settingKey: string
  scopeType: SettingScopeType
  scopeId: string
  settingValue: string
  valueType: string
  settingName: string
  settingDesc: string
  options: string | null
  validationRules: string | null
  sortOrder: number
  requiredPermission: string | null
  isEnabled: number
  isSystem: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** 分组查询响应 */
export interface SysSettingGroupResponse {
  records: SysSettingItem[]
}

/** 批量获取请求 */
export interface BatchGetValueRequest {
  keys: Array<{ key: string; scopeType?: SettingScopeType; scopeId?: string }>
}

/** 批量获取响应 */
export interface BatchGetValueResponse {
  [key: string]: string
}

export const sysSettingApi = {
  /**
   * 根据分组获取设置列表
   */
  async getGroupSettings(group: string): Promise<SysSettingItem[]> {
    const resp = await get<SysSettingGroupResponse>(`/v1/settings/groups/${group}`) || { records: [] }
    return resp.records || []
  },

  /**
   * 获取设置值（分层查找：user → store → global）
   */
  async getValue(key: string, scopeType?: SettingScopeType, scopeId?: string): Promise<string> {
    const params: Record<string, unknown> = {}
    if (scopeType) params.scopeType = scopeType
    if (scopeId) params.scopeId = scopeId
    return await get<string>(`/v1/settings/values/${key}`, params) || ''
  },

  /**
   * 批量获取设置值
   */
  async batchGetValues(items: BatchGetValueRequest['keys']): Promise<BatchGetValueResponse> {
    return await post<BatchGetValueResponse>('/v1/settings/values/batch', { keys: items }) || {}
  },

  /**
   * 设置全局配置
   */
  async setGlobal(key: string, value: string): Promise<void> {
    await post<void>(`/v1/settings/global/${key}`, { value })
  },

  /**
   * 设置门店配置
   * 后端参数：storeId 作为 URL Query Param，value 作为纯字符串 Body
   */
  async setStore(key: string, value: string, storeId: string): Promise<void> {
    await post<void>(`/v1/settings/store/${key}?storeId=${storeId}`, value)
  },

  /**
   * 设置用户配置
   * 后端参数：userId 作为 URL Query Param，value 作为纯字符串 Body
   */
  async setUser(key: string, value: string, userId: string): Promise<void> {
    await post<void>(`/v1/settings/user/${key}?userId=${userId}`, value)
  },

  /**
   * 初始化默认设置
   */
  async initDefaults(): Promise<void> {
    await post<void>('/v1/settings/init')
  },
}

export default sysSettingApi
