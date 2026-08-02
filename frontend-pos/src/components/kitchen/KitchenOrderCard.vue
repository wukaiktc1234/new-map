<template>
  <div
    class="kitchen-order-card"
    :class="[
      `status-${order.status}`,
      `priority-${order.priority}`,
      { 'is-urgent': isUrgent },
      { 'is-timeout-order': isTimeout && !isCritical },
      { 'is-critical-order': isCritical }
    ]"
  >
    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="order-info">
        <span class="order-number">{{ order.orderNumber }}</span>
        <el-tag
          v-if="order.priority > 0"
          :type="order.priority === 2 ? 'danger' : 'warning'"
          size="small"
          effect="dark"
          class="priority-tag"
        >
          {{ order.priority === 2 ? '特急' : '加急' }}
        </el-tag>
      </div>
      <div class="order-meta">
        <span v-if="order.tableNumber" class="table-number">
          <el-icon><Grid /></el-icon>
          {{ order.tableNumber }}
        </span>
        <span class="wait-time" :class="{ 'is-timeout': isTimeout }">
          <el-icon><Clock /></el-icon>
          {{ waitTimeText }}
        </span>
        <!-- 超时警告标签 -->
        <el-tag
          v-if="isTimeout && !isCritical"
          type="warning"
          size="small"
          effect="dark"
          class="timeout-tag"
        >
          已等待 {{ waitMinutes }} 分钟
        </el-tag>
        <!-- 紧急标签（超过30分钟） -->
        <el-tag
          v-if="isCritical"
          type="danger"
          size="small"
          effect="dark"
          class="timeout-tag critical-tag"
        >
          紧急 {{ waitMinutes }} 分钟
        </el-tag>
      </div>
    </div>

    <!-- 菜品列表 -->
    <div class="dish-list">
      <div
        v-for="(dish, index) in parsedDishItems"
        :key="index"
        class="dish-item"
      >
        <span class="dish-name">{{ dish.name }}</span>
        <span class="dish-quantity">×{{ dish.quantity }}</span>
      </div>
    </div>

    <!-- 卡片底部 -->
    <div class="card-footer">
      <div v-if="order.remark" class="remark">
        <el-icon><Document /></el-icon>
        {{ order.remark }}
      </div>
      <div class="chef-info" v-if="order.chefName">
        <el-icon><User /></el-icon>
        {{ order.chefName }}
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <template v-if="order.status === 'pending' || order.status === 'received'">
        <el-button
          type="primary"
          size="large"
          @click="handleStartMake"
          class="action-btn start-btn"
        >
          <el-icon><VideoPlay /></el-icon>
          开始制作
        </el-button>
      </template>
      <template v-else-if="order.status === 'making'">
        <el-button
          type="success"
          size="large"
          @click="handleComplete"
          class="action-btn complete-btn"
        >
          <el-icon><CircleCheck /></el-icon>
          完成制作
        </el-button>
      </template>
      <template v-else-if="order.status === 'completed'">
        <el-button
          type="warning"
          size="large"
          @click="handleServe"
          class="action-btn serve-btn"
        >
          <el-icon><Check /></el-icon>
          出餐
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Grid,
  Clock,
  Document,
  User,
  VideoPlay,
  CircleCheck,
  Check,
} from '@element-plus/icons-vue';
import type { KitchenOrderFullDTO, KitchenDishItem } from '@/types/kitchen';
import { useKitchenStore } from '@/stores/kitchen';

// Props定义
interface Props {
  order: KitchenOrderFullDTO;
}

const props = defineProps<Props>();

// Store
const kitchenStore = useKitchenStore();

// 解析菜品列表
const parsedDishItems = computed<KitchenDishItem[]>(() => {
  try {
    if (!props.order.dishItems) return [];
    if (typeof props.order.dishItems === 'string') {
      return JSON.parse(props.order.dishItems);
    }
    return props.order.dishItems as KitchenDishItem[];
  } catch (error) {
    console.error('解析菜品列表失败:', error);
    return [];
  }
});

// 计算等待时间（分钟）
const waitMinutes = computed<number>(() => {
  if (!props.order.createTime) return 0;
  const createTime = new Date(props.order.createTime).getTime();
  const now = new Date().getTime();
  return Math.floor((now - createTime) / (1000 * 60));
});

// 等待时间文本
const waitTimeText = computed<string>(() => {
  const mins = waitMinutes.value;
  if (mins < 1) return '刚刚';
  if (mins < 60) return `${mins}分钟`;
  const hours = Math.floor(mins / 60);
  const remainingMins = mins % 60;
  return remainingMins > 0 ? `${hours}小时${remainingMins}分` : `${hours}小时`;
});

// 是否超时（超过15分钟）
const isTimeout = computed<boolean>(() => waitMinutes.value >= 15);

// 是否紧急（超过30分钟）
const isCritical = computed<boolean>(() => waitMinutes.value >= 30);

// 是否紧急（优先级>0或超时）
const isUrgent = computed<boolean>(
  () => props.order.priority > 0 || isTimeout.value
);

