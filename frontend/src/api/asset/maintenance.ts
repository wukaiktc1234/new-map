/**
 * 资产维修管理API
 * 对应后端: /v1/asset/maintenance (AssetMaintenanceController)
 *
 * 后端接口：
 * - GET /v1/asset/maintenance/page          分页查询维修记录（flow_type=4）
 * - GET /v1/asset/maintenance/{id}          获取维修详情
 * - POST /v1/asset/maintenance/{assetId}/start    开始维修
 * - POST /v1/asset/maintenance/{assetId}/complete 完成维修
 * - GET /v1/asset/maintenance/{assetId}/records   查询资产维护历史
 *
 * 后端维修记录来源于 AssetFlowRecord（flow_type=4 维修）
 */
import { get, post } from '../request'
import type { RequestOptions } from '../request'
import type {
  MaintenanceRecord,
  MaintenanceCreateDTO,
  MaintenanceUpdateDTO,
  MaintenanceStatus,
  PageResponse,
} from '../../types/asset'

// ============================================================
// 后端字段类型定义（AssetFlowRecordNew）
// ============================================================

/** 后端资产变动记录实体（维修类型 flow_type=4） */
interface FlowRecordBackend {
  flowId?: number
  assetId?: number
  flowType?: number
  beforeValue?: number
  afterValue?: number
  changeAmount?: number
  referenceNo?: string
  flowDate?: string
  operatorId?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 后端分页响应 */
interface FlowPageBackend {
  records: FlowRecordBackend[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
}

// ============================================================
// 数据转换
// ============================================================

/**
 * 后端变动记录 → 前端 MaintenanceRecord 转换
 * AssetFlowRecord 是通用变动记录，维修记录为 flow_type=4
 * 根据 beforeValue/afterValue 推断维修状态
 */
function toMaintenanceRecord(vo: FlowRecordBackend): MaintenanceRecord {
  // 根据变动前后值推断状态：有 afterValue 视为已完成，否则视为进行中
  const status: MaintenanceStatus = vo.afterValue != null ? 'completed' : 'in_progress'
  return {
    id: String(vo.flowId ?? ''),
    assetId: vo.assetId != null ? String(vo.assetId) : '',
    assetCode: '',
    assetName: '',
    faultDescription: vo.remark || '',
    repairType: 'internal',
    repairCost: vo.changeAmount ?? 0,
    startTime: vo.flowDate || vo.createTime || '',
    completedTime: status === 'completed' ? (vo.flowDate || undefined) : undefined,
    status,
    resultDescription: vo.remark,
    repairmanId: vo.operatorId != null ? String(vo.operatorId) : undefined,
    repairmanName: '',
    createTime: vo.createTime || '',
  }
}

// ============================================================
// API 实现
// ============================================================

export const maintenanceApi = {
  /**
   * 获取维修记录列表
   * 后端路径：GET /v1/asset/maintenance/page
   */
  async getList(params?: { assetId?: string; status?: MaintenanceStatus }): Promise<PageResponse<MaintenanceRecord>> {
    const query: Record<string, unknown> = {
      page: 1,
      size: 1000,
    }
    if (params?.assetId) {
      query.assetId = params.assetId
    }

    const res = await get<FlowPageBackend | null>('/v1/asset/maintenance/page', query)
    let records = (res?.records || []).map(toMaintenanceRecord)

    // 后端不支持按 status 筛选，前端过滤
    if (params?.status) {
      records = records.filter(r => r.status === params.status)
    }

    return {
      records,
      total: res?.total ?? records.length,
      current: res?.current ?? 1,
      size: res?.size ?? 1000,
      pages: res?.pages ?? 1,
    }
  },

  /**
   * 根据ID获取维修记录
   * 后端路径：GET /v1/asset/maintenance/{id}
   */
  async getById(id: string): Promise<MaintenanceRecord | null> {
    try {
      const res = await get<FlowRecordBackend>(`/v1/asset/maintenance/${id}`)
      return res ? toMaintenanceRecord(res) : null
    } catch {
      return null
    }
  },

  /**
   * 创建维修记录（开始维修）
   * 后端路径：POST /v1/asset/maintenance/{assetId}/start
   * 后端开始维修返回 void，此处构造前端对象返回
   */
  async create(data: MaintenanceCreateDTO): Promise<MaintenanceRecord> {
    const operatorName = data.vendorId || ''
    await post<void>(`/v1/asset/maintenance/${data.assetId}/start`, undefined, {
      params: { operatorName },
    } as Partial<RequestOptions>)
    return {
      id: '',
      assetId: data.assetId,
      faultDescription: data.faultDescription,
      repairType: data.repairType,
      vendorId: data.vendorId,
      repairCost: data.estimatedCost || 0,
      startTime: new Date().toISOString(),
      status: 'in_progress',
      createTime: new Date().toISOString(),
    }
  },

  /**
   * 更新维修记录
   * 后端无直接更新接口，抛出异常提示
   */
  async update(_id: string, _data: MaintenanceUpdateDTO): Promise<MaintenanceRecord | null> {
    throw new Error('后端暂不支持直接更新维修记录')
  },

  /**
   * 删除维修记录
   * 后端无对应接口，抛出异常提示
   */
  async delete(_id: string): Promise<boolean> {
    throw new Error('后端暂不支持删除维修记录')
  },

  /**
   * 开始维修
   * 后端路径：POST /v1/asset/maintenance/{assetId}/start
   * 注意：前端传入的是维修记录ID，需先获取记录得到 assetId
   */
  async start(id: string): Promise<MaintenanceRecord | null> {
    const record = await maintenanceApi.getById(id)
    if (!record) return null
    await post<void>(`/v1/asset/maintenance/${record.assetId}/start`, undefined, {
      params: { operatorName: record.repairmanName || '' },
    } as Partial<RequestOptions>)
    return { ...record, status: 'in_progress' }
  },

  /**
   * 完成维修
   * 后端路径：POST /v1/asset/maintenance/{assetId}/complete
   * 注意：前端传入的是维修记录ID，需先获取记录得到 assetId
   */
  async complete(id: string, data: { resultDescription?: string; repairCost?: number }): Promise<MaintenanceRecord | null> {
    const record = await maintenanceApi.getById(id)
    if (!record) return null
    await post<void>(`/v1/asset/maintenance/${record.assetId}/complete`, undefined, {
      params: { operatorName: record.repairmanName || '' },
    } as Partial<RequestOptions>)
    return {
      ...record,
      status: 'completed',
      completedTime: new Date().toISOString(),
      resultDescription: data.resultDescription,
      repairCost: data.repairCost ?? record.repairCost,
    }
  },
}
