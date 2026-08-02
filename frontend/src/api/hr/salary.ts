/**
 * 薪资管理API
 * 对应后端: /v1/salary (SalaryController) + /v1/salary/batches (SalaryBatchController)
 *
 * 两级视图：
 * - 主表：账期（按自然月汇总）→ SalaryBatch
 * - 从表：账期下的个人薪资明细 → SalaryRecordDetail
 *
 * 数据转换说明（重要）：
 * - 后端 SalaryRecord 金额字段使用 BigDecimal（单位：元），前端 SalaryRecord 金额字段使用 number（单位：分）
 * - 后端字段名：salaryMonth/allowance/totalEarnings/insurance/otherDeductions/finalSalary
 *   前端字段名：period/subsidy/grossSalary/socialInsurance/deductions/netSalary
 * - 后端状态使用中文字符串（"待确认"/"已确认"/"已发放"），前端使用 SalaryStatus 枚举（PENDING/APPROVED/PAID）
 * - 以上转换在 API 边界处完成（normalizeSalaryRecord / toBackendPayload）
 *
 * TODO（后端待补充）：
 * - GET /records 暂不支持 departmentId/status 筛选（参数已传但后端未应用过滤）
 * - GET /salary/statistics 不存在，前端 getStatistics 客户端计算
 * - POST /salary/{id}/approve 不存在（仅有 /confirm/{id}），reject 无对应接口
 * - POST /salary/{id}/pay 不存在
 * - 财务同步相关接口（getFinanceSyncData/markFinanceSynced）不存在
 * - 后端 SalaryRecord 金额使用 BigDecimal 元，违反规范"金额统一用 Long 分"（P0-6 相关）
 */
import { get, post, put, del } from '../request'
import type {
  SalaryRecord,
  SalaryStatisticsVO,
  SalaryQueryDTO,
  SalaryApprovalDTO,
  SalaryPaymentDTO,
  SalaryBatchPaymentDTO,
  SalaryFinanceSyncData,
  SalaryBatch,
  SalaryBatchQueryDTO,
  SalaryBatchCreateDTO,
  SalaryBatchApprovalDTO,
  SalaryBatchPayDTO,
  SalaryBatchStatisticsVO,
  SalaryRecordDetail,
  SalaryBatchDataSource,
} from '../../types/hr/salary'
import { SalaryStatus } from '../../types/hr/salary'
// 金额转换统一委托给 utils/money：
// - yuanToFen：后端 BigDecimal 元 → 前端 number 分（标准签名）
// - fenToYuanNumber（别名 fenToYuan）：前端 number 分 → 后端 number 元（保留原变量名以减少调用点改动）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

/** 后端薪资接口基础路径 */
const SALARY_BASE = '/v1/salary'

// ============================================================
// 数据转换工具（元 ↔ 分、字段映射、状态映射）
// 金额转换函数 yuanToFen / fenToYuan 统一来自 utils/money（见文件顶部 import）
// ============================================================

/** 后端状态（中文）→ 前端 SalaryStatus 枚举 */
function mapBackendStatus(status: string | undefined | null): SalaryStatus {
  if (!status) return SalaryStatus.PENDING
  const map: Record<string, SalaryStatus> = {
    '待确认': SalaryStatus.PENDING,
    '已确认': SalaryStatus.APPROVED,
    '已发放': SalaryStatus.PAID,
    '草稿': SalaryStatus.DRAFT,
    '已撤销': SalaryStatus.CANCELLED,
    // 兼容英文状态
    pending: SalaryStatus.PENDING,
    approved: SalaryStatus.APPROVED,
    paid: SalaryStatus.PAID,
    draft: SalaryStatus.DRAFT,
    cancelled: SalaryStatus.CANCELLED,
  }
  return map[status] || SalaryStatus.PENDING
}

/** 前端 SalaryStatus 枚举 → 后端状态（中文） */
function mapFrontendStatus(status: SalaryStatus): string {
  const map: Record<SalaryStatus, string> = {
    [SalaryStatus.DRAFT]: '草稿',
    [SalaryStatus.PENDING]: '待确认',
    [SalaryStatus.APPROVED]: '已确认',
    [SalaryStatus.PAID]: '已发放',
    [SalaryStatus.CANCELLED]: '已撤销',
  }
  return map[status] || '待确认'
}

/** 后端 SalaryRecord 原始数据（字段名/单位与前端不同） */
type BackendSalaryRecord = Record<string, unknown>

