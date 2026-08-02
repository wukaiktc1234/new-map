/**
 * 合同管理类型定义
 * 支持8种法定用工模式：
 * 1. 劳动合同（labor）- 《劳动合同法》
 * 2. 非全日制用工/兼职（part_time）- 《劳动合同法》第三节
 * 3. 劳务派遣（dispatch）- 《劳务派遣暂行规定》
 * 4. 业务外包（outsourcing）- 《合同法》
 * 5. 实习协议（intern）- 《职业学校学生实习管理规定》
 * 6. 超龄用工协议（over_age）- 《超龄劳动者基本权益保障暂行规定》（2026.7.1生效）
 * 7. 保密协议（confidentiality）- 《反不正当竞争法》《劳动合同法》
 * 8. 竞业限制协议（non_compete）- 《劳动合同法》第23-24条
 */

/** 合同类型 */
export type ContractType =
  | 'labor'           // 劳动合同（标准）
  | 'over_age'        // 超龄用工协议（超龄劳动者）
  | 'intern'          // 实习协议
  | 'part_time'       // 兼职协议（非全日制用工）
  | 'confidentiality' // 保密协议
  | 'non_compete'     // 竞业限制协议
  | 'dispatch'        // 劳务派遣
  | 'outsourcing'     // 劳务外包

/** 合同类型选项 */
export const ContractTypeOptions: { value: ContractType; label: string }[] = [
  { value: 'labor', label: '劳动合同' },
  { value: 'over_age', label: '超龄用工协议' },
  { value: 'intern', label: '实习协议' },
  { value: 'part_time', label: '兼职协议' },
  { value: 'confidentiality', label: '保密协议' },
  { value: 'non_compete', label: '竞业限制协议' },
  { value: 'dispatch', label: '劳务派遣' },
  { value: 'outsourcing', label: '劳务外包' },
]

/** 合同类型到StatusTag的状态映射 */
export const ContractTypeTagMap: Record<ContractType, string> = {
  labor: 'primary',
  over_age: 'info',
  intern: 'warning',
  part_time: 'default',
  confidentiality: 'default',
  non_compete: 'default',
  dispatch: 'success',
  outsourcing: 'pending',
}

/**
 * 获取合同类型分类
 * - standard: 标准用工合同（含特殊用工模式）
 * - supplementary: 补充协议
 * - special: 预留分类（暂无类型使用）
 * @param type - 合同类型
 * @returns 合同类型分类
 */
export function getContractTypeCategory(type: ContractType): 'standard' | 'special' | 'supplementary' {
  const supplementary: ContractType[] = ['confidentiality', 'non_compete']
  if (supplementary.includes(type)) return 'supplementary'
  return 'standard'
}

/**
 * 合同状态（签署状态机扩展）
 * 完整状态流转：
 * draft → pending_approval → pending_sign → company_signed → signed → active → expiring → expired → terminated → archived
 *                                                  ↑续签 renewed
 */
export type ContractStatus =
  | 'draft'              // 草稿
  | 'pending_approval'   // 审批中
  | 'pending_sign'       // 待签署
  | 'company_signed'     // 公司已签
  | 'signed'             // 双方已签（待生效）
  | 'active'             // 执行中
  | 'expiring'           // 即将到期（30天内）
  | 'expired'            // 已到期
  | 'terminated'         // 已终止
  | 'renewed'            // 已续签
  | 'archived'           // 已归档

/** 合同状态选项 */
export const ContractStatusOptions: { value: ContractStatus; label: string }[] = [
  { value: 'draft', label: '草稿' },
  { value: 'pending_approval', label: '审批中' },
  { value: 'pending_sign', label: '待签署' },
  { value: 'company_signed', label: '公司已签' },
  { value: 'signed', label: '双方已签' },
  { value: 'active', label: '执行中' },
  { value: 'expiring', label: '即将到期' },
  { value: 'expired', label: '已到期' },
  { value: 'terminated', label: '已终止' },
  { value: 'renewed', label: '已续签' },
  { value: 'archived', label: '已归档' },
]

/** 合同状态到StatusTag的映射 */
export const ContractStatusTagMap: Record<ContractStatus, string> = {
  draft: 'default',
  pending_approval: 'pending',
  pending_sign: 'warning',
  company_signed: 'info',
  signed: 'info',
  active: 'active',
  expiring: 'warning',
  expired: 'error',
  terminated: 'inactive',
  renewed: 'success',
  archived: 'inactive',
}

/**
 * 获取合同状态分类（用于统计卡片）
 * - pending: 待处理（草稿+审批中+待签署+公司已签+双方已签）
 * - active: 执行中（执行中+即将到期）
 * - closed: 已结束（已到期+已终止+已续签+已归档）
 */
export function getContractStatusCategory(status: ContractStatus): 'pending' | 'active' | 'closed' {
  const pending: ContractStatus[] = ['draft', 'pending_approval', 'pending_sign', 'company_signed', 'signed']
  const active: ContractStatus[] = ['active', 'expiring']
  if (pending.includes(status)) return 'pending'
  if (active.includes(status)) return 'active'
  return 'closed'
}

/** 劳动合同期限类型 */
export type LaborContractTerm =
  | 'fixed_3year'    // 固定期限3年
  | 'fixed_5year'    // 固定期限5年
  | 'unfixed'        // 无固定期限
  | 'project'        // 以完成一定工作任务为期限

