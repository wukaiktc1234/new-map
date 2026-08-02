export interface FinanceRecord {
  id: number;
  type: string;
  amount: number;
  category: string;
  subCategory?: string;
  description: string;
  recordDate: string;
  auditStatus: 'pending' | 'approved' | 'rejected';
  applyUser?: string;
  applyDate?: string;
  auditor?: string;
  auditDate?: string;
  auditComment?: string;
  recordSource: 'system' | 'manual';
  originalRecordId?: number;
  correctionReason?: string;
}

export interface StatCardData {
  title: string;
  value: number;
  iconName: string;
  colorType: 'primary' | 'success' | 'danger';
  change?: number;
}

export interface IncomeExpenseFormData {
  id?: number;
  type: string;
  amount: number;
  category: string;
  subCategory?: string;
  description: string;
  recordDate: string;
}

export interface CorrectFormData {
  originalRecordId: number;
  correctionReason: string;
  description: string;
  amount?: number;
  category?: string;
  subCategory?: string;
}

export interface FirstCategory {
  name: string;
  type: string;
}

export interface SubCategory {
  name: string;
}

export interface ParsedCategoriesData {
  firstCategories: FirstCategory[];
  subCategoriesMap: Record<string, SubCategory[]>;
}

export interface SummaryMethodParams {
  columns: Array<{ property?: string
  label?: string }>
}
