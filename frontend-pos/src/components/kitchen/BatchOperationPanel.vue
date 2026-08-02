<template>
  <Transition name="batch-panel">
    <div v-if="visible && selectedIds.length > 0" class="batch-operation-panel">
      <!-- 面板头部 -->
      <div class="panel-header">
        <div class="selection-info">
          <el-checkbox
            :model-value="isAllSelected"
            @change="handleSelectAll"
            class="select-all-checkbox"
          >
            选择全部
          </el-checkbox>
          <span class="selected-count">
            已选: <strong>{{ selectedIds.length }}</strong> 道菜
          </span>
        </div>
        <button class="close-btn" @click="handleClose" aria-label="关闭">
          ✕
        </button>
      </div>

      <!-- 主要操作按钮 -->
      <div class="main-actions">
        <button
          class="action-btn btn-start"
          @click="handleStartCooking"
          :disabled="loading"
        >
          <span class="btn-icon">🍳</span>
          <span class="btn-text">开始制作</span>
        </button>

        <button
          class="action-btn btn-complete"
          @click="handleMarkComplete"
          :disabled="loading"
        >
          <span class="btn-icon">✅</span>
          <span class="btn-text">标记完成</span>
        </button>

        <button
          class="action-btn btn-reset"
          @click="handleResetSelection"
          :disabled="loading"
        >
          <span class="btn-icon">🔄</span>
          <span class="btn-text">重置选择</span>
        </button>
      </div>

      <!-- 快捷操作区域 -->
      <div class="quick-actions">
        <div class="quick-actions-title">快捷操作</div>
        <div class="quick-actions-buttons">
          <button
            class="quick-btn btn-quick-complete"
            @click="handleQuickCompleteAll"
            :disabled="loading || pendingCount === 0"
          >
            一键完成所有待制作
          </button>
          <button
            class="quick-btn btn-quick-serve"
            @click="handleQuickServeAll"
            :disabled="loading || completedCount === 0"
          >
            一键出餐所有已完成
          </button>
        </div>
      </div>

      <!-- 操作进度提示 -->
      <div v-if="loading" class="operation-progress">
        <div class="progress-spinner"></div>
        <span>正在处理中...</span>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useOperationFeedback } from '@/composables/kitchen/useOperationFeedback'

/**
 * 组件属性定义
 */
interface Props {
  /** 是否显示面板 */
  visible: boolean
  /** 已选中的订单ID列表 */
  selectedIds: string[]
  /** 待制作的订单数量 */
  pendingCount: number
  /** 已完成的订单数量 */
  completedCount: number
  /** 总订单数（用于全选判断） */
  totalCount: number
  /** 是否正在加载/处理中 */
  loading?: boolean
}

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'select-all', selected: boolean): void
  (e: 'start-cooking', ids: string[]): void
  (e: 'mark-complete', ids: string[]): void
  (e: 'reset-selection'): void
  (e: 'quick-complete-all'): void
  (e: 'quick-serve-all'): void
}>()

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// 使用操作反馈系统
const { showSuccess, showWarning } = useOperationFeedback()

// 计算属性：是否全选
const isAllSelected = computed(() => {
  return props.selectedIds.length > 0 && props.selectedIds.length >= props.totalCount
})

/**
 * 处理全选/取消全选
 * @param checked - 是否选中
 */
const handleSelectAll = (checked: boolean | string | number): void => {
  const isSelected = Boolean(checked)
  emit('select-all', isSelected)

  if (isSelected) {
    showSuccess(`已选中全部 ${props.totalCount} 道菜`)
  } else {
    showWarning('已取消选择')
  }
}

/**
 * 关闭面板
 */
const handleClose = (): void => {
  emit('close')
}

/**
 * 开始制作选中的订单
 */
const handleStartCooking = (): void => {
  if (props.selectedIds.length === 0) {
    showWarning('请先选择要制作的订单', { vibration: true })
    return
  }

  emit('start-cooking', props.selectedIds)
  showSuccess(`开始制作 ${props.selectedIds.length} 道菜`, { sound: true, vibration: true })
}

/**
 * 标记选中订单为已完成
 */
const handleMarkComplete = (): void => {
  if (props.selectedIds.length === 0) {
    showWarning('请先选择要完成的订单', { vibration: true })
    return
  }

  emit('mark-complete', props.selectedIds)
  showSuccess(`已完成 ${props.selectedIds.length} 道菜`, { sound: true, vibration: true })
}

/**
 * 重置选择
 */
const handleResetSelection = (): void => {
  emit('reset-selection')
  showWarning('已重置选择', { sound: false })
}

/**
 * 一键完成所有待制作订单
 */
const handleQuickCompleteAll = (): void => {
  if (props.pendingCount === 0) {
    showWarning('没有待制作的订单', { vibration: true })
    return
  }

  emit('quick-complete-all')
  showSuccess(`正在完成 ${props.pendingCount} 个待制作订单...`, {
    duration: 3000,
    sound: true,
    vibration: true
  })
}

/**
 * 一键出餐所有已完成订单
 */
const handleQuickServeAll = (): void => {
  if (props.completedCount === 0) {
    showWarning('没有已完成的订单可出餐', { vibration: true })
    return
  }

  emit('quick-serve-all')
  showSuccess(`正在出餐 ${props.completedCount} 个已完成订单...`, {
    duration: 3000,
    sound: true,
    vibration: true
  })
}
</script>