/** 劳动合同期限选项 */
export const LaborContractTermOptions: { value: LaborContractTerm; label: string }[] = [
  { value: 'fixed_3year', label: '固定期限3年' },
  { value: 'fixed_5year', label: '固定期限5年' },
  { value: 'unfixed', label: '无固定期限' },
  { value: 'project', label: '以完成一定工作任务为期限' },
]

/* ============================================================
 * 合同条款管理
 * ============================================================ */

/** 条款类型 */
export type ClauseType = 'essential' | 'optional' | 'special'
/** 必备条款/可选条款/特殊条款 */

/** 条款类型选项 */
export const ClauseTypeOptions: { value: ClauseType; label: string }[] = [
  { value: 'essential', label: '必备条款' },
  { value: 'optional', label: '可选条款' },
  { value: 'special', label: '特殊条款' },
]

/** 合同条款 */
export interface ContractClause {
  /** 条款ID */
  id: string
  /** 所属合同类型 */
  contractType: ContractType
  /** 条款类型：必备/可选/特殊 */
  clauseType: ClauseType
  /** 条款标题 */
  title: string
  /** 条款内容（支持模板变量，如 {{employeeName}}、{{startDate}}） */
  content: string
  /** 排序序号 */
  sortOrder: number
  /** 是否必须包含 */
  isRequired: boolean
}

/** 合同条款集合（合同实例的条款快照） */
export interface ContractContent {
  /** 关联合同ID */
  contractId: string
  /** 条款列表（基于标准模板，可编辑） */
  clauses: ContractClause[]
  /** 自定义补充条款 */
  customClauses?: string
  /** 附件URL列表 */
  attachments?: string[]
}

/* ============================================================
 * 合同归档管理
 * ============================================================ */

/** 归档状态 */
export type ContractArchiveStatus = 'active' | 'archived' | 'destroyed'

/** 归档状态选项 */
export const ContractArchiveStatusOptions: { value: ContractArchiveStatus; label: string }[] = [
  { value: 'active', label: '未归档' },
  { value: 'archived', label: '已归档' },
  { value: 'destroyed', label: '已销毁' },
]

/** 归档状态到StatusTag的映射 */
export const ContractArchiveStatusTagMap: Record<ContractArchiveStatus, string> = {
  active: 'default',
  archived: 'info',
  destroyed: 'inactive',
}

/** 合同归档信息 */
export interface ContractArchive {
  /** 关联合同ID */
  contractId: string
  /** 归档状态 */
  archiveStatus: ContractArchiveStatus
  /** 归档日期 */
  archiveDate?: string
  /** 归档位置（实体柜号/电子路径） */
  archiveLocation?: string
  /** 归档编号 */
  archiveNo?: string
  /** 保管期限（年） */
  retentionPeriod?: number
  /** 保管到期日 */
  retentionEndDate?: string
  /** 归档人 */
  archivedBy?: string
  /** 销毁日期 */
  destroyDate?: string
  /** 销毁原因 */
  destroyReason?: string
  /** 销毁人 */
  destroyedBy?: string
}

/** 合同归档表单数据 */
export interface ContractArchiveFormData {
  archiveDate: string
  archiveLocation: string
  archiveNo?: string
  retentionPeriod: number
  remark?: string
}

/** 合同销毁表单数据 */
export interface ContractDestroyFormData {
  destroyDate: string
  destroyReason: string
}

/* ============================================================
 * 法律风险提示
 * ============================================================ */

/** 法律风险严重程度（对应 el-alert 类型） */
export type LegalWarningLevel = 'info' | 'warning' | 'error'

/** 法律风险提示项 */
export interface LegalWarning {
  /** 严重程度 */
  level: LegalWarningLevel
  /** 提示标题 */
  title: string
  /** 提示内容 */
  content: string
}

/**
 * 8种合同类型的法律风险提示
 * 依据：设计方案2.2.5节
 */
