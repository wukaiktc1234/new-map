<template>
  <div class="kitchen-display" :data-theme="isDark ? 'dark' : 'light'">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="header-left">
        <div class="logo-section">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 13.87A4 4 0 0 1 7.41 6a5.11 5.11 0 0 1 1.05-1.54 5 5 0 0 1 7.08 0A5.11 5.11 0 0 1 16.59 6 4 4 0 0 1 18 13.87V21H6Z"></path>
              <line x1="6" y1="17" x2="18" y2="17"></line>
            </svg>
          </div>
          <div class="logo-text">
            <h1>后厨工作板</h1>
            <span class="subtitle">Kitchen Display System</span>
          </div>
        </div>
        <div class="time-display">
          <div class="time">{{ currentTime }}</div>
          <div class="date">{{ currentDate }}</div>
        </div>
      </div>

      <div class="header-actions">
        <el-button
          class="action-btn"
          :type="kitchenStore.soundEnabled ? 'primary' : 'info'"
          @click="handleToggleSound"
        >
          <el-icon v-if="kitchenStore.soundEnabled"><Microphone /></el-icon>
          <el-icon v-else><Mute /></el-icon>
          {{ kitchenStore.soundEnabled ? '声音开' : '声音关' }}
        </el-button>

        <el-button type="primary" class="action-btn" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>

        <el-button class="action-btn" @click="showSettings = true">
          <el-icon><Setting /></el-icon>
          设置
        </el-button>

        <el-button class="action-btn theme-btn" @click="toggleTheme">
          <el-icon v-if="isDark"><Sunny /></el-icon>
          <el-icon v-else><Moon /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 主内容区域：三栏布局 + 统计面板 -->
    <div class="main-content">
      <!-- 左侧三栏看板 -->
      <div class="board-columns">
        <!-- 待制作栏 -->
        <div class="column pending-column">
          <div class="column-header">
            <h3 class="column-title">
              <el-icon><Clock /></el-icon>
              待制作
            </h3>
            <el-badge :value="kitchenStore.pendingOrders.length" :max="99" type="warning" />
          </div>
          <div class="column-content" ref="pendingColumnRef">
            <template v-if="kitchenStore.pendingOrders.length > 0">
              <KitchenOrderCard
                v-for="order in kitchenStore.pendingOrders"
                :key="order.kitchenOrderId"
                :order="order"
              />
            </template>
            <div v-else class="empty-state">
              <el-icon :size="48"><Box /></el-icon>
              <p>暂无待制作订单</p>
            </div>
          </div>
        </div>

        <!-- 制作中栏 -->
        <div class="column making-column">
          <div class="column-header">
            <h3 class="column-title">
              <el-icon><Loading /></el-icon>
              制作中
            </h3>
            <el-badge :value="kitchenStore.makingOrders.length" :max="99" type="primary" />
          </div>
          <div class="column-content" ref="makingColumnRef">
            <template v-if="kitchenStore.makingOrders.length > 0">
              <KitchenOrderCard
                v-for="order in kitchenStore.makingOrders"
                :key="order.kitchenOrderId"
                :order="order"
              />
            </template>
            <div v-else class="empty-state">
              <el-icon :size="48"><Box /></el-icon>
              <p>暂无制作中订单</p>
            </div>
          </div>
        </div>

        <!-- 已完成栏 -->
        <div class="column completed-column">
          <div class="column-header">
            <h3 class="column-title">
              <el-icon><CircleCheck /></el-icon>
              已完成
            </h3>
            <el-badge :value="kitchenStore.completedOrders.length" :max="99" type="success" />
          </div>
          <div class="column-content" ref="completedColumnRef">
            <template v-if="kitchenStore.completedOrders.length > 0">
              <KitchenOrderCard
                v-for="order in kitchenStore.completedOrders"
                :key="order.kitchenOrderId"
                :order="order"
              />
            </template>
            <div v-else class="empty-state">
              <el-icon :size="48"><Box /></el-icon>
              <p>暂无已完成订单</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧统计面板 -->
      <div class="statistics-panel">
        <div class="panel-header">
          <h3 class="panel-title">今日统计</h3>
        </div>

        <div class="stats-grid">
          <div class="stat-card total-orders">
            <div class="stat-value">{{ kitchenStore.todayStats.totalOrders }}</div>
            <div class="stat-label">总订单数</div>
          </div>

          <div class="stat-card completion-rate">
            <div class="stat-value">{{ kitchenStore.todayStats.completionRate }}%</div>
            <div class="stat-label">完成率</div>
          </div>

          <div class="stat-card avg-wait-time">
            <div class="stat-value">{{ kitchenStore.todayStats.avgWaitTime }}min</div>
            <div class="stat-label">平均等待</div>
          </div>

          <div class="stat-card pending-count">
            <div class="stat-value">{{ kitchenStore.todayStats.pendingCount }}</div>
            <div class="stat-label">待处理</div>
          </div>

          <div class="stat-card making-count">
            <div class="stat-value">{{ kitchenStore.todayStats.makingCount }}</div>
            <div class="stat-label">制作中</div>
          </div>

          <div class="stat-card completed-count">
            <div class="stat-value">{{ kitchenStore.todayStats.completedCount }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>

        <!-- WebSocket状态指示器 -->
        <div class="ws-status" :class="kitchenStore.wsStatus">
          <div class="status-dot"></div>
          <span class="status-text">
            {{ wsStatusText }}
          </span>
        </div>
      </div>
    </div>

    <!-- 底部状态栏 -->
    <div class="bottom-bar">
      <div class="bar-left">
        <span class="connection-info">
          WebSocket: {{ wsStatusText }}
        </span>
      </div>
      <div class="bar-center">
        <span v-if="kitchenStore.lastUpdateTime">
          最后更新: {{ kitchenStore.lastUpdateTime }}
        </span>
      </div>
      <div class="bar-right">
        <span>厨师: {{ kitchenStore.currentChef.chefName }}</span>
      </div>
    </div>

    <!-- 设置对话框 -->
    <el-dialog v-model="showSettings" title="设置" width="500px" :lock-scroll="false">
      <el-form label-width="100px">
        <el-form-item label="厨师姓名">
          <el-input v-model="chefNameInput" placeholder="请输入厨师姓名" />
        </el-form-item>
        <el-form-item label="自动刷新">
          <el-switch v-model="autoRefreshEnabled" />
        </el-form-item>
        <el-form-item label="刷新间隔(秒)" v-if="autoRefreshEnabled">
          <el-input-number v-model="refreshInterval" :min="5" :max="60" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  Microphone,
  Mute,
  Refresh,
  Setting,
  Sunny,
  Moon,
  Clock,
  Loading,
  CircleCheck,
  Box,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import KitchenOrderCard from '@/components/kitchen/KitchenOrderCard.vue';
import { useKitchenStore } from '@/stores/kitchen';
import { useOperationFeedback } from '@/composables/kitchen/useOperationFeedback';

// Store
const kitchenStore = useKitchenStore();

// 操作反馈（声音提醒）
const { showWarning: showTimeoutWarning, cleanup: cleanupFeedback } = useOperationFeedback();

// 超时订单检测与声音提醒
const TIMEOUT_THRESHOLD_MINUTES = 15;
const CRITICAL_THRESHOLD_MINUTES = 30;
/** 记录已提醒过的超时订单ID，避免重复播放 */
const alertedOrderIds = ref<Set<string>>(new Set());

/**
 * 检查订单是否超时
 * @param createTime - 订单创建时间字符串
 * @returns 等待分钟数，未超时返回0
 */
function getWaitMinutes(createTime: string | undefined): number {
  if (!createTime) return 0;
  const created = new Date(createTime).getTime();
  const now = Date.now();
  return Math.floor((now - created) / (1000 * 60));
}

/**
 * 获取当前超时订单列表（pending + making 中等待超过阈值的）
 */
const timeoutOrders = computed(() => {
  const allOrders = [...kitchenStore.pendingOrders, ...kitchenStore.makingOrders];
  return allOrders.filter((order: { createTime?: string; kitchenOrderId?: string }) => {
    const mins = getWaitMinutes(order.createTime);
    return mins >= TIMEOUT_THRESHOLD_MINUTES;
  });
});

/**
 * 监听超时订单变化，首次发现超时时播放警告音
 */
watch(timeoutOrders, (newTimeoutOrders, oldTimeoutOrders) => {
  // 声音关闭时不播放
  if (!kitchenStore.soundEnabled) return;

  // 找出新增的超时订单（之前不在超时列表中的）
  const oldIds = new Set((oldTimeoutOrders || []).map((o: { kitchenOrderId?: string }) => o.kitchenOrderId));
  const newAlerts = newTimeoutOrders.filter(
    (o: { kitchenOrderId?: string }) => !oldIds.has(o.kitchenOrderId) && !alertedOrderIds.value.has(o.kitchenOrderId || '')
  );

  if (newAlerts.length > 0) {
    // 标记已提醒
    newAlerts.forEach((o: { kitchenOrderId?: string }) => {
      if (o.kitchenOrderId) alertedOrderIds.value.add(o.kitchenOrderId);
    });

    // 统计紧急程度
    const criticalCount = newAlerts.filter((o: { createTime?: string }) => getWaitMinutes(o.createTime) >= CRITICAL_THRESHOLD_MINUTES).length;
    const message = criticalCount > 0
      ? `检测到 ${newAlerts.length} 个超时订单（${criticalCount} 个紧急）`
      : `检测到 ${newAlerts.length} 个超时订单`;

    // 播放警告音（440Hz warning 类型）
    showTimeoutWarning(message, { sound: true, vibration: true });
  }
}, { deep: true });

// 响应式状态
const isDark = ref(false);
const showSettings = ref(false);
const chefNameInput = ref('');
const autoRefreshEnabled = ref(true);
const refreshInterval = ref(10);

// 时间显示
const currentTime = ref('');
const currentDate = ref('');

// 定时器
let timeTimer: number | null = null;

// 列容器引用（用于滚动到顶部，预留扩展）
// const pendingColumnRef = ref<HTMLElement | null>(null);
// const makingColumnRef = ref<HTMLElement | null>(null);
// const completedColumnRef = ref<HTMLElement | null>(null);

// 计算属性：WebSocket状态文本
const wsStatusText = computed<string>(() => {
  const statusMap: Record<string, string> = {
    connected: '已连接',
    disconnected: '未连接',
    connecting: '连接中...',
    reconnecting: '重连中...',
  };
  return statusMap[kitchenStore.wsStatus] || '未知';
});

// 更新时间显示
function updateTime(): void {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long',
  });
}

