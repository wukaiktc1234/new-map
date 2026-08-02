export interface PendingItem {
  id: string;
  title: string;
  description: string;
  level: 'urgent' | 'important' | 'remind' | 'normal';
  category: string;
  route: string;
  timestamp: string;
}

export interface StatCardConfig {
  key: string;
  label: string;
  value: string;
  icon: string;
  colorType: 'primary' | 'success' | 'warning' | 'danger';
  trend?: number;
  trendLabel?: string;
  route?: string;
  requiredPermissions: string[];
}

export interface QuickActionConfig {
  key: string;
  title: string;
  icon: string;
  color: string;
  route: string;
  requiredPermissions: string[];
}

export interface SalesOverviewResponse {
  totalSales: number;
  totalOrders: number;
  avgOrderAmount: number;
  growthRate: number;
}

export interface InventoryOverviewResponse {
  totalStock: number;
  warningCount: number;
  alertCount: number;
}

export interface PurchaseStatsResponse {
  totalRequests: number;
  pendingCount: number;
  approvedCount: number;
  completedCount: number;
  totalAmount: number;
  pendingAmount: number;
}

export interface UnreadCountResponse {
  count: number;
}

export interface EmployeeCountResponse {
  count: number;
}

// 注：原 fenToYuan 函数已删除——金额转换请统一使用 utils/money 中的实现。
// 该函数原先未被任何文件 import，删除后不影响调用方。
