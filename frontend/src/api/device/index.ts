/**
 * 设备管理 API + DataConverter
 * 对应后端: /v1/devices (DeviceController)
 *
 * DataConverter 处理内容：
 * - 设备类型 Integer ↔ String（后端 1~5 ↔ 前端 PRINTER/SCANNER/SCALE/LOCKER/OTHER）
 * - 连接类型 Integer ↔ String（后端 1~4 ↔ 前端 USB/SERIAL/NETWORK/BLUETOOTH）
 * - 设备状态 Integer ↔ String（后端 0~3 ↔ 前端 offline/online/fault/maintenance）
 *
 * 遵循规范：禁止在组件中直接做状态字段映射，统一在此处完成 API 边界转换。
 */
import { get, post, put, del } from '../request'
import type { PageResponse } from '@/types'
import type {
  DeviceInfo,
  DeviceFormData,
  DeviceQueryForm,
  DeviceType,
  DeviceConnectionType,
  DeviceStatus,
} from '@/types/device'

// ============================================================
// 后端原始类型定义（Integer 编码）
// ============================================================

/**
 * 后端返回的设备 VO（Integer 编码）
 * 注意：connectionParams / lastOnlineTime / remark 后端 VO 当前未直接暴露，
 *      此处声明为可选字段以保证前向兼容，缺失时降级为空字符串。
 */
interface DeviceBackendVO {
  deviceId: number
  deviceCode: string | null
  deviceName: string
  deviceType: number          // 1=打印机 2=扫码枪 3=称重秤 4=取餐柜 5=其他
  deviceModel: string | null
  manufacturer: string | null
  serialNo: string | null
  connectionType: number      // 1=USB 2=串口 3=网络 4=蓝牙
  connectionParams: string | null
  location: string | null
  storeId: number | null
  status: number              // 0=离线 1=在线 2=故障 3=维护中
  lastHeartbeatTime: string | null
  lastOnlineTime: string | null
  remark: string | null
}

