/**
 * 后厨工作板API封装
 * 封装所有后厨相关的API调用
 * 注意：request实例已在响应拦截器中提取data.data，因此返回值是实际数据而非AxiosResponse
 */
import request from './request';
import type {
  KitchenOrder,
  KitchenOrderFullDTO,
  KitchenStatistics,
  ChefInfo,
} from '@/types/kitchen';

/** 后厨API对象 */
export const kitchenApi = {
  /**
   * 获取待处理订单完整信息列表（活跃订单）
   * @returns 订单完整信息列表
   */
  getActiveOrdersFull(): Promise<KitchenOrderFullDTO[]> {
    return request.get('/v1/kitchen-order/active/full') as Promise<KitchenOrderFullDTO[]>;
  },

  /**
   * 获取最近订单完整信息列表
   * @param limit - 数量限制，默认50
   * @returns 订单完整信息列表
   */
  getRecentOrdersFull(limit: number = 50): Promise<KitchenOrderFullDTO[]> {
    return request.get('/v1/kitchen-order/recent/full', { params: { limit } }) as Promise<KitchenOrderFullDTO[]>;
  },

  /**
   * 根据ID查询后厨订单
   * @param kitchenOrderId - 后厨订单ID
   * @returns 后厨订单信息
   */
  getById(kitchenOrderId: string): Promise<KitchenOrder> {
    return request.get(`/v1/kitchen-order/${kitchenOrderId}`) as Promise<KitchenOrder>;
  },

  /**
   * 根据ID查询后厨订单完整信息（含金额）
   * @param kitchenOrderId - 后厨订单ID
   * @returns 完整订单信息
   */
  getFullById(kitchenOrderId: string): Promise<KitchenOrderFullDTO> {
    return request.get(`/v1/kitchen-order/${kitchenOrderId}/full`) as Promise<KitchenOrderFullDTO>;
  },

  /**
   * 分页查询后厨订单列表
   * @param params - 查询参数
   * @returns 分页结果
   */
  getList(params?: {
    page?: number;
    size?: number;
    status?: string;
    storeId?: number;
    chefId?: number;
  }): Promise<{ records: KitchenOrder[]; total: number }> {
    return request.get('/v1/kitchen-order/list', params ? { params } : undefined) as Promise<{ records: KitchenOrder[]; total: number }>;
  },

  /**
   * 接单操作
   * @param kitchenOrderId - 后厨订单ID
   * @param chef - 厨师信息
   * @returns 操作结果
   */
  receiveOrder(kitchenOrderId: string, chef: ChefInfo): Promise<boolean> {
    return request.post(
      `/v1/kitchen-order/${kitchenOrderId}/receive`,
      null,
      { params: { chefId: chef.chefId, chefName: chef.chefName } }
    ) as Promise<boolean>;
  },

  /**
   * 开始制作
   * @param kitchenOrderId - 后厨订单ID
   * @returns 操作结果
   */
  startMake(kitchenOrderId: string): Promise<boolean> {
    return request.post(`/v1/kitchen-order/${kitchenOrderId}/start-make`) as Promise<boolean>;
  },

  /**
   * 完成制作
   * @param kitchenOrderId - 后厨订单ID
   * @returns 操作结果
   */
  completeMake(kitchenOrderId: string): Promise<boolean> {
    return request.post(`/v1/kitchen-order/${kitchenOrderId}/complete-make`) as Promise<boolean>;
  },

  /**
   * 出餐操作
   * @param kitchenOrderId - 后厨订单ID
   * @returns 操作结果
   */
  serve(kitchenOrderId: string): Promise<boolean> {
    return request.post(`/v1/kitchen-order/${kitchenOrderId}/serve`) as Promise<boolean>;
  },

  /**
   * 取消订单
   * @param kitchenOrderId - 后厨订单ID
   * @param reason - 取消原因
   * @returns 操作结果
   */
  cancelOrder(kitchenOrderId: string, reason: string): Promise<boolean> {
    return request.post(
      `/v1/kitchen-order/${kitchenOrderId}/cancel`,
      null,
      { params: { reason } }
    ) as Promise<boolean>;
  },

  /**
   * 更新优先级
   * @param kitchenOrderId - 后厨订单ID
   * @param priority - 优先级(0-普通, 1-加急, 2-特急)
   * @returns 操作结果
   */
  updatePriority(kitchenOrderId: string, priority: number): Promise<boolean> {
    return request.put(
      `/v1/kitchen-order/${kitchenOrderId}/priority`,
      null,
      { params: { priority } }
    ) as Promise<boolean>;
  },

  /**
   * 获取门店统计信息
   * @param storeId - 门店ID
   * @returns 统计数据
   */
  getStatistics(storeId: number): Promise<KitchenStatistics> {
    return request.get(`/v1/kitchen-order/statistics/${storeId}`) as Promise<KitchenStatistics>;
  },
};
