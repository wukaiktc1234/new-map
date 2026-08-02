/**
 * 后厨订单相关类型定义
 * 对应后端实体: KitchenOrder (kitchen_order
 */

/**
 * 后厨订单状态枚举
 */
export type KitchenOrderStatus =
  | 'pending'      // 待制作
  | 'received'     // 已接收
  | 'making'       // 制作中
  | 'completed'    // 已完成
  | 'served'       // 已出餐
  | 'cancelled';   // 已取消
/**
 * 后厨订单优先级枚举
 */
export type KitchenOrderPriority =
  | 0   // 普通
  | 1   // 加急
  | 2   // 特急
/**
 * 后厨订单信息
 */
export interface KitchenOrderInfo {
  /** 主键ID
 */
  id: number;
  /** 后厨订单ID（业务编码） */
  kitchenOrderId: string;
  /** 关联的订单ID
 */
  orderId: string;
  /** 关联的订单编
 */
  orderNumber: string;
  /** 取餐号（堂食用） */
  pickupNumber: string;
  /** 取餐码（外食用） */
  pickupCode: string;
  /** 订单类型-堂食, 1-外卖, 2-自提
 */
  orderType: number;
  /** 桌号（堂食时使用
 */
  tableNumber: string;
  /** 菜品列表（JSON数组字符串） */
  dishItems: string;
  /** 菜品总数
 */
  totalDishes: number;
  /** 订单总金额（来自关联订单
 */
  totalAmount: number;
  /** 支付方式（来自关联订单） */
  paymentMethod: string;
  /** 优先级（0-普 1-加 2-特急） */
  priority: KitchenOrderPriority;
  /** 状态（pending/received/making/completed/served/cancelled
 */
  status: KitchenOrderStatus;
  /** 接单时间
 */
  receiveTime: string;
  /** 开始制作时
 */
  makeStartTime: string;
  /** 制作完成时间
 */
  makeCompleteTime: string;
  /** 出餐时间
 */
  serveTime: string;
  /** 取消时间
 */
  cancelTime: string;
  /** 取消原因
 */
  cancelReason: string;
  /** 制作人员ID
 */
  chefId: number;
  /** 制作人员姓名
 */
  chefName: string;
  /** 门店ID
 */
  storeId: number;
  /** 门店名称
 */
  storeName: string;
  /** 原料是否已扣减（0-未扣减，1-已扣减） */
  materialConsumed: number;
  /** 原料扣减时间
 */
  materialConsumeTime: string;
  /** 原料是否已锁定（0-未锁定，1-已锁定） */
  materialLocked: number;
  /** 原料锁定时间
 */
  materialLockTime: string;
  /** 生成的食品追溯码列表（JSON数组
 */
  foodTraceCodes: string;
  /** 托盘ID
 */
  trayId: number;
  /** 托盘
 */
  trayCode: string;
  /** 托盘绑定时间
 */
  trayBindTime: string;
  /** 备注
 */
  remark: string;
  /** 创建时间
 */
  createTime: string;
  /** 更新时间
 */
  updateTime: string;
  /** 创建
 */
  createBy: string;
  /** 更新
 */
  updateBy: string;
}

/**
 * 后厨订单查询参数
 */
export interface KitchenOrderQueryForm {
  /** 后厨订单编号
 */
  kitchenOrderId?: string;
  /** 关联订单编号
 */
  orderNumber?: string;
  /** 桌号
 */
  tableNumber?: string;
  /** 订单类型
 */
  orderType?: number | null;
  /** 状
 */
  status?: KitchenOrderStatus | null;
  /** 优先
 */
  priority?: KitchenOrderPriority | null;
  /** 开始时
 */
  startTime?: string;
  /** 结束时间
 */
  endTime?: string;
  /** 制作人员姓名
 */
  chefName?: string;
}

/** 后厨订单状态选项
 */
export const KitchenOrderStatusOptions = [
{ label: '待制',
value: 'pending' },
{ label: '已接',
value: 'received' },
{ label: '制作',
value: 'making' },
{ label: '已完成',
value: 'completed' },
{ label: '已出',
value: 'served' },
{ label: '已取消',
value: 'cancelled' },
] as const;

/** 后厨订单状态文本映
 */
export const KitchenOrderStatusText: Record<KitchenOrderStatus, string> = {
  pending: '待制',
  received: '已接',
  making: '制作',
  completed: '已完成',
  served: '已出',
  cancelled: '已取消',
};

/** 后厨订单状态颜色映
 */
export const KitchenOrderStatusColor: Record<KitchenOrderStatus, string> = {
  pending: 'info',
  received: 'primary',
  making: 'warning',
  completed: 'success',
  served: 'success',
  cancelled: 'danger',
};

/** 后厨订单优先级选项
 */
export const KitchenOrderPriorityOptions = [
{ label: '普',
value: 0 },
{ label: '加',
value: 1 },
{ label: '特',
value: 2 },
] as const;

/** 后厨订单优先级文本映
 */
export const KitchenOrderPriorityText: Record<KitchenOrderPriority, string> = {
  0: '普',
  1: '加',
  2: '特',
};

/** 后厨订单优先级颜色映
 */
export const KitchenOrderPriorityColor: Record<KitchenOrderPriority, string> = {
  0: 'info',
  1: 'warning',
  2: 'danger',
};

/**
 * 后厨订单（简化版，用于API交互
 */
export interface KitchenOrder {
  id: number;
  kitchenOrderId: string;
  orderId: string;
  orderNumber: string;
  orderType: number;
  tableNumber: string;
  dishItems: string;
  totalDishes: number;
  priority: number;
  status: KitchenOrderStatus;
  receiveTime: string;
  makeStartTime: string;
  makeCompleteTime: string;
  serveTime: string;
  cancelTime: string;
  cancelReason: string;
  chefId: number;
  chefName: string;
  storeId: number;
  storeName: string;
  materialConsumed: number;
  materialConsumeTime: string;
  foodTraceCodes: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

/**
 * 后厨订单创建DTO
 */
export interface KitchenOrderCreateDTO {
  orderId: string;
  orderNumber: string;
  orderType: number;
  tableNumber: string;
  dishItems: DishItemDTO[];
  priority: number;
  storeId: number;
  storeName: string;
  remark: string;
}

/**
 * 菜品项DTO
 */
export interface DishItemDTO {
  dishId: string;
  dishName: string;
  quantity: number;
  remark: string;
}

/**
 * 原料扫描消耗DTO
 */
export interface MaterialScanConsumeDTO {
  materialTraceCode: string;
  kitchenOrderId: string;
  orderId: string;
  dishId: string;
  dishName: string;
  consumeQuantity: number;
  operatorId: number;
  operatorName: string;
  scanDevice: string;
  storeId: number;
  storeName: string;
  remark: string;
}

/** 后厨订单状态映射（含标签和类型
 */
export const KitchenOrderStatusMap: Record<KitchenOrderStatus, { label: string; type: string }> = {
pending: {
label: '待制',
type: 'info' },
received: {
label: '已接',
type: 'primary' },
making: {
label: '制作',
type: 'warning' },
completed: {
label: '已完成',
type: 'success' },
served: {
label: '已出',
type: '' },
cancelled: {
label: '已取消',
type: 'danger' },
};

/** 优先级映射（含标签和类型
 */
export const PriorityMap: Record<number, { label: string; type: string }> = {
0: {
label: '普',
type: '' },
1: {
label: '加',
type: 'warning' },
2: {
label: '特',
type: 'danger' },
};
