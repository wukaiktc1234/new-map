/* mock 数据已清理（2026-06-30） */
/**
 * 合同管理API
 * 对应后端: /v1/contracts（EmployeeLaborContractController）
 *
 * 【对接说明】
 * 后端 EmployeeLaborContractController 实际提供的端点：
 * 1. GET /v1/contracts/list                     分页查询合同列表
 * 2. GET /v1/contracts/statistics               合同统计
 * 3. GET /v1/contracts/pending                   待签署合同
 * 4. GET /v1/contracts/{id}                      合同详情
 * 5. GET /v1/contracts/archive/{archiveId}       按档案ID查询合同
 * 6. POST /v1/contracts/{id}/sign                签署合同
 * 7. POST /v1/contracts/{id}/extend               延长合同
 * 8. POST /v1/contracts/{id}/terminate            终止合同
 * 9. GET /v1/contracts/{id}/document              当前合同正文
 * 10. GET /v1/contracts/{id}/document/history     正文版本历史
 * 11. GET /v1/contracts/{id}/document/{version}   指定版本正文
 * 12. POST /v1/contracts/{id}/document             保存合同正文
 * 13. GET /v1/contracts/{id}/signatures            签署记录列表
 * 14. GET /v1/contracts/{id}/signature-status      签署状态看板
 * 15. POST /v1/contracts/{id}/init-signatures       初始化签署记录
 * 16. POST /v1/contracts/{id}/sign-with-signature   带签名数据签署
 * 17. POST /v1/contracts/{id}/reject-signature      拒绝签署
 * 18. POST /v1/contracts/{id}/send-verify-code      发送验证码
 *
 * 后端尚未提供端点的方法（renew/archive/destroy/getClauses/
 * autoGenerate/generateDocument/submitApproval/getExpiringContracts/getArchived/
 * createSignTask/getSignTasks/uploadPaperScan/companySign/verifyContract/downloadPdf/
 * getChangeRecords/createChange/submitChangeApproval/activateChange），
 * 调用时直接抛错，避免静默使用 mock 数据污染真实业务流程。
 *
 * 【更新记录 2026-07-17】后端已实现 POST /v1/contracts、PUT /v1/contracts/{id}、DELETE /v1/contracts/{id}，
 * create/update/delete 方法已对接真实后端。
 */
import { get, post, put, del } from '../request'
import type { RequestOptions } from '../request'
import type {
  EmployeeContract,
  ContractFormData,
  ContractQueryParams,
  ContractRenewData,
  ContractTerminateData,
  ContractClause,
  ContractArchiveFormData,
  ContractDestroyFormData,
  ContractType,
  ContractStatistics,
  SignatureRecord,
  ContractDocument,
  ContractDocumentEditForm,
  SignatureStatusDTO,
  ContractSignRequestForm,
  ContractRejectForm,
  VerifyCodeResponse,
  SignatoryType,
  SignTask,
  CreateSignTaskForm,
  CompanySignForm,
  ContractVerifyForm,
  ContractVerifyResult,
  ContractGenerateDocumentData,
  ContractSubmitApprovalData,
  ContractChangeRecord,
  ContractChangeCreateForm,
  ContractAutoGenerateRequest,
  ContractAutoGenerateResult,
} from '../../types/hr/contract'
import { ContractStatusMap, ContractDataConverter } from './converters'

/**
 * 映射分页查询参数：前端 page/pageSize → 后端 current/size
 * 同时转换 status 字段（前端语义字符串 → 后端数字编码）
 */
function mapQueryParams(params?: ContractQueryParams): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, status, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  if (status !== undefined && typeof status === 'string') {
    result.status = ContractStatusMap.toBackend[status]
  }
  return result
}

