/**
 * 财务中心模块数据转换器
 * ============================================================
 *
 * 【定位】
 * 在API边界处完成数据格式转换，遵循项目规范第十六章。
 * 组件层禁止直接做状态映射或金额元分转换，必须通过Converter。
 *
 * 【转换规则】
 * - 金额：后端分(Long) ↔ 前端元(number/string)，使用 fenToYuan / yuanToFen
 * - 状态：后端数字 ↔ 前端语义字符串
 * - 日期：后端ISO 8601 ↔ 前端 YYYY-MM-DD HH:mm:ss
 *
 * 【使用方式】
 * ```typescript
 * // API层调用
 * const backendData = await subjectApi.getList(params)
 * const frontendData = subjectConverter.toFrontend(backendData.records[0])
 *
 * // 表单提交前转换
 * const dto = subjectConverter.toCreateDTO(formData)
 * await subjectApi.create(dto)
 * ```
 */

import { fenToYuan, yuanToFen, fenToYuanNumber, fenToWan } from '@/utils/money'

// ============================================================
// 金额转换工具（统一委托给 utils/money，保留导出以兼容已有调用方）
// ============================================================

export { fenToYuan, yuanToFen, fenToYuanNumber, fenToWan }

// ============================================================
// 通用状态映射
// ============================================================

/**
 * 通用状态映射：后端数字 ↔ 前端语义字符串
 * 1=active, 0=inactive, 2=probation
 */
export function mapStatus(status: number): string {
  switch (status) {
    case 1:
      return 'active'
    case 0:
      return 'inactive'
    case 2:
      return 'probation'
    default:
      return 'inactive'
  }
}

/**
 * 通用状态反向映射：前端语义字符串 → 后端数字
 */
export function mapStatusToNumber(status: string): number {
  switch (status) {
    case 'active':
      return 1
    case 'inactive':
      return 0
    case 'probation':
      return 2
    default:
      return 0
  }
}

// ============================================================
// 会计科目状态映射
// ============================================================

/** 科目状态映射 */
export const SubjectStatusMap = {
  toFrontend: {
    1: 'active',
    0: 'inactive',
  } as Record<number, string>,
  toBackend: {
    active: 1,
    inactive: 0,
  } as Record<string, number>,
}

/** 科目类型映射（后端枚举 → 前端语义） */
export const SubjectTypeMap = {
  toFrontend: {
    1: 'asset',
    2: 'liability',
    3: 'equity',
    4: 'cost',
    5: 'income',
    6: 'profit',
  } as Record<number, string>,
  toBackend: {
    asset: 1,
    liability: 2,
    equity: 3,
    cost: 4,
    income: 5,
    profit: 6,
  } as Record<string, number>,
}

// ============================================================
// 凭证状态映射
// ============================================================

/** 凭证状态映射 */
export const VoucherStatusMap = {
  toFrontend: {
    1: 'draft',
    2: 'audited',
    3: 'posted',
    4: 'cancelled',
  } as Record<number, string>,
  toBackend: {
    draft: 1,
    audited: 2,
    posted: 3,
    cancelled: 4,
  } as Record<string, number>,
}

/** 凭证类型映射 */
export const VoucherTypeMap = {
  toFrontend: {
    1: 'receipt',
    2: 'payment',
    3: 'transfer',
    4: 'general',
  } as Record<number, string>,
  toBackend: {
    receipt: 1,
    payment: 2,
    transfer: 3,
    general: 4,
  } as Record<string, number>,
}

// ============================================================
// 应收/应付状态映射
// ============================================================

/** 应收/应付状态映射 */
export const ReceivablePayableStatusMap = {
  toFrontend: {
    1: 'unpaid',
    2: 'partial',
    3: 'settled',
    4: 'overdue',
  } as Record<number, string>,
  toBackend: {
    unpaid: 1,
    partial: 2,
    settled: 3,
    overdue: 4,
  } as Record<string, number>,
}

// ============================================================
// 预算状态映射
// ============================================================

/** 预算状态映射 */
export const BudgetStatusMap = {
  toFrontend: {
    1: 'draft',
    2: 'approved',
    3: 'executing',
    4: 'closed',
  } as Record<number, string>,
  toBackend: {
    draft: 1,
    approved: 2,
    executing: 3,
    closed: 4,
  } as Record<string, number>,
}

// ============================================================
// 财务记录状态映射
// ============================================================

