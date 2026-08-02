/**
 * 电子签章 API
 * 对接后端: /v1/seals
 *
 * 字段转换说明：
 * 后端 SealVO 使用 sealImageUrl，前端 SealInfo 使用 sealImage（Base64），
 * 在 API 边界处完成转换，遵循项目规范第十六章（DataConverter 模式）。
 * 枚举字段（sealType/status/authorizedScenes）后端使用 String，前端使用字面量联合类型，
 * 值域完全一致，通过类型断言转换。
 */
import { get, post, put, del } from '../request'
import type {
  SealInfo,
  SealFormData,
  SealQueryForm,
  SealUsageLog,
  SealUsageLogQueryForm,
  SealScene,
  SealStatus,
  SealType,
} from '@/types/seal'

// ============================================================
// 后端 VO 类型定义（与 SealController 返回结构对齐）
// ============================================================

/** 后端印章视图对象（对应 SealVO.java） */
interface SealVOBackend {
  sealId: string
  sealName: string
  sealType: string
  sealTypeName?: string
  /** 后端字段名为 sealImageUrl，实际存储 Base64/data URI */
  sealImageUrl?: string
  status: string
  statusName?: string
  keeper: string
  authorizedUsers?: string[]
  authorizedScenes?: string[]
  createBy?: string
  remark?: string
  /** ISO 8601 字符串或 LocalDateTime 序列化结果 */
  createTime?: string
  updateTime?: string
}

/** 后端印章使用记录（对应 SealUsageLog 实体） */
interface SealUsageLogBackend {
  logId: string
  sealId: string
  sealName?: string
  businessType: string
  businessId?: string
  businessNo?: string
  operator?: string
  operateTime?: string
  ipAddress?: string
  remark?: string
}

/** 后端分页结果（对应 PageResult.java） */
interface PageResultBackend<T> {
  total: number
  records: T[]
  current?: number
  size?: number
  page?: number
  pageSize?: number
}

// ============================================================
// 数据转换器（SealVO → SealInfo）
// ============================================================

/**
 * 后端 SealVO 转前端 SealInfo
 * 关键转换：sealImageUrl → sealImage
 */
function toSealInfo(vo: SealVOBackend): SealInfo {
  return {
    sealId: vo.sealId,
    sealName: vo.sealName,
    sealType: vo.sealType as SealType,
    sealImage: vo.sealImageUrl ?? '',
    status: vo.status as SealStatus,
    keeper: vo.keeper,
    authorizedUsers: vo.authorizedUsers ?? [],
    authorizedScenes: (vo.authorizedScenes ?? []) as SealScene[],
    createTime: vo.createTime ?? '',
    updateTime: vo.updateTime ?? '',
    createBy: vo.createBy ?? '',
    remark: vo.remark,
  }
}

/** 后端 SealUsageLog 转前端 SealUsageLog */
function toSealUsageLog(log: SealUsageLogBackend): SealUsageLog {
  return {
    logId: log.logId,
    sealId: log.sealId,
    sealName: log.sealName ?? '',
    businessType: log.businessType as SealScene,
    businessId: log.businessId ?? '',
    businessNo: log.businessNo ?? '',
    operator: log.operator ?? '',
    operateTime: log.operateTime ?? '',
    ipAddress: log.ipAddress ?? '',
    remark: log.remark,
  }
}

// ============================================================
// API 方法
// ============================================================

export const sealApi = {
  /**
   * 查询印章列表
   * 对接后端: GET /v1/seals/page
   */
  async getList(params?: SealQueryForm): Promise<{ records: SealInfo[]; total: number }> {
    const res = await get<PageResultBackend<SealVOBackend>>('/v1/seals/page', params as Record<string, unknown>)
    return {
      records: (res?.records ?? []).map(toSealInfo),
      total: res?.total ?? 0,
    }
  },

  /**
   * 获取印章详情
   * 对接后端: GET /v1/seals/{id}
   */
  async getById(id: string): Promise<SealInfo | null> {
    const res = await get<SealVOBackend>(`/v1/seals/${id}`)
    return res ? toSealInfo(res) : null
  },

  /**
   * 新增印章
   * 对接后端: POST /v1/seals
   */
  async create(data: SealFormData): Promise<SealInfo> {
    const res = await post<SealVOBackend>('/v1/seals', data)
    return toSealInfo(res)
  },

  /**
   * 更新印章
   * 对接后端: PUT /v1/seals/{id}
   */
  async update(id: string, data: Partial<SealFormData>): Promise<SealInfo> {
    const res = await put<SealVOBackend>(`/v1/seals/${id}`, data)
    return toSealInfo(res)
  },

  /**
   * 作废印章（逻辑删除）
   * 对接后端: DELETE /v1/seals/{id}
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/seals/${id}`)
  },

  /**
   * 切换印章状态（启用/停用）
   * 对接后端: PUT /v1/seals/{id}/status
   */
  async updateStatus(id: string, status: SealStatus): Promise<SealInfo> {
    const res = await put<SealVOBackend>(`/v1/seals/${id}/status`, { status })
    return toSealInfo(res)
  },

  /**
   * 获取当前用户已授权的印章（用于签署时选择）
   * 对接后端: GET /v1/seals/authorized
   */
  async getAuthorized(scene: SealScene, userId?: string): Promise<SealInfo[]> {
    const params: Record<string, unknown> = { scene }
    if (userId) params.userId = userId
    const res = await get<SealVOBackend[]>('/v1/seals/authorized', params)
    return (res ?? []).map(toSealInfo)
  },

  /**
   * 获取印章使用记录
   * 对接后端: GET /v1/seals/usage-logs
   */
  async getUsageLogs(params: SealUsageLogQueryForm): Promise<{ records: SealUsageLog[]; total: number }> {
    const res = await get<PageResultBackend<SealUsageLogBackend>>('/v1/seals/usage-logs', params as Record<string, unknown>)
    return {
      records: (res?.records ?? []).map(toSealUsageLog),
      total: res?.total ?? 0,
    }
  },

  /**
   * 记录印章使用（签署时调用）
   * 对接后端: POST /v1/seals/usage-logs
   * @param sealId 印章ID
   * @param businessType 业务类型（hr_contract/purchase_contract/electronic_contract）
   * @param businessId 业务ID（合同ID）
   * @param businessNo 业务编号（合同编号）
   * @param operator 操作人
   */
  async recordUsage(
    sealId: string,
    businessType: SealScene,
    businessId: string,
    businessNo: string,
    operator: string,
  ): Promise<SealUsageLog> {
    const res = await post<SealUsageLogBackend>('/v1/seals/usage-logs', {
      sealId,
      businessType,
      businessId,
      businessNo,
      operator,
    })
    return toSealUsageLog(res)
  },
}

export default sealApi
