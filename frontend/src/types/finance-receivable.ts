/**
 * 应收应付模块类型定义
 * 从 api/receivablePayable.ts 迁移而来
 */

/** 发票状态枚举值 */
export type InvoiceStatus = 'unpaid' | 'partial' | 'paid';

/** 客户状态枚举值 */
export type CustomerStatus = 'active' | 'frozen';

/** 收款方式枚举值 */
export type PaymentMethodType = 'bank_transfer' | 'cash' | 'wechat' | 'alipay';

/** 付款记录状态枚举值 */
export type PaymentStatus = 'pending' | 'completed' | 'cancelled';

export interface Customer {
  id: string;
  customerCode: string;
  customerName: string;
  contactPerson: string;
  phone: string;
  email?: string;
  address?: string;
  creditLimit: number;
  receivableAmount: number;
  status: 'active' | 'frozen';
  createTime: string;
}

export interface ReceivableSupplier {
  id: string;
  supplierCode: string;
  supplierName: string;
  contactPerson: string;
  phone: string;
  email?: string;
  address?: string;
  creditLimit: number;
  payableAmount: number;
  status: 'active' | 'frozen';
  createTime: string;
}

export interface SalesInvoice {
  id: string;
  invoiceNo: string;
  customerId: string;
  customerName: string;
  invoiceDate: string;
  invoiceAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  dueDate: string;
  status: 'unpaid' | 'partial' | 'paid';
  remark?: string;
}

export interface PurchaseInvoice {
  id: string;
  invoiceNo: string;
  supplierId: string;
  supplierName: string;
  invoiceDate: string;
  invoiceAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  dueDate: string;
  status: 'unpaid' | 'partial' | 'paid';
  remark?: string;
}

export interface PaymentRecord {
  id: string;
  paymentNo: string;
  paymentDate: string;
  paymentType: 'receive' | 'pay';
  customerId?: string;
  customerName?: string;
  supplierId?: string;
  supplierName?: string;
  amount: number;
  paymentMethod: string;
  bankAccountId: string;
  invoiceId?: string;
  remark?: string;
  status: 'pending' | 'completed' | 'cancelled';
}

export interface ReceivableStatistics {
  totalReceivable: number;
  received: number;
  unreceived: number;
  overdue: number;
}

export interface ReceivablePayableStatistics {
  totalPayable: number;
  paid: number;
  unpaid: number;
  overdue: number;
}

/** 账龄分析汇总数据 */
export interface AgingSummary {
  /** 未到期金额 */
  notDue: number;
  /** 1-30天金额 */
  days1to30: number;
  /** 31-60天金额 */
  days31to60: number;
  /** 61-90天金额 */
  days61to90: number;
  /** 90天以上金额 */
  over90Days: number;
  /** 合计金额 */
  total: number;
}

/** 账龄分析明细（按客户维度） */
export interface AgingDetail {
  /** 客户名称 */
  customerName: string;
  /** 未到期金额 */
  notDue: number;
  /** 1-30天金额 */
  days1to30: number;
  /** 31-60天金额 */
  days31to60: number;
  /** 61-90天金额 */
  days61to90: number;
  /** 90天以上金额 */
  over90Days: number;
  /** 合计金额 */
  total: number;
}

/** 账龄分析API返回结构 */
export interface AgingAnalysisResponse {
  /** 汇总数据 */
  summary: AgingSummary;
  /** 明细列表 */
  details: AgingDetail[];
}

/** 客户表单数据 */
export interface CustomerFormData {
  id?: string;
  customerCode: string;
  customerName: string;
  contactPerson: string;
  phone: string;
  creditLimit: number;
}

/** 发票表单数据 */
export interface InvoiceFormData {
  customerId: string;
  customerName: string;
  invoiceAmount: number;
  invoiceDate: string;
  dueDate: string;
}

/** 收款表单数据 */
export interface ReceiveFormData {
  customerId: string;
  customerName: string;
  invoiceId: string;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethodType;
  bankAccountId: string;
}