/** 财务记录状态映射 */
export const FinanceRecordStatusMap = {
  toFrontend: {
    1: 'draft',
    2: 'pending',
    3: 'approved',
    4: 'rejected',
  } as Record<number, string>,
  toBackend: {
    draft: 1,
    pending: 2,
    approved: 3,
    rejected: 4,
  } as Record<string, number>,
}

/** 财务记录类型映射 */
export const FinanceRecordTypeMap = {
  toFrontend: {
    1: 'income',
    2: 'expense',
  } as Record<number, string>,
  toBackend: {
    income: 1,
    expense: 2,
  } as Record<string, number>,
}

// ============================================================
// 银行账户状态映射
// ============================================================

/** 银行账户状态映射 */
export const BankAccountStatusMap = {
  toFrontend: {
    1: 'active',
    2: 'frozen',
    3: 'closed',
  } as Record<number, string>,
  toBackend: {
    active: 1,
    frozen: 2,
    closed: 3,
  } as Record<string, number>,
}

/** 银行账户类型映射 */
export const BankAccountTypeMap = {
  toFrontend: {
    1: 'basic',
    2: 'general',
    3: 'payroll',
    4: 'reserve',
    5: 'other',
  } as Record<number, string>,
  toBackend: {
    basic: 1,
    general: 2,
    payroll: 3,
    reserve: 4,
    other: 5,
  } as Record<string, number>,
}

// ============================================================
// 资金流水状态映射
// ============================================================

/** 资金流水状态映射 */
export const FundFlowStatusMap = {
  toFrontend: {
    1: 'pending',
    2: 'completed',
    3: 'cancelled',
  } as Record<number, string>,
  toBackend: {
    pending: 1,
    completed: 2,
    cancelled: 3,
  } as Record<string, number>,
}

/** 资金流水类型映射 */
export const FundFlowTypeMap = {
  toFrontend: {
    1: 'income',
    2: 'expense',
  } as Record<number, string>,
  toBackend: {
    income: 1,
    expense: 2,
  } as Record<string, number>,
}

// ============================================================
// 会计期间状态映射
// ============================================================

/** 会计期间状态映射 */
export const AccountingPeriodStatusMap = {
  toFrontend: {
    1: 'open',
    2: 'closing',
    3: 'closed',
  } as Record<number, string>,
  toBackend: {
    open: 1,
    closing: 2,
    closed: 3,
  } as Record<string, number>,
}

// ============================================================
// 发票状态映射
// ============================================================

/** 发票状态映射 */
export const InvoiceStatusMap = {
  toFrontend: {
    1: 'draft',
    2: 'issued',
    3: 'cancelled',
    4: 'red_flushed',
  } as Record<number, string>,
  toBackend: {
    draft: 1,
    issued: 2,
    cancelled: 3,
    red_flushed: 4,
  } as Record<string, number>,
}

/** 发票类型映射 */
export const InvoiceTypeMap = {
  toFrontend: {
    1: 'special',
    2: 'normal',
    3: 'electronic',
    4: 'electronic_special',
  } as Record<number, string>,
  toBackend: {
    special: 1,
    normal: 2,
    electronic: 3,
    electronic_special: 4,
  } as Record<string, number>,
}

// ============================================================
// 成本类型映射
// ============================================================

/** 成本类型映射 */
export const CostTypeMap = {
  toFrontend: {
    1: 'material',
    2: 'labor',
    3: 'rent',
    4: 'energy',
    5: 'marketing',
    6: 'other',
  } as Record<number, string>,
  toBackend: {
    material: 1,
    labor: 2,
    rent: 3,
    energy: 4,
    marketing: 5,
    other: 6,
  } as Record<string, number>,
}

// ============================================================
// 预警状态映射
// ============================================================

/** 预警状态映射 */
export const WarningStatusMap = {
  toFrontend: {
    1: 'normal',
    2: 'warning',
    3: 'over',
  } as Record<number, string>,
  toBackend: {
    normal: 1,
    warning: 2,
    over: 3,
  } as Record<string, number>,
}

// ============================================================
// 税种/纳税人类型映射
// ============================================================

/**
 * 税种映射
 * 后端：1-增值税 2-城建税 3-教育费附加 4-地方教育附加 5-企业所得税 6-个人所得税 7-印花税
 */
export const TaxTypeMap = {
  toFrontend: {
    1: 'vat',
    2: 'urban_construction',
    3: 'education_surcharge',
    4: 'local_education',
    5: 'corporate_income',
    6: 'individual_income',
    7: 'stamp',
  } as Record<number, string>,
  toBackend: {
    vat: 1,
    urban_construction: 2,
    education_surcharge: 3,
    local_education: 4,
    corporate_income: 5,
    individual_income: 6,
    stamp: 7,
  } as Record<string, number>,
}

