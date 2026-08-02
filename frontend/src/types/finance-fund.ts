/**
 * 资金管理模块类型定义
 * 从 api/fund.ts 迁移而来
 */

export interface BankAccount {
  id: string;
  bankName: string;
  accountNo: string;
  accountName: string;
  accountType: 'basic' | 'general' | 'payroll' | 'reserve';
  balance: number;
  status: 'active' | 'frozen';
  openDate: string;
  remark: string;
}

export interface BankAccountCreate {
  bankName: string;
  accountNo: string;
  accountName: string;
  accountType: string;
  openDate: string;
  initialBalance: number;
  remark: string;
}

export interface FundTransaction {
  id: string;
  transactionNo: string;
  transactionDate: string;
  transactionType: 'income' | 'expense';
  counterparty: string;
  amount: number;
  bankAccountId: string;
  bankAccountName: string;
  abstract: string;
  voucherNo: string;
  status: 'pending' | 'completed' | 'cancelled';
  createTime: string;
}

export interface FundTransactionCreate {
  transactionType: 'income' | 'expense';
  counterparty: string;
  amount: number;
  bankAccountId: string;
  abstract: string;
  transactionDate: string;
  voucherNo: string;
}

export interface FundTransfer {
  id: string;
  transferNo: string;
  transferDate: string;
  fromAccountId: string;
  fromAccountName: string;
  toAccountId: string;
  toAccountName: string;
  amount: number;
  status: 'pending' | 'completed' | 'cancelled';
  remark: string;
  createTime: string;
}

export interface FundTransferCreate {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  transferDate: string;
  remark: string;
}

export interface BankReconciliation {
  id: string;
  bankAccountId: string;
  period: string;
  bookBalance: number;
  bankBalance: number;
  difference: number;
  unreconciledCount: number;
  status: 'pending' | 'completed';
}

export interface UnreconciledItem {
  id: string;
  date: string;
  type: 'income' | 'expense';
  amount: number;
  summary: string;
  source: 'bank' | 'book';
  matched: boolean;
}

export interface FundStatistics {
  totalFund: number;
  bankDeposit: number;
  cash: number;
  other: number;
}
