/**
 * 健康证管理API
 * 对应后端: /v1/hr/health-certificate (HealthCertificateController)
 *
 * 对接策略：
 * - 所有方法对接真实后端 HealthCertificateController，已移除全部 Mock 数据
 * - 后端返回 Page<T> 结构（MyBatis Plus IPage：records/total/current/size）
 * - 后端 create/update/delete/状态变更 返回 Result<String>（成功消息），非实体对象
 * - 前端审批流程通过后端 PUT /update-status/{id}?status= 接口实现
 *
 * TODO（后端待补充）：
 * - GET /list 暂不支持 keyword 模糊搜索（员工姓名/证件编号），前端搜索框暂不生效
 * - 报销查询暂不支持分页/状态/日期范围筛选，仅支持按 healthCertificateId 查询
 */
import { get, post, put, del } from '../request'
import type {
  HealthCertificate,
  HealthCertificateExpense,
  HealthCertificateApprovalRequest,
  HealthCertificateExpenseRequest,
  ExpenseQueryParams,
  HealthCertificateStatistics,
} from '../../types/healthCertificate'

/** 后端健康证接口基础路径（单数，对应 HealthCertificateController @RequestMapping） */
const HC_BASE = '/v1/hr/health-certificate'

/** 分页查询参数 */
export interface HealthCertificateListParams {
  /** 页码（从1开始） */
  page?: number
  /** 每页条数（前端字段名，API 内部映射为后端 size 参数） */
  pageSize?: number
  /** 关键字（员工姓名/证件编号）- TODO: 后端 /list 暂不支持，需后端补充 */
  keyword?: string
  /** 状态筛选（valid/expiring/expired） */
  status?: string
  /** 门店/部门筛选 */
  store?: string
}

/** 分页查询结果（对应后端 Page<HealthCertificate> 结构） */
export interface HealthCertificatePageResult {
  records: HealthCertificate[]
  total: number
  current: number
  size: number
}