/**
 * 纳税人类型映射
 * 后端：1-一般纳税人 2-小规模纳税人
 */
export const TaxpayerTypeMap = {
  toFrontend: {
    1: 'general',
    2: 'small',
  } as Record<number, string>,
  toBackend: {
    general: 1,
    small: 2,
  } as Record<string, number>,
}

// ============================================================
// 审批状态映射
// ============================================================

/** 审批状态映射 */
export const ApprovalStatusMap = {
  toFrontend: {
    1: 'pending',
    2: 'approved',
    3: 'rejected',
  } as Record<number, string>,
  toBackend: {
    pending: 1,
    approved: 2,
    rejected: 3,
  } as Record<string, number>,
}

// ============================================================
// 转换器类
// ============================================================

/**
 * 会计科目数据转换器
 */
export const SubjectDataConverter = {
  /** 后端数据 → 前端展示 */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    return {
      ...backend,
      status: SubjectStatusMap.toFrontend[backend.status as number] ?? backend.status,
      subjectType: SubjectTypeMap.toFrontend[backend.subjectType as number] ?? backend.subjectType,
    }
  },

  /** 前端表单 → 创建DTO */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return {
      ...form,
      status: SubjectStatusMap.toBackend[form.status as string] ?? form.status,
      subjectType: SubjectTypeMap.toBackend[form.subjectType as string] ?? form.subjectType,
    }
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 凭证数据转换器
 * 处理多字段金额分↔元转换
 */
