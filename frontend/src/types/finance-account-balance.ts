/**
 * 科目余额表模块类型定义
 * 从 api/accountBalance.ts 迁移而来
 * 注意：AccountBalance 与 types/finance.ts 中的同名类型不同，
 * 此处使用 SubjectAccountBalance 以避免冲突
 */

export interface SubjectAccountBalance {
  id: string;
  subjectCode: string;
  subjectName: string;
  category: 'ASSET' | 'LIABILITY' | 'EQUITY' | 'COST' | 'INCOME';
  openingDebit: number;
  openingCredit: number;
  currentDebit: number;
  currentCredit: number;
  endingDebit: number;
  endingCredit: number;
}

export interface BalanceDetail {
  voucherDate: string;
  voucherNo: string;
  summary: string;
  debit: number;
  credit: number;
  balance: number;
}

export interface AccountBalanceQuery {
  subjectCode?: string;
  subjectName?: string;
  category?: string;
  page?: number;
  size?: number;
}