export const healthCertificateApi = {
  /**
   * 分页查询健康证列表
   * GET /v1/hr/health-certificate/list?page&size&store&status
   * @param params - 分页查询参数
   * @returns 分页结果（records + total）
   */
  async getList(params?: HealthCertificateListParams): Promise<{ records: HealthCertificate[]; total: number }> {
    const res = await get<HealthCertificatePageResult>(`${HC_BASE}/list`, {
      page: params?.page ?? 1,
      size: params?.pageSize ?? 10,
      store: params?.store,
      status: params?.status,
    })
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
    }
  },

  /**
   * 根据ID获取健康证详情
   * GET /v1/hr/health-certificate/{id}
   */
  async getById(id: string): Promise<HealthCertificate> {
    return get<HealthCertificate>(`${HC_BASE}/${id}`)
  },

  /**
   * 根据员工ID获取健康证
   * GET /v1/hr/health-certificate/employee/{employeeId}
   */
  async getByEmployeeId(employeeId: string): Promise<HealthCertificate> {
    return get<HealthCertificate>(`${HC_BASE}/employee/${employeeId}`)
  },

  /**
   * 根据门店获取健康证列表
   * GET /v1/hr/health-certificate/store/{store}
   */
  async getByStore(store: string): Promise<HealthCertificate[]> {
    return get<HealthCertificate[]>(`${HC_BASE}/store/${store}`)
  },

  /**
   * 获取所有健康证（不分页）
   * GET /v1/hr/health-certificate/all
   */
  async getAll(): Promise<HealthCertificate[]> {
    return get<HealthCertificate[]>(`${HC_BASE}/all`)
  },

  /**
   * 创建健康证
   * POST /v1/hr/health-certificate/add
   * 后端返回 Result<String>（成功消息），这里回传输入数据用于前端 UI 反馈
   */
  async create(data: Partial<HealthCertificate>): Promise<HealthCertificate> {
    await post<string>(`${HC_BASE}/add`, data)
    return { ...data, id: data.id ?? '' } as HealthCertificate
  },

  /**
   * 更新健康证
   * PUT /v1/hr/health-certificate/update
   * 后端使用 RequestBody 接收完整实体，ID 从 body 中读取
   */
  async update(id: string, data: Partial<HealthCertificate>): Promise<HealthCertificate> {
    await put<string>(`${HC_BASE}/update`, { ...data, id })
    return { ...data, id } as HealthCertificate
  },

  /**
   * 删除健康证
   * DELETE /v1/hr/health-certificate/{id}
   */
  async delete(id: string): Promise<void> {
    await del<string>(`${HC_BASE}/${id}`)
  },

  /**
   * 批量删除健康证
   * DELETE /v1/hr/health-certificate/batch
   * 后端使用 RequestBody 接收 ID 列表（List<String>）
   */
  async batchDelete(ids: string[]): Promise<void> {
    await del<string>(`${HC_BASE}/batch`, undefined, { data: ids })
  },

  /**
   * 更新健康证状态
   * PUT /v1/hr/health-certificate/update-status/{id}?status=
   */
  async updateStatus(id: string, status: string): Promise<void> {
    await put<string>(`${HC_BASE}/update-status/${id}`, undefined, { params: { status } })
  },

  /**
   * 批量更新健康证状态
   * PUT /v1/hr/health-certificate/batch-update-status?status=
   * 后端使用 RequestBody 接收 ID 列表（List<String>），status 为 URL 参数
   */
  async batchUpdateStatus(ids: string[], status: string): Promise<void> {
    await put<string>(`${HC_BASE}/batch-update-status`, ids, { params: { status } })
  },

  /**
   * 计算并更新所有健康证的剩余天数和状态
   * POST /v1/hr/health-certificate/calculate-status
   */
  async calculateAndUpdateStatus(): Promise<void> {
    await post<string>(`${HC_BASE}/calculate-status`)
  },

  /**
   * 提交审批
   * 后端无独立审批流程接口，通过 PUT /update-status/{id}?status=pending 实现
   * TODO: 后端未提供独立的审批流（审批人/审批意见/审批历史），此处仅更新状态
   */
  async submitForApproval(id: string): Promise<void> {
    await put<string>(`${HC_BASE}/update-status/${id}`, undefined, { params: { status: 'pending' } })
  },

  /**
   * 审批通过
   * 后端无独立审批接口，通过 PUT /update-status/{id}?status=approved 实现
   * TODO: 后端未持久化 approvalBy/approvalDate/comment，需后端补充
   */
  async approve(_id: string, _data: HealthCertificateApprovalRequest): Promise<void> {
    await put<string>(`${HC_BASE}/update-status/${_id}`, undefined, { params: { status: 'approved' } })
  },

  /**
   * 审批驳回
   * 后端无独立审批接口，通过 PUT /update-status/{id}?status=rejected 实现
   * TODO: 后端未持久化 rejectReason，需后端补充
   */
  async reject(_id: string, _data: HealthCertificateApprovalRequest): Promise<void> {
    await put<string>(`${HC_BASE}/update-status/${_id}`, undefined, { params: { status: 'rejected' } })
  },

  /**
   * 提交报销申请
   * POST /v1/hr/health-certificate/expense/add
   * 后端返回 Result<String>（成功消息），这里回传输入数据用于前端 UI 反馈
   */
  async submitExpense(data: HealthCertificateExpenseRequest): Promise<HealthCertificateExpense> {
    await post<string>(`${HC_BASE}/expense/add`, data)
    return {
      id: '',
      healthCertificateId: data.healthCertificateId,
      employeeId: '',
      employeeName: '',
      amount: data.amount,
      invoiceType: data.invoiceType,
      invoiceNumber: data.invoiceNumber,
      invoiceDate: data.invoiceDate,
      invoiceAttachments: data.invoiceAttachments,
      reason: data.reason,
      note: data.note,
      status: 'pending',
      applyDate: new Date().toISOString(),
      approvalHistory: [],
      expenseType: data.expenseType,
    }
  },

  /**
   * 获取健康证报销记录列表
   * GET /v1/hr/health-certificate/expense/{healthCertificateId}
   * 后端返回 List<HealthCertificateExpense>
   * TODO: 后端仅支持按 healthCertificateId 查询，不支持分页/状态/日期范围筛选
   */
  async getExpenseList(params?: ExpenseQueryParams): Promise<{ records: HealthCertificateExpense[]; total: number }> {
    if (!params?.healthCertificateId) {
      return { records: [], total: 0 }
    }
    const records = await get<HealthCertificateExpense[]>(`${HC_BASE}/expense/${params.healthCertificateId}`)
    return {
      records: records ?? [],
      total: records?.length ?? 0,
    }
  },

  /**
   * 发送到期提醒
   * POST /v1/hr/health-certificate/send-reminders?days=30
   * @param days - 提前天数（默认30天）
   */
  async sendExpiryReminders(days = 30): Promise<void> {
    await post<string>(`${HC_BASE}/send-reminders`, undefined, { params: { days } })
  },

  /**
   * 获取健康证全局统计
   * GET /v1/hr/health-certificate/statistics
   * 后端返回 Map<String, Object>，含 total/valid/expiring/expired
   * 统计数据不受列表筛选条件影响，反映全量健康证状态
   */
  async getStatistics(): Promise<HealthCertificateStatistics> {
    const res = await get<Record<string, number>>(`${HC_BASE}/statistics`)
    return {
      total: res?.total || 0,
      valid: res?.valid || 0,
      expiring: res?.expiring || 0,
      expired: res?.expired || 0,
    }
  },
}