export const LegalWarningMap: Record<ContractType, LegalWarning> = {
  labor: {
    level: 'info',
    title: '劳动合同法律提示',
    content: '依据《劳动合同法》：1）试用期最长不超过6个月；2）连续订立二次固定期限劳动合同后，应订立无固定期限劳动合同；3）劳动合同应包含法定必备条款（期限、工作内容、工作地点、工作时间、休息休假、劳动报酬、社会保险、劳动保护等）。',
  },
  over_age: {
    level: 'error',
    title: '超龄用工协议法律提示（2026.7.1新规）',
    content: '依据《超龄劳动者基本权益保障暂行规定》（2026年7月1日生效）：1）必须签订书面劳务协议；2）支付报酬不得低于当地最低工资标准；3）不安排加班、不安排夜班、不安排高危作业；4）用人单位应依法缴纳工伤保险或购买意外伤害保险；5）协议应明确工作内容、工作时间、劳动报酬、劳动保护、协议终止条件等；6）定期组织健康体检。',
  },
  part_time: {
    level: 'warning',
    title: '非全日制用工（兼职）法律提示',
    content: '依据《劳动合同法》第三节：1）劳动者每日工作时间不超过4小时，每周累计不超过24小时；2）计酬标准不得低于当地最低小时工资标准；3）劳动报酬结算支付周期最长不超过15日；4）双方当事人可以口头协议，但建议签订书面协议；5）双方当事人任何一方都可以随时通知对方终止用工。',
  },
  dispatch: {
    level: 'warning',
    title: '劳务派遣法律提示',
    content: '依据《劳务派遣暂行规定》：1）使用的被派遣劳动者数量不得超过用工总量的10%；2）只能在临时性、辅助性、替代性岗位使用；3）应实行同工同酬，不得歧视被派遣劳动者；4）派遣协议应明确派遣岗位、人员数量、派遣期限、劳动报酬和社会保险费的数额与支付方式等；5）派遣期限一般不超过3年。',
  },
  outsourcing: {
    level: 'warning',
    title: '劳务外包法律提示',
    content: '依据《合同法》：1）外包协议应明确服务范围、服务质量标准、结算方式；2）避免"假外包真派遣"（即以外包名义行派遣之实）；3）发包方不直接管理外包人员，由承包方自行管理；4）建议明确知识产权归属、保密义务、违约责任等条款；5）注意区分业务外包与劳务派遣的法律边界。',
  },
  intern: {
    level: 'info',
    title: '实习协议法律提示',
    content: '依据《职业学校学生实习管理规定》：1）每日工作时间不超过8小时，不得安排加班和夜班；2）实习单位应购买实习责任险；3）应支付实习补贴，不得低于当地最低工资标准的80%；4）实习期限一般不超过6个月；5）应明确学校联系人、实习指导教师；6）不得安排未满16周岁的学生实习。',
  },
  confidentiality: {
    level: 'info',
    title: '保密协议法律提示',
    content: '依据《反不正当竞争法》《劳动合同法》：1）保密范围应明确界定（技术信息、经营信息等）；2）保密期限一般不超过2年（商业秘密可约定更长）；3）可约定保密费，但保密义务不因是否支付保密费而免除；4）应明确违约责任和赔偿标准；5）注意与竞业限制协议的区别（保密协议不限制就业）。',
  },
  non_compete: {
    level: 'warning',
    title: '竞业限制协议法律提示',
    content: '依据《劳动合同法》第23-24条：1）竞业限制期限不得超过2年；2）竞业限制补偿金不得低于劳动者离职前12个月平均工资的30%，且不得低于当地最低工资标准；3）竞业限制范围应明确（竞争对手清单、地域范围、业务领域）；4）应明确违约金标准；5）仅限于高级管理人员、高级技术人员和其他负有保密义务的人员；6）用人单位可在竞业限制期内随时解除，但需额外支付3个月补偿金。',
  },
}

/* ============================================================
 * 员工合同实体
 * ============================================================ */

/** 员工合同 */
export interface EmployeeContract {
  id: string
  employeeId: string
  employeeName: string
  employeeCode?: string
  departmentName?: string
  positionName?: string
  /** 合同类型 */
  contractType: ContractType
  /** 合同编号 */
  contractNo: string
  /** 合同状态 */
  status: ContractStatus
  /** 合同期限类型（仅劳动合同） */
  termType?: LaborContractTerm
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate?: string
  /** 试用期结束日期（仅劳动合同） */
  probationEndDate?: string
  /** 签订日期 */
  signDate?: string
  /** 续签次数 */
  renewCount: number
  /** 原合同ID（续签时关联） */
  parentContractId?: string
  /** 附件URL列表 */
  attachments?: string[]

  /* ===== 劳务派遣专属字段 ===== */
  /** 派遣公司名称 */
  dispatchCompanyName?: string
  /** 派遣公司统一社会信用代码 */
  dispatchCompanyCode?: string
  /** 派遣期限（月） */
  dispatchPeriod?: number
  /** 派遣岗位类别（临时性/辅助性/替代性） */
  dispatchPositionCategory?: string
  /** 管理费比例（%） */
  dispatchManagementFeeRate?: number

  /* ===== 劳务外包专属字段 ===== */
  /** 外包公司名称 */
  outsourcingCompanyName?: string
  /** 外包公司统一社会信用代码 */
  outsourcingCompanyCode?: string
  /** 服务范围 */
  outsourcingServiceScope?: string
  /** 结算方式 */
  outsourcingSettlementMethod?: string

  /* ===== 超龄用工协议专属字段 ===== */
  /** 意外伤害保险单号 */
  insurancePolicyNo?: string
  /** 保险有效期至 */
  insuranceExpiryDate?: string
  /** 体检日期 */
  medicalExamDate?: string
  /** 体检结果 */
  medicalExamResult?: string
  /** 退休日期（超龄用工） */
  retirementDate?: string
  /** 养老金发放地 */
  pensionLocation?: string
  /** 岗位适配说明 */
  positionAdaptationNote?: string

  /* ===== 兼职协议专属字段 ===== */
  /** 兼职时薪（分） */
  hourlyRate?: number
  /** 每周工作时长（小时） */
  weeklyHours?: number
  /** 每日最大工时（小时） */
  dailyMaxHours?: number
  /** 结算周期（天） */
  settlementCycle?: number

  /* ===== 实习协议专属字段 ===== */
  /** 实习学校 */
  internSchool?: string
  /** 实习专业 */
  internMajor?: string
  /** 实习期限（月） */
  internPeriod?: number
  /** 实习补贴（分/月） */
  internSubsidy?: number
  /** 学校联系人 */
  internSchoolContact?: string

  /* ===== 保密协议专属字段 ===== */
  /** 保密范围 */
  confidentialityScope?: string
  /** 保密期限（月） */
  confidentialityPeriod?: number
  /** 保密费（分/月） */
  confidentialityFee?: number
  /** 违约金（分） */
  confidentialityPenalty?: number

