/**
 * 预算管理模块类型定义
 * 从 api/budget.ts 迁移而来
 */

export interface BudgetItem {
  id: string;
  budgetName: string;
  budgetType: 'operating' | 'procurement' | 'hr' | 'marketing';
  department: string;
  totalAmount: number;
  executedAmount: number;
  executionRate: number;
  status: 'draft' | 'approved' | 'rejected';
  year: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface BudgetFormData {
  id?: string;
  budgetName: string;
  budgetType: 'operating' | 'procurement' | 'hr' | 'marketing';
  department: string;
  totalAmount: number;
  year: number;
  remark?: string;
}

export interface ControlRule {
  id: string;
  ruleName: string;
  budgetItem: string;
  controlType: 'strict' | 'warning';
  threshold: number;
  currentUsage: number;
  status: 'active' | 'inactive';
}

export interface ControlRuleFormData {
  id?: string;
  ruleName: string;
  budgetItem: string;
  controlType: 'strict' | 'warning';
  threshold: number;
  status: 'active' | 'inactive';
}

export interface BudgetStatistics {
  annualBudget: number;
  executedBudget: number;
  remainingBudget: number;
  executionRate: number;
}

export interface BudgetAnalysis {
  name: string;
  budget: number;
  actual: number;
  variance: number;
}

export interface ExecutionDetail {
  department: string;
  budgetItem: string;
  budgetAmount: number;
  actualAmount: number;
  variance: number;
  executionRate: number;
  remark: string;
}
