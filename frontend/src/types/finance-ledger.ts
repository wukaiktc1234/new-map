/**
 * 总账模块类型定义
 * 从 api/ledger.ts 迁移而来
 * 注意：AccountingSubject 与 types/finance.ts 中的同名类型不同，
 * 此处使用 LedgerAccountingSubject 以避免冲突
 */

export interface LedgerAccountingSubject {
  id: string;
  subjectCode: string;
  subjectName: string;
  subjectType: 'asset' | 'liability' | 'equity' | 'cost' | 'income';
  level: number;
  balanceDirection: 'debit' | 'credit';
  status: 'active' | 'inactive';
  parentId?: string;
}

export interface SubjectFormData {
  id?: string;
  subjectCode: string;
  subjectName: string;
  subjectType: 'asset' | 'liability' | 'equity' | 'cost' | 'income';
  level: number;
  balanceDirection: 'debit' | 'credit';
  parentId?: string;
}

export interface LedgerVoucher {
  id: string;
  voucherNo: string;
  voucherDate: string;
  abstract: string;
  debitAmount: number;
  creditAmount: number;
  status: 'draft' | 'audited' | 'posted';
  creatorName: string;
  auditorName?: string;
  auditTime?: string;
  entries: VoucherEntry[];
}

export interface VoucherEntry {
  id: string;
  subjectCode: string;
  subjectName: string;
  debitAmount: number;
  creditAmount: number;
  remark?: string;
}

export interface VoucherFormData {
  voucherDate: string;
  abstract: string;
  entries: {
    subjectCode: string;
    debitAmount: number;
    creditAmount: number;
    remark?: string;
  }[];
}

export interface TransferTemplate {
  id: string;
  templateName: string;
  templateType: string;
  frequency: string;
  status: 'active' | 'inactive';
  entries: {
    debitSubject: string;
    creditSubject: string;
    amount: number;
    formula?: string;
  }[];
}

export interface TemplateFormData {
  templateName: string;
  templateType: string;
  frequency: string;
  entries: {
    debitSubject: string;
    creditSubject: string;
    amount: number;
    formula?: string;
  }[];
}

export interface PeriodInfo {
  currentPeriod: string;
  status: string;
  voucherCount: number;
  lastClosingDate: string;
}

export interface BalanceCheckDetail {
  subjectCode: string;
  subjectName: string;
  debitTotal: number;
  creditTotal: number;
  difference: number;
}