/** 后端 SalaryRecord → 前端 SalaryRecord（字段映射 + 元转分 + 状态映射） */
function normalizeSalaryRecord(raw: BackendSalaryRecord | null | undefined): SalaryRecord {
  const r = raw || {}
  return {
    id: String(r.id ?? ''),
    employeeId: String(r.employeeId ?? ''),
    employeeName: String(r.employeeName ?? ''),
    employeeCode: String(r.employeeCode ?? r.employeeId ?? ''),
    departmentId: String(r.departmentId ?? ''),
    departmentName: String(r.department ?? r.departmentName ?? ''),
    positionName: String(r.position ?? r.positionName ?? ''),
    period: String(r.salaryMonth ?? r.period ?? ''),
    basicSalary: yuanToFen(r.basicSalary as number),
    performanceBonus: yuanToFen(r.performanceBonus as number),
    overtimePay: yuanToFen(r.overtimePay as number),
    subsidy: yuanToFen(r.allowance as number),
    deductions: yuanToFen(r.otherDeductions as number),
    socialInsurance: yuanToFen(r.insurance as number),
    tax: yuanToFen(r.tax as number),
    grossSalary: yuanToFen(r.totalEarnings as number),
    netSalary: yuanToFen(r.finalSalary as number),
    status: mapBackendStatus(r.status as string),
    paidAt: (r.paidAt as string | undefined) || undefined,
    paymentMethod: r.paymentMethod as SalaryRecord['paymentMethod'],
    bankTransactionNo: (r.bankTransactionNo as string | undefined) || undefined,
    voucherNo: (r.voucherNo as string | undefined) || undefined,
    financeSyncStatus: r.financeSyncStatus as SalaryRecord['financeSyncStatus'],
    financeSyncedAt: (r.financeSyncedAt as string | undefined) || undefined,
    remark: (r.remark as string | undefined) || undefined,
    createTime: String(r.createTime ?? r.create_time ?? ''),
    updateTime: String(r.updateTime ?? r.update_time ?? ''),
  }
}

/** 批量适配薪资记录列表 */
function normalizeSalaryRecordList(list: BackendSalaryRecord[] | null | undefined): SalaryRecord[] {
  return (list || []).map(item => normalizeSalaryRecord(item))
}

/** 前端 SalaryRecord → 后端 payload（字段映射 + 分转元 + 状态映射） */
function toBackendPayload(data: Partial<SalaryRecord>): Record<string, unknown> {
  const payload: Record<string, unknown> = {}
  if (data.employeeId !== undefined) payload.employeeId = data.employeeId
  if (data.employeeName !== undefined) payload.employeeName = data.employeeName
  if (data.departmentName !== undefined) payload.department = data.departmentName
  if (data.positionName !== undefined) payload.position = data.positionName
  if (data.period !== undefined) payload.salaryMonth = data.period
  if (data.basicSalary !== undefined) payload.basicSalary = fenToYuan(data.basicSalary)
  if (data.performanceBonus !== undefined) payload.performanceBonus = fenToYuan(data.performanceBonus)
  if (data.overtimePay !== undefined) payload.overtimePay = fenToYuan(data.overtimePay)
  if (data.subsidy !== undefined) payload.allowance = fenToYuan(data.subsidy)
  if (data.deductions !== undefined) payload.otherDeductions = fenToYuan(data.deductions)
  if (data.socialInsurance !== undefined) payload.insurance = fenToYuan(data.socialInsurance)
  if (data.tax !== undefined) payload.tax = fenToYuan(data.tax)
  if (data.grossSalary !== undefined) payload.totalEarnings = fenToYuan(data.grossSalary)
  if (data.netSalary !== undefined) payload.finalSalary = fenToYuan(data.netSalary)
  if (data.status !== undefined) payload.status = mapFrontendStatus(data.status)
  if (data.remark !== undefined) payload.remark = data.remark
  return payload
}

/** 后端 IPage 响应结构 */
interface BackendPageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/**
 * 映射分页查询参数：前端 page/pageSize → 后端 current/size
 * 同时映射 period → salaryMonth
 */
function mapQueryParams(params?: SalaryQueryDTO): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, period, ...rest } = params
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  if (period !== undefined) result.salaryMonth = period
  // 移除前端特有字段，后端不支持
  delete result.departmentId
  return result
}

/**
 * 映射分页查询参数：前端 page/pageSize → 后端 current/size（账期查询用）
 */
function mapBatchQueryParams(params?: object): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  return result
}