// 切换主题
function toggleTheme(): void {
  isDark.value = !isDark.value;
  localStorage.setItem('kitchen-theme', isDark.value ? 'dark' : 'light');
}

// 切换声音
function handleToggleSound(): void {
  kitchenStore.toggleSound();
  ElMessage.success(kitchenStore.soundEnabled ? '声音已开启' : '声音已关闭');
}

// 手动刷新
async function handleRefresh(): Promise<void> {
  await kitchenStore.loadActiveOrders();
  ElMessage.success('数据已刷新');
}

// 保存设置
function saveSettings(): void {
  if (chefNameInput.value.trim()) {
    kitchenStore.setCurrentChef({
      chefId: kitchenStore.currentChef.chefId,
      chefName: chefNameInput.value.trim(),
    });
  }

  if (autoRefreshEnabled.value) {
    kitchenStore.startAutoRefresh(refreshInterval.value * 1000);
  } else {
    kitchenStore.stopAutoRefresh();
  }

  localStorage.setItem('kitchen-auto-refresh', String(autoRefreshEnabled.value));
  localStorage.setItem('kitchen-refresh-interval', String(refreshInterval.value));

  showSettings.value = false;
  ElMessage.success('设置已保存');
}

// 加载保存的设置
function loadSavedSettings(): void {
  const savedTheme = localStorage.getItem('kitchen-theme');
  if (savedTheme === 'dark') {
    isDark.value = true;
  }

  const savedAutoRefresh = localStorage.getItem('kitchen-auto-refresh');
  if (savedAutoRefresh !== null) {
    autoRefreshEnabled.value = savedAutoRefresh === 'true';
  }

  const savedInterval = localStorage.getItem('kitchen-refresh-interval');
  if (savedInterval) {
    refreshInterval.value = parseInt(savedInterval, 10) || 10;
  }
}