  /* ===== 竞业限制协议专属字段 ===== */
  /** 竞业限制补偿金（分/月） */
  nonCompeteCompensation?: number
  /** 竞业限制期限（月） */
  nonCompetePeriod?: number
  /** 限制范围（竞争对手清单/地域/业务领域） */
  nonCompeteScope?: string
  /** 违约金（分） */
  nonCompetePenalty?: number

  /* ===== 归档信息 ===== */
  /** 归档状态 */
  archiveStatus?: ContractArchiveStatus
  /** 归档日期 */
  archiveDate?: string
  /** 归档位置 */
  archiveLocation?: string
  /** 归档编号 */
  archiveNo?: string
  /** 保管期限（年） */
  retentionPeriod?: number
  /** 保管到期日 */
  retentionEndDate?: string
  /** 归档人 */
  archivedBy?: string

  /* ===== 签署相关（电子化签署方案） ===== */
  /** 签署方式 */
  signMethod?: SignMethod
  /** 使用的合同模板ID */
  templateId?: string
  /** 模板版本号 */
  templateVersion?: string
  /** 合同文档hash（防篡改） */
  documentHash?: string
  /** 未签署PDF URL */
  unsignedPdfUrl?: string
  /** 已签署PDF URL */
  signedPdfUrl?: string
  /** 公司签署时间 */
  companySignTime?: string
  /** 员工签署时间 */
  employeeSignTime?: string
  /** 审批人 */
  approvedBy?: string
  /** 审批时间 */
  approvalTime?: string

  /* ===== 载体类型（纸质/电子双档管理，新增） ===== */
  /** 合同载体类型（默认 electronic，兼容历史数据） */
  carrier?: ContractCarrier
  /** 纸质合同归档信息（carrier 为 paper 时有值） */
  paperArchive?: PaperArchiveInfo
  /** 电子合同归档信息（carrier 为 electronic 时有值） */
  electronicArchive?: ElectronicArchiveInfo
  /** 安全验证信息（电子签署时记录） */
  securityVerification?: SecurityVerificationInfo

  /* ===== 生命周期关联 ===== */
  /** 生命周期关联信息 */
  lifecycleLink?: ContractLifecycleLink

  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string

  /* ===== 用工与薪资信息（扩展字段，用于详情展示） ===== */
  /** 用工形式 */
  employmentForm?: string
  /** 工作地点 */
  workLocation?: string
  /** 试用期（月） */
  probationPeriod?: number | string
  /** 试用期薪资 */
  probationSalary?: number | string
  /** 基本薪资 */
  baseSalary?: number | string
  /** 薪资结构 */
  salaryStructure?: string
  /** 创建人 */
  createdBy?: string
  /** 纸质合同档案编号（冗余字段，兼容旧数据展示） */
  paperArchiveNo?: string
  /** 纸质合同存放位置（冗余字段，兼容旧数据展示） */
  paperArchiveLocation?: string
}

/** 合同表单数据 */
export interface ContractFormData {
  employeeId: string
  contractType: ContractType
  contractNo: string
  termType?: LaborContractTerm
  startDate: string
  endDate?: string
  probationEndDate?: string
  signDate?: string
  attachments?: string[]
  /** 合同载体类型（新增） */
  carrier?: ContractCarrier
  /** 纸质合同档案编号（carrier 为 paper 时填写） */
  paperArchiveNo?: string
  /** 纸质合同存放位置（carrier 为 paper 时填写） */
  paperArchiveLocation?: string
  /** 电子合同是否允许打印（carrier 为 electronic 时填写，默认 true） */
  electronicAllowPrint?: boolean

  /* ===== 劳务派遣专属字段 ===== */
  dispatchCompanyName?: string
  dispatchCompanyCode?: string
  dispatchPeriod?: number
  dispatchPositionCategory?: string
  dispatchManagementFeeRate?: number

  /* ===== 劳务外包专属字段 ===== */
  outsourcingCompanyName?: string
  outsourcingCompanyCode?: string
  outsourcingServiceScope?: string
  outsourcingSettlementMethod?: string

  /* ===== 超龄用工协议专属字段 ===== */
  insurancePolicyNo?: string
  insuranceExpiryDate?: string
  medicalExamDate?: string
  medicalExamResult?: string
  retirementDate?: string
  pensionLocation?: string
  positionAdaptationNote?: string

  /* ===== 兼职协议专属字段 ===== */
  hourlyRate?: number
  weeklyHours?: number
  dailyMaxHours?: number
  settlementCycle?: number

  /* ===== 实习协议专属字段 ===== */
  internSchool?: string
  internMajor?: string
  internPeriod?: number
  internSubsidy?: number
  internSchoolContact?: string

  /* ===== 保密协议专属字段 ===== */
  confidentialityScope?: string
  confidentialityPeriod?: number
  confidentialityFee?: number
  confidentialityPenalty?: number

  /* ===== 竞业限制协议专属字段 ===== */
  nonCompeteCompensation?: number
  nonCompetePeriod?: number
  nonCompeteScope?: string
  nonCompetePenalty?: number

  /** 使用的合同模板ID（起草时选择） */
  templateId?: string
  remark?: string
}

/** 合同查询参数 */
export interface ContractQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  contractType?: ContractType
  status?: ContractStatus
  archiveStatus?: ContractArchiveStatus
  /** 合同载体类型筛选（新增） */
  carrier?: ContractCarrier
  startDateFrom?: string
  startDateTo?: string
  endDateFrom?: string
  endDateTo?: string
}

