/**
 * 资产管理API
 * 对应后端: /v1/asset (AssetController)
 *
 * 后端接口：
 * - GET /v1/asset/overview           资产概览
 * - GET /v1/asset/list               资产列表（分页）
 * - GET /v1/asset/{id}               资产详情
 * - POST /v1/asset                   新增资产
 * - PUT /v1/asset/{id}               更新资产
 * - DELETE /v1/asset/{id}            删除资产
 * - POST /v1/asset/{id}/repair       开始维修
 * - POST /v1/asset/{id}/complete-repair 完成维修
 * - POST /v1/asset/{id}/scrap        报废
 */
import { get, post, put, del } from '../request'
import type {
  Asset,
  AssetStatistics,
  AssetStatisticsVO,
  AssetQueryDTO,
  PageResponse,
} from '../../types/asset'
import { AssetStatus, DepreciationMethod } from '../../types/asset'
import { fenToYuanNumber } from '@/utils/money'

// ============================================================
// 后端字段类型定义（AssetMasterVO）
// ============================================================

/** 后端资产VO原始结构 */
interface AssetBackendVO {
  id?: number
  assetCode?: string
  assetName?: string
  categoryId?: number
  categoryName?: string
  specification?: string
  brand?: string
  unit?: string
  purchaseDate?: string
  originalValue?: number
  netValue?: number
  accumulatedDepreciation?: number
  residualValue?: number
  depreciationMethod?: string
  usefulLifeMonths?: number
  status?: string
  statusName?: string
  storeId?: number
  storeName?: string
  location?: string
  assetType?: string
  qrCode?: string
  useCount?: number
  currentOrderId?: string
  currentKitchenOrderId?: number
  custodianName?: string
  imageUrl?: string
  createTime?: string
  updateTime?: string
}

