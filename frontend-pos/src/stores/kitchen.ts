/**
 * 后厨工作板Pinia Store
 * 管理订单列表、筛选条件、WebSocket连接状态等
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { kitchenApi } from '@/api/kitchen';
import type {
  KitchenOrderFullDTO,
  KitchenOrderStatus,
  TodayStatistics,
  WebSocketStatus,
  ChefInfo,
} from '@/types/kitchen';

/** 默认门店ID（后续可从配置或登录信息获取） */
// const DEFAULT_STORE_ID = 1;

export const useKitchenStore = defineStore('kitchen', () => {
  // ========== 状态定义 ==========

  /** 所有活跃订单列表 */
  const orders = ref<KitchenOrderFullDTO[]>([]);

  /** WebSocket连接状态 */
  const wsStatus = ref<WebSocketStatus>('disconnected');

  /** 最后更新时间 */
  const lastUpdateTime = ref<string>('');

  /** 加载状态 */
  const loading = ref<boolean>(false);

  /** 声音开关 */
  const soundEnabled = ref<boolean>(true);

  /** 当前厨师信息 */
  const currentChef = ref<ChefInfo>({
    chefId: 1,
    chefName: '默认厨师',
  });

  /** 自动刷新定时器ID */
  let refreshTimerId: number | null = null;

  // ========== 计算属性 ==========

  /** 待制作订单 (pending + received) */
  const pendingOrders = computed(() =>
    orders.value.filter(
      (order) => order.status === 'pending' || order.status === 'received'
    )
  );

  /** 制作中订单 (making) */
  const makingOrders = computed(() =>
    orders.value.filter((order) => order.status === 'making')
  );

  /** 已完成待出餐订单 (completed) */
  const completedOrders = computed(() =>
    orders.value.filter((order) => order.status === 'completed')
  );

  /** 今日统计数据 */
  const todayStats = computed<TodayStatistics>(() => {
    const total = orders.value.length;
    const completed = completedOrders.value.length;
    const making = makingOrders.value.length;
    const pending = pendingOrders.value.length;

    // 计算平均等待时间（分钟）
    const now = new Date().getTime();
    const totalWaitTime = orders.value.reduce((sum, order) => {
      if (order.createTime) {
        const createTime = new Date(order.createTime).getTime();
        const waitMinutes = (now - createTime) / (1000 * 60);
        return sum + waitMinutes;
      }
      return sum;
    }, 0);
    const avgWaitTime = total > 0 ? Math.round(totalWaitTime / total) : 0;

    // 完成率
    const completionRate = total > 0 ? Math.round((completed / total) * 100) : 0;

    return {
      totalOrders: total,
      completedCount: completed,
      completionRate,
      avgWaitTime,
      pendingCount: pending,
      makingCount: making,
    };
  });

  /** WebSocket是否已连接 */
  const isWsConnected = computed(() => wsStatus.value === 'connected');

  // ========== 操作方法 ==========

  /**
   * 加载活跃订单列表
   */
  async function loadActiveOrders(): Promise<void> {
    loading.value = true;
    try {
      const data = await kitchenApi.getActiveOrdersFull();
      if (Array.isArray(data)) {
        orders.value = data;
        lastUpdateTime.value = new Date().toLocaleString('zh-CN');
      }
    } catch (error) {
      console.error('加载活跃订单失败:', error);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 接单操作
   * @param kitchenOrderId - 后厨订单ID
   */
  async function receiveOrder(kitchenOrderId: string): Promise<boolean> {
    try {
      const result = await kitchenApi.receiveOrder(kitchenOrderId, currentChef.value);
      if (result) {
        // 更新本地订单状态
        updateOrderStatus(kitchenOrderId, 'received');
        return true;
      }
      return false;
    } catch (error) {
      console.error('接单失败:', error);
      return false;
    }
  }

  /**
   * 开始制作
   * @param kitchenOrderId - 后厨订单ID
   */
  async function startMake(kitchenOrderId: string): Promise<boolean> {
    try {
      const result = await kitchenApi.startMake(kitchenOrderId);
      if (result) {
        updateOrderStatus(kitchenOrderId, 'making');
        return true;
      }
      return false;
    } catch (error) {
      console.error('开始制作失败:', error);
      return false;
    }
  }

  /**
   * 完成制作
   * @param kitchenOrderId - 后厨订单ID
   */
  async function completeMake(kitchenOrderId: string): Promise<boolean> {
    try {
      const result = await kitchenApi.completeMake(kitchenOrderId);
      if (result) {
        updateOrderStatus(kitchenOrderId, 'completed');
        return true;
      }
      return false;
    } catch (error) {
      console.error('完成制作失败:', error);
      return false;
    }
  }

  /**
   * 出餐操作
   * @param kitchenOrderId - 后厨订单ID
   */
  async function serveOrder(kitchenOrderId: string): Promise<boolean> {
    try {
      const result = await kitchenApi.serve(kitchenOrderId);
      if (result) {
        // 从列表中移除已出餐订单
        orders.value = orders.value.filter(
          (order) => order.kitchenOrderId !== kitchenOrderId
        );
        return true;
      }
      return false;
    } catch (error) {
      console.error('出餐失败:', error);
      return false;
    }
  }

  /**
   * 取消订单
   * @param kitchenOrderId - 后厨订单ID
   * @param reason - 取消原因
   */
  async function cancelOrder(
    kitchenOrderId: string,
    reason: string
  ): Promise<boolean> {
    try {
      const result = await kitchenApi.cancelOrder(kitchenOrderId, reason);
      if (result) {
        orders.value = orders.value.filter(
          (order) => order.kitchenOrderId !== kitchenOrderId
        );
        return true;
      }
      return false;
    } catch (error) {
      console.error('取消订单失败:', error);
      return false;
    }
  }

  /**
   * 更新本地订单状态
   * @param kitchenOrderId - 后厨订单ID
   * @param status - 新状态
   */
  function updateOrderStatus(
    kitchenOrderId: string,
    status: KitchenOrderStatus
  ): void {
    const index = orders.value.findIndex(
      (order) => order.kitchenOrderId === kitchenOrderId
    );
    if (index !== -1) {
      orders.value[index].status = status;
      lastUpdateTime.value = new Date().toLocaleString('zh-CN');
    }
  }

  /**
   * 设置WebSocket连接状态
   * @param status - 连接状态
   */
  function setWsStatus(status: WebSocketStatus): void {
    wsStatus.value = status;
  }

  /**
   * 切换声音开关
   */
  function toggleSound(): void {
    soundEnabled.value = !soundEnabled.value;
    localStorage.setItem('kitchen-sound', String(soundEnabled.value));
  }

  /**
   * 设置当前厨师信息
   * @param chef - 厨师信息
   */
  function setCurrentChef(chef: ChefInfo): void {
    currentChef.value = chef;
    localStorage.setItem('kitchen-chef', JSON.stringify(chef));
  }

  /**
   * 启动自动刷新定时器
   * @param interval - 刷新间隔(毫秒)，默认10秒
   */
  function startAutoRefresh(interval: number = 10000): void {
    stopAutoRefresh();
    refreshTimerId = window.setInterval(() => {
      loadActiveOrders();
    }, interval);
  }

  /**
   * 停止自动刷新定时器
   */
  function stopAutoRefresh(): void {
    if (refreshTimerId) {
      clearInterval(refreshTimerId);
      refreshTimerId = null;
    }
  }

  /**
   * 初始化Store（从localStorage恢复设置）
   */
  function initialize(): void {
    // 恢复声音设置
    const savedSound = localStorage.getItem('kitchen-sound');
    if (savedSound !== null) {
      soundEnabled.value = savedSound === 'true';
    }

    // 恢复厨师信息
    const savedChef = localStorage.getItem('kitchen-chef');
    if (savedChef) {
      try {
        currentChef.value = JSON.parse(savedChef);
      } catch (e) {
        console.error('恢复厨师信息失败:', e);
      }
    }
  }

  return {
    // 状态
    orders,
    wsStatus,
    lastUpdateTime,
    loading,
    soundEnabled,
    currentChef,

    // 计算属性
    pendingOrders,
    makingOrders,
    completedOrders,
    todayStats,
    isWsConnected,

    // 方法
    loadActiveOrders,
    receiveOrder,
    startMake,
    completeMake,
    serveOrder,
    cancelOrder,
    setWsStatus,
    toggleSound,
    setCurrentChef,
    startAutoRefresh,
    stopAutoRefresh,
    initialize,
  };
});
