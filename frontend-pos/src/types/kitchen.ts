/**
 * 后厨工作板相关类型定义
 * 包含订单、菜品、统计等核心数据结构
 */

/** 后厨订单状态枚举 */
export type KitchenOrderStatus =
  | 'pending'      // 待制作
  | 'received'     // 已接单
  | 'making'       // 制作中
  | 'completed'    // 已完成（待出餐）
  | 'served'       // 已出餐
  | 'cancelled';   // 已取消

/** 订单类型 */
export type OrderType = 0 | 1 | 2;  // 0-堂食, 1-外卖, 2-自提

/** 优先级 */
export type Priority = 0 | 1 | 2;  // 0-普通, 1-加急, 2-特急

/** 菜品项（来自dishItems JSON） */
export interface KitchenDishItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  dishType?: string;
  /** P1-COMBO-ORDER-001: 套餐明细组件（拉单时展开） */
  productType?: number;
  comboId?: number | string;
  components?: Array<{
    foodId?: string;
    name: string;
    quantity: number;
  }>;
}

/** 后厨订单实体（对应后端KitchenOrder） */
export interface KitchenOrder {
  id: number;
  kitchenOrderId: string;
  orderId: string;
  orderNumber: string;
  pickupNumber?: string;
  pickupCode?: string;
  orderType: OrderType;
  tableNumber?: string;
  dishItems: string;  // JSON字符串
  totalDishes: number;
  totalAmount?: number;
  paymentMethod?: string;
  priority: Priority;
  status: KitchenOrderStatus;
  receiveTime?: string;
  makeStartTime?: string;
  makeCompleteTime?: string;
  serveTime?: string;
  cancelTime?: string;
  cancelReason?: string;
  chefId?: number;
  chefName?: string;
  storeId?: number;
  storeName?: string;
  remark?: string;
  createTime: string;
  updateTime: string;
}

/** 后厨订单完整信息DTO（含金额和支付信息） */
export interface KitchenOrderFullDTO extends KitchenOrder {
  actualAmount?: number;
  orderStatus?: number;
  paymentTime?: string;
  orderRemarks?: string;
  contactName?: string;
  contactPhone?: string;
}

/** 订单状态通知（WebSocket推送） */
export interface OrderStatusNotification {
  notificationType: 'new_order' | 'status_change' | 'ready_for_pickup';
  kitchenOrderId: string;
  orderId: string;
  orderNumber: string;
  orderType: OrderType;
  tableNumber?: string;
  status: string;
  previousStatus?: string;
  totalDishes: number;
  priority: Priority;
  chefName?: string;
  remark?: string;
  notificationTime: string;
}

/** 统计数据 */
export interface KitchenStatistics {
  pending: number;    // 待制作数量
  received: number;   // 已接单数量
  making: number;     // 制作中数量
  completed: number;  // 已完成数量
  served: number;     // 已出餐数量
}

/** 今日统计面板数据 */
export interface TodayStatistics {
  totalOrders: number;        // 今日总订单数
  completedCount: number;     // 已完成数
  completionRate: number;     // 完成率(百分比)
  avgWaitTime: number;        // 平均等待时间(分钟)
  pendingCount: number;       // 待处理数
  makingCount: number;        // 制作中数
}

/** WebSocket连接状态 */
export type WebSocketStatus = 'connected' | 'disconnected' | 'connecting' | 'reconnecting';

/** 厨师信息（用于接单） */
export interface ChefInfo {
  chefId: number;
  chefName: string;
}

/** 筛选条件 */
export interface KitchenFilter {
  status?: KitchenOrderStatus | 'all';
  orderType?: OrderType | 'all';
  priority?: Priority | 'all';
  keyword?: string;  // 搜索关键词（订单号/桌号）
}