/** 合同续签数据 */
export interface ContractRenewData {
  contractId: string
  newStartDate: string
  newEndDate?: string
  newTermType?: LaborContractTerm
  remark?: string
  /** 是否自动生成新合同正文（默认true） */
  autoGenerateDocument?: boolean
}

/** 合同正文生成请求数据 */
export interface ContractGenerateDocumentData {
  /** 合同ID */
  contractId: string
  /** 模板ID */
  templateId: string
  /** 变量键值对 */
  variables: Record<string, string | number>
}

/** 合同审批提交数据 */
export interface ContractSubmitApprovalData {
  /** 合同ID */
  contractId: string
  /** 审批人 */
  approver?: string
  /** 审批备注 */
  remark?: string
}

/** 合同终止数据 */
export interface ContractTerminateData {
  contractId: string
  terminateDate: string
  terminateReason: string
  remark?: string
}

/* ============================================================
 * 合同正文管理（电子化签署方案）
 * ============================================================ */

/** 签署方式 */
export type SignMethod = 'electronic' | 'paper'

/** 签署方类型 */
export type SignatoryType = 'company' | 'employee'

/**
 * 签署记录
 * 记录每次签署动作（公司签/员工签）的详细信息，用于司法存证
 */
export interface SignatureRecord {
  /** 记录ID */
  id: string
  /** 关联合同ID */
  contractId: string
  /** 签署方类型 */
  signatoryType: SignatoryType
  /** 签署方名称（公司名称/员工姓名） */
  signatoryName: string
  /** 签署方式 */
  signMethod: SignMethod
  /** 签署时间（ISO 8601） */
  signTime: string
  /** 签署IP地址 */
  signIp?: string
  /** 签署设备信息 */
  signDevice?: string
  /** 签名图片Base64（手写签名/印章图片） */
  signatureImage?: string
  /** 验证码（电子签署时的短信验证码） */
  verifyCode?: string
  /** 验证码发送手机号（脱敏） */
  verifyPhone?: string
  /** 合同PDF的hash值（防篡改） */
  contractHash?: string
  /** 签署状态 */
  status: 'success' | 'failed' | 'pending'
  /** 失败原因 */
  failReason?: string
}

/* ============================================================
 * 签署流程管理（Phase 2）
 * ============================================================ */

/** 签署记录状态 */
export type SignatureStatus = 'pending' | 'signed' | 'rejected'

/** 签署记录状态选项 */
export const SignatureStatusOptions: { value: SignatureStatus; label: string }[] = [
  { value: 'pending', label: '待签署' },
  { value: 'signed', label: '已签署' },
  { value: 'rejected', label: '已拒绝' },
]

/** 签署记录状态到StatusTag的映射 */
export const SignatureStatusTagMap: Record<SignatureStatus, string> = {
  pending: 'pending',
  signed: 'active',
  rejected: 'error',
}

/** 签署方详情（看板用） */
export interface SignerDetail {
  /** 签署人类型 */
  signerType: SignatoryType
  /** 签署人姓名 */
  signerName: string
  /** 签署状态 */
  status: SignatureStatus
  /** 签署时间 */
  signTime?: string
  /** 签署IP */
  signIp?: string
  /** 签署设备 */
  signDevice?: string
  /** 拒绝原因（status 为 rejected 时有值） */
  rejectReason?: string
}

/** 签署状态看板数据 */
export interface SignatureStatusDTO {
  /** 合同ID */
  contractId: string
  /** 合同编号 */
  contractNo: string
  /** 员工姓名 */
  employeeName: string
  /** 总签署人数 */
  totalSigners: number
  /** 已签署人数 */
  signedCount: number
  /** 待签署人数 */
  pendingCount: number
  /** 已拒绝人数 */
  rejectedCount: number
  /** 签署进度百分比 */
  progressPercent: number
  /** 各方签署详情 */
  signers: SignerDetail[]
}

/** 签署请求表单 */
export interface ContractSignRequestForm {
  /** 签署人类型 */
  signerType: SignatoryType
  /** 签名数据（Base64） */
  signatureData: string
  /** 验证码 */
  verifyCode: string
  /** 签署IP */
  signIp?: string
  /** 签署设备 */
  signDevice?: string
}

/** 拒绝签署表单 */
export interface ContractRejectForm {
  /** 签署人类型 */
  signerType: SignatoryType
  /** 拒绝原因 */
  reason: string
}

/** 验证码响应 */
export interface VerifyCodeResponse {
  /** 验证码 */
  verifyCode: string
  /** 过期时间 */
  expireTime: string
}

/** 合同正文来源类型 */
export type DocumentSourceType =
  | 'template'      // 从模板生成
  | 'manual'        // 手动创建
  | 'renewal'       // 续签生成
  | 'import'        // 外部导入
  | 'edit'          // 编辑修改

/** 合同正文来源类型选项 */
export const DocumentSourceTypeOptions: { value: DocumentSourceType; label: string }[] = [
  { value: 'template', label: '模板生成' },
  { value: 'manual', label: '手动创建' },
  { value: 'renewal', label: '续签生成' },
  { value: 'import', label: '外部导入' },
  { value: 'edit', label: '编辑修改' },
]

