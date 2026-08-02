/**
 * 成本管理模块类型定义
 * 从 api/cost.ts 迁移而来
 */

export interface CostSummary {
  totalCost: number;
  costByType: {
    material: number;
    labor: number;
    operation: number;
    other: number;
  };
  costByDate: CostDateItem[];
}

export interface CostDateItem {
  date: string;
  amount: number;
}

export interface CostRecord {
  costId: string;
  costType: 'material' | 'labor' | 'operation' | 'other';
  costName: string;
  amount: number;
  storeName: string;
  costDate: string;
  allocationType: 'none' | 'store' | 'department' | 'product';
  createTime: string;
  remark?: string;
}

export interface CostFormData {
  costType: 'material' | 'labor' | 'operation' | 'other';
  costName: string;
  amount: number;
  costDate: Date;
  allocationType: 'none' | 'store' | 'department' | 'product';
  remark?: string;
}

export interface CostQueryParams {
  storeId?: number;
  costType?: string;
  startDate?: string;
  endDate?: string;
}

export interface CostElement {
  id: string;
  code: string;
  name: string;
  type: 'material' | 'labor' | 'manufacturing';
  allocationMethod: 'direct' | 'labor_hour' | 'output' | 'machine_hour';
  rate: number;
  status: 'active' | 'inactive';
}

export interface CostCalculation {
  id: string;
  period: string;
  costCenter: string;
  totalCost: number;
  directMaterial: number;
  directLabor: number;
  manufacturing: number;
  status: 'draft' | 'calculated' | 'confirmed';
}

export interface CostAdjustment {
  id: string;
  costCenter: string;
  adjustmentType: 'increase' | 'decrease';
  amount: number;
  adjustmentDate: string;
  reason: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface InventoryValuation {
  id: string;
  materialName: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  valuationMethod: 'fifo' | 'weighted_average' | 'moving_average';
}
