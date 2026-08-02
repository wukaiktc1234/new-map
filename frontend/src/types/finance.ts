/**
 * 财务中心模块类型定义
 *
 * 说明：
 * - 本文件包含财务中心模块新API所需的类型定义
 * - 已有的财务类型分布在 finance-ledger.ts、finance-budget.ts、finance-cost.ts、
 *   finance-receivable.ts、finance-payable.ts、finance-fund.ts、finance-report.ts、
 *   finance-account-balance.ts、finance-electronic-voucher.ts、finance-workflow.ts、
 *   finance-integration.ts 等文件中
 * - 为避免命名冲突，新类型使用 Finance 前缀或更具描述性的名称
 */
import { IPage } from './pagination'

// ============================================================
// 通用类型
// ============================================================

/**
 * 分页响应泛型
 * 响应拦截器已自动提取 res.data，前端直接使用此结构
 */
export interface PageResponse<T> {
  /** 数据列表 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
  /** 总页数 */
  pages?: number
}

/** 兼容 IPage 类型别名 */
export type FinancePage<T> = IPage<T>

/** 基础分页查询参数 */
export interface FinancePageQuery {
  /** 当前页码（从1开始） */
  page?: number
  /** 每页条数 */
  size?: number
}

// ============================================================
// 会计科目
// ============================================================

/** 会计科目类型 */
export type SubjectType = 'asset' | 'liability' | 'equity' | 'cost' | 'income' | 'profit'

/** 余额方向 */
export type BalanceDirection = 'debit' | 'credit'

/** 会计科目状态 */
export type SubjectStatus = 'active' | 'inactive'

/**
 * 会计科目（新版，对应后端 /v1/subjects）
 * 注意：与 finance-ledger.ts 中的 LedgerAccountingSubject 不同，
 * 此处使用 FinanceSubject 以避免冲突
 */