/** 合同正文来源类型到StatusTag的映射 */
export const DocumentSourceTypeTagMap: Record<DocumentSourceType, string> = {
  template: 'primary',
  manual: 'info',
  renewal: 'success',
  import: 'warning',
  edit: 'default',
}

/** 合同正文文档 */
export interface ContractDocument {
  /** 文档版本ID（版本控制用） */
  documentId?: string
  /** 关联合同ID */
  contractId: string
  /** 合同正文HTML内容（含变量替换后的最终内容） */
  htmlContent: string
  /** 未签署PDF URL */
  unsignedPdfUrl?: string
  /** 已签署PDF URL（双方签署完成后生成） */
  signedPdfUrl?: string
  /** 使用的模板ID */
  templateId?: string
  /** 模板版本号 */
  templateVersion?: string
  /** 文档hash（用于防篡改比对） */
  documentHash?: string
  /** 生成时间 */
  generatedAt?: string
  /** 版本号（每次编辑递增） */
  version?: number
  /** 来源类型 */
  sourceType?: DocumentSourceType
  /** 来源描述（如"从模板 XXX 生成"、"续签自合同 XXX"） */
  sourceDesc?: string
  /** 创建人 */
  createdBy?: string
  /** 创建时间 */
  createTime?: string
  /** 最后修改人 */
  updatedBy?: string
  /** 最后修改时间 */
  updateTime?: string
  /** 是否为当前版本（版本控制用） */
  isCurrent?: boolean
  /** 修改备注 */
  editRemark?: string
}

/** 合同正文编辑表单数据 */
export interface ContractDocumentEditForm {
  /** 合同ID */
  contractId: string
  /** 合同正文HTML内容 */
  htmlContent: string
  /** 修改备注 */
  editRemark?: string
}

/* ============================================================
 * 合同载体类型（纸质/电子双档管理，新增）
 * ============================================================ */

/** 合同载体类型 */
export type ContractCarrier = 'paper' | 'electronic'

/** 合同载体类型选项 */
export const ContractCarrierOptions: { value: ContractCarrier; label: string }[] = [
  { value: 'paper', label: '纸质合同' },
  { value: 'electronic', label: '电子合同' },
]

/** 合同载体类型到StatusTag的映射 */
export const ContractCarrierTagMap: Record<ContractCarrier, string> = {
  paper: 'warning',
  electronic: 'primary',
}

/** 纸质合同归档信息 */
export interface PaperArchiveInfo {
  /** 扫描件URL */
  scanFileUrl?: string
  /** 扫描件文件名 */
  scanFileName?: string
  /** 扫描归档时间 */
  scanTime?: string
  /** 扫描操作人 */
  scanOperator?: string
  /** 纸质原件存放位置（如"档案柜A-3"） */
  archiveLocation?: string
  /** 档案编号 */
  archiveNumber?: string
  /** 档案编号（别名，兼容前端展示） */
  archiveNo?: string
}

/** 电子合同归档信息 */
export interface ElectronicArchiveInfo {
  /** 电子合同PDF URL */
  pdfUrl?: string
  /** 已签署PDF URL */
  signedPdfUrl?: string
  /** 使用的合同模板ID */
  templateId?: string
  /** 下载次数 */
  downloadCount?: number
  /** 最后下载时间 */
  lastDownloadTime?: string
  /** 是否允许打印 */
  allowPrint: boolean
  /** 是否带水印 */
  watermark?: boolean
}

/** 安全验证信息（电子签署时记录） */
export interface SecurityVerificationInfo {
  /** 验证方式 */
  method: 'sms' | 'email' | 'face'
  /** 验证时间 */
  verifiedAt?: string
  /** 验证方 */
  verifiedBy?: string
  /** 签署IP地址 */
  ipAddress?: string
  /** 设备信息 */
  deviceInfo?: string
  /** 合同内容SHA-256哈希（防篡改） */
  hashValue?: string
}

/* ============================================================
 * 员工端签署任务（推送至 employee-frontend 待办）
 * ============================================================ */

/** 签署任务类型 */
export type SignTaskType = 'sign' | 'cosign' | 'confirm'

/** 签署任务状态 */
export type SignTaskStatus = 'pending' | 'completed' | 'rejected' | 'expired'

/** 签署任务状态选项 */
export const SignTaskStatusOptions: { value: SignTaskStatus; label: string }[] = [
  { value: 'pending', label: '待签署' },
  { value: 'completed', label: '已完成' },
  { value: 'rejected', label: '已拒绝' },
  { value: 'expired', label: '已过期' },
]

/** 签署任务状态到StatusTag的映射 */
export const SignTaskStatusTagMap: Record<SignTaskStatus, string> = {
  pending: 'pending',
  completed: 'active',
  rejected: 'error',
  expired: 'inactive',
}

/** 签署任务（员工端待办） */
export interface SignTask {
  /** 任务ID */
  taskId: string
  /** 关联合同ID */
  contractId: string
  /** 合同编号 */
  contractNo: string
  /** 员工ID */
  employeeId: string
  /** 员工姓名 */
  employeeName: string
  /** 任务类型 */
  taskType: SignTaskType
  /** 任务状态 */
  status: SignTaskStatus
  /** 创建时间 */
  createdAt: string
  /** 截止时间 */
  expireAt: string
  /** 完成时间 */
  completedAt?: string
  /** 拒绝原因 */
  rejectReason?: string
  /** 通知渠道 */
  notifyChannels: ('sms' | 'email' | 'app')[]
}

