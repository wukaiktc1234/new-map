import request from './request'

export interface KitchenOrder {
  kitchenOrderId: string
  orderNumber: string
  tableNumber?: string
  status: 'pending' | 'received' | 'making' | 'completed' | 'served' | 'cancelled'
  priority: number
  totalDishes: number
  dishItems: string
  chefId?: number
  chefName?: string
  storeId?: number
  storeName?: string
  remark?: string
  createTime: string
  receiveTime?: string
  makeStartTime?: string
  makeCompleteTime?: string
  serveTime?: string
  updateTime?: string
}

export interface OrderMaterialRequirement {
  id: string
  kitchenOrderId: string
  orderNumber: string
  materialId: string
  materialName: string
  requiredQuantity: number
  usedQuantity: number
  unit: string
  status: 'pending' | 'partial' | 'completed'
  dishName: string
}

export interface MaterialScanConsumeDTO {
  traceCode: string
}

export interface KitchenOrderCreateDTO {
  orderNumber: string
  tableNumber?: number
  orderType: number
  priority?: number
  storeId?: number
  storeName?: string
  remark?: string
  dishItems: Array<{
    name: string
    quantity: number
    price: number
  }>
}

export const kitchenOrderApi = {
  create: (data: KitchenOrderCreateDTO) => {
    return request.post<KitchenOrder>('/v1/kitchen-order/create', data)
  },

  list: (params: { 
    page: number
    size: number
    status?: string
    storeId?: number
    chefId?: number
  }) => {
    return request.get<{ records: KitchenOrder[]; total: number }>('/v1/kitchen-order/list', params)
  },

  getPendingOrders: (storeId?: number) => {
    return request.get<KitchenOrder[]>('/v1/kitchen/pending-orders', storeId ? { storeId } : undefined)
  },

  getMakingOrders: (storeId?: number) => {
    return request.get<KitchenOrder[]>('/v1/kitchen/making-orders', storeId ? { storeId } : undefined)
  },

  getById: (kitchenOrderId: string) => {
    return request.get<KitchenOrder>(`/v1/kitchen-order/${kitchenOrderId}`)
  },

  getByOrderId: (orderId: string) => {
    return request.get<KitchenOrder>(`/v1/kitchen-order/order/${orderId}`)
  },

  receiveOrder: (kitchenOrderId: string, chefId: number, chefName: string) => {
    return request.post<boolean>(`/v1/kitchen-order/${kitchenOrderId}/receive`, { chefId, chefName })
  },

  startMake: (kitchenOrderId: string) => {
    return request.post<boolean>(`/v1/kitchen-order/${kitchenOrderId}/start-make`)
  },

  completeMake: (kitchenOrderId: string) => {
    return request.post<boolean>(`/v1/kitchen-order/${kitchenOrderId}/complete-make`)
  },

  serve: (kitchenOrderId: string) => {
    return request.post<boolean>(`/v1/kitchen-order/${kitchenOrderId}/serve`)
  },

  cancel: (kitchenOrderId: string, reason: string) => {
    return request.post<boolean>(`/v1/kitchen-order/${kitchenOrderId}/cancel`, { reason })
  },

  scanConsumeMaterial: (traceCode: string) => {
    return request.post<{
      success: boolean
      message: string
      materialName?: string
      usedQuantity?: number
      unit?: string
      orderNumber?: string
      dishName?: string
      triggeredMaking?: boolean
    }>('/v1/kitchen/scan', { traceCode })
  },

  getOrderRequirements: (kitchenOrderId: string) => {
    return request.get<OrderMaterialRequirement[]>(`/v1/kitchen/order-requirements/${kitchenOrderId}`)
  },

  getStoreActiveOrders: (storeId: number) => {
    return request.get<KitchenOrder[]>(`/v1/kitchen-order/store-active/${storeId}`)
  },

  getChefActiveOrders: (chefId: number) => {
    return request.get<KitchenOrder[]>(`/v1/kitchen-order/chef-active/${chefId}`)
  },

  updatePriority: (kitchenOrderId: string, priority: number) => {
    return request.put<boolean>(`/v1/kitchen-order/${kitchenOrderId}/priority`, { priority })
  },

  /**
   * 手动流转托盘状态（兜底/测试轨）—— 与扫码自动轨并存。
   * 后端强制要求 reason 并写入审计（tray_scan_record.scan_type=MANUAL）。
   * 允许目标：making / ready / cleaning / idle；served 必须走出餐确认（触发扣料与追溯码）。
   */
  manualTrayTransition: (trayCode: string, targetStatus: string, reason: string) => {
    return request.post<unknown>('/v1/tray/manual-transition', null, {
      params: { trayCode, targetStatus, reason },
    })
  },
  getStatistics: (storeId: number) => {
    return request.get<Record<string, number>>(`/v1/kitchen-order/statistics/${storeId}`)
  }
}
