/**
 * 食品追溯码类型定义
 * 迁移自 types/foodTraceCode.ts，并补充 Backend/Display 类型
 * 对应后端 FoodTraceCodeController（/v1/food-trace-code）
 */

/** 食品制作状态 */
export type FoodMakeStatus = 'pending' | 'making' | 'completed' | 'served'

/** 食品追溯码状态 */
export type FoodTraceCodeStatus = 'created' | 'printed' | 'served' | 'expired'

/** 食品追溯码（基础类型） */
export interface FoodTraceCode {
  id?: number
  traceCodeId?: string
  traceCode?: string
  qrCodeUrl?: string
  orderId?: string
  orderNumber?: string
  orderType?: number
  dishId?: string
  dishName?: string
  dishPrice?: number
  quantity?: number
  materialTraceCodes?: string
  materialDetails?: string
  kitchenOrderId?: number
  makeStatus?: FoodMakeStatus
  makeStartTime?: string
  makeCompleteTime?: string
  chefId?: number
  chefName?: string
  serveTime?: string
  serveMethod?: string
  tableNumber?: string
  storeId?: number
  storeName?: string
  status?: FoodTraceCodeStatus
  printTime?: string
  printCount?: number
  materialCost?: number
  laborCost?: number
  totalCost?: number
  remark?: string
  createTime?: string
  updateTime: string
}

/** 食品追溯码生成 DTO */
export interface FoodTraceCodeGenerateDTO {
  orderId?: string
  orderNumber?: string
  orderType?: number
  dishId?: string
  dishName?: string
  dishPrice?: number
  quantity?: number
  kitchenOrderId?: number
  chefId?: number
  chefName?: string
  storeId?: number
  storeName?: string
  tableNumber?: string
  materialTraceCodes?: string[]
  materialCost?: number
  laborCost?: number
  remark: string
}

/** 食品追溯码查询参数 */
export interface FoodTraceCodeQuery {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 订单ID */
  orderId?: string
  /** 状态 */
  status?: FoodTraceCodeStatus
  /** 制作状态 */
  makeStatus?: FoodMakeStatus
  /** 门店ID */
  storeId?: number
  /** 关键字（追溯码/订单号/菜品名） */
  keyword?: string
}

// ============================================================
// 补充类型（Backend/Display，用于 DataConverter）
// ============================================================

/** 后端原始类型（成本为分） */
export interface FoodTraceCodeBackend {
  id?: number
  traceCodeId?: string
  traceCode?: string
  qrCodeUrl?: string
  orderId?: string
  orderNumber?: string
  orderType?: number
  dishId?: string
  dishName?: string
  dishPrice?: number
  quantity?: number
  materialTraceCodes?: string
  materialDetails?: string
  kitchenOrderId?: number
  makeStatus?: FoodMakeStatus
  makeStartTime?: string
  makeCompleteTime?: string
  chefId?: number
  chefName?: string
  serveTime?: string
  serveMethod?: string
  tableNumber?: string
  storeId?: number
  storeName?: string
  status?: FoodTraceCodeStatus
  printTime?: string
  printCount?: number
  /** 原料成本（分） */
  materialCost?: number
  /** 人工成本（分） */
  laborCost?: number
  /** 总成本（分） */
  totalCost?: number
  remark?: string
  createTime?: string
  updateTime: string
}

/** 前端展示类型（成本为元字符串，含标签字段） */
export interface FoodTraceCodeDisplay extends Omit<FoodTraceCode, 'materialCost' | 'laborCost' | 'totalCost'> {
  /** 原料成本（元） */
  materialCostYuan?: string
  /** 人工成本（元） */
  laborCostYuan?: string
  /** 总成本（元） */
  totalCostYuan?: string
  /** 制作状态标签 */
  makeStatusLabel?: string
  /** 状态标签 */
  statusLabel?: string
}

/** 食品制作状态映射 */
export const FoodMakeStatusMap: Record<FoodMakeStatus, { label: string; type: string }> = {
  pending: { label: '待制作', type: 'info' },
  making: { label: '制作中', type: 'warning' },
  completed: { label: '已完成', type: 'success' },
  served: { label: '已出餐', type: 'primary' },
}

/** 食品追溯码状态映射 */
export const FoodTraceCodeStatusMap: Record<FoodTraceCodeStatus, { label: string; type: string }> = {
  created: { label: '已创建', type: 'info' },
  printed: { label: '已打印', type: 'warning' },
  served: { label: '已出餐', type: 'success' },
  expired: { label: '已过期', type: 'danger' },
}

/** 订单类型映射 */
export const OrderTypeMap: Record<number, { label: string; type: string }> = {
  0: { label: '堂食', type: 'primary' },
  1: { label: '外卖', type: 'warning' },
  2: { label: '自提', type: 'success' },
}