/** 后端分页响应 */
interface AssetPageBackend {
  records: AssetBackendVO[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
}

/** 后端概览响应 */
interface OverviewBackend {
  total?: number
  idle?: number
  inUse?: number
  repairing?: number
  damaged?: number
  statusStats?: Array<Record<string, unknown>>
  typeStats?: Array<Record<string, unknown>>
  storeStats?: Array<Record<string, unknown>>
}

// ============================================================
// 数据转换：后端状态字符串 → 前端枚举
// ============================================================

/** 后端状态 → 前端 AssetStatus 映射 */
const BackendStatusMap: Record<string, AssetStatus> = {
  idle: AssetStatus.IDLE,
  in_use: AssetStatus.ACTIVE,
  repairing: AssetStatus.MAINTENANCE,
  damaged: AssetStatus.TO_BE_DISPOSED,
  active: AssetStatus.ACTIVE,
  maintenance: AssetStatus.MAINTENANCE,
  to_be_disposed: AssetStatus.TO_BE_DISPOSED,
  disposed: AssetStatus.DISPOSED,
  scrapped: AssetStatus.SCRAPPED,
  transferred: AssetStatus.TRANSFERRED,
}

/** 前端 AssetStatus → 后端状态字符串 */
const FrontendStatusMap: Record<AssetStatus, string> = {
  [AssetStatus.ACTIVE]: 'in_use',
  [AssetStatus.IDLE]: 'idle',
  [AssetStatus.MAINTENANCE]: 'repairing',
  [AssetStatus.TO_BE_DISPOSED]: 'damaged',
  [AssetStatus.DISPOSED]: 'disposed',
  [AssetStatus.SCRAPPED]: 'scrapped',
  [AssetStatus.TRANSFERRED]: 'transferred',
}

/** 前端折旧方法 → 后端编码映射 */
const DepreciationMethodToBackend: Record<string, number> = {
  straight_line: 1,
  double_declining: 2,
  sum_of_years: 3,
}

/**
 * 后端 VO → 前端 Asset 转换
 * 处理字段名映射、ID 类型转换、状态枚举映射
 */
function toAsset(vo: AssetBackendVO): Asset {
  const backendStatus = vo.status || 'idle'
  return {
    id: String(vo.id ?? ''),
    assetCode: vo.assetCode || '',
    assetName: vo.assetName || '',
    categoryId: vo.categoryId != null ? String(vo.categoryId) : '',
    categoryName: vo.categoryName,
    originalValue: vo.originalValue ?? 0,
    currentValue: vo.netValue ?? 0,
    accumulatedDepreciation: vo.accumulatedDepreciation ?? 0,
    purchaseDate: vo.purchaseDate || '',
    usefulLifeMonths: vo.usefulLifeMonths ?? 0,
    usedMonths: vo.useCount ?? 0,
    depreciationMethod: (vo.depreciationMethod as DepreciationMethod) || DepreciationMethod.STRAIGHT_LINE,
    salvageRate: 0,
    departmentId: '',
    departmentName: '',
    storeId: vo.storeId != null ? String(vo.storeId) : undefined,
    storeName: vo.storeName,
    location: vo.location || '',
    status: BackendStatusMap[backendStatus] || AssetStatus.IDLE,
    responsibleUserName: vo.custodianName,
    specification: vo.specification,
    serialNumber: vo.qrCode,
    createTime: vo.createTime || '',
    updateTime: vo.updateTime || '',
  }
}

/**
 * 前端 Asset 部分字段 → 后端创建/更新 DTO
 * 金额字段：originalValue 为分（后端原值），需转为元赋给 originalCostYuan
 */
function toBackendDTO(data: Partial<Asset> & Record<string, unknown>): Record<string, unknown> {
  const dto: Record<string, unknown> = {}
  if (data.assetCode !== undefined) dto.assetCode = data.assetCode
  if (data.assetName !== undefined) dto.assetName = data.assetName
  // categoryId：前端为字符串，后端为 Long，需转换
  if (data.categoryId !== undefined) dto.categoryId = Number(data.categoryId)
  if (data.specification !== undefined) dto.specification = data.specification
  if (data.purchaseDate !== undefined) dto.purchaseDate = data.purchaseDate
  // originalValue（分）→ originalCostYuan（元），修复原值未转换的 P0 Bug
  if (data.originalValue !== undefined) dto.originalCostYuan = fenToYuanNumber(data.originalValue as number)
  if (data.usefulLifeMonths !== undefined) dto.usefulLifeMonths = data.usefulLifeMonths
  // depreciationMethod：前端字符串枚举 → 后端数字编码
  if (data.depreciationMethod !== undefined) {
    dto.depreciationMethod = DepreciationMethodToBackend[data.depreciationMethod as string] || 1
  }
  if (data.location !== undefined) dto.location = data.location
  // storeId：前端为字符串，后端为 Long
  if (data.storeId !== undefined) dto.storeId = Number(data.storeId)
  // status 由后端自动管理，创建时无需传入
  // supplierName → supplierInfo（供应商信息）
  if (data.supplierName !== undefined) dto.supplierInfo = data.supplierName
  return dto
}

// ============================================================
// API 实现
// ============================================================

const assetApi = {
  /**
   * 获取资产列表（分页）
   * 后端路径：GET /v1/asset/list
   */
  async getList(params?: AssetQueryDTO): Promise<PageResponse<Asset>> {
    const query: Record<string, unknown> = {
      current: params?.page ?? 1,
      size: params?.pageSize ?? 20,
    }
    if (params?.keyword) query.keyword = params.keyword
    if (params?.categoryId) query.categoryId = params.categoryId
    if (params?.status) query.status = FrontendStatusMap[params.status as AssetStatus] || params.status
    if (params?.departmentId) query.departmentId = params.departmentId

    const res = await get<AssetPageBackend | null>('/v1/asset/list', query)
    const records = (res?.records || []).map(toAsset)
    return {
      records,
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.pageSize ?? 20),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取资产详情
   * 后端路径：GET /v1/asset/{id}
   */
  async getById(id: string): Promise<Asset | null> {
    try {
      const res = await get<AssetBackendVO>(`/v1/asset/${id}`)
      return res ? toAsset(res) : null
    } catch {
      return null
    }
  },

  /**
   * 创建资产
   * 后端路径：POST /v1/asset
   */
  async create(data: Partial<Asset>): Promise<Asset> {
    const dto = toBackendDTO(data)
    const res = await post<AssetBackendVO>('/v1/asset', dto)
    return toAsset(res)
  },

  /**
   * 更新资产
   * 后端路径：PUT /v1/asset/{id}
   */
  async update(id: string, data: Partial<Asset>): Promise<Asset | null> {
    const dto = toBackendDTO(data)
    const res = await put<AssetBackendVO>(`/v1/asset/${id}`, dto)
    return res ? toAsset(res) : null
  },

  /**
   * 删除资产
   * 后端路径：DELETE /v1/asset/{id}
   */
  async delete(id: string): Promise<boolean> {
    await del<void>(`/v1/asset/${id}`)
    return true
  },

  /**
   * 获取资产统计概览
   * 后端路径：GET /v1/asset/overview
   * 注：后端返回状态分类统计，此处转换为前端 AssetStatistics 结构
   */
  async getStatistics(): Promise<AssetStatistics> {
    const res = await get<OverviewBackend>('/v1/asset/overview')
    return {
      originalValue: 0,
      accumulatedDepreciation: 0,
      netValue: 0,
      monthlyDepreciation: 0,
    }
  },

  /**
   * 获取资产统计视图对象
   * 后端路径：GET /v1/asset/overview
   * 注：后端 overview 接口返回状态分类计数，此处映射为前端 VO
   */
  async getStatisticsVO(): Promise<AssetStatisticsVO> {
    const res = await get<OverviewBackend>('/v1/asset/overview')
    const total = res?.total ?? 0
    const idleCount = res?.idle ?? 0
    const activeCount = res?.inUse ?? 0
    const maintenanceCount = res?.repairing ?? 0
    const toBeDisposedCount = res?.damaged ?? 0
    return {
      totalAssets: total,
      totalOriginalValue: 0,
      totalOriginalValueYuan: '0.00',
      totalCurrentValue: 0,
      totalCurrentValueYuan: '0.00',
      thisMonthNewCount: 0,
      thisMonthNewValue: 0,
      thisMonthNewValueYuan: '0.00',
      activeCount,
      idleCount,
      maintenanceCount,
      toBeDisposedCount,
      disposedCount: 0,
      scrappedCount: 0,
      transferredCount: 0,
    }
  },
}

export default assetApi