export interface FinanceSubject {
  /** 科目ID */
  id: string
  /** 科目编码 */
  subjectCode: string
  /** 科目名称 */
  subjectName: string
  /** 科目类型 */
  subjectType: SubjectType
  /** 余额方向 */
  balanceDirection: BalanceDirection
  /** 层级 */
  level: number
  /** 父级科目ID */
  parentId?: string
  /** 父级科目名称 */
  parentName?: string
  /** 是否末级科目 */
  isLeaf?: boolean
  /** 余额（分） */
  balance?: number
  /** 状态 */
  status: SubjectStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 科目树节点 */
export interface SubjectTreeNode {
  /** 科目ID */
  id: string
  /** 科目编码 */
  subjectCode: string
  /** 科目名称 */
  subjectName: string
  /** 科目类型 */
  subjectType: SubjectType
  /** 余额方向 */
  balanceDirection: BalanceDirection
  /** 层级 */
  level: number
  /** 状态 */
  status: SubjectStatus
  /** 子节点 */
  children?: SubjectTreeNode[]
}

/** 会计科目表单数据 */
export interface SubjectFormData {
  id?: string
  subjectCode: string
  subjectName: string
  subjectType: SubjectType
  balanceDirection: BalanceDirection
  level: number
  parentId?: string
  status: SubjectStatus
  remark?: string
}

/** 会计科目查询表单 */
export interface SubjectQueryForm extends FinancePageQuery {
  subjectCode?: string
  subjectName?: string
  subjectType?: SubjectType
  status?: SubjectStatus
  parentId?: string
}

// ============================================================
// 凭证
// ============================================================

/** 凭证状态 */
export type VoucherStatus = 'draft' | 'audited' | 'posted' | 'cancelled'

/** 凭证类型 */
export type VoucherType = 'receipt' | 'payment' | 'transfer' | 'general'

/**
 * 凭证明细项
 */
export interface FinanceVoucherEntry {
  /** 明细ID */
  id?: string
  /** 凭证ID */
  voucherId?: string
  /** 科目ID */
  subjectId: string
  /** 科目编码 */
  subjectCode: string
  /** 科目名称 */
  subjectName: string
  /** 借方金额（分） */
  debitAmount: number
  /** 贷方金额（分） */
  creditAmount: number
  /** 摘要 */
  summary: string
}

/**
 * 财务凭证（新版，对应后端 /v1/vouchers）
 */
export interface FinanceVoucherNew {
  /** 凭证ID */
  id: string
  /** 凭证编号 */
  voucherNo: string
  /** 凭证类型 */
  voucherType: VoucherType
  /** 凭证日期 */
  voucherDate: string
  /** 摘要 */
  summary: string
  /** 借方合计（分） */
  debitTotal: number
  /** 贷方合计（分） */
  creditTotal: number
  /** 附件张数 */
  attachmentCount?: number
  /** 状态 */
  status: VoucherStatus
  /** 制单人 */
  creatorName: string
  /** 制单时间 */
  createTime: string
  /** 审核人 */
  auditorName?: string
  /** 审核时间 */
  auditTime?: string
  /** 凭证明细列表 */
  entries: FinanceVoucherEntry[]
  /** 备注 */
  remark?: string
  /** 更新时间 */
  updateTime?: string
}

/** 凭证表单数据 */
export interface VoucherFormData {
  id?: string
  voucherType: VoucherType
  voucherDate: string
  summary: string
  attachmentCount?: number
  entries: Array<{
    subjectId: string
    subjectCode: string
    subjectName: string
    debitAmount: number
    creditAmount: number
    summary: string
  }>
  remark?: string
}

/** 凭证查询表单 */
export interface VoucherQueryForm extends FinancePageQuery {
  voucherNo?: string
  voucherType?: VoucherType
  status?: VoucherStatus
  startDate?: string
  endDate?: string
  summary?: string
}

// ============================================================
// 财务记录
// ============================================================

/** 财务记录类型 */
export type FinanceRecordType = 'income' | 'expense'

/** 财务记录状态 */
export type FinanceRecordStatus = 'draft' | 'pending' | 'approved' | 'rejected'

/**
 * 财务记录（新版，对应后端 /v1/finance-records）
 */
export interface FinanceRecordNew {
  /** 记录ID */
  id: string
  /** 记录编号 */
  recordNo?: string
  /** 记录类型 */
  recordType: FinanceRecordType
  /** 分类 */
  category: string
  /** 金额（分） */
  amount: number
  /** 交易日期 */
  transactionDate: string
  /** 摘要 */
  description: string
  /** 状态 */
  status: FinanceRecordStatus
  /** 关联凭证ID */
  voucherId?: string
  /** 关联凭证号 */
  voucherNo?: string
  /** 申请人 */
  applicant?: string
  /** 审批人 */
  approver?: string
  /** 审批时间 */
  approveTime?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime?: string
}

/** 财务记录表单数据 */
export interface FinanceRecordFormData {
  id?: string
  recordType: FinanceRecordType
  category: string
  amount: number
  transactionDate: string
  description: string
  remark?: string
}

/** 财务记录查询表单 */
export interface FinanceRecordQueryForm extends FinancePageQuery {
  recordNo?: string
  recordType?: FinanceRecordType
  category?: string
  status?: FinanceRecordStatus
  startDate?: string
  endDate?: string
}

// ============================================================
// 预算
// ============================================================

/** 预算类型 */
export type BudgetType = 'operating' | 'procurement' | 'hr' | 'marketing' | 'capital'

/** 预算状态 */
export type BudgetStatus = 'draft' | 'approved' | 'executing' | 'closed'

/**
 * 预算（新版，对应后端 /v1/budgets）
 */
export interface FinanceBudget {
  /** 预算ID */
  id: string
  /** 预算名称 */
  budgetName: string
  /** 预算类型 */
  budgetType: BudgetType
  /** 预算年度 */
  year: number
  /** 预算月份 */
  month?: number
  /** 部门 */
  department?: string
  /** 预算金额（分） */
  budgetAmount: number
  /** 实际金额（分） */
  actualAmount: number
  /** 完成率 */
  executionRate?: number
  /** 状态 */
  status: BudgetStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 预算表单数据 */
export interface FinanceBudgetFormData {
  id?: string
  budgetName: string
  budgetType: BudgetType
  year: number
  month?: number
  department?: string
  budgetAmount: number
  remark?: string
}

/** 预算查询表单 */
export interface FinanceBudgetQueryForm extends FinancePageQuery {
  budgetName?: string
  budgetType?: BudgetType
  year?: number
  month?: number
  department?: string
  status?: BudgetStatus
}

/** 更新实际金额参数 */
export interface UpdateActualAmountDTO {
  actualAmount: number
}

// ============================================================
// 成本记录
// ============================================================

/** 成本类型 */
export type FinanceCostType = 'material' | 'labor' | 'rent' | 'energy' | 'marketing' | 'other'

/**
 * 成本记录（新版，对应后端 /v1/costs）
 */
export interface FinanceCostRecord {
  /** 成本记录ID */
  id: string
  /** 成本日期 */
  costDate: string
  /** 成本类型 */
  costType: FinanceCostType
  /** 成本类别名称 */
  categoryName: string
  /** 金额（分） */
  amount: number
  /** 预算金额（分） */
  budgetAmount?: number
  /** 偏差（分） */
  variance?: number
  /** 偏差率 */
  varianceRate?: string
  /** 说明 */
  description: string
  /** 部门 */
  department?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 成本记录表单数据 */
export interface FinanceCostFormData {
  id?: string
  costDate: string
  costType: FinanceCostType
  categoryName: string
  amount: number
  budgetAmount?: number
  description: string
  department?: string
  remark?: string
}

/** 成本记录查询表单 */
export interface FinanceCostQueryForm extends FinancePageQuery {
  costType?: FinanceCostType
  categoryName?: string
  startDate?: string
  endDate?: string
  department?: string
}

// ============================================================
// 应收账款
// ============================================================

/** 应收账款状态 */
export type ReceivableStatus = 'unpaid' | 'partial' | 'settled' | 'overdue'

/**
 * 应收账款（新版，对应后端 /v1/receivables）
 */
export interface FinanceReceivable {
  /** 应收账款ID */
  id: string
  /** 单据编号 */
  receivableNo: string
  /** 客户名称 */
  customerName: string
  /** 客户ID */
  customerId?: string
  /** 应收金额（分） */
  amount: number
  /** 已收金额（分） */
  receivedAmount: number
  /** 未收金额（分） */
  remainAmount: number
  /** 开票日期 */
  invoiceDate: string
  /** 到期日 */
  dueDate: string
  /** 账龄 */
  aging?: string
  /** 状态 */
  status: ReceivableStatus
  /** 关联发票号 */
  invoiceNo?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 应收账款表单数据 */
export interface FinanceReceivableFormData {
  id?: string
  customerName: string
  customerId?: string
  amount: number
  receivedAmount?: number
  invoiceDate: string
  dueDate: string
  invoiceNo?: string
  remark?: string
}

/** 应收账款查询表单 */
export interface FinanceReceivableQueryForm extends FinancePageQuery {
  receivableNo?: string
  customerName?: string
  status?: ReceivableStatus
  startDate?: string
  endDate?: string
}

// ============================================================
// 应付账款
// ============================================================

/** 应付账款状态 */
export type PayableStatus = 'unpaid' | 'partial' | 'settled' | 'overdue'

/**
 * 应付账款（新版，对应后端 /v1/payables）
 */
export interface FinancePayable {
  /** 应付账款ID */
  id: string
  /** 单据编号 */
  payableNo: string
  /** 供应商名称 */
  supplierName: string
  /** 供应商ID */
  supplierId?: string
  /** 关联采购入库单ID */
  stockinId?: string
  /** 关联采购入库单号 */
  stockinNo?: string
  /** 关联采购订单号 */
  orderNo?: string
  /** 应付金额（分） */
  amount: number
  /** 已付金额（分） */
  paidAmount: number
  /** 未付金额（分） */
  remainAmount: number
  /** 开票日期 */
  invoiceDate: string
  /** 到期日 */
  dueDate: string
  /** 账龄 */
  aging?: string
  /** 状态 */
  status: PayableStatus
  /** 关联发票号 */
  invoiceNo?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 应付账款表单数据 */
export interface FinancePayableFormData {
  id?: string
  supplierName: string
  supplierId?: string
  amount: number
  paidAmount?: number
  invoiceDate: string
  dueDate: string
  invoiceNo?: string
  remark?: string
}

/** 应付账款查询表单 */
export interface FinancePayableQueryForm extends FinancePageQuery {
  payableNo?: string
  supplierName?: string
  status?: PayableStatus
  startDate?: string
  endDate?: string
}

// ============================================================
// 发票
// ============================================================

/** 发票类型 */
export type FinanceInvoiceType = 'special' | 'normal' | 'electronic' | 'electronic_special'

/** 发票状态 */
export type FinanceInvoiceStatus = 'draft' | 'issued' | 'cancelled' | 'red_flushed'

/**
 * 发票（新版，对应后端 /v1/invoices）
 */
export interface FinanceInvoiceNew {
  /** 发票ID */
  id: string
  /** 发票编号 */
  invoiceNo: string
  /** 发票代码 */
  invoiceCode?: string
  /** 发票类型 */
  invoiceType: FinanceInvoiceType
  /** 开票日期 */
  invoiceDate: string
  /** 购方名称 */
  buyerName: string
  /** 购方税号 */
  buyerTaxNo?: string
  /** 销方名称 */
  sellerName: string
  /** 销方税号 */
  sellerTaxNo?: string
  /** 金额（不含税，分） */
  amountWithoutTax: number
  /** 税额（分） */
  taxAmount: number
  /** 价税合计（分） */
  totalAmount: number
  /** 税率 */
  taxRate?: number
  /** 状态 */
  status: FinanceInvoiceStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 发票表单数据 */
export interface FinanceInvoiceFormData {
  id?: string
  invoiceNo: string
  invoiceCode?: string
  invoiceType: FinanceInvoiceType
  invoiceDate: string
  buyerName: string
  buyerTaxNo?: string
  sellerName: string
  sellerTaxNo?: string
  amountWithoutTax: number
  taxAmount: number
  totalAmount: number
  taxRate?: number
  remark?: string
}

/** 发票查询表单 */
export interface FinanceInvoiceQueryForm extends FinancePageQuery {
  invoiceNo?: string
  invoiceType?: FinanceInvoiceType
  status?: FinanceInvoiceStatus
  startDate?: string
  endDate?: string
  buyerName?: string
  sellerName?: string
}

// ============================================================
// 银行账户
// ============================================================

/** 银行账户类型 */
export type BankAccountType = 'basic' | 'general' | 'payroll' | 'reserve' | 'other'

/** 银行账户状态 */
export type BankAccountStatus = 'active' | 'frozen' | 'closed'

/**
 * 银行账户（新版，对应后端 /v1/bank-accounts）
 */
export interface FinanceBankAccount {
  /** 账户ID */
  id: string
  /** 银行名称 */
  bankName: string
  /** 账户号 */
  accountNo: string
  /** 账户名称 */
  accountName: string
  /** 账户类型 */
  accountType: BankAccountType
  /** 余额（分） */
  balance: number
  /** 状态 */
  status: BankAccountStatus
  /** 开户日期 */
  openDate: string
  /** 最后交易时间 */
  lastTransactionTime?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 银行账户表单数据 */
export interface FinanceBankAccountFormData {
  id?: string
  bankName: string
  accountNo: string
  accountName: string
  accountType: BankAccountType
  openDate: string
  initialBalance?: number
  remark?: string
}

/** 银行账户查询表单 */
export interface FinanceBankAccountQueryForm extends FinancePageQuery {
  bankName?: string
  accountNo?: string
  accountName?: string
  accountType?: BankAccountType
  status?: BankAccountStatus
}

/** 更新余额参数 */
export interface UpdateBalanceDTO {
  balance: number
}

// ============================================================
// 资金流水
// ============================================================

/** 资金流水类型 */
export type FundFlowType = 'income' | 'expense'

/** 资金流水状态 */
export type FundFlowStatus = 'pending' | 'completed' | 'cancelled'

/**
 * 资金流水（对应后端 /v1/fund-flows）
 */
export interface FundFlow {
  /** 流水ID */
  id: string
  /** 流水编号 */
  flowNo: string
  /** 交易日期 */
  transactionDate: string
  /** 交易类型 */
  flowType: FundFlowType
  /** 金额（分） */
  amount: number
  /** 银行账户ID */
  bankAccountId: string
  /** 银行账户名称 */
  bankAccountName: string
  /** 对方单位 */
  counterparty: string
  /** 摘要 */
  summary: string
  /** 余额（分） */
  balance?: number
  /** 关联凭证号 */
  voucherNo?: string
  /** 状态 */
  status: FundFlowStatus
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime?: string
}

/** 资金流水表单数据 */
export interface FundFlowFormData {
  flowType: FundFlowType
  amount: number
  bankAccountId: string
  counterparty: string
  summary: string
  transactionDate: string
  voucherNo?: string
}

/** 资金流水查询表单 */
export interface FundFlowQueryForm extends FinancePageQuery {
  flowNo?: string
  flowType?: FundFlowType
  bankAccountId?: string
  startDate?: string
  endDate?: string
  counterparty?: string
  status?: FundFlowStatus
}

/** 资金流水统计 */
export interface FundFlowStatistics {
  /** 总收入（分） */
  totalIncome: number
  /** 总支出（分） */
  totalExpense: number
  /** 净额（分） */
  netAmount: number
  /** 交易笔数 */
  count: number
}

// ============================================================
// 会计期间
// ============================================================

/** 会计期间状态 */
export type AccountingPeriodStatus = 'open' | 'closing' | 'closed'

/**
 * 会计期间（对应后端 /v1/accounting-periods）
 */
export interface AccountingPeriod {
  /** 期间ID */
  id: string
  /** 期间名称（如：2026-05） */
  periodName: string
  /** 期间类型：1=月度 2=季度 3=年度 */
  periodType?: number
  /** 年度 */
  year: number
  /** 月份（periodType=1 时有值） */
  month?: number
  /** 季度（periodType=2 时有值） */
  quarter?: number
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** 状态 */
  status: AccountingPeriodStatus
  /** 凭证数 */
  voucherCount?: number
  /** 结账人 */
  closedBy?: string
  /** 结账时间 */
  closedTime?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 会计期间查询表单 */
export interface AccountingPeriodQueryForm extends FinancePageQuery {
  /** 期间类型：1=月度 2=季度 3=年度 */
  periodType?: number
  /** 年度 */
  year?: number
  /** 状态 */
  status?: AccountingPeriodStatus
}

/** 试算平衡结果 */
export interface TrialBalanceResult {
  /** 是否平衡 */
  balanced: boolean
  /** 借方合计（分） */
  totalDebit: number
  /** 贷方合计（分） */
  totalCredit: number
  /** 差额（分） */
  difference: number
  /** 明细列表 */
  details: Array<{
    subjectCode: string
    subjectName: string
    debitAmount: number
    creditAmount: number
  }>
}

/** 结账清单项 */
export interface ClosingChecklistItem {
  /** 检查项名称 */
  itemName: string
  /** 是否完成 */
  completed: boolean
  /** 说明 */
  description?: string
}

/** 损益结转结果 */
export interface ProfitTransferResult {
  /** 是否成功 */
  success: boolean
  /** 结转凭证号 */
  voucherNo?: string
  /** 结转金额（分） */
  transferAmount: number
  /** 消息 */
  message: string
}

// ============================================================
// 摘要模板
// ============================================================

/**
 * 摘要模板（对应后端 /v1/summary-templates）
 */
export interface SummaryTemplate {
  /** 模板ID */
  id: string
  /** 模板名称 */
  templateName: string
  /** 摘要内容 */
  summaryContent: string
  /** 模板分类 */
  category?: string
  /** 使用次数 */
  usageCount: number
  /** 状态 */
  status: 'active' | 'inactive'
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 摘要模板表单数据 */
export interface SummaryTemplateFormData {
  id?: string
  templateName: string
  summaryContent: string
  category?: string
  status?: 'active' | 'inactive'
}

/** 摘要模板查询表单 */
export interface SummaryTemplateQueryForm extends FinancePageQuery {
  templateName?: string
  category?: string
  status?: 'active' | 'inactive'
  keyword?: string
}

// ============================================================
// 结转模板
// ============================================================

/**
 * 结转模板（新版，对应后端 /v1/transfer-templates）
 */
export interface FinanceTransferTemplate {
  /** 模板ID */
  id: string
  /** 模板名称 */
  templateName: string
  /** 模板类型 */
  templateType: string
  /** 执行频率 */
  frequency: string
  /** 借方科目 */
  debitSubject?: string
  /** 贷方科目 */
  creditSubject?: string
  /** 金额公式 */
  formula?: string
  /** 是否启用 */
  enabled: boolean
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 结转模板表单数据 */
export interface FinanceTransferTemplateFormData {
  id?: string
  templateName: string
  templateType: string
  frequency: string
  debitSubject?: string
  creditSubject?: string
  formula?: string
  enabled?: boolean
  remark?: string
}

/** 结转模板查询表单 */
export interface FinanceTransferTemplateQueryForm extends FinancePageQuery {
  templateName?: string
  templateType?: string
  frequency?: string
  enabled?: boolean
}

// ============================================================
// 税率配置
// ============================================================

/** 税种类型 */
export type TaxType = 'vat' | 'corporate_income' | 'urban_construction' | 'education_surcharge' | 'local_education' | 'stamp' | 'individual_income'

/**
 * 税率配置（对应后端 /v1/tax-rate-configs）
 */
export interface TaxRateConfig {
  /** 配置ID */
  id: string
  /** 税种 */
  taxType: TaxType
  /** 税种名称 */
  taxTypeName: string
  /** 税率（百分比） */
  taxRate: number
  /** 生效日期 */
  effectiveDate: string
  /** 失效日期 */
  expiryDate?: string
  /** 是否有效 */
  effective: boolean
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 税率配置表单数据 */
export interface TaxRateConfigFormData {
  id?: string
  taxType: TaxType
  taxTypeName: string
  taxRate: number
  effectiveDate: string
  expiryDate?: string
  remark?: string
}

/** 税率配置查询表单 */
export interface TaxRateConfigQueryForm extends FinancePageQuery {
  taxType?: TaxType
  effective?: boolean
}

// ============================================================
// 标准成本卡
// ============================================================

/**
 * 标准成本卡（对应后端 /v1/standard-cost-cards）
 */
export interface StandardCostCard {
  /** 成本卡ID */
  id: string
  /** 菜品ID */
  dishId: string
  /** 菜品名称 */
  dishName: string
  /** 标准成本（分） */
  standardCost: number
  /** 实际成本（分） */
  actualCost?: number
  /** 偏差（分） */
  variance?: number
  /** 偏差率 */
  varianceRate?: string
  /** 预警状态 */
  warningStatus: 'normal' | 'warning' | 'over'
  /** 配料明细 */
  ingredients?: Array<{
    materialId: string
    materialName: string
    quantity: number
    unit: string
    unitPrice: number
    amount: number
  }>
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 标准成本卡表单数据 */
export interface StandardCostCardFormData {
  id?: string
  dishId: string
  dishName: string
  standardCost: number
  ingredients?: Array<{
    materialId: string
    materialName: string
    quantity: number
    unit: string
    unitPrice: number
    amount: number
  }>
  remark?: string
}

/** 标准成本卡查询表单 */
export interface StandardCostCardQueryForm extends FinancePageQuery {
  dishId?: string
  dishName?: string
  warningStatus?: 'normal' | 'warning' | 'over'
}

/** 更新预警状态参数 */
export interface UpdateWarningStatusDTO {
  warningStatus: 'normal' | 'warning' | 'over'
}

// ============================================================
// 审批流配置
// ============================================================

/** 单据类型 */
export type ApprovalDocumentType = 'voucher' | 'expense' | 'invoice' | 'report' | 'budget' | 'payment'

/**
 * 审批流配置（对应后端 /v1/approval-flow-configs）
 */
export interface ApprovalFlowConfig {
  /** 配置ID */
  id: string
  /** 配置名称 */
  configName: string
  /** 单据类型 */
  documentType: ApprovalDocumentType
  /** 审批节点列表 */
  nodes: Array<{
    nodeId: string
    nodeName: string
    approverIds: string[]
    approverNames: string[]
    order: number
  }>
  /** 是否启用 */
  enabled: boolean
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 审批流配置表单数据 */
export interface ApprovalFlowConfigFormData {
  id?: string
  configName: string
  documentType: ApprovalDocumentType
  nodes: Array<{
    nodeId?: string
    nodeName: string
    approverIds: string[]
    order: number
  }>
  enabled?: boolean
  remark?: string
}

/** 审批流配置查询表单 */
export interface ApprovalFlowConfigQueryForm extends FinancePageQuery {
  configName?: string
  documentType?: ApprovalDocumentType
  enabled?: boolean
}

// ============================================================
// 财务审计日志
// ============================================================

/** 审计操作类型 */
export type AuditOperationType = 'create' | 'update' | 'delete' | 'audit' | 'post' | 'close' | 'export' | 'other'

/**
 * 财务审计日志（对应后端 /v1/finance-audit-logs）
 */
export interface FinanceAuditLog {
  /** 日志ID */
  id: string
  /** 操作类型 */
  operationType: AuditOperationType
  /** 操作模块 */
  module: string
  /** 业务ID */
  businessId?: string
  /** 业务编号 */
  businessNo?: string
  /** 操作描述 */
  description: string
  /** 操作人ID */
  operatorId: string
  /** 操作人名称 */
  operatorName: string
  /** 操作时间 */
  operationTime: string
  /** IP地址 */
  ipAddress?: string
  /** 操作前数据 */
  beforeData?: string
  /** 操作后数据 */
  afterData?: string
}

/** 审计日志查询表单 */
export interface FinanceAuditLogQueryForm extends FinancePageQuery {
  operationType?: AuditOperationType
  module?: string
  operatorName?: string
  startDate?: string
  endDate?: string
  businessNo?: string
}

// ============================================================
// 财务报表
// ============================================================

/** 报表类型 */
export type FinanceReportType = 'profit' | 'balance' | 'cash_flow'

/** 报表期间 */
export type FinanceReportPeriod = 'monthly' | 'quarterly' | 'yearly'

/** 报表查询参数 */
export interface FinanceReportQueryForm {
  /** 报表类型 */
  reportType: FinanceReportType
  /** 开始期间 */
  startDate: string
  /** 结束期间 */
  endDate: string
  /** 期间类型 */
  period?: FinanceReportPeriod
}

/** 利润表数据 */
export interface ProfitStatementData {
  /** 报表期间 */
  period: string
  /** 营业收入（分） */
  operatingIncome: number
  /** 营业成本（分） */
  operatingCost: number
  /** 毛利（分） */
  grossProfit: number
  /** 毛利率 */
  grossProfitRate?: string
  /** 期间费用（分） */
  periodExpense: number
  /** 营业利润（分） */
  operatingProfit: number
  /** 利润总额（分） */
  totalProfit: number
  /** 所得税费用（分） */
  incomeTaxExpense: number
  /** 净利润（分） */
  netProfit: number
  /** 明细列表 */
  details: Array<{
    item: string
    currentPeriod: number
    previousPeriod: number
    changeRate?: string
  }>
}

/** 资产负债表数据 */
export interface BalanceSheetData {
  /** 报表日期 */
  reportDate: string
  /** 资产总计（分） */
  totalAssets: number
  /** 负债总计（分） */
  totalLiabilities: number
  /** 所有者权益总计（分） */
  totalEquity: number
  /** 资产明细 */
  assets: Array<{
    item: string
    amount: number
  }>
  /** 负债明细 */
  liabilities: Array<{
    item: string
    amount: number
  }>
  /** 权益明细 */
  equity: Array<{
    item: string
    amount: number
  }>
}

/** 现金流量表数据 */
export interface CashFlowStatementData {
  /** 报表期间 */
  period: string
  /** 经营活动现金流入（分） */
  operatingInflow: number
  /** 经营活动现金流出（分） */
  operatingOutflow: number
  /** 经营活动现金流量净额（分） */
  operatingNet: number
  /** 投资活动现金流入（分） */
  investingInflow: number
  /** 投资活动现金流出（分） */
  investingOutflow: number
  /** 投资活动现金流量净额（分） */
  investingNet: number
  /** 筹资活动现金流入（分） */
  financingInflow: number
  /** 筹资活动现金流出（分） */
  financingOutflow: number
  /** 筹资活动现金流量净额（分） */
  financingNet: number
  /** 现金净增加额（分） */
  netIncrease: number
}

// ============================================================
// 保留原有类型（向后兼容）
// ============================================================

export interface FinanceVoucher {
  id: number
  voucherNo: string
  voucherType: string
  businessType: string
  businessId: number
  debitAmount: number
  creditAmount: number
  currency: string
  voucherDate: string
  status: string
  remark: string
  createdAt: string
  updatedAt: string
}

export interface FinanceVoucherPageParams {
  current: number
  size: number
  voucherNo?: string
  voucherType?: string
  businessType?: string
  status?: string
  startDate?: string
  endDate?: string
}

export interface AccountingSubject {
  id: number
  code: string
  name: string
  type: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'INCOME' | 'EXPENSE'
  parentId?: number
  level: number
  balance: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface FinanceReport {
  id: number
  reportType: 'PROFIT' | 'BALANCE' | 'CASH_FLOW'
  reportDate: string
  period: 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  data: unknown
  status: 'DRAFT' | 'PUBLISHED'
  createdBy: string
  createTime: string
}

export interface FinanceVoucherItem {
  id?: number
  voucherId: number
  subjectId: number
  subjectName: string
  debitAmount: number
  creditAmount: number
  description: string
}

export interface FinanceApproval {
  id: number
  approvalType: 'VOUCHER' | 'EXPENSE' | 'INVOICE' | 'REPORT'
  businessId: number
  businessNo: string
  amount: number
  applicant: string
  applyTime: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  approver?: string
  approveTime?: string
  remark?: string
}

export interface FinanceWarning {
  id: number
  warningType: 'BUDGET_OVER' | 'AR_OVERDUE' | 'INVENTORY_OVER' | 'CASH_FLOW_RISK' | 'TAX_RISK'
  warningLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  title: string
  description: string
  amount?: number
  threshold?: number
  status: 'UNHANDLED' | 'HANDLED' | 'IGNORED'
  handleBy?: string
  handleTime?: string
  handleRemark?: string
  createTime: string
}

export interface FinanceRecord {
  id: number
  type: 'INCOME' | 'EXPENSE'
  category: string
  amount: number
  transactionDate: string
  description?: string
  remark?: string
  status: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED'
  createdBy: string
  createTime: string
  updateTime?: string
}

export interface Invoice {
  id: number
  invoiceNo: string
  orderId: number
  orderNo: string
  customerName: string
  customerType: string
  taxpayerId: string
  invoiceType: string
  invoiceDate: string
  goodsName: string
  quantity: number
  unitPrice: number
  taxRate: number
  taxExcludedAmount: number
  taxAmount: number
  totalAmount: number
  invoiceStatus: string
  printStatus: string
  printCount: number
  remark: string
  createTime: string
  updateTime: string
}

export interface AccountBalance {
  id: number
  accountNo: string
  accountName: string
  accountType: 'CASH' | 'BANK' | 'ALIPAY' | 'WECHAT' | 'OTHER'
  balance: number
  currency: string
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED'
  lastTransactionTime?: string
  remark?: string
  createTime: string
  updateTime: string
}

// ============================================================
// 付款单（四账联动）
// ============================================================

/** 付款方式 */
export type PaymentMethod = 'bank_transfer' | 'cash' | 'check'

/** 付款单状态 */
export type PaymentStatus = 'confirmed' | 'voided'

/**
 * 付款单（对应后端 /v1/payments）
 * 记录每次向供应商付款的明细，支持四账联动
 */
export interface FinancePayment {
  /** 付款单ID */
  id: string
  /** 付款单号 */
  paymentNo: string
  /** 关联应付账款ID */
  payableId: string
  /** 关联应付编号 */
  payableNo: string
  /** 关联采购入库单ID */
  stockinId?: string
  /** 关联采购入库单号 */
  stockinNo?: string
  /** 关联采购订单号 */
  orderNo?: string
  /** 供应商ID */
  supplierId?: string
  /** 供应商名称 */
  supplierName: string
  /** 付款金额（分） */
  paymentAmount: number
  /** 付款方式 */
  paymentMethod: PaymentMethod
  /** 付款银行账户ID */
  bankAccountId: string
  /** 付款账户名称 */
  bankAccountName?: string
  /** 付款日期 */
  paymentDate: string
  /** 关联资金流水ID */
  fundFlowId?: string
  /** 关联流水号 */
  fundFlowNo?: string
  /** 关联会计凭证ID */
  voucherId?: string
  /** 关联凭证号 */
  voucherNo?: string
  /** 状态 */
  status: PaymentStatus
  /** 备注 */
  remark?: string
  /** 作废时间 */
  voidTime?: string
  /** 作废备注 */
  voidRemark?: string
  /** 作废后新关联资金流水ID */
  voidFundFlowId?: string
  /** 作废后新关联流水号 */
  voidFundFlowNo?: string
  /** 作废后新关联会计凭证ID */
  voidVoucherId?: string
  /** 作废后新关联凭证号 */
  voidVoucherNo?: string
  /** 创建人ID */
  createUserId?: string
  /** 创建人姓名 */
  createUserName?: string
  /** 创建时间 */
  createTime?: string
}

/** 付款登记表单数据 */
export interface PaymentFormData {
  /** 关联应付账款ID */
  payableId: string
  /** 付款金额（分） */
  paymentAmount: number
  /** 付款方式 */
  paymentMethod: PaymentMethod
  /** 付款银行账户ID */
  bankAccountId: string
  /** 付款日期 */
  paymentDate: string
  /** 备注 */
  remark?: string
}

/** 付款单作废表单数据 */
export interface PaymentVoidFormData {
  /** 作废备注 */
  remark?: string
}

// ============================================================
// 收款单（四账联动）
// ============================================================

/** 收款方式 */
export type ReceiptMethod = 'bank_transfer' | 'cash' | 'check'

/** 收款单状态 */
export type ReceiptStatus = 'confirmed' | 'voided'

/**
 * 收款单（对应后端 /v1/receipts）
 * 记录每次向客户的收款明细，支持四账联动
 */
export interface FinanceReceipt {
  /** 收款单ID */
  id: string
  /** 收款单号 */
  receiptNo: string
  /** 关联应收账款ID */
  receivableId: string
  /** 关联应收编号 */
  receivableNo: string
  /** 客户ID */
  customerId?: string
  /** 客户名称 */
  customerName: string
  /** 收款金额（分） */
  receiptAmount: number
  /** 收款方式 */
  receiptMethod: ReceiptMethod
  /** 收款银行账户ID */
  bankAccountId: string
  /** 收款账户名称 */
  bankAccountName?: string
  /** 收款日期 */
  receiptDate: string
  /** 关联资金流水ID */
  fundFlowId?: string
  /** 关联流水号 */
  fundFlowNo?: string
  /** 关联会计凭证ID */
  voucherId?: string
  /** 关联凭证号 */
  voucherNo?: string
  /** 状态 */
  status: ReceiptStatus
  /** 备注 */
  remark?: string
  /** 创建人ID */
  createUserId?: string
  /** 创建人姓名 */
  createUserName?: string
  /** 创建时间 */
  createTime?: string
}

/** 收款登记表单数据 */
export interface ReceiptFormData {
  /** 关联应收账款ID */
  receivableId: string
  /** 收款金额（分） */
  receiptAmount: number
  /** 收款方式 */
  receiptMethod: ReceiptMethod
  /** 收款银行账户ID */
  bankAccountId: string
  /** 收款日期 */
  receiptDate: string
  /** 备注 */
  remark?: string
}

// ============================================================
// 发票报销（对应后端 /v1/finance/invoice-reimbursements）
// ============================================================

/** 报销单状态（后端数字 ↔ 前端语义字符串） */
export type ReimbursementStatus = 'draft' | 'approved' | 'paid' | 'cancelled' | 'rejected'

/** 报销明细 */
export interface ReimbursementItem {
  /** 明细ID */
  itemId?: string
  /** 关联发票ID */
  relatedInvoiceId?: string
  /** 凭证ID */
  voucherId?: string
  /** 凭证编号 */
  voucherNo?: string
  /** 凭证类型 */
  voucherType?: string
  /** 明细金额（分） */
  amount: number
  /** 审批通过金额（分） */
  approvedAmount?: number
  /** 费用类型 */
  expenseType: string
  /** 费用说明 */
  expenseDescription?: string
}

/** 报销审批记录 */
export interface ReimbursementApprovalRecord {
  /** 记录ID */
  recordId?: string
  /** 操作人ID */
  operatorId?: string
  /** 操作人姓名 */
  operatorName?: string
  /** 操作动作 */
  action: string
  /** 操作备注 */
  remark?: string
  /** 操作时间 */
  operateTime?: string
}

/** 报销单视图对象 */
export interface InvoiceReimbursementVO {
  /** 报销单ID */
  reimbursementId: string
  /** 报销单号 */
  reimbursementNo: string
  /** 申请人ID */
  applicantId?: string
  /** 申请人姓名 */
  applicantName?: string
  /** 部门ID */
  departmentId?: string
  /** 部门名称 */
  departmentName?: string
  /** 报销类型 */
  reimbursementType: string
  /** 报销总金额（分） */
  totalAmount: number
  /** 审批通过金额（分） */
  approvedAmount?: number
  /** 单据状态 */
  status: ReimbursementStatus
  /** 状态名称 */
  statusName?: string
  /** 申请日期 */
  applyDate: string
  /** 审批日期 */
  approveDate?: string
  /** 审批人ID */
  approverId?: string
  /** 审批人姓名 */
  approverName?: string
  /** 拒绝/取消原因 */
  rejectReason?: string
  /** 备注 */
  remark?: string
  /** 付款状态：0-未付款 1-已付款 */
  paymentStatus?: number
  /** 付款状态名称 */
  paymentStatusName?: string
  /** 付款日期 */
  paymentDate?: string
  /** 付款凭证号 */
  paymentVoucherNo?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 报销明细列表 */
  items: ReimbursementItem[]
  /** 审批记录列表 */
  approvalRecords?: ReimbursementApprovalRecord[]
}

/** 报销创建表单数据（金额单位：元，提交时转分） */
export interface ReimbursementFormData {
  /** 部门ID */
  departmentId?: string
  /** 部门名称 */
  departmentName?: string
  /** 报销类型 */
  reimbursementType: string
  /** 报销总金额（元） */
  totalAmount: number
  /** 申请日期 */
  applyDate: string
  /** 报销明细列表 */
  items: Array<{
    relatedInvoiceId?: string
    voucherId?: string
    voucherNo?: string
    voucherType?: string
    /** 明细金额（元） */
    amount: number
    expenseType: string
    expenseDescription?: string
  }>
  /** 备注 */
  remark?: string
}

/** 报销查询表单 */
export interface ReimbursementQueryForm extends FinancePageQuery {
  reimbursementNo?: string
  applicantName?: string
  departmentId?: string
  status?: ReimbursementStatus
  reimbursementType?: string
  startDate?: string
  endDate?: string
}

/** 报销审批表单 */
export interface ReimbursementApproveForm {
  /** 是否通过 */
  approved: boolean
  /** 审批备注 */
  remark?: string
  /** 审批通过金额（元） */
  approvedAmount?: number
}

/** 报销付款表单 */
export interface ReimbursementPayForm {
  /** 付款凭证号 */
  paymentVoucherNo: string
  /** 付款日期 */
  paymentDate: string
}

/** 报销统计数据 */
export interface ReimbursementStats {
  /** 总数 */
  totalCount: number
  /** 待审核数量 */
  pendingCount: number
  /** 已审批数量 */
  approvedCount: number
  /** 已付款数量 */
  paidCount: number
  /** 已取消数量 */
  cancelledCount: number
  /** 已拒绝数量 */
  rejectedCount: number
  /** 报销总金额（分） */
  totalAmount: number
  /** 已审批金额（分） */
  approvedAmount: number
  /** 已付款金额（分） */
  paidAmount: number
  /** 统计周期 */
  period?: string
}

// ============================================================
// 财务统计概览（对应后端 /v1/finance/statistics/overview）
// ============================================================

/** 财务统计查询参数 */
export interface FinanceStatisticsQuery {
  /** 起始日期（yyyy-MM-dd） */
  startDate?: string
  /** 结束日期（yyyy-MM-dd） */
  endDate?: string
  /** 统计周期（如 2026-06，优先于日期） */
  period?: string
}

/** 财务统计概览数据（金额单位：分） */
export interface FinanceStatisticsOverview {
  /** 当月收入（分） */
  monthlyIncome: number
  /** 当月支出（分） */
  monthlyExpense: number
  /** 当月利润（分） */
  monthlyProfit: number
  /** 收入环比变化率（百分比） */
  monthlyIncomeChange?: number
  /** 支出环比变化率（百分比） */
  monthlyExpenseChange?: number
  /** 利润环比变化率（百分比） */
  monthlyProfitChange?: number
  /** 上月收入（分） */
  lastMonthIncome?: number
  /** 上月支出（分） */
  lastMonthExpense?: number
  /** 上月利润（分） */
  lastMonthProfit?: number
  /** 统计周期 */
  period?: string
}
