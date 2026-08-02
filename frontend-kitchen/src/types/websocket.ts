export interface OrderStatusNotification {
  notificationType: 'new_order' | 'status_change' | 'ready_for_pickup';
  kitchenOrderId: string;
  orderId: string;
  orderNumber: string;
  orderType: number;
  tableNumber?: string;
  status: string;
  previousStatus?: string;
  totalDishes: number;
  priority: number;
  chefName?: string;
  remark?: string;
  notificationTime: string;
}

export interface WebSocketConfig {
  url: string;
  reconnectInterval: number;
  maxReconnectAttempts: number;
}
