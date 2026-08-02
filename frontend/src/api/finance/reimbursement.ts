/**
 * 发票报销API
 * 对应后端: /v1/finance/invoice-reimbursements
 *
 * 【设计说明】
 * - 后端 InvoiceReimbursementController 9 个端点：
 *   1. POST /                  创建报销申请（申请人信息由后端从登录态注入）
 *   2. GET /                   分页查询报销单列表
 *   3. GET /{id}               查询报销单详情（含明细+审批记录）
 *   4. PUT /{id}               更新报销单（仅草稿状态）
 *   5. PUT /{id}/approve       审批报销申请（审批人信息由后端注入）
 *   6. PUT /{id}/cancel        取消报销申请（query param: cancelReason）
 *   7. PUT /{id}/pay           标记报销单已付款
 *   8. GET /stats              获取报销统计数据
 *   9. GET /budget-info        获取预算信息
 * - 状态机：草稿(0) → 已审批(1) → 已付款(2)；草稿(0) → 已拒绝(4)；草稿(0)/已审批(1) → 已取消(3)
 *
 * 【改造说明】
 * - 内置 ReimbursementDataConverter 处理：
 *   - 报销状态数字↔字符串（后端 0~4 ↔ 前端 'draft'/'approved'/'paid'/'cancelled'/'rejected'）
 *   - 金额字段 totalAmount/approvedAmount 分↔元
 *   - 明细金额 amount/approvedAmount 分↔元
 */
import { get, post, put } from '../request'
import { ReimbursementDataConverter, ReimbursementStatusMap, fenToYuanNumber, yuanToFen } from './converters'
import type {
  InvoiceReimbursementVO,
  ReimbursementFormData,
  ReimbursementQueryForm,
  ReimbursementApproveForm,
  ReimbursementPayForm,
  ReimbursementStats,
  PageResponse,
} from '@/types/finance'

/** 后端 IPage 分页响应 */
interface ReimbursementPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 status 数字
 */
function mapQueryParams(params?: ReimbursementQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = ReimbursementStatusMap.toBackend[status]
  }
  return result
}

