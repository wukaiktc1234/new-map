/**
 * 系统配置 API（新版）
 * 对应后端: /v1/sys-config (SysConfigController)
 *
 * 管理系统级配置项，支持分组管理、历史记录、缓存清理
 * 与 sys-setting.ts（分层 scope）不同，本模块为全局扁平配置
 */
import { get, put, post, del } from '../request'
import type {
  SysConfigItem,
  SysConfigQueryParams,
  PageResponse,
  BatchUpdateItem,
  BatchUpdateResult,
  ConfigHistoryRecord,
  HistoryQueryParams,
  SysConfigStats,
  ConfigGroupInfo,
} from '@/types/sys-config'

export const sysConfigApi = {
  /**
   * 获取单个配置值
   */
  async getValue(configKey: string): Promise<string> {
    return await get<string>(`/v1/sys-config/value/${configKey}`) || ''
  },

  /**
   * 按分组获取所有键值对
   */
  async getGroupValues(groupName: string): Promise<Record<string, string>> {
    return await get<Record<string, string>>(`/v1/sys-config/group/${groupName}/values`) || {}
  },

  /**
   * 获取配置详情（脱敏）
   */
  async getConfig(configKey: string): Promise<SysConfigItem | null> {
    return await get<SysConfigItem | null>(`/v1/sys-config/${configKey}`)
  },

  /**
   * 分页查询配置列表
   */
  async getList(params: SysConfigQueryParams = {}): Promise<PageResponse<SysConfigItem>> {
    return await get<PageResponse<SysConfigItem>>('/v1/sys-config', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 20,
      pages: 0,
    }
  },

  /**
   * 获取所有分组列表
   */
  async getGroups(): Promise<ConfigGroupInfo[]> {
    return await get<ConfigGroupInfo[]>('/v1/sys-config/groups') || []
  },

  /**
   * 修改配置值
   */
  async updateValue(configKey: string, value: string): Promise<void> {
    await put<void>(`/v1/sys-config/value/${configKey}`, { value })
  },

  /**
   * 批量修改配置
   */
  async batchUpdate(items: BatchUpdateItem[]): Promise<BatchUpdateResult> {
    // 后端 @RequestBody List<SysConfigBatchUpdateItemDTO> 期望直接接收 JSON 数组
    return await post<BatchUpdateResult>('/v1/sys-config/batch', items)
  },

  /**
   * 重置为默认值
   */
  async resetToDefault(configKey: string): Promise<void> {
    await post<void>(`/v1/sys-config/${configKey}/reset`)
  },

  /**
   * 重置整个分组（仅 ADMIN）
   */
  async resetGroup(groupName: string): Promise<void> {
    await post<void>(`/v1/sys-config/group/${groupName}/reset`)
  },

  /**
   * 启用配置
   */
  async enable(configKey: string): Promise<void> {
    await post<void>(`/v1/sys-config/${configKey}/enable`)
  },

  /**
   * 禁用配置
   */
  async disable(configKey: string): Promise<void> {
    await post<void>(`/v1/sys-config/${configKey}/disable`)
  },

  /**
   * 查询变更历史
   */
  async getHistory(params: HistoryQueryParams = {}): Promise<PageResponse<ConfigHistoryRecord>> {
    return await get<PageResponse<ConfigHistoryRecord>>('/v1/sys-config/history', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 20,
      pages: 0,
    }
  },

  /**
   * 清除配置缓存（仅 ADMIN）
   */
  async clearCache(configKey: string): Promise<void> {
    await del<void>(`/v1/sys-config/cache/${configKey}`)
  },

  /**
   * 清除分组缓存（仅 ADMIN）
   */
  async clearGroupCache(groupName: string): Promise<void> {
    await del<void>(`/v1/sys-config/cache/group/${groupName}`)
  },

  /**
   * 清除全部缓存（仅 ADMIN）
   */
  async clearAllCache(): Promise<void> {
    await del<void>('/v1/sys-config/cache/all')
  },

  /**
   * 获取统计信息
   */
  async getStats(): Promise<SysConfigStats> {
    return await get<SysConfigStats>('/v1/sys-config/stats') || {
      totalConfigs: 0,
      enabledConfigs: 0,
      disabledConfigs: 0,
      sensitiveConfigs: 0,
      readonlyConfigs: 0,
      editableConfigs: 0,
      groups: [],
      totalHistoryRecords: 0,
    }
  },
}

export default sysConfigApi
