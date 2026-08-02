/**
 * 设备状态历史 API
 * 对应后端: /v1/device-status-history (DeviceStatusHistoryController)
 *
 * 注意：后端 DeviceStatusHistory 实体的 deviceType 为 String（如 "PRINTER"），
 * 与 Device 实体的 Integer deviceType 不同，本模块不做转换，直接传递字符串。
 *
 * DM-3 改造：getByDeviceType 和 getByDeviceId 已改为后端真分页，
 * 返回 PageResponse<DeviceStatusHistoryInfo> 而非数组。
 */
import { get } from '../request'
import type { PageResponse } from '@/types'
import type { DeviceStatusHistoryInfo, DeviceStatusHistoryQueryForm } from '@/types/device'

// 后端返回的原始类型（字段名与实体一致）
interface DeviceStatusHistoryBackend {
  id: number
  deviceType: string | null
  deviceId: number | null
  deviceName: string | null
  deviceModel: string | null
  online: boolean | null
  responseTime: number | null
  firmwareVersion: string | null
  connectionType: string | null
  ipAddress: string | null
  port: string | null
  checkTime: string | null
  details: string | null
  errorMessage: string | null
  storeId: number | null
  createdAt: string | null
}

// 后端 IPage<DeviceStatusHistory> 序列化结构
interface DeviceStatusHistoryPageBackend {
  records: DeviceStatusHistoryBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

function toHistoryInfo(backend: DeviceStatusHistoryBackend): DeviceStatusHistoryInfo {
  return {
    id: backend.id,
    deviceType: backend.deviceType ?? '',
    deviceId: backend.deviceId ?? 0,
    deviceName: backend.deviceName ?? '',
    deviceModel: backend.deviceModel ?? '',
    online: backend.online ?? false,
    responseTime: backend.responseTime ?? 0,
    firmwareVersion: backend.firmwareVersion ?? '',
    connectionType: backend.connectionType ?? '',
    ipAddress: backend.ipAddress ?? '',
    port: backend.port ?? '',
    checkTime: backend.checkTime ?? '',
    details: backend.details ?? '',
    errorMessage: backend.errorMessage ?? '',
    storeId: backend.storeId,
    createdAt: backend.createdAt ?? '',
  }
}

function toHistoryList(list: DeviceStatusHistoryBackend[] | null | undefined): DeviceStatusHistoryInfo[] {
  if (!list || !Array.isArray(list)) return []
  return list.map(toHistoryInfo)
}

/**
 * 将后端 IPage 结构转换为前端 PageResponse 结构
 */
function toPageResponse(
  res: DeviceStatusHistoryPageBackend | null,
  fallbackPage: number,
  fallbackSize: number
): PageResponse<DeviceStatusHistoryInfo> {
  const records = toHistoryList(res?.records)
  const total = res?.total ?? 0
  const current = res?.current ?? fallbackPage
  const size = res?.size ?? fallbackSize
  const pages = res?.pages ?? (size > 0 ? Math.ceil(total / size) : 0)
  return { records, total, current, size, pages }
}

export const deviceHistoryApi = {
  /**
   * 获取过去7天的设备状态历史记录
   * @param deviceType 设备类型（字符串，如 "PRINTER"）
   * @param storeId 门店ID（可选）
   */
  async getLast7Days(deviceType: string, storeId?: number): Promise<DeviceStatusHistoryInfo[]> {
    const params: Record<string, unknown> = { deviceType }
    if (storeId != null) params.storeId = storeId
    const res = await get<DeviceStatusHistoryBackend[] | null>('/v1/device-status-history/last-7-days', params)
    return toHistoryList(res)
  },

  /**
   * 获取过去24小时的设备状态历史记录
   */
  async getLast24Hours(deviceType: string, storeId?: number): Promise<DeviceStatusHistoryInfo[]> {
    const params: Record<string, unknown> = { deviceType }
    if (storeId != null) params.storeId = storeId
    const res = await get<DeviceStatusHistoryBackend[] | null>('/v1/device-status-history/last-24-hours', params)
    return toHistoryList(res)
  },

  /**
   * 按设备类型查询设备状态历史记录（后端真分页）
   * @returns 分页结果，包含 records/total/current/size/pages
   */
  async getByDeviceType(params: DeviceStatusHistoryQueryForm): Promise<PageResponse<DeviceStatusHistoryInfo>> {
    const pageNum = params.pageNum ?? 1
    const pageSize = params.pageSize ?? 20
    const query: Record<string, unknown> = {
      pageSize,
      pageNum,
    }
    if (params.deviceType) query.deviceType = params.deviceType
    if (params.startTime) query.startTime = params.startTime
    if (params.endTime) query.endTime = params.endTime
    if (params.storeId != null) query.storeId = params.storeId
    const res = await get<DeviceStatusHistoryPageBackend | null>('/v1/device-status-history', query)
    return toPageResponse(res, pageNum, pageSize)
  },

  /**
   * 查询指定设备的状态历史记录（后端真分页）
   * @returns 分页结果，包含 records/total/current/size/pages
   */
  async getByDeviceId(deviceId: number, params?: Omit<DeviceStatusHistoryQueryForm, 'deviceId' | 'deviceType'>): Promise<PageResponse<DeviceStatusHistoryInfo>> {
    const pageNum = params?.pageNum ?? 1
    const pageSize = params?.pageSize ?? 20
    const query: Record<string, unknown> = {
      pageSize,
      pageNum,
    }
    if (params?.startTime) query.startTime = params.startTime
    if (params?.endTime) query.endTime = params.endTime
    const res = await get<DeviceStatusHistoryPageBackend | null>(`/v1/device-status-history/device/${deviceId}`, query)
    return toPageResponse(res, pageNum, pageSize)
  },

  /**
   * 统计设备状态变化次数
   */
  async countStatusChanges(deviceType: string, startTime?: string, endTime?: string, storeId?: number): Promise<number> {
    const params: Record<string, unknown> = { deviceType }
    if (startTime) params.startTime = startTime
    if (endTime) params.endTime = endTime
    if (storeId != null) params.storeId = storeId
    const res = await get<number>('/v1/device-status-history/status-changes-count', params)
    return res ?? 0
  },
}

export default deviceHistoryApi