export const salaryApi = {
  /**
   * 分页查询薪资记录列表
   * GET /v1/salary/records?current&size&employeeName&salaryMonth
   * @param params - 查询参数（period → salaryMonth, pageSize → size）
   * TODO: 后端 /records 暂未应用 employeeName/salaryMonth 过滤（参数已传，后端需补充）
   */
  async getList(params?: SalaryQueryDTO): Promise<{ records: SalaryRecord[]; total: number; current: number; size: number; pages: number }> {
    const res = await get<BackendPageResult<BackendSalaryRecord>>(`${SALARY_BASE}/records`, mapQueryParams(params))
    return {
      records: normalizeSalaryRecordList(res?.records),
      total: res?.total ?? 0,
      current: res?.current ?? params?.page ?? 1,
      size: res?.size ?? params?.pageSize ?? 20,
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取薪资记录详情
   * GET /v1/salary/records/{id}
   */
  async getById(id: string): Promise<SalaryRecord | null> {
    const res = await get<BackendSalaryRecord>(`${SALARY_BASE}/records/${id}`)
    return normalizeSalaryRecord(res)
  },

  /**
   * 获取薪资统计
   * 后端无 /statistics 接口，客户端计算：拉取当月全部记录后汇总
   * @param period - 薪资期间（YYYY-MM），不传则统计全部
   * TODO: 后端补充 GET /v1/salary/statistics 接口
   */
  async getStatistics(period?: string): Promise<SalaryStatisticsVO> {
    const res = await get<BackendPageResult<BackendSalaryRecord>>(`${SALARY_BASE}/records`, {
      current: 1,
      size: 1000,
      salaryMonth: period,
    })
    const list = normalizeSalaryRecordList(res?.records)
    const paidList = list.filter(s => s.status === SalaryStatus.PAID)
    const pendingList = list.filter(s => s.status === SalaryStatus.APPROVED || s.status === SalaryStatus.PENDING)
    const totalAmount = list.reduce((sum, s) => sum + s.netSalary, 0)
    const paidAmount = paidList.reduce((sum, s) => sum + s.netSalary, 0)
    const pendingAmount = pendingList.reduce((sum, s) => sum + s.netSalary, 0)
    return {
      totalCount: list.length,
      paidCount: paidList.length,
      pendingCount: pendingList.length,
      totalAmount,
      paidAmount,
      pendingAmount,
      avgSalary: list.length > 0 ? Math.round(totalAmount / list.length) : 0,
    }
  },

  /**
   * 创建薪资记录
   * POST /v1/salary/records
   */
  async create(data: Partial<SalaryRecord>): Promise<SalaryRecord> {
    const res = await post<BackendSalaryRecord>(`${SALARY_BASE}/records`, toBackendPayload(data))
    return normalizeSalaryRecord(res)
  },

  /**
   * 更新薪资记录
   * PUT /v1/salary/records/{id}
   */
  async update(id: string, data: Partial<SalaryRecord>): Promise<SalaryRecord> {
    const res = await put<BackendSalaryRecord>(`${SALARY_BASE}/records/${id}`, toBackendPayload(data))
    return normalizeSalaryRecord(res)
  },

  /**
   * 删除薪资记录
   * DELETE /v1/salary/records/{id}
   */
  async delete(id: string): Promise<void> {
    await del<void>(`${SALARY_BASE}/records/${id}`)
  },

  /**
   * 审核薪资记录
   * 后端仅有 POST /v1/salary/confirm/{id}（确认，相当于 approve=true）
   * reject（approve=false）无对应后端接口
   * TODO: 后端补充 reject 接口
   */
  async approve(data: SalaryApprovalDTO): Promise<SalaryRecord | null> {
    if (data.approved) {
      const res = await post<BackendSalaryRecord>(`${SALARY_BASE}/confirm/${data.id}`)
      return normalizeSalaryRecord(res)
    }
    // 驳回：后端无对应接口
    // TODO: 后端补充 POST /v1/salary/reject/{id} 接口
    throw new Error('后端暂不支持薪资驳回操作（POST /v1/salary/reject/{id} 未实现）')
  },

  /**
   * 发放薪资
   * TODO: 后端未提供 POST /v1/salary/{id}/pay 接口
   */
  async pay(_data: SalaryPaymentDTO): Promise<SalaryRecord | null> {
    // TODO: 后端补充 POST /v1/salary/pay/{id} 接口
    throw new Error('后端暂不支持薪资发放操作（POST /v1/salary/pay/{id} 未实现）')
  },

  /**
   * 批量发放薪资
   * TODO: 后端未提供 POST /v1/salary/batch-pay 接口
   */
  async batchPay(_data: SalaryBatchPaymentDTO): Promise<{ successCount: number }> {
    // TODO: 后端补充 POST /v1/salary/batch-pay 接口
    throw new Error('后端暂不支持薪资批量发放操作（POST /v1/salary/batch-pay 未实现）')
  },

  /**
   * 获取财务同步数据
   * TODO: 后端未提供 GET /v1/salary/finance-sync 接口
   */
  async getFinanceSyncData(_ids: string[]): Promise<SalaryFinanceSyncData[]> {
    // TODO: 后端补充 GET /v1/salary/finance-sync 接口
    throw new Error('后端暂不支持获取财务同步数据（GET /v1/salary/finance-sync 未实现）')
  },

  /**
   * 标记财务同步完成
   * TODO: 后端未提供 POST /v1/salary/finance-synced 接口
   */
  async markFinanceSynced(_ids: string[]): Promise<boolean> {
    // TODO: 后端补充 POST /v1/salary/finance-synced 接口
    throw new Error('后端暂不支持标记财务同步完成（POST /v1/salary/finance-synced 未实现）')
  },
}

/* ============================================================
 * 账期级（月份总账）API - 两级视图主表
 * 对接后端 SalaryBatchController (/v1/salary/batches)
 *
 * 后端已实现端点：
 * - POST /generate 生成账期
 * - GET / 获取账期列表
 * - GET /{id} 获取账期详情
 * - POST /{id}/confirm 确认账期
 * - DELETE /{id} 删除账期
 *
 * TODO（后端待补充）：
 * - GET /{id}/records 账期下个人明细
 * - POST /{id}/approve 审核账期
 * - POST /{id}/pay 发放账期
 * - POST /{id}/cancel 撤销账期
 * - POST /{id}/sync 同步数据来源
 * - GET /statistics 账期统计
 * ============================================================ */

export const salaryBatchApi = {
  /**
   * 获取账期列表（月份总账）
   * GET /v1/salary/batches
   */
  async getList(params?: SalaryBatchQueryDTO): Promise<{ records: SalaryBatch[]; total: number }> {
    const res = await get<{ records: SalaryBatch[]; total: number } | SalaryBatch[]>('/v1/salary/batches', mapBatchQueryParams(params))
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: SalaryBatch[]; total: number }
  },

  /**
   * 获取账期详情
   * GET /v1/salary/batches/{id}
   */
  async getById(id: string): Promise<SalaryBatch> {
    return await get<SalaryBatch>(`/v1/salary/batches/${id}`)
  },

  /**
   * 获取账期下的个人薪资明细
   * TODO: 后端未提供 GET /v1/salary/batches/{id}/records 接口
   */
  async getBatchRecords(batchId: string, params?: { departmentId?: string; employeeName?: string; page?: number; pageSize?: number }): Promise<{ records: SalaryRecordDetail[]; total: number }> {
    // TODO: 后端补充 GET /v1/salary/batches/{batchId}/records 接口
    const res = await get<{ records: SalaryRecordDetail[]; total: number } | SalaryRecordDetail[]>(`/v1/salary/batches/${batchId}/records`, mapBatchQueryParams(params))
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: SalaryRecordDetail[]; total: number }
  },

  /**
   * 创建账期（自动联动数据来源）
   * POST /v1/salary/batches
   */
  async create(data: SalaryBatchCreateDTO): Promise<SalaryBatch> {
    return await post<SalaryBatch>('/v1/salary/batches', data)
  },

  /**
   * 审核账期
   * TODO: 后端未提供 POST /v1/salary/batches/{id}/approve 接口
   */
  async approve(data: SalaryBatchApprovalDTO): Promise<void> {
    // TODO: 后端补充 POST /v1/salary/batches/{id}/approve 接口
    await post(`/v1/salary/batches/${data.batchId}/approve`, { approved: data.approved, remark: data.remark })
  },

  /**
   * 发放账期
   * TODO: 后端未提供 POST /v1/salary/batches/{id}/pay 接口
   */
  async pay(data: SalaryBatchPayDTO): Promise<void> {
    // TODO: 后端补充 POST /v1/salary/batches/{id}/pay 接口
    await post(`/v1/salary/batches/${data.batchId}/pay`, { paymentMethod: data.paymentMethod, remark: data.remark })
  },

  /**
   * 撤销账期
   * TODO: 后端未提供 POST /v1/salary/batches/{id}/cancel 接口
   */
  async cancel(batchId: string): Promise<void> {
    // TODO: 后端补充 POST /v1/salary/batches/{id}/cancel 接口
    await post(`/v1/salary/batches/${batchId}/cancel`)
  },

  /**
   * 手动同步数据来源
   * TODO: 后端未提供 POST /v1/salary/batches/{id}/sync 接口
   */
  async syncData(batchId: string, sourceType: SalaryBatchDataSource['type']): Promise<SalaryBatchDataSource> {
    // TODO: 后端补充 POST /v1/salary/batches/{id}/sync 接口
    return await post<SalaryBatchDataSource>(`/v1/salary/batches/${batchId}/sync`, { sourceType })
  },

  /**
   * 获取账期统计
   * TODO: 后端未提供 GET /v1/salary/batches/statistics 接口
   */
  async getStatistics(): Promise<SalaryBatchStatisticsVO> {
    // TODO: 后端补充 GET /v1/salary/batches/statistics 接口
    return await get<SalaryBatchStatisticsVO>('/v1/salary/batches/statistics')
  },
}
