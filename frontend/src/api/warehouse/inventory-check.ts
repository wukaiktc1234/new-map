/**
 * 库存盘点 API + DataConverter
 * 对应后端: /v1/inventory-checks
 *
 * DataConverter 处理内容：
 * - 盘点类型数字↔字符串（后端 1~3 ↔ 前端 'blind'/'open'/'cycle'）
 * - 盘点状态数字↔字符串（后端 0~4 ↔ 前端 'pending'/'checking'/'approved'/'completed'/'cancelled'）
 *   注：'cancelled' 对应后端 4，避免错误映射到 2=已审核
 * - ID number↔string（避免大数精度丢失）
 * - 差异金额 分↔元（后端 Long 分 ↔ 前端 string 元，2位小数）
 */
import { get, post, put } from '../request'
// 金额转换统一委托给 utils/money（fenToYuan 返回 string，保留 2 位小数）
import { fenToYuan } from '@/utils/money'

import type {
  InventoryCheckInfo,
  InventoryCheckQueryForm,
  InventoryCheckCreateForm,
  InventoryCheckItemForm,
  InventoryCheckItemInfo,
  CheckType,
  CheckStatus,
  CheckMode,
} from '@/types/warehouse-check'
import { inventoryCheckConverter } from './converters'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的盘点明细 VO */
interface InventoryCheckItemBackend {
  checkItemId: number
  checkId: number
  inventoryId: number
  materialName: string | null
  specification: string | null
  unit: string | null
  bookQty: number
  actualQty: number | null
  diffQty: number | null
  diffAmount: number | null  // 单位：分
  reason: string | null
  dualConfirmed: number | null  // 0/1
  hasPhoto: number | null         // 0/1
  varianceExplanation: string | null
}

/** 后端返回的盘点单 VO */
interface InventoryCheckBackend {
  checkId: number
  checkCode: string | null
  warehouseId: number
  warehouseName: string | null
  checkType: number  // 1=盲盘 2=明盘 3=循环盘点
  checkStatus: number  // 0=待盘点 1=盘点中 2=已审核 3=已完成
  checkDate: string | null  // yyyy-MM-dd
  completeDate: string | null  // yyyy-MM-dd
  createUserId: number | null
  createUserName: string | null
  approveUserId: number | null
  approveUserName: string | null
  approveTime: string | null
  participatingDepts: string[] | null
  remark: string | null
  items?: InventoryCheckItemBackend[] | null
  createTime: string | null
  updateTime: string | null
}

/** 后端 IPage 分页响应（部分接口可能返回数组，类型宽松兼容） */
interface InventoryCheckPageBackend {
  records?: InventoryCheckBackend[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
}

// ============================================================
// 内部 DataConverter（API 层，金额转换统一来自 utils/money）
// ============================================================

/**
 * 盘点数据转换器（API 层）
 *
 * 复用 converters.ts 导出的 inventoryCheckConverter 处理类型/状态字符串↔数字，
 * 补充后端 VO → 前端 Info 的全字段映射。
 */
const inventoryCheckApiConverter = {
  /** 后端明细 VO → 前端 InventoryCheckItemInfo */
  toItemFrontend(backend: InventoryCheckItemBackend): InventoryCheckItemInfo {
    return {
      checkItemId: String(backend.checkItemId),
      checkId: String(backend.checkId),
      inventoryId: String(backend.inventoryId),
      materialName: backend.materialName ?? '',
      specification: backend.specification ?? '',
      unit: backend.unit ?? '',
      bookQty: backend.bookQty,
      actualQty: backend.actualQty ?? 0,
      diffQty: backend.diffQty ?? 0,
      diffAmount: fenToYuan(backend.diffAmount),
      reason: backend.reason ?? '',
      dualConfirmed: backend.dualConfirmed === 1,
      hasPhoto: backend.hasPhoto === 1,
      varianceExplanation: backend.varianceExplanation ?? undefined,
    }
  },

  /** 后端 VO → 前端 InventoryCheckInfo */
  toFrontend(backend: InventoryCheckBackend): InventoryCheckInfo {
    const checkType: CheckType = inventoryCheckConverter.toFrontendType(backend.checkType)
    const checkStatus: CheckStatus = inventoryCheckConverter.toFrontendStatus(backend.checkStatus)
    return {
      checkId: String(backend.checkId),
      checkCode: backend.checkCode ?? '',
      warehouseId: String(backend.warehouseId),
      warehouseName: backend.warehouseName ?? '',
      checkType,
      checkTypeName: inventoryCheckConverter.toTypeLabel(checkType),
      checkStatus,
      checkStatusName: inventoryCheckConverter.toStatusLabel(checkStatus),
      checkDate: backend.checkDate ?? '',
      completeDate: backend.completeDate ?? '',
      createUserId: backend.createUserId != null ? String(backend.createUserId) : '',
      createUserName: backend.createUserName ?? '',
      approveUserId: backend.approveUserId != null ? String(backend.approveUserId) : '',
      approveUserName: backend.approveUserName ?? '',
      approveTime: backend.approveTime ?? '',
      participatingDepts: backend.participatingDepts ?? [],
      deptConfirmations: [],
      // 后端实体未直接存储 checkMode/checkTeam/freezeInventory/varianceThreshold
      // 默认根据 checkType 推断 checkMode（盲盘=blind, 其他=open）
      checkMode: (checkType === 'blind' ? 'blind' : 'open') as CheckMode,
      checkTeam: {
        leaderId: '',
        leaderName: '',
        counterIds: [],
        counterNames: [],
        financeSupervisorId: '',
        financeSupervisorName: '',
        warehouseManagerId: '',
        warehouseManagerName: '',
      },
      freezeInventory: false,
      varianceThreshold: 0,
      warehouseApprovalStatus: 'pending',
      warehouseApproverName: '',
      warehouseApproveTime: '',
      financeConfirmStatus: 'pending',
      financeConfirmerName: '',
      financeConfirmTime: '',
      remark: backend.remark ?? '',
      items: (backend.items ?? []).map(inventoryCheckApiConverter.toItemFrontend),
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryCheckBackend[] | null | undefined): InventoryCheckInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryCheckApiConverter.toFrontend)
  },

