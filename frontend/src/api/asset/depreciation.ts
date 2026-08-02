/**
 * 资产折旧管理API
 * 对应后端: /v1/asset/depreciation (AssetDepreciationController)
 *
 * 后端接口：
 * - GET /v1/asset/depreciation/page          分页查询折旧记录
 * - GET /v1/asset/depreciation/{assetId}/records  查询资产折旧记录
 * - POST /v1/asset/depreciation/execute      执行月度折旧计算（period参数）
 * - GET /v1/asset/depreciation/summary       折旧汇总统计
 */
import { get, post } from '../request'
import type { RequestOptions } from '../request'
import type {
  DepreciationRecord,
  DepreciationAdjustDTO,
  DepreciationBatchDTO,
  PageResponse,
} from '../../types/asset'
import { DepreciationMethod } from '../../types/asset'

// ============================================================
// 后端字段类型定义（AssetDepreciationRecord）
// ============================================================

/** 后端折旧记录实体 */
interface DepreciationRecordBackend {
  recordId?: number
  assetId?: number
  period?: string
  originalCost?: number
  thisPeriodDepreciation?: number
  accumulatedDepreciation?: number
  netBookValueAfter?: number
  createTime?: string
}

/** 后端分页响应 */
interface DepreciationPageBackend {
  records: DepreciationRecordBackend[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
}

// ============================================================
// 数据转换
// ============================================================

/**
 * 后端折旧记录 → 前端 DepreciationRecord 转换
 */
function toDepreciationRecord(vo: DepreciationRecordBackend): DepreciationRecord {
  return {
    id: String(vo.recordId ?? ''),
    assetId: vo.assetId != null ? String(vo.assetId) : '',
    assetCode: '',
    assetName: '',
    period: vo.period || '',
    depreciationAmount: vo.thisPeriodDepreciation ?? 0,
    accumulatedDepreciation: vo.accumulatedDepreciation ?? 0,
    netValue: vo.netBookValueAfter ?? 0,
    method: DepreciationMethod.STRAIGHT_LINE,
    isManualAdjustment: false,
    calculatedAt: vo.createTime || '',
    operatorId: 'system',
    operatorName: '系统自动',
  }
}

// ============================================================
// API 实现
// ============================================================

export const depreciationApi = {
  /**
   * 获取折旧记录列表
   * 后端路径：GET /v1/asset/depreciation/page
   */
  async getList(params?: { period?: string; assetId?: string }): Promise<PageResponse<DepreciationRecord>> {
    const query: Record<string, unknown> = {
      page: 1,
      size: 1000,
    }
    if (params?.period) {
      query.period = params.period
    }
    if (params?.assetId) {
      query.assetId = params.assetId
    }

    const res = await get<DepreciationPageBackend | null>('/v1/asset/depreciation/page', query)
    const records = (res?.records || []).map(toDepreciationRecord)
    return {
      records,
      total: res?.total ?? records.length,
      current: res?.current ?? 1,
      size: res?.size ?? 1000,
      pages: res?.pages ?? 1,
    }
  },

  /**
   * 根据ID获取折旧记录
   * 后端无单条记录查询接口，通过列表查询后过滤
   * 后端路径：GET /v1/asset/depreciation/page（前端过滤 recordId）
   */
  async getById(id: string): Promise<DepreciationRecord | null> {
    try {
      const res = await get<DepreciationPageBackend | null>('/v1/asset/depreciation/page', {
        page: 1,
        size: 1000,
      })
      const records = (res?.records || []).map(toDepreciationRecord)
      return records.find(r => r.id === id) || null
    } catch {
      return null
    }
  },

  /**
   * 创建折旧记录
   * 后端无直接创建接口，通过执行月度折旧计算实现
   * 后端路径：POST /v1/asset/depreciation/execute
   */
  async create(data: Partial<DepreciationRecord>): Promise<DepreciationRecord> {
    const period = data.period || new Date().toISOString().slice(0, 7)
    await post<number>('/v1/asset/depreciation/execute', undefined, {
      params: { period },
    } as Partial<RequestOptions>)
    return {
      id: '',
      assetId: data.assetId || '',
      assetCode: data.assetCode,
      assetName: data.assetName,
      period,
      depreciationAmount: data.depreciationAmount || 0,
      accumulatedDepreciation: data.accumulatedDepreciation || 0,
      netValue: data.netValue || 0,
      method: data.method || DepreciationMethod.STRAIGHT_LINE,
      isManualAdjustment: false,
      calculatedAt: new Date().toISOString(),
      operatorId: 'system',
      operatorName: '系统自动',
    }
  },

  /**
   * 更新折旧记录
   * 后端无对应接口，抛出异常提示
   */
  async update(_id: string, _data: Partial<DepreciationRecord>): Promise<DepreciationRecord | null> {
    throw new Error('后端暂不支持直接更新折旧记录')
  },

  /**
   * 删除折旧记录
   * 后端无对应接口，抛出异常提示
   */
  async delete(_id: string): Promise<boolean> {
    throw new Error('后端暂不支持删除折旧记录')
  },

  /**
   * 计算单项资产折旧
   * 后端无单资产折旧接口，通过执行月度折旧实现
   * 后端路径：POST /v1/asset/depreciation/execute
   */
  async calculate(assetId: string, period: string): Promise<DepreciationRecord> {
    await post<number>('/v1/asset/depreciation/execute', undefined, {
      params: { period },
    } as Partial<RequestOptions>)
    return {
      id: '',
      assetId,
      period,
      depreciationAmount: 0,
      accumulatedDepreciation: 0,
      netValue: 0,
      method: DepreciationMethod.STRAIGHT_LINE,
      isManualAdjustment: false,
      calculatedAt: new Date().toISOString(),
      operatorId: 'system',
      operatorName: '系统自动',
    }
  },

  /**
   * 批量计提折旧
   * 后端路径：POST /v1/asset/depreciation/execute
   * 后端返回折旧的资产数量（Integer），此处返回空数组
   */
  async batchCalculate(data: DepreciationBatchDTO): Promise<DepreciationRecord[]> {
    await post<number>('/v1/asset/depreciation/execute', undefined, {
      params: { period: data.period },
    } as Partial<RequestOptions>)
    // 批量计提后重新查询该期间的记录返回
    const res = await depreciationApi.getList({ period: data.period })
    return res.records
  },

  /**
   * 手动调整折旧
   * 后端无对应接口，抛出异常提示
   */
  async adjust(_data: DepreciationAdjustDTO): Promise<DepreciationRecord> {
    throw new Error('后端暂不支持手动调整折旧，请使用批量计提功能')
  },
}
