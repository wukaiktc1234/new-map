/**
 * 应付账款模块类型定义
 * 从 api/payable.ts 迁移而来
 * 注意：Supplier/Invoice 等与 types/supplier.ts 和 types/finance.ts 中的同名类型不同，
 * 此处使用 PayableSupplier/PayableInvoice 以避免冲突
 */

export interface PayableStatistics {
  totalPayable: number;
  paid: number;
  unpaid: number;
  overdue: number;
}

export interface PayableSupplier {
  id: string;
  supplierCode: string;
  supplierName: string;
  contactPerson: string;
  phone: string;
  creditLimit: number;
  payableAmount: number;
  status: 'active' | 'frozen';
  address?: string;
  bankName?: string;
  bankAccount?: string;
  createTime?: string;
}

export interface PayableSupplierFormData {
  id?: string;
  supplierName: string;
  contactPerson: string;
  phone: string;
  creditLimit: number;
  address?: string;
  bankName?: string;
  bankAccount?: string;
}

export interface PayableInvoice {
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

export interface PayableInvoiceFormData {
  supplierId: string;
  invoiceNo: string;
  invoiceDate: string;
  invoiceAmount: number;
  dueDate: string;
  remark?: string;
}

export interface Payment {
  id: string;
  paymentNo: string;
  supplierId: string;
  supplierName: string;
  paymentDate: string;
  paymentAmount: number;
  paymentMethod: 'bank_transfer' | 'alipay' | 'wechat' | 'cash';
  bankAccount: string;
  status: 'pending' | 'confirmed';
  remark?: string;
}

export interface PaymentFormData {
  supplierId: string;
  invoiceIds?: string[];
  paymentAmount: number;
  paymentMethod: 'bank_transfer' | 'alipay' | 'wechat' | 'cash';
  bankAccount: string;
  paymentDate: string;
  remark?: string;
}

export interface AgingData {
  notDue: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  over90Days: number;
  total: number;
}

export interface AgingDetail {
  supplierName: string;
  notDue: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  over90Days: number;
  total: number;
}