  /** 前端 CreateForm → 后端 CreateDTO */
  toCreateDTO(form: InventoryCheckCreateForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {
      warehouseId: Number(form.warehouseId),
      checkType: inventoryCheckConverter.toBackendType(form.checkType),
      checkDate: form.checkDate,
    }
    if (form.participatingDepts) dto.participatingDepts = form.participatingDepts
    if (form.remark) dto.remark = form.remark
    return dto
  },

  /** 前端 InventoryCheckItemForm 列表 → 后端提交 DTO */
  toItemSubmitDTO(items: InventoryCheckItemForm[]): Record<string, unknown>[] {
    return items.map(item => {
      const dto: Record<string, unknown> = {
        checkItemId: Number(item.checkItemId),
        actualQty: item.actualQty,
      }
      if (item.reason !== undefined) dto.reason = item.reason
      if (item.dualConfirmed !== undefined) dto.dualConfirmed = item.dualConfirmed ? 1 : 0
      if (item.hasPhoto !== undefined) dto.hasPhoto = item.hasPhoto ? 1 : 0
      if (item.varianceExplanation !== undefined) dto.varianceExplanation = item.varianceExplanation
      return dto
    })
  },
}

// ============================================================
// API 实现
// ============================================================

/** 盘点查询参数（传给后端） */
interface InventoryCheckQueryParams {
  current?: number
  size?: number
  warehouseId?: number
  checkStatus?: number
  checkCode?: string
  startDate?: string
  endDate?: string
  [key: string]: unknown
}

export const inventoryCheckApi = {
  /**
   * 创建盘点单
   * @param data 创建表单
   */
  async create(data: InventoryCheckCreateForm): Promise<InventoryCheckInfo> {
    const dto = inventoryCheckApiConverter.toCreateDTO(data)
    const res = await post<InventoryCheckBackend>('/v1/inventory-checks', dto)
    return inventoryCheckApiConverter.toFrontend(res)
  },

  /**
   * 查询盘点单列表
   * @param params 查询参数
   */
  async getList(params?: InventoryCheckQueryForm): Promise<InventoryCheckInfo[]> {
    const query: InventoryCheckQueryParams = {
      current: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    if (params?.warehouseId) query.warehouseId = Number(params.warehouseId)
    if (params?.checkStatus) query.checkStatus = inventoryCheckConverter.toBackendStatus(params.checkStatus)
    if (params?.checkCode) query.checkCode = params.checkCode
    if (params?.startDate) query.startDate = params.startDate
    if (params?.endDate) query.endDate = params.endDate

    const res = await get<InventoryCheckPageBackend | InventoryCheckBackend[] | null>(
      '/v1/inventory-checks',
      query,
    )
    // 兼容两种响应格式：分页对象（含 records）或裸数组
    if (Array.isArray(res)) {
      return inventoryCheckApiConverter.toFrontendList(res)
    }
    return inventoryCheckApiConverter.toFrontendList(res?.records)
  },

  /**
   * 获取盘点单详情（含明细）
   * @param checkId 盘点单ID
   */
  async getById(checkId: string): Promise<InventoryCheckInfo> {
    const res = await get<InventoryCheckBackend>(`/v1/inventory-checks/${checkId}`)
    return inventoryCheckApiConverter.toFrontend(res)
  },

  /**
   * 提交盘点结果
   * @param checkId 盘点单ID
   * @param items 盘点明细列表
   */
  async submitCheckItems(checkId: string, items: InventoryCheckItemForm[]): Promise<InventoryCheckInfo> {
    const dto = { items: inventoryCheckApiConverter.toItemSubmitDTO(items) }
    const res = await put<InventoryCheckBackend>(`/v1/inventory-checks/${checkId}/items`, dto)
    return inventoryCheckApiConverter.toFrontend(res)
  },

  /**
   * 审核盘点单
   * @param checkId 盘点单ID
   * @param approved 是否审核通过
   * @param remark 审核备注（可选）
   */
  async approve(checkId: string, approved: boolean, remark?: string): Promise<InventoryCheckInfo> {
    const dto: Record<string, unknown> = { approved }
    if (remark !== undefined) dto.remark = remark
    const res = await post<InventoryCheckBackend>(`/v1/inventory-checks/${checkId}/approve`, dto)
    return inventoryCheckApiConverter.toFrontend(res)
  },

  /**
   * 完成盘点
   * @param checkId 盘点单ID
   */
  async complete(checkId: string): Promise<InventoryCheckInfo> {
    const res = await post<InventoryCheckBackend>(`/v1/inventory-checks/${checkId}/complete`)
    return inventoryCheckApiConverter.toFrontend(res)
  },
}

export default inventoryCheckApi