// 组件挂载时初始化
onMounted(async () => {
  // 初始化Store
  kitchenStore.initialize();

  // 加载保存的设置
  loadSavedSettings();

  // 更新时间
  updateTime();
  timeTimer = window.setInterval(updateTime, 1000);

  // 加载订单数据
  await kitchenStore.loadActiveOrders();

  // 启动自动刷新
  if (autoRefreshEnabled.value) {
    kitchenStore.startAutoRefresh(refreshInterval.value * 1000);
  }

  // 初始化厨师名称输入框
  chefNameInput.value = kitchenStore.currentChef.chefName;

  // TODO: 后续集成WebSocket连接
  // kitchenStore.setWsStatus('connecting');
});

// 组件卸载时清理
onUnmounted(() => {
  // 清理定时器
  if (timeTimer) clearInterval(timeTimer);

  // 停止自动刷新
  kitchenStore.stopAutoRefresh();

  // 清理操作反馈资源（AudioContext）
  cleanupFeedback();
});
</script>

<style scoped>
.kitchen-display {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--pos-bg-primary);
  overflow: hidden;
  transition: background-color 0.3s ease;
}

/* ========== 顶部操作栏 ========== */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--pos-glass-bg);
  backdrop-filter: var(--pos-glass-blur);
  border-bottom: 1px solid var(--pos-border-color);
  min-height: 72px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-icon svg {
  width: 28px;
  height: 28px;
}