/** 创建签署任务表单 */
export interface CreateSignTaskForm {
  /** 员工ID */
  employeeId: string
  /** 任务类型 */
  taskType: SignTaskType
  /** 截止时间 */
  expireAt: string
  /** 通知渠道 */
  notifyChannels: ('sms' | 'email' | 'app')[]
}

/** 公司方签署表单（3步骤对话框提交） */
export interface CompanySignForm {
  /** 验证码 */
  verifyCode: string
  /** 印章图片Base64 */
  sealImage: string
  /** 签署声明已阅读同意 */
  agreed: boolean
  /** 签署IP（自动获取） */
  signIp?: string
  /** 签署设备信息（自动获取） */
  signDevice?: string
}

/** 合同验真表单 */
export interface ContractVerifyForm {
  /** 合同编号 */
  contractNo: string
  /** 哈希前8位 */
  hashPrefix: string
}

/** 合同验真结果 */
export interface ContractVerifyResult {
  /** 验真是否成功 */
  verified: boolean
  /** 合同编号 */
  contractNo?: string
  /** 员工姓名 */
  employeeName?: string
  /** 合同类型 */
  contractType?: ContractType
  /** 签署时间 */
  signDate?: string
  /** 公司签署时间 */
  companySignTime?: string
  /** 员工签署时间 */
  employeeSignTime?: string
  /** 失败原因（verified 为 false 时有值） */
  failReason?: string
}

/* ============================================================
 * 合同模板管理
 * ============================================================ */

/** 模板状态 */
export type TemplateStatus = 'draft' | 'active' | 'inactive'

/** 模板状态选项 */
export const TemplateStatusOptions: { value: TemplateStatus; label: string }[] = [
  { value: 'draft', label: '草稿' },
  { value: 'active', label: '启用' },
  { value: 'inactive', label: '停用' },
]

/** 模板状态到StatusTag的映射 */
export const TemplateStatusTagMap: Record<TemplateStatus, string> = {
  draft: 'default',
  active: 'active',
  inactive: 'inactive',
}

/** 模板变量定义 */
export interface TemplateVariable {
  /** 变量名（如 employeeName） */
  name: string
  /** 变量描述 */
  description: string
  /** 变量类型 */
  type: 'string' | 'number' | 'date' | 'currency' | 'text'
  /** 是否必填 */
  required: boolean
  /** 默认值 */
  defaultValue?: string
  /** 示例值 */
  example?: string
}

/**
 * 合同模板
 * 对应后端 ContractTemplate 实体，8种合同类型对应8套标准模板
 */