export const reimbursementApi = {
  /**
   * 创建报销申请
   * 申请人信息（applicantId/applicantName）由后端从 SecurityContext 自动注入
   * @param data - 报销表单数据（前端语义字符串，金额为元）
   * @returns 创建后的报销单VO（已转换状态/金额）
   */
  async create(data: ReimbursementFormData): Promise<InvoiceReimbursementVO> {
    const dto = ReimbursementDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/invoice-reimbursements', dto)
    return ReimbursementDataConverter.toFrontend(res) as unknown as InvoiceReimbursementVO
  },

  /**
   * 分页查询报销单列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/金额）
   */
  async getList(params?: ReimbursementQueryForm): Promise<PageResponse<InvoiceReimbursementVO>> {
    const query = mapQueryParams(params)
    const res = await get<ReimbursementPageBackend | null>('/v1/finance/invoice-reimbursements', query)
    const records = (res?.records || []).map(item =>
      ReimbursementDataConverter.toFrontend(item) as unknown as InvoiceReimbursementVO
    )
    return {
      records,
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 查询报销单详情（含明细+审批记录）
   * @param id - 报销单ID
   * @returns 报销单VO（已转换状态/金额）
   */
  async getById(id: string): Promise<InvoiceReimbursementVO> {
    const res = await get<Record<string, unknown>>(`/v1/finance/invoice-reimbursements/${id}`)
    return ReimbursementDataConverter.toFrontend(res) as unknown as InvoiceReimbursementVO
  },

  /**
   * 更新报销单（仅草稿状态可更新）
   * @param id - 报销单ID
   * @param data - 报销表单数据（前端语义字符串，金额为元）
   * @returns 更新后的报销单VO（已转换状态/金额）
   */
  async update(id: string, data: ReimbursementFormData): Promise<InvoiceReimbursementVO> {
    const dto = ReimbursementDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/invoice-reimbursements/${id}`, dto)
    return ReimbursementDataConverter.toFrontend(res) as unknown as InvoiceReimbursementVO
  },

  /**
   * 审批报销申请
   * 审批人信息（approverId/approverName）由后端从 SecurityContext 自动注入
   * @param id - 报销单ID
   * @param form - 审批表单（含 approved/remark/approvedAmount，金额为元）
   * @returns 审批后的报销单VO（已转换状态/金额）
   */
  async approve(id: string, form: ReimbursementApproveForm): Promise<InvoiceReimbursementVO> {
    const dto: Record<string, unknown> = {
      approved: form.approved,
    }
    if (form.remark) dto.remark = form.remark
    if (form.approvedAmount !== undefined) {
      dto.approvedAmount = yuanToFen(form.approvedAmount)
    }
    const res = await put<Record<string, unknown>>(`/v1/finance/invoice-reimbursements/${id}/approve`, dto)
    return ReimbursementDataConverter.toFrontend(res) as unknown as InvoiceReimbursementVO
  },

  /**
   * 取消报销申请（仅草稿/已审批状态可取消）
   * @param id - 报销单ID
   * @param cancelReason - 取消原因
   * @returns 取消后的报销单VO（已转换状态/金额）
   */
  async cancel(id: string, cancelReason: string): Promise<InvoiceReimbursementVO> {
    const res = await put<Record<string, unknown>>(
      `/v1/finance/invoice-reimbursements/${id}/cancel`,
      null,
      { params: { cancelReason } }
    )
    return ReimbursementDataConverter.toFrontend(res) as unknown as InvoiceReimbursementVO
  },

  /**
   * 标记报销单已付款（状态流转：已审批 → 已付款）
   * @param id - 报销单ID
   * @param form - 付款表单（含付款凭证号+付款日期）
   * @returns 付款后的报销单VO（已转换状态/金额）
   */
  async pay(id: string, form: ReimbursementPayForm): Promise<InvoiceReimbursementVO> {
    const res = await put<Record<string, unknown>>(`/v1/finance/invoice-reimbursements/${id}/pay`, form)
    return ReimbursementDataConverter.toFrontend(res) as unknown as InvoiceReimbursementVO
  },

  /**
   * 获取报销统计数据
   * @param params - 查询条件（含起止日期/部门/状态过滤）
   * @returns 报销统计VO（金额字段已转元）
   */
  async getStats(params?: ReimbursementQueryForm): Promise<ReimbursementStats> {
    const query = mapQueryParams(params)
    const res = await get<Record<string, unknown> | null>('/v1/finance/invoice-reimbursements/stats', query)
    if (!res) {
      return {
        totalCount: 0, pendingCount: 0, approvedCount: 0, paidCount: 0,
        cancelledCount: 0, rejectedCount: 0,
        totalAmount: 0, approvedAmount: 0, paidAmount: 0,
      }
    }
    return {
      totalCount: Number(res.totalCount ?? 0),
      pendingCount: Number(res.pendingCount ?? 0),
      approvedCount: Number(res.approvedCount ?? 0),
      paidCount: Number(res.paidCount ?? 0),
      cancelledCount: Number(res.cancelledCount ?? 0),
      rejectedCount: Number(res.rejectedCount ?? 0),
      totalAmount: fenToYuanNumber(res.totalAmount as number),
      approvedAmount: fenToYuanNumber(res.approvedAmount as number),
      paidAmount: fenToYuanNumber(res.paidAmount as number),
      period: res.period as string | undefined,
    }
  },

  /**
   * 获取预算信息（用于报销页面的预算执行率展示）
   * @param departmentId - 部门ID（可空，查询全部部门）
   * @param budgetYear - 预算年度（可空，默认当前年）
   * @returns 预算VO列表
   */
  async getBudgetInfo(departmentId?: string, budgetYear?: number): Promise<Record<string, unknown>[]> {
    const params: Record<string, unknown> = {}
    if (departmentId) params.departmentId = departmentId
    if (budgetYear) params.budgetYear = budgetYear
    const res = await get<Record<string, unknown>[] | null>('/v1/finance/invoice-reimbursements/budget-info', params)
    return res ?? []
  },
}

export default reimbursementApi
