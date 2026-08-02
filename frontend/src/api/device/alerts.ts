/**
 * 设备告警 API + DataConverter
 * 对应后端: /v1/device-alerts (DeviceAlertController)
 *
 * DataConverter 处理内容：
 * - 告警类型 Integer ↔ String（后端 1~5 ↔ 前端 offline_timeout/paper_out/ribbon_out/fault/maintenance_reminder）
 * - 告警级别 Integer ↔ String（后端 1~4 ↔ 前端 info/warning/critical/urgent）
 * - 告警状态 Integer ↔ String（后端 0~3 ↔ 前端 pending/handling/resolved/ignored）
 */
import { get, put } from '../request'
import type { PageResponse } from '@/types'
import type {
  DeviceAlertInfo,
  DeviceAlertQueryForm,
  DeviceAlertType,
  DeviceAlertLevel,
  DeviceAlertStatus,
  DeviceType,
} from '@/types/device'

// ============================================================
// 后端原始类型定义（Integer 编码）
// ============================================================

interface DeviceAlertBackendVO {
  alertId: number
  deviceId: number
  deviceName: string | null
  deviceType: number | null
  alertType: number
  alertLevel: number
  alertMessage: string | null
  alertStatus: number
  isHandled: boolean | null
  triggerTime: string | null
  handleTime: string | null
  handleResult: string | null
  handleUserId: number | null
  resolveTime: string | null
  deviceStatusSnapshot: string | null
  createTime: string | null
}

interface DeviceAlertPageBackend {
  records: DeviceAlertBackendVO[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 枚举转换映射表
// ============================================================

const alertTypeBackendMap: Record<number, DeviceAlertType> = {
  1: 'offline_timeout',
  2: 'paper_out',
  3: 'ribbon_out',
  4: 'fault',
  5: 'maintenance_reminder',
}
const alertTypeFrontendMap: Record<DeviceAlertType, number> = {
  offline_timeout: 1,
  paper_out: 2,
  ribbon_out: 3,
  fault: 4,
  maintenance_reminder: 5,
}

const alertLevelBackendMap: Record<number, DeviceAlertLevel> = {
  1: 'info',
  2: 'warning',
  3: 'critical',
  4: 'urgent',
}
const alertLevelFrontendMap: Record<DeviceAlertLevel, number> = {
  info: 1,
  warning: 2,
  critical: 3,
  urgent: 4,
}

const alertStatusBackendMap: Record<number, DeviceAlertStatus> = {
  0: 'pending',
  1: 'handling',
  2: 'resolved',
  3: 'ignored',
}
const alertStatusFrontendMap: Record<DeviceAlertStatus, number> = {
  pending: 0,
  handling: 1,
  resolved: 2,
  ignored: 3,
}

const deviceTypeBackendMap: Record<number, DeviceType> = {
  1: 'PRINTER',
  2: 'SCANNER',
  3: 'SCALE',
  4: 'LOCKER',
  5: 'OTHER',
}

// ============================================================
// DataConverter
// ============================================================

function toAlertInfo(backend: DeviceAlertBackendVO): DeviceAlertInfo {
  return {
    alertId: backend.alertId,
    deviceId: backend.deviceId,
    deviceName: backend.deviceName ?? '',
    deviceType: backend.deviceType != null ? (deviceTypeBackendMap[backend.deviceType] ?? null) : null,
    alertType: alertTypeBackendMap[backend.alertType] ?? 'fault',
    alertLevel: alertLevelBackendMap[backend.alertLevel] ?? 'info',
    alertMessage: backend.alertMessage ?? '',
    alertStatus: alertStatusBackendMap[backend.alertStatus] ?? 'pending',
    isHandled: backend.isHandled ?? false,
    triggerTime: backend.triggerTime ?? '',
    handleTime: backend.handleTime ?? '',
    handleResult: backend.handleResult ?? '',
    handleUserId: backend.handleUserId,
    resolveTime: backend.resolveTime ?? '',
    deviceStatusSnapshot: backend.deviceStatusSnapshot ?? '',
    createTime: backend.createTime ?? '',
  }
}

function toAlertInfoList(list: DeviceAlertBackendVO[] | null | undefined): DeviceAlertInfo[] {
  if (!list || !Array.isArray(list)) return []
  return list.map(toAlertInfo)
}

function toBackendQuery(params: DeviceAlertQueryForm & { page: number; size: number }): Record<string, unknown> {
  const query: Record<string, unknown> = {
    page: params.page,
    size: params.size,
  }
  if (params.deviceId != null) query.deviceId = params.deviceId
  if (params.alertType) query.alertType = alertTypeFrontendMap[params.alertType]
  if (params.alertLevel) query.alertLevel = alertLevelFrontendMap[params.alertLevel]
  // 告警状态优先：alertStatus 不为空时传 alertStatus（Integer），保留精确语义
  // 兼容性：alertStatus 为空但 isHandled 不为空时，仍传 isHandled（Boolean）
  if (params.alertStatus) query.alertStatus = alertStatusFrontendMap[params.alertStatus]
  else if (params.isHandled != null) query.isHandled = params.isHandled
  if (params.startTime) query.startTime = params.startTime
  if (params.endTime) query.endTime = params.endTime
  return query
}

// ============================================================
// API 实现
// ============================================================

export const deviceAlertApi = {
  /**
   * 分页查询告警列表
   */
  async getList(params: DeviceAlertQueryForm & { page: number; size: number }): Promise<PageResponse<DeviceAlertInfo>> {
    const query = toBackendQuery(params)
    const res = await get<DeviceAlertPageBackend | null>('/v1/device-alerts/page', query)
    const records = toAlertInfoList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? params.page
    const size = res?.size ?? params.size
    const pages = res?.pages ?? (size > 0 ? Math.ceil(total / size) : 0)
    return { records, total, current, size, pages }
  },

  /**
   * 查询告警详情
   */
  async getById(alertId: number): Promise<DeviceAlertInfo> {
    const res = await get<DeviceAlertBackendVO>(`/v1/device-alerts/${alertId}`)
    return toAlertInfo(res)
  },

  /**
   * 查询设备的未处理告警
   */
  async getUnhandledByDevice(deviceId: number): Promise<DeviceAlertInfo[]> {
    const res = await get<DeviceAlertBackendVO[] | null>(`/v1/device-alerts/device/${deviceId}/unhandled`)
    return toAlertInfoList(res)
  },

  /**
   * 查询所有未处理告警
   */
  async getAllUnhandled(): Promise<DeviceAlertInfo[]> {
    const res = await get<DeviceAlertBackendVO[] | null>('/v1/device-alerts/unhandled')
    return toAlertInfoList(res)
  },

  /**
   * 处理告警
   * @param alertId 告警ID
   * @param handleResult 处理结果说明
   */
  async handle(alertId: number, handleResult: string): Promise<void> {
    await put<void>(`/v1/device-alerts/${alertId}/handle`, { handleResult })
  },

  /**
   * 批量处理告警
   */
  async batchHandle(alertIds: number[], handleResult: string): Promise<void> {
    await put<void>('/v1/device-alerts/batch-handle', { alertIds, handleResult })
  },

  /**
   * 获取告警统计信息
   * 后端返回 Object，前端按需取用
   */
  async getStatistics(): Promise<Record<string, unknown>> {
    return await get<Record<string, unknown>>('/v1/device-alerts/statistics')
  },
}

export default deviceAlertApi