export interface ContractTemplate {
  /** 模板ID */
  id: string
  /** 模板编码（如 STANDARD_LABOR_CONTRACT） */
  templateCode: string
  /** 模板名称 */
  templateName: string
  /** 适用合同类型 */
  contractType: ContractType
  /** 模板版本号 */
  version: string
  /** 模板状态 */
  status: TemplateStatus
  /** 模板HTML内容（含 {{var}} 占位符） */
  templateContent: string
  /** 模板变量定义 */
  templateVariables: TemplateVariable[]
  /** 模板描述 */
  description?: string
  /** 创建人 */
  createdBy?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 合同模板表单数据 */
export interface ContractTemplateFormData {
  templateCode: string
  templateName: string
  contractType: ContractType
  version: string
  templateContent: string
  templateVariables: TemplateVariable[]
  description?: string
}

/** 合同模板查询参数 */
export interface ContractTemplateQueryParams {
  page?: number
  pageSize?: number
  contractType?: ContractType
  status?: TemplateStatus
  keyword?: string
}

/** 合同模板预览参数 */
export interface ContractTemplatePreviewData {
  /** 模板ID */
  templateId: string
  /** 预览变量值 */
  variables: Record<string, string | number>
}

/* ============================================================
 * 合同审批流
 * ============================================================ */

/** 审批动作 */
export type ApprovalAction = 'submit' | 'approve' | 'reject' | 'withdraw'

/** 审批状态 */
export type ApprovalStatus = 'pending' | 'approved' | 'rejected' | 'withdrawn'

/** 合同审批记录 */
export interface ContractApprovalRecord {
  /** 记录ID */
  id: string
  /** 关联合同ID */
  contractId: string
  /** 审批节点名称（如 HR初审、部门经理复审、法务终审） */
  nodeName: string
  /** 审批节点顺序 */
  nodeOrder: number
  /** 审批人ID */
  approverId?: string
  /** 审批人姓名 */
  approverName?: string
  /** 审批动作 */
  action: ApprovalAction
  /** 审批状态 */
  status: ApprovalStatus
  /** 审批意见 */
  comment?: string
  /** 审批时间 */
  approveTime?: string
}

/* ============================================================
 * 合同生命周期关联（与入职/离职/内部调整联动）
 * ============================================================ */

/** 入职档案状态（与后端 OnboardingArchive 状态机对齐） */
export type OnboardingArchiveStatus =
  | 'CREATED'              // 已创建
  | 'PENDING_HR'           // 待HR形式审查
  | 'PENDING_SUBSTANTIVE'  // 待实质审查
  | 'APPROVED'             // 审批通过
  | 'CONTRACT_PENDING'     // 待签合同
  | 'CONTRACT_SIGNED'      // 合同已签
  | 'REGISTERED'           // 已注册入职
  | 'REJECTED'             // 审批驳回

/** 合同生命周期关联信息 */
export interface ContractLifecycleLink {
  /** 关联入职档案ID */
  archiveId?: string
  /** 入职档案状态 */
  archiveStatus?: OnboardingArchiveStatus
  /** 触发来源 */
  triggerSource: 'onboarding' | 'renewal' | 'transfer' | 'resignation' | 'manual'
  /** 触发来源描述 */
  triggerSourceDesc: string
  /** 关联离职记录ID */
  resignationId?: string
  /** 离职日期 */
  resignationDate?: string
  /** 是否触发竞业限制生效 */
  triggerNonCompete?: boolean
  /** 是否触发保密协议延续 */
  triggerConfidentiality?: boolean
}

/* ============================================================
 * 合同变更与补充协议
 * ============================================================ */

/** 合同变更类型 */
export type ContractChangeType =
  | 'position_change'     // 岗位/部门变更
  | 'term_change'         // 合同期限变更
  | 'supplementary'       // 补充协议

/** 合同变更类型选项 */
export const ContractChangeTypeOptions: { value: ContractChangeType; label: string }[] = [
  { value: 'position_change', label: '岗位/部门变更' },
  { value: 'term_change', label: '合同期限变更' },
  { value: 'supplementary', label: '补充协议' },
]

/** 合同变更类型到StatusTag的映射 */
export const ContractChangeTypeTagMap: Record<ContractChangeType, string> = {
  position_change: 'info',
  term_change: 'warning',
  supplementary: 'primary',
}

/** 合同变更状态 */
export type ContractChangeStatus = 'draft' | 'pending_approval' | 'approved' | 'rejected' | 'signed' | 'active'

/** 合同变更状态选项 */
export const ContractChangeStatusOptions: { value: ContractChangeStatus; label: string }[] = [
  { value: 'draft', label: '草稿' },
  { value: 'pending_approval', label: '审批中' },
  { value: 'approved', label: '已批准' },
  { value: 'rejected', label: '已驳回' },
  { value: 'signed', label: '已签署' },
  { value: 'active', label: '已生效' },
]

/** 合同变更状态到StatusTag的映射 */
export const ContractChangeStatusTagMap: Record<ContractChangeStatus, string> = {
  draft: 'default',
  pending_approval: 'pending',
  approved: 'info',
  rejected: 'error',
  signed: 'active',
  active: 'active',
}

/** 合同变更记录 */
export interface ContractChangeRecord {
  /** 变更ID */
  id: string
  /** 关联原合同ID */
  contractId: string
  /** 变更类型 */
  changeType: ContractChangeType
  /** 变更标题（如"岗位变更协议"、"薪资调整补充协议"） */
  title: string
  /** 变更状态 */
  status: ContractChangeStatus
  /** 变更前内容摘要 */
  beforeSummary: string
  /** 变更后内容摘要 */
  afterSummary: string
  /** 变更详情（结构化数据） */
  changeDetails: ContractChangeDetail[]
  /** 变更原因 */
  reason: string
  /** 关联补充协议模板ID */
  templateId?: string
  /** 补充协议正文HTML */
  documentHtml?: string
  /** 审批人 */
  approvedBy?: string
  /** 审批时间 */
  approvalTime?: string
  /** 生效日期 */
  effectiveDate?: string
  /** 创建人 */
  createdBy?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 合同变更详情项 */
export interface ContractChangeDetail {
  /** 变更字段名（如 departmentName, positionName, endDate） */
  field: string
  /** 字段中文名 */
  fieldLabel: string
  /** 变更前值 */
  beforeValue: string
  /** 变更后值 */
  afterValue: string
}

/** 合同变更创建表单 */
export interface ContractChangeCreateForm {
  /** 关联原合同ID */
  contractId: string
  /** 变更类型 */
  changeType: ContractChangeType
  /** 变更标题 */
  title: string
  /** 变更原因 */
  reason: string
  /** 变更详情 */
  changeDetails: ContractChangeDetail[]
  /** 生效日期 */
  effectiveDate: string
  /** 是否自动生成补充协议正文 */
  autoGenerateDocument: boolean
}

/* ============================================================
 * 合同自动生成（入职触发）
 * ============================================================ */

/** 合同自动生成请求 */
export interface ContractAutoGenerateRequest {
  /** 员工ID */
  employeeId: string
  /** 入职档案ID */
  archiveId?: string
  /** 合同类型（根据用工关系自动推断，也可手动指定） */
  contractType?: ContractType
  /** 是否自动生成正文 */
  autoGenerateDocument: boolean
}

/** 合同自动生成结果 */
export interface ContractAutoGenerateResult {
  /** 生成的合同ID */
  contractId: string
  /** 合同编号 */
  contractNo: string
  /** 使用的模板ID */
  templateId: string
  /** 使用的模板名称 */
  templateName: string
  /** 是否自动生成了正文 */
  documentGenerated: boolean
  /** 提示信息 */
  message: string
}

/* ============================================================
 * 合同统计
 * ============================================================ */

/** 合同统计信息 */
export interface ContractStatistics {
  /** 总数 */
  total: number
  /** 待处理（草稿+审批中+待签署+公司已签+双方已签） */
  pending: number
  /** 执行中（含即将到期） */
  active: number
  /** 即将到期（30天内） */
  expiring: number
  /** 已结束（已到期+已终止+已续签+已归档） */
  closed: number
  /** 已归档 */
  archived: number
}
