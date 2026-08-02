// 财务报表类型定义

// 报表模板接口
export interface ReportTemplate {
  id: string;
  name: string;
  reportType: string;
  periodType: string;
  dateRange?: Date[];
  departmentId?: string;
  projectId?: string;
  accountSubjectId?: string;
  [key: string]: unknown;
}

// 报表单接口
export interface ReportForm {
  reportType: string;
  periodType: string;
  dateRange: Date[];
  departmentId: string;
  projectId: string;
  accountSubjectId: string;
}

// 错误项接口
export interface ErrorItem {
  title: string;
  description: string;
}

// 报表详情项接口
export interface ReportDetail {
  id?: string;
  item: string;
  amount?: number;
  currentPeriod?: number;
  previousPeriod?: number;
  change?: number;
  percentage?: number;
  '同比增长'?: number;
  changeRate?: number;
  status?: string;
  isTotal?: boolean;
  isSubTotal?: boolean;
  category?: 'operating' | 'investing' | 'financing' | 'income' | 'expense' | 'assets' | 'liabilities' | 'equity';
  taxType?: string;
  taxAmount?: number;
  paidAmount?: number;
  relations?: RelatedData[];
  footnoteIds?: string[];
  [key: string]: unknown;
}

// 报表数据接口
export interface ReportData {
  reportType?: string;
  totalIncome: number;
  totalExpense: number;
  totalProfit: number;
  totalTaxAmount: number;
  details: ReportDetail[];
  footnotes?: Footnote[];
  [key: string]: unknown;
}

// 选中的报表接口
export interface SelectedReport {
  reportName: string;
  reportPeriod: string;
  generateTime: string;
  [key: string]: unknown;
}

// 附注接口
export interface Footnote {
  id?: string;
  content: string;
  [key: string]: unknown;
}

// 图表类型选项接口
export interface ChartTypeOption {
  label: string;
  value: string;
}

// 数据透视表配置接口
export interface PivotTableConfig {
  rows: string[];
  columns: string[];
  values: string[];
  filters: string[];
  availableFields: Array<{
    field: string;
    label: string;
  }>;
}

// 自定义模板表单接口
export interface CustomTemplateForm {
  name: string;
  reportType: string;
  description?: string;
  config: {
    columns: string[];
    chartType?: string;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
  };
}

// 自定义模板接口
export interface CustomTemplate {
  id: string;
  name: string;
  reportType: string;
  description?: string;
  config: {
    columns: string[];
    chartType?: string;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
  };
  createTime: string;
  [key: string]: unknown;
}

// 明细数据项接口
export interface DetailDataItem {
  date: string;
  businessType: string;
  transactionNo: string;
  description: string;
  dept: string;
  amount: number;
  status: string;
  operator: string;
  [key: string]: unknown;
}

// 关联数据接口
export interface RelatedData {
  id: string;
  type: string;
  name: string;
  [key: string]: unknown;
}

// ECharts数据点接
export interface ChartDataPoint {
  name: string;
  value: number;
}

// ECharts参数接口
export interface EChartsParams {
  name: string;
  value: number;
}

// 财务报表状态枚举
export enum ReportStatus {
  GENERATED = 'GENERATED',
  APPROVED = 'APPROVED',
  PUBLISHED = 'PUBLISHED'
}

// 报表类型枚举
export enum ReportType {
  INCOME_EXPENSE = 'INCOME_EXPENSE',
  PROFIT = 'PROFIT',
  TAX = 'TAX',
  BALANCE = 'BALANCE',
  CASH_FLOW = 'CASH_FLOW'
}

// 报表期间类型枚举
export enum PeriodType {
  DAY = 'day',
  WEEK = 'week',
  MONTH = 'month',
  QUARTER = 'quarter',
  YEAR = 'year'
}