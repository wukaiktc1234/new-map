/**
 * 证件管理 API
 *
 * 后端 Controller：StoreHealthCertificateController
 * 路径前缀：/v1/store/health-certificate
 *
 * 设计模式：真实 API 调用
 * - 调用真实后端 API，失败时抛出错误由调用方处理
 * - 返回 OperationResult 的写操作方法在失败时返回失败结果
 *
 * 注意：后端 HealthCertificate 实体为员工健康证管理设计，字段与前端通用证照模型存在差异。
 *      真实 API 调用成功后，会通过 certificateDataConverter 尝试转换。
 */
import { get, post, put, del } from '../request'
import { certificateDataConverter } from './converters'
import type {
  Certificate,
  CertificateQueryParams,
  CreateCertificateDTO,
  UpdateCertificateDTO,
  CertificateExpenseRecord,
  CreateExpenseDTO,
  ExpenseStatus,
  PageResponse,
  OperationResult,
  UpdateExpenseStatusDTO,
} from '@/types/store-operation'

// ============================================================
// 后端响应类型定义（与 HealthCertificate 实体字段匹配）
// ============================================================

/**
 * 后端 HealthCertificate 实体响应类型
 * 字段与 com.foodtraceability.entity.HealthCertificate 对应
 */
export interface HealthCertificateBackend {
  id: string
  employeeId?: string
  employeeName?: string
  store?: string
  storeId?: string
  storeName?: string
  certificateNumber?: string
  issueDate?: string
  expiryDate?: string
  status?: string
  issuer?: string
  note?: string
  expenseStatus?: string
  approvalStatus?: string
  certificateImage?: string
  expiryDays?: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

/**
 * 后端 HealthCertificateExpense 实体响应类型
 * 字段与 com.foodtraceability.entity.HealthCertificateExpense 对应
 */
export interface HealthCertificateExpenseBackend {
  id: number
  healthCertificateId: string
  employeeId?: string
  employeeName?: string
  store?: string
  amount?: number
  invoiceType?: string
  invoiceNumber?: string
  reason?: string
  note?: string
  status?: string
  expenseType?: string
  applyDate?: string
  approveDate?: string
  reimburseDate?: string
  rejectReason?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

/**
 * 后端分页响应（MyBatis-Plus Page 结构）
 */
interface BackendPageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
  [key: string]: unknown
}

// ============================================================
// 数据转换工具
// ============================================================

/**
 * 将后端 HealthCertificate 实体转换为前端 Certificate 类型
 *
 * 由于后端实体字段与前端模型存在差异（后端为员工健康证设计，前端为通用证照管理），
 * 此处做尽力转换；无法映射的字段保留默认值。
 */
function toFrontendCertificate(backend: HealthCertificateBackend): Certificate {
  // 借用已有的 certificateDataConverter 处理到期天数等计算逻辑
  // 字段映射通过中间对象完成
  const intermediate = {
    certificateId: backend.id ?? String(backend.id ?? ''),
    certName: backend.employeeName ? `${backend.employeeName}健康证` : '健康证',
    certType: 5, // health_certificate
    certNumber: backend.certificateNumber,
    holderType: 2, // employee
    holderId: backend.employeeId ?? '',
    holderName: backend.employeeName ?? '',
    issueDate: backend.issueDate ?? '',
    expiryDate: backend.expiryDate ?? '',
    status: mapBackendStatus(backend.status),
    fileUrl: backend.certificateImage,
    issuer: backend.issuer,
    createTime: backend.createTime ?? '',
    updateTime: backend.updateTime ?? '',
  }
  return certificateDataConverter.toFrontend(intermediate as unknown as Parameters<typeof certificateDataConverter.toFrontend>[0])
}

/**
 * 后端状态字符串映射为前端状态数字（certificateDataConverter 需要）
 */
function mapBackendStatus(status?: string): number {
  switch (status) {
    case 'valid': return 1
    case 'expiring': return 2
    case 'expired': return 3
    default: return 1
  }
}

/**
 * 将后端 HealthCertificateExpense 实体转换为前端 CertificateExpenseRecord 类型
 */
function toFrontendExpense(
  backend: HealthCertificateExpenseBackend,
  certificateId: string,
  certName: string,
): CertificateExpenseRecord {
  const now = new Date().toISOString()
  return {
    expenseId: String(backend.id ?? ''),
    certificateId,
    certName,
    renewalRecordId: '',
    amount: backend.amount ?? 0,
    expenseStatus: mapBackendExpenseStatus(backend.status),
    remark: backend.reason ?? backend.note ?? '',
    createTime: backend.createTime ?? now,
    updateTime: backend.updateTime ?? now,
  }
}

/**
 * 后端报销状态字符串映射为前端 ExpenseStatus
 */
function mapBackendExpenseStatus(status?: string): ExpenseStatus {
  switch (status) {
    case 'pending': return 'unreimbursed'
    case 'approved': return 'manager_approved'
    case 'reimbursed': return 'reimbursed'
    case 'rejected': return 'unreimbursed'
    default: return 'unreimbursed'
  }
}

// ============================================================
// 证书管理 API
// ============================================================

export const certificateApi = {
  /**
   * 获取证件列表
   * 后端端点：GET /v1/store/health-certificate/list
   */
  async getCertificateList(params: CertificateQueryParams): Promise<PageResponse<Certificate>> {
    // 前端语义化状态 → 后端状态字符串映射
    // 前端 CertStatus: 'active' | 'expiring' | 'expired' | 'revoked'
    // 后端实际存储: 'valid' | 'expiring' | 'expired'
    // 关键差异：前端 'active' 对应后端 'valid'
    const frontendToBackendStatus: Record<string, string> = {
      active: 'valid',
      expiring: 'expiring',
      expired: 'expired',
      revoked: 'expired', // 后端无 revoked 状态，归并到 expired
    }
    const backendStatus = params.status
      ? frontendToBackendStatus[params.status]
      : undefined

    // 注意：后端 /list 端点仅支持 store（门店名）与 status 筛选，
    // holderKeyword（持证人关键字）后端无对应模糊查询参数，本轮不发送，避免误传到 store 参数
    const res = await get<BackendPageResponse<HealthCertificateBackend>>(
      '/v1/store/health-certificate/list',
      {
        page: params.page,
        size: params.size,
        ...(backendStatus ? { status: backendStatus } : {}),
      },
    )
    return {
      records: (res?.records || []).map(toFrontendCertificate),
      total: res?.total ?? 0,
      current: res?.current ?? params.page,
      size: res?.size ?? params.size,
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据 ID 获取证件详情
   * 后端端点：GET /v1/store/health-certificate/{id}
   */
  async getCertificateById(id: string): Promise<Certificate | null> {
    const res = await get<HealthCertificateBackend>(
      `/v1/store/health-certificate/${id}`,
    )
    return res ? toFrontendCertificate(res) : null
  },

  /**
   * 根据员工 ID 获取证件信息
   * 后端端点：GET /v1/store/health-certificate/employee/{employeeId}
   */
  async getCertificateByEmployeeId(employeeId: string): Promise<Certificate | null> {
    const res = await get<HealthCertificateBackend>(
      `/v1/store/health-certificate/employee/${employeeId}`,
    )
    return res ? toFrontendCertificate(res) : null
  },

  /**
   * 新增证件
   * 后端端点：POST /v1/store/health-certificate/add
   * 失败时返回失败结果
   */
  async createCertificate(dto: CreateCertificateDTO): Promise<OperationResult> {
    try {
      const backendDto = {
        employeeId: dto.holderId,
        employeeName: dto.holderName,
        certificateNumber: dto.certNumber,
        issueDate: dto.issueDate,
        expiryDate: dto.expiryDate,
        issuer: dto.issuer,
        status: 'valid',
        note: dto.remark,
        certificateImage: dto.fileUrl,
      }
      const res = await post<string>('/v1/store/health-certificate/add', backendDto)
      return {
        success: true,
        message: typeof res === 'string' ? res : '创建成功',
      }
    } catch (error) {
      return { success: false, message: '创建证件失败，请重试' }
    }
  },

  /**
   * 更新证件
   * 后端端点：PUT /v1/store/health-certificate/update
   * 失败时返回失败结果
   */
  async updateCertificate(certId: string, dto: UpdateCertificateDTO): Promise<OperationResult> {
    try {
      const backendDto = {
        id: certId,
        employeeId: dto.holderId,
        employeeName: dto.holderName,
        certificateNumber: dto.certNumber,
        issueDate: dto.issueDate,
        expiryDate: dto.expiryDate,
        issuer: dto.issuer,
        certificateImage: dto.fileUrl,
        // 停用场景：dto.status === 'inactive' 时映射为后端状态
        status: dto.status === 'inactive' ? 'expired' : 'valid',
        note: dto.remark,
      }
      const res = await put<string>('/v1/store/health-certificate/update', backendDto)
      return {
        success: true,
        message: typeof res === 'string' ? res : '更新成功',
      }
    } catch (error) {
      return { success: false, message: '更新证件失败，请重试' }
    }
  },

  /**
   * 删除证件
   * 后端端点：DELETE /v1/store/health-certificate/{id}
   * 失败时返回失败结果
   */
  async deleteCertificate(certId: string): Promise<OperationResult> {
    try {
      await del<string>(`/v1/store/health-certificate/${certId}`)
      return { success: true, message: '删除成功' }
    } catch (error) {
      return { success: false, message: '删除证件失败，请重试' }
    }
  },

  /**
   * 获取证件的费用记录列表
   * 后端端点：GET /v1/store/health-certificate/expense/{healthCertificateId}
   */
  async getExpenseRecords(certificateId: string): Promise<CertificateExpenseRecord[]> {
    const res = await get<HealthCertificateExpenseBackend[]>(
      `/v1/store/health-certificate/expense/${certificateId}`,
    )
    if (!Array.isArray(res)) {
      return []
    }
    // 查找证件名称用于转换
    const certInfo = await this.getCertificateById(certificateId).catch(() => null)
    const certName = certInfo?.certName ?? ''
    return res.map((item) => toFrontendExpense(item, certificateId, certName))
  },

  /**
   * 获取全部费用记录（无 certificateId 筛选）
   * 后端端点：GET /v1/store/health-certificate/expense/all
   */
  async getAllExpenseRecords(params?: {
    page?: number
    size?: number
    expenseStatus?: ExpenseStatus
  }): Promise<PageResponse<CertificateExpenseRecord>> {
    const query: Record<string, unknown> = {
      page: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    if (params?.expenseStatus) {
      query.status = params.expenseStatus
    }
    const res = await get<PageResponse<CertificateExpenseRecord>>(
      '/v1/store/health-certificate/expense/all',
      query,
    ) || { records: [], total: 0, current: params?.page ?? 1, size: params?.size ?? 10, pages: 0 }
    return res
  },

  /**
   * 新增费用记录
   * 后端端点：POST /v1/store/health-certificate/expense/add
   * 失败时返回失败结果
   */
  async createExpenseRecord(dto: CreateExpenseDTO): Promise<OperationResult> {
    try {
      const backendDto = {
        healthCertificateId: dto.certificateId,
        employeeId: '',
        employeeName: '',
        amount: dto.amount,
        reason: dto.remark,
        note: dto.remark,
        expenseType: 'renewal',
        status: 'pending',
      }
      const res = await post<string>('/v1/store/health-certificate/expense/add', backendDto)
      return {
        success: true,
        message: typeof res === 'string' ? res : '费用记录已创建',
      }
    } catch (error) {
      return { success: false, message: '创建费用记录失败，请重试' }
    }
  },

  /**
   * 更新费用报销状态
   * 后端端点：PUT /v1/store/health-certificate/expense/{id}/status
   */
  async updateExpenseStatus(dto: UpdateExpenseStatusDTO): Promise<OperationResult> {
    try {
      await put<void>(
        `/v1/store/health-certificate/expense/${dto.expenseId}/status`,
        { status: dto.expenseStatus },
      )
      return { success: true, message: '报销状态更新成功' }
    } catch (error) {
      return { success: false, message: '更新报销状态失败，请重试' }
    }
  },
}