/** 后端 IPage 分页响应 */
interface DevicePageBackend {
  records: DeviceBackendVO[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 枚举转换映射表（后端 Integer ↔ 前端 String）
// ============================================================

/** 后端数值 → 前端设备类型 */
const deviceTypeBackendMap: Record<number, DeviceType> = {
  1: 'PRINTER',
  2: 'SCANNER',
  3: 'SCALE',
  4: 'LOCKER',
  5: 'OTHER',
}
/** 前端设备类型 → 后端数值 */
const deviceTypeFrontendMap: Record<DeviceType, number> = {
  PRINTER: 1,
  SCANNER: 2,
  SCALE: 3,
  LOCKER: 4,
  OTHER: 5,
}

/** 后端数值 → 前端连接类型 */
const connectionTypeBackendMap: Record<number, DeviceConnectionType> = {
  1: 'USB',
  2: 'SERIAL',
  3: 'NETWORK',
  4: 'BLUETOOTH',
}
/** 前端连接类型 → 后端数值 */
const connectionTypeFrontendMap: Record<DeviceConnectionType, number> = {
  USB: 1,
  SERIAL: 2,
  NETWORK: 3,
  BLUETOOTH: 4,
}

/** 后端数值 → 前端状态 */
const statusBackendMap: Record<number, DeviceStatus> = {
  0: 'offline',
  1: 'online',
  2: 'fault',
  3: 'maintenance',
}
/** 前端状态 → 后端数值 */
const statusFrontendMap: Record<DeviceStatus, number> = {
  offline: 0,
  online: 1,
  fault: 2,
  maintenance: 3,
}

// ============================================================
// DataConverter（API 层）
// ============================================================

/** 后端 VO → 前端 DeviceInfo */
function toDeviceInfo(backend: DeviceBackendVO): DeviceInfo {
  return {
    deviceId: backend.deviceId,
    deviceCode: backend.deviceCode ?? '',
    deviceName: backend.deviceName,
    deviceType: deviceTypeBackendMap[backend.deviceType] ?? 'OTHER',
    deviceModel: backend.deviceModel ?? '',
    manufacturer: backend.manufacturer ?? '',
    serialNo: backend.serialNo ?? '',
    connectionType: connectionTypeBackendMap[backend.connectionType] ?? 'USB',
    connectionParams: backend.connectionParams ?? '',
    location: backend.location ?? '',
    storeId: backend.storeId ?? 0,
    status: statusBackendMap[backend.status] ?? 'offline',
    lastHeartbeatTime: backend.lastHeartbeatTime ?? '',
    lastOnlineTime: backend.lastOnlineTime ?? '',
    remark: backend.remark ?? '',
  }
}

/** 后端 VO 列表 → 前端 DeviceInfo 列表 */
function toDeviceInfoList(list: DeviceBackendVO[] | null | undefined): DeviceInfo[] {
  if (!list || !Array.isArray(list)) return []
  return list.map(toDeviceInfo)
}

/** 前端表单 → 后端创建/更新 DTO */
function toBackendDTO(data: DeviceFormData): Record<string, unknown> {
  return {
    deviceCode: data.deviceCode,
    deviceName: data.deviceName,
    deviceType: deviceTypeFrontendMap[data.deviceType],
    deviceModel: data.deviceModel || null,
    manufacturer: data.manufacturer || null,
    serialNo: data.serialNo || null,
    connectionType: connectionTypeFrontendMap[data.connectionType],
    connectionParams: data.connectionParams || null,
    location: data.location || null,
    storeId: data.storeId || null,
    remark: data.remark || null,
  }
}

/**
 * 前端查询表单 → 后端查询参数（Integer 编码）
 * 后端 /page 接口支持 deviceName、deviceCode 模糊搜索（无 keyword 字段），
 * 此处将前端 keyword 统一映射为 deviceName 传递。
 */
function toBackendQuery(params: DeviceQueryForm & { page: number; size: number }): Record<string, unknown> {
  const query: Record<string, unknown> = {
    page: params.page,
    size: params.size,
  }
  if (params.keyword) query.deviceName = params.keyword
  if (params.deviceType) query.deviceType = deviceTypeFrontendMap[params.deviceType]
  if (params.status) query.status = statusFrontendMap[params.status]
  if (params.storeId != null) query.storeId = params.storeId
  return query
}

// ============================================================
// API 实现
// ============================================================

export const deviceApi = {
  /**
   * 分页查询设备列表
   * @param params 查询参数（含分页 page/size）
   */
  async getList(params: DeviceQueryForm & { page: number; size: number }): Promise<PageResponse<DeviceInfo>> {
    const query = toBackendQuery(params)
    const res = await get<DevicePageBackend | null>('/v1/devices/page', query)
    const records = toDeviceInfoList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? params.page
    const size = res?.size ?? params.size
    const pages = res?.pages ?? (size > 0 ? Math.ceil(total / size) : 0)
    return { records, total, current, size, pages }
  },

  /**
   * 查询设备详情
   * @param id 设备ID
   */
  async getById(id: number): Promise<DeviceInfo> {
    const res = await get<DeviceBackendVO>(`/v1/devices/${id}`)
    return toDeviceInfo(res)
  },

  /**
   * 查询在线设备列表
   */
  async getOnline(): Promise<DeviceInfo[]> {
    const res = await get<DeviceBackendVO[] | null>('/v1/devices/online')
    return toDeviceInfoList(res)
  },

  /**
   * 查询设备列表（不分页）
   */
  async getAll(): Promise<DeviceInfo[]> {
    const res = await get<DeviceBackendVO[] | null>('/v1/devices/list')
    return toDeviceInfoList(res)
  },

  /**
   * 创建设备
   * @param data 创建表单
   */
  async create(data: DeviceFormData): Promise<DeviceInfo> {
    const dto = toBackendDTO(data)
    const res = await post<DeviceBackendVO>('/v1/devices', dto)
    return toDeviceInfo(res)
  },

  /**
   * 更新设备
   * @param id 设备ID
   * @param data 更新表单
   */
  async update(id: number, data: DeviceFormData): Promise<DeviceInfo> {
    const dto = toBackendDTO(data)
    const res = await put<DeviceBackendVO>(`/v1/devices/${id}`, dto)
    return toDeviceInfo(res)
  },

  /**
   * 删除设备（后端为逻辑删除）
   * @param id 设备ID
   */
  async delete(id: number): Promise<void> {
    await del<void>(`/v1/devices/${id}`)
  },

  /**
   * 更新设备状态
   * @param id 设备ID
   * @param status 后端状态数值（0=离线 1=在线 2=故障 3=维护中）
   */
  async updateStatus(id: number, status: number): Promise<void> {
    // 后端通过 @RequestBody Map<String, Integer> 接收，使用 JSON body 传递
    await put<void>(`/v1/devices/${id}/status`, { status })
  },

  /**
   * 启用/停用设备
   * @param id 设备ID
   * @param enabled true=启用 false=停用（后端通过 @RequestParam 接收）
   */
  async toggle(id: number, enabled: boolean): Promise<void> {
    await put<void>(`/v1/devices/${id}/toggle?enabled=${enabled}`)
  },

  /**
   * 测试设备连接
   * @param id 设备ID
   * @returns 连接是否成功（请求成功即视为连接正常）
   */
  async testConnection(id: number): Promise<boolean> {
    await post<void>(`/v1/devices/${id}/test`)
    return true
  },
}

export default deviceApi