.logo-text h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--pos-text-primary);
}

.logo-text .subtitle {
  font-size: 12px;
  color: var(--pos-text-muted);
}

.time-display {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding-left: 20px;
  border-left: 2px solid var(--pos-border-color);
}

.time-display .time {
  font-size: 26px;
  font-weight: 600;
  font-family: 'SF Mono', 'Monaco', monospace;
  color: var(--pos-primary);
  line-height: 1.2;
}

.time-display .date {
  font-size: 12px;
  color: var(--pos-text-muted);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  height: 44px;
  padding: 0 18px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  touch-action: manipulation;
}

.action-btn .el-icon {
  font-size: 18px;
}

.theme-btn {
  width: 44px;
  padding: 0;
  justify-content: center;
}

/* ========== 主内容区域 ========== */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

/* 三栏布局 */
.board-columns {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  overflow: hidden;
}

.column {
  background: var(--pos-bg-card);
  border-radius: 16px;
  border: 1px solid var(--pos-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--pos-border-light);
  background: var(--pos-bg-secondary);
}

.column-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--pos-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.column-title .el-icon {
  font-size: 20px;
}

.pending-column .column-title {
  color: #f59e0b;
}

.making-column .column-title {
  color: #ea580c;
}

.completed-column .column-title {
  color: #10b981;
}

.column-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  scrollbar-width: thin;
  scrollbar-color: var(--pos-border-color) transparent;
}

.column-content::-webkit-scrollbar {
  width: 6px;
}

.column-content::-webkit-scrollbar-track {
  background: transparent;
}

.column-content::-webkit-scrollbar-thumb {
  background: var(--pos-border-color);
  border-radius: 3px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 12px;
  color: var(--pos-text-muted);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* ========== 右侧统计面板 ========== */
.statistics-panel {
  width: 280px;
  background: var(--pos-bg-card);
  border-radius: 16px;
  border: 1px solid var(--pos-border-light);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--pos-border-light);
  background: var(--pos-bg-secondary);
}

.panel-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--pos-text-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
}

.stat-card {
  background: var(--pos-bg-primary);
  border-radius: 12px;
  padding: 16px 12px;
  text-align: center;
  border: 1px solid var(--pos-border-light);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--pos-shadow-sm);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--pos-primary);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--pos-text-muted);
  font-weight: 500;
}

.total-orders .stat-value {
  color: #ea580c;
}

.completion-rate .stat-value {
  color: #10b981;
}

.avg-wait-time .stat-value {
  color: #f59e0b;
}

.pending-count .stat-value {
  color: #ef4444;
}

.making-count .stat-value {
  color: #f97316;
}

.completed-count .stat-value {
  color: #06b6d4;
}

/* WebSocket状态 */
.ws-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin: 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.ws-status.connected {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.ws-status.connected .status-dot {
  background: #10b981;
}

.ws-status.disconnected {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.ws-status.disconnected .status-dot {
  background: #ef4444;
}

.ws-status.connecting,
.ws-status.reconnecting {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.ws-status.connecting .status-dot,
.ws-status.reconnecting .status-dot {
  background: #f59e0b;
}

/* ========== 底部状态栏 ========== */
.bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: var(--pos-glass-bg);
  border-top: 1px solid var(--pos-border-color);
  font-size: 13px;
  color: var(--pos-text-secondary);
  min-height: 48px;
}

.bar-left,
.bar-center,
.bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.connection-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ========== 响应式设计 ========== */
@media (max-width: 1400px) {
  .statistics-panel {
    width: 240px;
  }

  .stat-value {
    font-size: 24px;
  }

  .board-columns {
    gap: 12px;
  }
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .board-columns {
    grid-template-columns: 1fr;
    flex: 1;
  }

  .statistics-panel {
    width: 100%;
    max-height: 250px;
  }

  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .top-bar {
    flex-direction: column;
    gap: 12px;
    padding: 12px 16px;
    min-height: auto;
  }

  .header-left {
    width: 100%;
    justify-content: space-between;
  }

  .header-actions {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }

  .action-btn {
    height: 40px;
    padding: 0 14px;
    font-size: 13px;
  }

  .bottom-bar {
    flex-direction: column;
    gap: 4px;
    text-align: center;
    padding: 8px 16px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