<style scoped>
/* ========== 批量操作面板主容器 ========== */
.batch-operation-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--pos-glass-bg);
  backdrop-filter: var(--pos-glass-blur);
  border-top: 2px solid var(--pos-primary);
  border-radius: var(--pos-border-radius-lg) var(--pos-border-radius-lg) 0 0;
  box-shadow: var(--pos-shadow-2xl);
  padding: var(--pos-spacing-md);
  z-index: var(--pos-z-fixed);
  animation: slide-up 0.3s ease-out;
}

@keyframes slide-up {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* ========== 面板头部 ========== */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--pos-spacing-md);
  padding-bottom: var(--pos-spacing-sm);
  border-bottom: 1px solid var(--pos-border-light);
}

.selection-info {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-md);
}

.select-all-checkbox {
  font-weight: var(--pos-font-weight-semibold);
  color: var(--pos-text-primary);
}

.selected-count {
  font-size: var(--pos-font-size-sm);
  color: var(--pos-text-secondary);
}

.selected-count strong {
  color: var(--pos-primary);
  font-size: var(--pos-font-size-md);
  margin: 0 4px;
}

.close-btn {
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--pos-bg-hover);
  color: var(--pos-text-muted);
  font-size: 18px;
  cursor: pointer;
  transition: all var(--pos-transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: var(--pos-danger);
  color: white;
  transform: rotate(90deg);
}

.close-btn:active {
  transform: scale(0.9) rotate(90deg);
}

/* ========== 主要操作按钮区域 ========== */
.main-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--pos-spacing-sm);
  margin-bottom: var(--pos-spacing-md);
}

.action-btn {
  min-height: 52px;
  padding: var(--pos-spacing-sm) var(--pos-spacing-md);
  border: none;
  border-radius: var(--pos-border-radius);
  font-size: var(--pos-font-size-sm);
  font-weight: var(--pos-font-weight-semibold);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all var(--pos-transition-normal);
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.action-btn:active::before {
  width: 200px;
  height: 200px;
}

.action-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 20px;
  line-height: 1;
}

.btn-text {
  font-size: var(--pos-font-size-xs);
  letter-spacing: 0.5px;
}

/* 开始制作按钮 - 橙色主题 */
.btn-start {
  background: var(--pos-primary-gradient);
  color: white;
  box-shadow: var(--pos-shadow-primary);
}

.btn-start:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(249, 115, 22, 0.4);
}

/* 标记完成按钮 - 绿色主题 */
.btn-complete {
  background: var(--pos-success-gradient);
  color: white;
  box-shadow: var(--pos-shadow-success);
}

.btn-complete:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(16, 185, 129, 0.4);
}

/* 重置选择按钮 - 灰色主题 */
.btn-reset {
  background: var(--pos-bg-hover);
  color: var(--pos-text-secondary);
  border: 1px solid var(--pos-border-color);
}

.btn-reset:hover:not(:disabled) {
  background: var(--pos-border-color);
  transform: translateY(-1px);
}

/* ========== 快捷操作区域 ========== */
.quick-actions {
  background: var(--pos-bg-hover);
  border-radius: var(--pos-border-radius-sm);
  padding: var(--pos-spacing-sm) var(--pos-spacing-md);
}

.quick-actions-title {
  font-size: var(--pos-font-size-xs);
  color: var(--pos-text-muted);
  margin-bottom: var(--pos-spacing-sm);
  font-weight: var(--pos-font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.quick-actions-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--pos-spacing-sm);
}

.quick-btn {
  min-height: 44px;
  padding: var(--pos-spacing-sm) var(--pos-spacing-md);
  border: 2px dashed transparent;
  border-radius: var(--pos-border-radius-sm);
  font-size: var(--pos-font-size-xs);
  font-weight: var(--pos-font-weight-medium);
  cursor: pointer;
  transition: all var(--pos-transition-fast);
  background: var(--pos-bg-card);
  color: var(--pos-text-primary);
}

.quick-btn:active:not(:disabled) {
  transform: scale(0.96);
}

.quick-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-quick-complete {
  border-color: var(--pos-success);
  color: var(--pos-success-dark);
}

.btn-quick-complete:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.1);
  border-style: solid;
}

.btn-quick-serve {
  border-color: var(--pos-primary);
  color: var(--pos-primary-dark);
}

.btn-quick-serve:hover:not(:disabled) {
  background: rgba(249, 115, 22, 0.1);
  border-style: solid;
}

/* ========== 操作进度提示 ========== */
.operation-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--pos-spacing-sm);
  margin-top: var(--pos-spacing-md);
  padding: var(--pos-spacing-sm);
  color: var(--pos-primary);
  font-size: var(--pos-font-size-sm);
  font-weight: var(--pos-font-weight-medium);
}

.progress-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--pos-border-color);
  border-top-color: var(--pos-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ========== 过渡动画 ========== */
.batch-panel-enter-active,
.batch-panel-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.batch-panel-enter-from,
.batch-panel-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .batch-operation-panel {
    padding: var(--pos-spacing-sm);
  }

  .main-actions {
    gap: var(--pos-spacing-xs);
  }

  .action-btn {
    min-height: 48px;
    padding: var(--pos-spacing-xs) var(--pos-spacing-sm);
  }

  .btn-icon {
    font-size: 18px;
  }

  .btn-text {
    font-size: 10px;
  }

  .quick-actions-buttons {
    grid-template-columns: 1fr;
  }

  .quick-btn {
    min-height: 40px;
  }
}
</style>
