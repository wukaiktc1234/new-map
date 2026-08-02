/**
 * 财务集成模块类型定义
 * 从 api/financeIntegration.ts 迁移而来
 */

/**
 * 采购订单数据接口
 */
export interface PurchaseOrderData {
  orderId: string;
  orderNo: string;
  supplierId: string;
  supplierName: string;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  status: string;
  orderDate: string;
  items: PurchaseOrderItemData[];
}

/**
 * 采购订单明细接口
 */
export interface PurchaseOrderItemData {
  itemId: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  amount: number;
}

/**
 * 库存数据接口
 */
export interface InventoryData {
  productId: string;
  productName: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
  warehouseId: string;
  warehouseName: string;
  lastUpdateTime: string;
}

/**
 * 收银交易数据接口
 */
export interface CashierTransaction {
  transactionId: string;
  orderNo: string;
  amount: number;
  paymentMethod: string;
  transactionTime: string;
  cashierId: string;
  cashierName: string;
}

/**
 * 后厨成本数据接口
 */
export interface KitchenCostData {
  date: string;
  materialCost: number;
  laborCost: number;
  overheadCost: number;
  totalCost: number;
  dishes: KitchenDishCost[];
}

/**
 * 菜品成本明细接口
 */
export interface KitchenDishCost {
  dishId: string;
  dishName: string;
  quantity: number;
  materialCost: number;
  laborCost: number;
  totalCost: number;
}

/**
 * 人事薪资数据接口
 */
export interface HRSalaryData {
  employeeId: string;
  employeeName: string;
  department: string;
  position: string;
  baseSalary: number;
  bonus: number;
  deduction: number;
  netSalary: number;
  payPeriod: string;
}

/**
 * 财务集成结果接口（泛型版本）
 */
export interface FinanceIntegrationResult<T = unknown> {
  success: boolean;
  message: string;
  data: T;
  syncedAt: string;
  isMock: boolean;
}

/**
 * 同步参数接口
 */
export interface SyncParams {
  startDate: string;
  endDate: string;
  payPeriod: string;
}