export const VoucherDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['debitTotal', 'creditTotal', 'debitAmount', 'creditAmount'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    VoucherDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = VoucherStatusMap.toFrontend[result.status as number] ?? result.status
    result.voucherType = VoucherTypeMap.toFrontend[result.voucherType as number] ?? result.voucherType
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    VoucherDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = VoucherStatusMap.toBackend[result.status as string] ?? result.status
    result.voucherType = VoucherTypeMap.toBackend[result.voucherType as string] ?? result.voucherType
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 应收账款数据转换器
 */
export const ReceivableDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['amount', 'receivedAmount', 'remainAmount'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    ReceivableDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = ReceivablePayableStatusMap.toFrontend[result.status as number] ?? result.status
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    ReceivableDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = ReceivablePayableStatusMap.toBackend[result.status as string] ?? result.status
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 计算账龄
 * @param dueDate 到期日
 * @returns 逾期天数或“未到期”
 */
function computeAging(dueDate: string | undefined): string {
  if (!dueDate) return '-'
  const due = new Date(dueDate)
  if (isNaN(due.getTime())) return '-'
  const diff = Math.floor((Date.now() - due.getTime()) / (1000 * 60 * 60 * 24))
  return diff > 0 ? `${diff}天` : '未到期'
}

/**
 * 应付账款数据转换器
 */
export const PayableDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['amount', 'paidAmount', 'remainAmount'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    // ID 类型转换：后端 number → 前端 string
    if (result.payableId !== undefined) result.id = String(result.payableId)
    if (result.supplierId !== undefined) result.supplierId = String(result.supplierId)
    // 后端字段名 → 前端字段名映射（VO 使用 originalAmount/balanceAmount，前端使用 amount/remainAmount）
    if (result.originalAmount !== undefined) result.amount = result.originalAmount
    if (result.balanceAmount !== undefined) result.remainAmount = result.balanceAmount
    // 来源字段透传：后端 VARCHAR 通常为字符串，此处做防御性非空确认与类型统一
    if (result.stockinNo != null) result.stockinNo = String(result.stockinNo)
    if (result.orderNo != null) result.orderNo = String(result.orderNo)
    PayableDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = ReceivablePayableStatusMap.toFrontend[result.status as number] ?? result.status
    // 账龄计算：根据到期日计算
    result.aging = computeAging(result.dueDate as string | undefined)
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    // 前端字段名 → 后端字段名映射（前端使用 amount，后端 DTO 使用 originalAmount）
    if (result.amount !== undefined) result.originalAmount = result.amount
    PayableDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = ReceivablePayableStatusMap.toBackend[result.status as string] ?? result.status
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 预算数据转换器
 */
export const BudgetDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['budgetAmount', 'actualAmount'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    BudgetDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = BudgetStatusMap.toFrontend[result.status as number] ?? result.status
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    BudgetDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = BudgetStatusMap.toBackend[result.status as string] ?? result.status
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 成本记录数据转换器
 */
export const CostDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['amount', 'budgetAmount', 'variance'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    CostDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.costType = CostTypeMap.toFrontend[result.costType as number] ?? result.costType
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    CostDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.costType = CostTypeMap.toBackend[result.costType as string] ?? result.costType
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 银行账户数据转换器
 */
export const BankAccountDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['balance', 'initialBalance'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    BankAccountDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = BankAccountStatusMap.toFrontend[result.status as number] ?? result.status
    result.accountType = BankAccountTypeMap.toFrontend[result.accountType as number] ?? result.accountType
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    BankAccountDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = BankAccountStatusMap.toBackend[result.status as string] ?? result.status
    result.accountType = BankAccountTypeMap.toBackend[result.accountType as string] ?? result.accountType
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 资金流水数据转换器
 */
export const FundFlowDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['amount', 'balance'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    FundFlowDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = FundFlowStatusMap.toFrontend[result.status as number] ?? result.status
    result.flowType = FundFlowTypeMap.toFrontend[result.flowType as number] ?? result.flowType
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    FundFlowDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = FundFlowStatusMap.toBackend[result.status as string] ?? result.status
    result.flowType = FundFlowTypeMap.toBackend[result.flowType as string] ?? result.flowType
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 发票数据转换器
 */
export const InvoiceDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['amountWithoutTax', 'taxAmount', 'totalAmount'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    InvoiceDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = InvoiceStatusMap.toFrontend[result.status as number] ?? result.status
    result.invoiceType = InvoiceTypeMap.toFrontend[result.invoiceType as number] ?? result.invoiceType
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    InvoiceDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.status = InvoiceStatusMap.toBackend[result.status as string] ?? result.status
    result.invoiceType = InvoiceTypeMap.toBackend[result.invoiceType as string] ?? result.invoiceType
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

// ============================================================
// 报销单状态映射
// ============================================================

/** 报销单状态映射（后端：0-草稿 1-已审批 2-已付款 3-已取消 4-已拒绝） */
export const ReimbursementStatusMap = {
  toFrontend: {
    0: 'draft',
    1: 'approved',
    2: 'paid',
    3: 'cancelled',
    4: 'rejected',
  } as Record<number, string>,
  toBackend: {
    draft: 0,
    approved: 1,
    paid: 2,
    cancelled: 3,
    rejected: 4,
  } as Record<string, number>,
}

/**
 * 报销单数据转换器
 * 处理金额分↔元转换和状态映射
 */
export const ReimbursementDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['totalAmount', 'approvedAmount'] as const,
  /** 明细金额字段（分↔元） */
  itemAmountFields: ['amount', 'approvedAmount'] as const,

  /** 后端数据 → 前端展示（金额分→元，状态映射） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    ReimbursementDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.status = ReimbursementStatusMap.toFrontend[result.status as number] ?? result.status
    // 转换明细列表中的金额
    const items = result.items
    if (Array.isArray(items)) {
      result.items = items.map((item: Record<string, unknown>) => {
        const convertedItem: Record<string, unknown> = { ...item }
        ReimbursementDataConverter.itemAmountFields.forEach(field => {
          if (convertedItem[field] !== undefined && convertedItem[field] !== null) {
            convertedItem[field] = fenToYuanNumber(convertedItem[field] as number)
          }
        })
        return convertedItem
      })
    }
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    ReimbursementDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    // 转换明细列表中的金额
    const items = result.items
    if (Array.isArray(items)) {
      result.items = items.map((item: Record<string, unknown>) => {
        const convertedItem: Record<string, unknown> = { ...item }
        ReimbursementDataConverter.itemAmountFields.forEach(field => {
          if (convertedItem[field] !== undefined && convertedItem[field] !== null) {
            convertedItem[field] = yuanToFen(convertedItem[field] as number | string)
          }
        })
        return convertedItem
      })
    }
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}

/**
 * 标准成本卡数据转换器
 */
export const StandardCostCardDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: ['standardCost', 'actualCost', 'variance'] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...backend }
    StandardCostCardDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuanNumber(result[field] as number)
      }
    })
    result.warningStatus = WarningStatusMap.toFrontend[result.warningStatus as number] ?? result.warningStatus
    return result
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO(form: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...form }
    StandardCostCardDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as number | string)
      }
    })
    result.warningStatus = WarningStatusMap.toBackend[result.warningStatus as string] ?? result.warningStatus
    return result
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO(form: Record<string, unknown>): Record<string, unknown> {
    return this.toCreateDTO(form)
  },
}