// 开始制作
async function handleStartMake(): Promise<void> {
  try {
    if (props.order.status === 'pending') {
      // 需要先接单
      const success = await kitchenStore.receiveOrder(props.order.kitchenOrderId);
      if (!success) {
        ElMessage.error('接单失败');
        return;
      }
    }

    const success = await kitchenStore.startMake(props.order.kitchenOrderId);
    if (success) {
      ElMessage.success('开始制作');
    } else {
      ElMessage.error('操作失败');
    }
  } catch (error) {
    console.error('开始制作失败:', error);
    ElMessage.error('操作失败');
  }
}

// 完成制作
async function handleComplete(): Promise<void> {
  try {
    const success = await kitchenStore.completeMake(props.order.kitchenOrderId);
    if (success) {
      ElMessage.success('制作完成');
    } else {
      ElMessage.error('操作失败');
    }
  } catch (error) {
    console.error('完成制作失败:', error);
    ElMessage.error('操作失败');
  }
}

// 出餐
async function handleServe(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认订单 ${props.order.orderNumber} 已出餐？`,
      '出餐确认',
      {
        confirmButtonText: '确认出餐',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    const success = await kitchenStore.serveOrder(props.order.kitchenOrderId);
    if (success) {
      ElMessage.success('出餐成功');
    } else {
      ElMessage.error('操作失败');
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      console.error('出餐失败:', error);
      ElMessage.error('操作失败');
    }
  }
}
</script>

<style scoped>
.kitchen-order-card {
  background: var(--pos-bg-card);
  border: 2px solid var(--pos-border-light);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.kitchen-order-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--pos-shadow-primary);
}

/* 状态样式 */
.kitchen-order-card.status-pending,
.kitchen-order-card.status-received {
  border-left: 4px solid #f59e0b;
}

.kitchen-order-card.status-making {
  border-left: 4px solid #ea580c;
  background: linear-gradient(135deg, rgba(234, 88, 12, 0.05) 0%, var(--pos-bg-card) 100%);
}

.kitchen-order-card.status-completed {
  border-left: 4px solid #10b981;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.05) 0%, var(--pos-bg-card) 100%);
}

/* 优先级样式 */
.kitchen-order-card.priority-1 {
  border-color: #f59e0b;
}

.kitchen-order-card.priority-2 {
  border-color: #ef4444;
  animation: pulse-urgent 2s ease-in-out infinite;
}

/* 超时状态：红色脉动边框 */
.kitchen-order-card.is-timeout-order {
  border-color: #ef4444 !important;
  animation: pulse-timeout-border 1.5s ease-in-out infinite;
}

/* 紧急状态（超过30分钟）：更强脉动效果 */
.kitchen-order-card.is-critical-order {
  border-color: #dc2626 !important;
  border-width: 3px !important;
  animation: pulse-critical-border 1s ease-in-out infinite;
  box-shadow: 0 0 16px rgba(220, 38, 38, 0.4);
}

@keyframes pulse-timeout-border {
  0%, 100% {
    border-color: #ef4444;
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.2);
  }
  50% {
    border-color: #f87171;
    box-shadow: 0 0 12px 4px rgba(239, 68, 68, 0.35);
  }
}

@keyframes pulse-critical-border {
  0%, 100% {
    border-color: #dc2626;
    box-shadow: 0 0 16px rgba(220, 38, 38, 0.4);
  }
  50% {
    border-color: #ef4444;
    box-shadow: 0 0 24px 8px rgba(220, 38, 38, 0.6);
  }
}

/* 紧急状态 */
.kitchen-order-card.is-urgent {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);
}

@keyframes pulse-urgent {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);
  }
  50% {
    box-shadow: 0 4px 20px rgba(239, 68, 68, 0.4);
  }
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--pos-border-light);
}

.order-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-number {
  font-size: 18px;
  font-weight: 700;
  color: var(--pos-text-primary);
  font-family: 'SF Mono', 'Monaco', monospace;
}

.priority-tag {
  font-weight: 600;
}

.order-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-number,
.wait-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--pos-text-secondary);
  font-weight: 500;
}

.wait-time.is-timeout {
  color: #ef4444;
  font-weight: 600;
}

/* 超时/紧急标签 */
.timeout-tag {
  font-weight: 700;
  font-size: 11px;
  animation: timeout-tag-pulse 2s ease-in-out infinite;
}

.critical-tag {
  animation: critical-tag-pulse 1s ease-in-out infinite;
}

@keyframes timeout-tag-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes critical-tag-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.05); }
}

/* 菜品列表 */
.dish-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.dish-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--pos-bg-primary);
  border-radius: 8px;
  font-size: 14px;
}

.dish-name {
  color: var(--pos-text-primary);
  font-weight: 500;
}

.dish-quantity {
  color: var(--pos-primary);
  font-weight: 600;
  font-size: 15px;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--pos-border-light);
}

.remark,
.chef-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--pos-text-muted);
}

.remark {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  touch-action: manipulation;
}

.action-btn .el-icon {
  font-size: 18px;
}

.start-btn {
  background: linear-gradient(135deg, #ea580c 0%, #c2410c 100%);
  border: none;
  color: #fff;
}

.complete-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  color: #fff;
}

.serve-btn {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border: none;
  color: #fff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .kitchen-order-card {
    padding: 12px;
  }

  .order-number {
    font-size: 16px;
  }

  .dish-item {
    font-size: 13px;
    padding: 6px 10px;
  }

  .action-btn {
    height: 44px;
    font-size: 14px;
  }
}
</style>