export const contractApi = {
  /* ============================================================
   * 合同查询（对接后端 /v1/contracts）
   * ============================================================ */

  /** 分页查询合同列表（对接后端 GET /v1/contracts/list） */
  async getList(params?: ContractQueryParams): Promise<{ records: EmployeeContract[]; total: number }> {
    const res = await get<{ records: EmployeeContract[]; total: number } | EmployeeContract[] | null>(
      '/v1/contracts/list',
      mapQueryParams(params)
    )
    // 后端返回 Map<String,Object>，可能是数组或 {records,total} 形式
    if (Array.isArray(res)) {
      return { records: res, total: res.length }
    }
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
    }
  },

  /** 获取合同统计（对接后端 GET /v1/contracts/statistics） */
  async getStatistics(): Promise<ContractStatistics> {
    const res = await get<Record<string, number> | null>('/v1/contracts/statistics')
    return {
      total: res?.total ?? 0,
      pending: res?.pending ?? 0,
      active: res?.active ?? 0,
      expiring: res?.expiring ?? 0,
      closed: res?.closed ?? 0,
      archived: res?.archived ?? 0,
    }
  },

  /** 获取待签署合同列表（对接后端 GET /v1/contracts/pending） */
  async getPendingContracts(): Promise<EmployeeContract[]> {
    const res = await get<EmployeeContract[] | null>('/v1/contracts/pending')
    return res ?? []
  },

  /** 根据ID获取合同详情（对接后端 GET /v1/contracts/{id}） */
  async getById(id: string): Promise<EmployeeContract> {
    return await get<EmployeeContract>(`/v1/contracts/${id}`)
  },

  /** 根据入职档案ID获取合同（对接后端 GET /v1/contracts/archive/{archiveId}） */
  async getByArchiveId(archiveId: string): Promise<EmployeeContract | null> {
    return await get<EmployeeContract | null>(`/v1/contracts/archive/${archiveId}`)
  },

  /* ============================================================
   * 合同创建/更新/删除（对接后端 POST/PUT/DELETE /v1/contracts）
   * ============================================================ */

  /**
   * 创建合同（对接后端 POST /v1/contracts）
   * 使用 ContractDataConverter 转换 status/contractType 字段
   */
  async create(data: ContractFormData): Promise<EmployeeContract> {
    // ContractFormData 接口无索引签名，需双重断言以满足 toCreateDTO 的泛型约束
    const payload = ContractDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    return await post<EmployeeContract>('/v1/contracts', payload)
  },

  /**
   * 更新合同（对接后端 PUT /v1/contracts/{id}）
   * 使用 ContractDataConverter 转换 status/contractType 字段
   */
  async update(id: string, data: Partial<ContractFormData>): Promise<EmployeeContract> {
    const payload = ContractDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    return await put<EmployeeContract>(`/v1/contracts/${id}`, payload)
  },

  /**
   * 删除合同（对接后端 DELETE /v1/contracts/{id}，逻辑删除）
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/contracts/${id}`)
  },

  /* ============================================================
   * 合同签署流程（对接后端 /v1/contracts/{id}/sign、extend、terminate）
   * ============================================================ */

  /**
   * 签署合同（对接后端 POST /v1/contracts/{id}/sign）
   * @param id 合同ID
   * @param signData 签署数据（signDate 签署日期）
   */
  async sign(id: string, signData?: { signDate?: string }): Promise<void> {
    await post(`/v1/contracts/${id}/sign`, signData ?? {})
  },

  /** 延长合同期限（对接后端 POST /v1/contracts/{id}/extend） */
  async extend(id: string, months: number): Promise<void> {
    await post(`/v1/contracts/${id}/extend`, { months })
  },

  /** 终止合同（对接后端 POST /v1/contracts/{id}/terminate） */
  async terminate(data: ContractTerminateData): Promise<void> {
    await post(`/v1/contracts/${data.contractId}/terminate`, { reason: data.terminateReason })
  },

  /* ============================================================
   * 合同续签/正文生成/审批/到期查询（后端待补充）
   * ============================================================ */

  /** 续签合同（后端待补充，调用即抛错） */
  async renew(_data: ContractRenewData): Promise<EmployeeContract> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/renew 端点
    throw new Error('合同续签 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/renew 端点')
  },

  /** 从模板生成合同正文（后端待补充，调用即抛错） */
  async generateDocument(_data: ContractGenerateDocumentData): Promise<ContractDocument> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/generate-document 端点
    throw new Error('合同正文生成 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/generate-document 端点')
  },

  /** 提交审批（后端待补充，调用即抛错） */
  async submitApproval(_data: ContractSubmitApprovalData): Promise<void> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/submit-approval 端点
    throw new Error('合同提交审批 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/submit-approval 端点')
  },

  /** 获取即将到期的合同（后端待补充，调用即抛错） */
  async getExpiringContracts(_days: number): Promise<EmployeeContract[]> {
    // TODO: 后端待补充 GET /v1/contracts/expiring?days={days} 端点
    throw new Error('合同到期查询 API 尚未对接，请联系后端配置 GET /v1/contracts/expiring?days={days} 端点')
  },

  /* ============================================================
   * 条款管理（后端待补充，由合同模板系统统一管理）
   * ============================================================ */

  /** 获取合同类型的标准条款模板（后端待补充，调用即抛错） */
  async getClauses(_contractType: ContractType): Promise<ContractClause[]> {
    // TODO: 后端待补充 GET /v1/contracts/clauses?contractType={type} 端点
    throw new Error('合同条款查询 API 尚未对接，请联系后端配置 GET /v1/contracts/clauses?contractType={type} 端点')
  },

  /* ============================================================
   * 归档管理（后端待补充）
   * ============================================================ */

  /** 归档合同（后端待补充，调用即抛错） */
  async archive(_contractId: string, _data: ContractArchiveFormData): Promise<EmployeeContract> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/archive 端点
    throw new Error('合同归档 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/archive 端点')
  },

  /** 销毁合同（后端待补充，调用即抛错） */
  async destroy(_contractId: string, _data: ContractDestroyFormData): Promise<EmployeeContract> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/destroy 端点
    throw new Error('合同销毁 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/destroy 端点')
  },

  /** 查询已归档合同（后端待补充，调用即抛错） */
  async getArchived(): Promise<EmployeeContract[]> {
    // TODO: 后端待补充 GET /v1/contracts/archived 端点
    throw new Error('已归档合同查询 API 尚未对接，请联系后端配置 GET /v1/contracts/archived 端点')
  },

  /* ============================================================
   * 合同正文管理（对接后端 /v1/contracts/{id}/document*）
   * ============================================================ */

  /** 获取合同正文（对接后端 GET /v1/contracts/{id}/document） */
  async getContractDocument(contractId: string): Promise<ContractDocument> {
    return await get<ContractDocument>(`/v1/contracts/${contractId}/document`)
  },

  /** 保存合同正文（对接后端 POST /v1/contracts/{id}/document） */
  async saveContractDocument(data: ContractDocumentEditForm): Promise<ContractDocument> {
    return await post<ContractDocument>(`/v1/contracts/${data.contractId}/document`, {
      htmlContent: data.htmlContent,
      editRemark: data.editRemark,
    })
  },

  /** 获取合同正文版本历史（对接后端 GET /v1/contracts/{id}/document/history） */
  async getDocumentHistory(contractId: string): Promise<ContractDocument[]> {
    const res = await get<ContractDocument[] | null>(`/v1/contracts/${contractId}/document/history`)
    return res ?? []
  },

  /** 获取指定版本的合同正文（对接后端 GET /v1/contracts/{id}/document/{version}） */
  async getDocumentByVersion(contractId: string, version: number): Promise<ContractDocument> {
    return await get<ContractDocument>(`/v1/contracts/${contractId}/document/${version}`)
  },

  /* ============================================================
   * 签署流程管理（对接后端 /v1/contracts/{id}/signature-*）
   * ============================================================ */

  /** 获取合同签署记录列表（对接后端 GET /v1/contracts/{id}/signatures） */
  async getSignatures(contractId: string): Promise<SignatureRecord[]> {
    const res = await get<SignatureRecord[] | null>(`/v1/contracts/${contractId}/signatures`)
    return res ?? []
  },

  /** 获取合同签署状态看板（对接后端 GET /v1/contracts/{id}/signature-status） */
  async getSignatureStatus(contractId: string): Promise<SignatureStatusDTO> {
    return await get<SignatureStatusDTO>(`/v1/contracts/${contractId}/signature-status`)
  },

  /** 初始化合同签署记录（对接后端 POST /v1/contracts/{id}/init-signatures） */
  async initSignatures(contractId: string): Promise<void> {
    await post(`/v1/contracts/${contractId}/init-signatures`, {})
  },

  /** 签署合同（带签名数据，对接后端 POST /v1/contracts/{id}/sign-with-signature） */
  async signWithSignature(contractId: string, data: ContractSignRequestForm): Promise<void> {
    await post(`/v1/contracts/${contractId}/sign-with-signature`, data)
  },

  /** 拒绝签署（对接后端 POST /v1/contracts/{id}/reject-signature） */
  async rejectSignature(contractId: string, data: ContractRejectForm): Promise<void> {
    await post(`/v1/contracts/${contractId}/reject-signature`, data)
  },

  /** 发送验证码（对接后端 POST /v1/contracts/{id}/send-verify-code） */
  async sendVerifyCode(contractId: string, signerType: SignatoryType): Promise<VerifyCodeResponse> {
    return await post<VerifyCodeResponse>(`/v1/contracts/${contractId}/send-verify-code`, undefined, {
      params: { signerType },
    } as Partial<RequestOptions>)
  },

  /* ============================================================
   * 合同载体双档管理（后端待补充）
   * TODO: 后端待补充 sign-tasks、paper-scan、company-sign、verify、pdf 端点
   * ============================================================ */

  /** 创建签署任务（后端待补充，调用即抛错） */
  async createSignTask(_contractId: string, _data: CreateSignTaskForm): Promise<SignTask> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/sign-tasks 端点
    throw new Error('创建签署任务 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/sign-tasks 端点')
  },

  /** 获取合同签署任务列表（后端待补充，调用即抛错） */
  async getSignTasks(_contractId: string): Promise<SignTask[]> {
    // TODO: 后端待补充 GET /v1/contracts/{id}/sign-tasks 端点
    throw new Error('获取签署任务 API 尚未对接，请联系后端配置 GET /v1/contracts/{id}/sign-tasks 端点')
  },

  /** 上传纸质合同扫描件（后端待补充，调用即抛错） */
  async uploadPaperScan(_contractId: string, _data: { scanFileUrl: string; scanFileName: string; archiveLocation?: string; archiveNumber?: string; remark?: string }): Promise<EmployeeContract> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/paper-scan 端点
    throw new Error('上传纸质扫描件 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/paper-scan 端点')
  },

  /** 公司方签署（后端待补充，调用即抛错） */
  async companySign(_contractId: string, _data: CompanySignForm): Promise<EmployeeContract> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/company-sign 端点
    throw new Error('公司方签署 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/company-sign 端点')
  },

  /** 合同验真（后端待补充，调用即抛错） */
  async verifyContract(_data: ContractVerifyForm): Promise<ContractVerifyResult> {
    // TODO: 后端待补充 POST /v1/contracts/verify 端点
    throw new Error('合同验真 API 尚未对接，请联系后端配置 POST /v1/contracts/verify 端点')
  },

  /** 下载电子合同 PDF（后端待补充，调用即抛错） */
  async downloadPdf(_contractId: string): Promise<Blob> {
    // TODO: 后端待补充 GET /v1/contracts/{id}/pdf 端点
    throw new Error('下载合同PDF API 尚未对接，请联系后端配置 GET /v1/contracts/{id}/pdf 端点')
  },

  /* ============================================================
   * 合同自动生成与变更管理（后端待补充）
   * ============================================================ */

  /** 根据入职信息自动生成合同（后端待补充，调用即抛错） */
  async autoGenerate(_data: ContractAutoGenerateRequest): Promise<ContractAutoGenerateResult> {
    // TODO: 后端待补充 POST /v1/contracts/auto-generate 端点
    throw new Error('自动生成合同 API 尚未对接，请联系后端配置 POST /v1/contracts/auto-generate 端点')
  },

  /** 获取合同变更记录列表（后端待补充，调用即抛错） */
  async getChangeRecords(_contractId: string): Promise<ContractChangeRecord[]> {
    // TODO: 后端待补充 GET /v1/contracts/{id}/changes 端点
    throw new Error('获取变更记录 API 尚未对接，请联系后端配置 GET /v1/contracts/{id}/changes 端点')
  },

  /** 创建合同变更/补充协议（后端待补充，调用即抛错） */
  async createChange(_data: ContractChangeCreateForm): Promise<ContractChangeRecord> {
    // TODO: 后端待补充 POST /v1/contracts/{id}/changes 端点
    throw new Error('创建变更 API 尚未对接，请联系后端配置 POST /v1/contracts/{id}/changes 端点')
  },

  /** 提交变更审批（后端待补充，调用即抛错） */
  async submitChangeApproval(_changeId: string): Promise<void> {
    // TODO: 后端待补充 POST /v1/contracts/changes/{changeId}/approval 端点
    throw new Error('提交变更审批 API 尚未对接，请联系后端配置 POST /v1/contracts/changes/{changeId}/approval 端点')
  },

  /** 确认变更生效（后端待补充，调用即抛错） */
  async activateChange(_changeId: string): Promise<void> {
    // TODO: 后端待补充 POST /v1/contracts/changes/{changeId}/activate 端点
    throw new Error('确认变更生效 API 尚未对接，请联系后端配置 POST /v1/contracts/changes/{changeId}/activate 端点')
  },
}

export default contractApi
