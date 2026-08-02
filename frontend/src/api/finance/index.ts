/**
 * 财务中心模块API统一导出
 *
 * 使用方式：
 * ```typescript
 * import { subjectApi, voucherApi } from '@/api/finance'
 *
 * // 获取科目列表
 * const result = await subjectApi.getList({ page: 1, size: 10 })
 *
 * // 创建凭证
 * const voucher = await voucherApi.create(formData)
 * ```
 *
 * 也可以按需导入单个API模块：
 * ```typescript
 * import subjectApi from '@/api/finance/subject'
 * ```
 */

// 导入所有API模块
import subjectApi from './subject'
import voucherApi from './voucher'
import financeRecordApi from './record'
import budgetApi from './budget'
import costApi from './cost'
import receivableApi from './receivable'
import payableApi from './payable'
import reportApi from './report'
import invoiceApi from './invoice'
import reimbursementApi from './reimbursement'
import statisticsApi from './statistics'
import accountingPeriodApi from './accounting-period'
import summaryTemplateApi from './summary-template'
import transferTemplateApi from './transfer-template'
import taxRateApi from './tax-rate'
import standardCostApi from './standard-cost'
import bankAccountApi from './bank-account'
import fundFlowApi from './fund-flow'
import approvalFlowApi from './approval-flow'
import auditLogApi from './audit-log'
import paymentApi from './payment'
import receiptApi from './receipt'

// 具名导出所有API
export {
  subjectApi,
  voucherApi,
  financeRecordApi,
  budgetApi,
  costApi,
  receivableApi,
  payableApi,
  reportApi,
  invoiceApi,
  reimbursementApi,
  statisticsApi,
  accountingPeriodApi,
  summaryTemplateApi,
  transferTemplateApi,
  taxRateApi,
  standardCostApi,
  bankAccountApi,
  fundFlowApi,
  approvalFlowApi,
  auditLogApi,
  paymentApi,
  receiptApi,
}

// 数据转换器
export {
  // 金额转换工具
  yuanToFen,
  fenToYuan,
  fenToYuanNumber,
  fenToWan,
  // 状态映射工具
  mapStatus,
  mapStatusToNumber,
  // 状态映射表
  SubjectStatusMap,
  SubjectTypeMap,
  VoucherStatusMap,
  VoucherTypeMap,
  ReceivablePayableStatusMap,
  BudgetStatusMap,
  FinanceRecordStatusMap,
  FinanceRecordTypeMap,
  BankAccountStatusMap,
  BankAccountTypeMap,
  FundFlowStatusMap,
  FundFlowTypeMap,
  AccountingPeriodStatusMap,
  InvoiceStatusMap,
  InvoiceTypeMap,
  CostTypeMap,
  WarningStatusMap,
  ApprovalStatusMap,
  ReimbursementStatusMap,
  // 数据转换器
  SubjectDataConverter,
  VoucherDataConverter,
  ReceivableDataConverter,
  PayableDataConverter,
  BudgetDataConverter,
  CostDataConverter,
  BankAccountDataConverter,
  FundFlowDataConverter,
  InvoiceDataConverter,
  StandardCostCardDataConverter,
  ReimbursementDataConverter,
} from './converters'

// 默认导出所有API
export default {
  subjectApi,
  voucherApi,
  financeRecordApi,
  budgetApi,
  costApi,
  receivableApi,
  payableApi,
  reportApi,
  invoiceApi,
  reimbursementApi,
  statisticsApi,
  accountingPeriodApi,
  summaryTemplateApi,
  transferTemplateApi,
  taxRateApi,
  standardCostApi,
  bankAccountApi,
  fundFlowApi,
  approvalFlowApi,
  auditLogApi,
  paymentApi,
  receiptApi,
}
