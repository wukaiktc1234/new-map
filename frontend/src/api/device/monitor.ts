/**
 * 设备监控 API
 * 对应后端: /v1/device-alerts/monitor/* (DeviceAlertController 中的监控接口)
 *
 * 该模块提供实时监控相关能力：
 * - 手动触发设备状态检查
 * - 获取设备统计信息（在线/离线/故障/维护中数量）
 * - 获取最近状态变更记录
 * - 模拟设备上线/离线（测试用）
 */
import { get, post } from '../request'
import type { DeviceMonitorStatistics, DeviceRecentStatusChange } from '@/types/device'

export const deviceMonitorApi = {
  /**
   * 手动触发设备状态检查
   * 后端返回 Map<String, Object>，包含检查结果摘要
   */
  async checkStatus(): Promise<Record<string, unknown>> {
    return await post<Record<string, unknown>>('/v1/device-alerts/monitor/check', {})
  },

  /**
   * 获取设备统计信息（在线/离线/故障/维护中数量）
   * 后端返回 Map<String, Long>，字段名可能为 total/online/offline/fault/maintenance
   */
  async getStatistics(): Promise<DeviceMonitorStatistics> {
    const res = await get<Record<string, number> | null>('/v1/device-alerts/monitor/statistics')
    return {
      total: res?.total ?? 0,
      online: res?.online ?? 0,
      offline: res?.offline ?? 0,
      fault: res?.fault ?? 0,
      maintenance: res?.maintenance ?? 0,
    }
  },

  /**
   * 获取最近的状态变更记录
   * @param minutes 最近N分钟（默认60）
   * @param limit 返回条数（默认50）
   */
  async getRecentStatusChanges(minutes = 60, limit = 50): Promise<DeviceRecentStatusChange[]> {
    const res = await get<Array<Record<string, unknown>> | null>(
      '/v1/device-alerts/monitor/recent-changes',
      { minutes, limit }
    )
    if (!res || !Array.isArray(res)) return []
    return res.map((item) => ({
      deviceId: Number(item.deviceId ?? 0),
      deviceName: String(item.deviceName ?? ''),
      oldStatus: String(item.oldStatus ?? ''),
      newStatus: String(item.newStatus ?? ''),
      changeTime: String(item.changeTime ?? ''),
    }))
  },

  /**
   * 模拟设备上线（测试用）
   */
  async simulateOnline(deviceId: number): Promise<void> {
    await post<void>(`/v1/device-alerts/monitor/online/${deviceId}`, {})
  },

  /**
   * 模拟设备离线（测试用）
   */
  async simulateOffline(deviceId: number): Promise<void> {
    await post<void>(`/v1/device-alerts/monitor/offline/${deviceId}`, {})
  },
}

export default deviceMonitorApi
