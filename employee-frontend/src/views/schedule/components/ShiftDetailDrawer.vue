<template>
  <el-drawer
    :model-value="visible"
    direction="rtl"
    size="400px"
    :with-header="false"
    :destroy-on-close="true"
    @update:model-value="(val: boolean) => $emit('update:visible', val)"
  >
    <div v-if="detail" :class="['shift-detail-drawer', { 'drawer-dark': isDark }]">
      <!-- 头部：关闭 + 标题 -->
      <div class="drawer-header">
        <h3 class="drawer-title">任务详情</h3>
        <el-button
          link
          size="small"
          class="close-btn"
          @click="$emit('update:visible', false)"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>

      <!-- 员工信息 -->
      <div class="employee-section">
        <div class="employee-avatar">
          <img
            v-if="detail.avatar"
            :src="detail.avatar"
            :alt="detail.employeeName"
            class="avatar-img"
          />
          <div v-else class="avatar-placeholder">
            {{ detail.employeeName.charAt(0) }}
          </div>
        </div>
        <div class="employee-info">
          <span class="employee-name">{{ detail.employeeName }}</span>
          <span class="employee-position">{{ detail.position }}</span>
        </div>
      </div>

      <!-- 任务信息卡片（派遣场景） -->
      <div class="info-card">
        <div class="info-row">
          <span class="info-label"><el-icon :size="14"><Calendar /></el-icon> 派遣日期</span>
          <span class="info-value">{{ formatDate(detail.date) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label"><el-icon :size="14"><Clock /></el-icon> 工作时间</span>
          <span class="info-value">{{ detail.startTime }} - {{ detail.endTime }}<span v-if="workHours"> ({{ workHours }}小时)</span></span>
        </div>
        <div class="info-row">
          <span class="info-label"><el-icon :size="14"><PriceTag /></el-icon> 任务类型</span>
          <span class="info-value">
            <StatusTag :status="detail.shiftType" :label="shiftLabel" size="small" />
          </span>
        </div>
        <div v-if="detail.targetStore" class="info-row">
          <span class="info-label"><el-icon :size="14"><Location /></el-icon> 目标门店</span>
          <span class="info-value">{{ detail.targetStore }}</span>
        </div>
        <div v-if="detail.dispatchType" class="info-row">
          <span class="info-label"><el-icon :size="14"><Aim /></el-icon> 派遣类型</span>
          <span class="info-value">
            <StatusTag :status="dispatchStatusMap[detail.dispatchType]" :label="dispatchTypeLabel" variant="badge" size="small" />
          </span>
        </div>
        <div v-if="detail.dispatchReason" class="info-row info-row--multiline">
          <span class="info-label"><el-icon :size="14"><EditPen /></el-icon> 派遣原因</span>
          <span class="info-value info-value--reason">{{ detail.dispatchReason }}</span>
        </div>
      </div>

      <!-- 备注 -->
      <div v-if="detail.note" class="remark-section">
        <div class="remark-label">备注</div>
        <p class="remark-content">{{ detail.note }}</p>
      </div>

      <!-- 分隔线 + 操作按钮 -->
      <template v-if="showActionButton">
        <div class="divider"></div>
        <div class="action-buttons">
          <el-button
            type="primary"
            class="action-btn action-btn--primary"
            @click="handleConfirm"
          >
            ✅ 确认接收
          </el-button>
          <el-button
            class="action-btn action-btn--secondary"
            @click="handleViewRoute"
          >
            <el-icon :size="14"><MapLocation /></el-icon> 查看路线
          </el-button>
        </div>
      </template>
    </div>

    <!-- 无数据占位 -->
    <div v-else class="empty-detail">
      <EmptyState title="暂无任务详情" description="暂无任务详情" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
/**
 * ShiftDetailDrawer - 任务详情抽屉面板组件（Layer 3）
 *
 * 从右侧滑出，展示员工班次/派遣任务的完整信息，
 * 支持借调/学习/临时支援等派遣场景。
 */
import { computed } from 'vue'
import { Close, Calendar, Clock, PriceTag, Location, Aim, EditPen, MapLocation } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { useTheme } from '@/composables/useTheme'
import type { ShiftDetail } from '@/types/schedule'
import { SHIFT_LABEL_MAP } from '@/types/schedule'

/** 派遣类型配置映射 */
const DISPATCH_TYPE_CONFIG: Record<string, { label: string; status: string }> = {
  loan: { label: '借调', status: 'warning' },
  training: { label: '学习', status: 'info' },
  support: { label: '临时支援', status: 'active' },
}

interface Props {
  /** 抽屉可见性 */
  visible: boolean
  /** 班次/任务详情数据 */
  detail: ShiftDetail | null
  /** 是否显示操作按钮（默认显示） */
  showActionButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showActionButton: true,
})

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'confirm', detail: ShiftDetail): void
  (e: 'view-route', detail: ShiftDetail): void
}>()

const { isDark } = useTheme()

/** 班次类型标签文字 */
const shiftLabel = computed(() => {
  if (!props.detail) return ''
  return SHIFT_LABEL_MAP[props.detail.shiftType] || props.detail.shiftType
})

/** 派遣类型到 StatusTag status 的映射 */
const dispatchStatusMap = computed(() => {
  const map: Record<string, string> = {}
  Object.entries(DISPATCH_TYPE_CONFIG).forEach(([key, config]) => {
    map[key] = config.status
  })
  return map
})

/** 派遣类型标签文字 */
const dispatchTypeLabel = computed(() => {
  if (!props.detail?.dispatchType) return ''
  return DISPATCH_TYPE_CONFIG[props.detail.dispatchType]?.label || props.detail.dispatchType
})

/** 计算工作时长（小时） */
const workHours = computed(() => {
  if (!props.detail?.startTime || !props.detail?.endTime) return 0
  if (props.detail.dispatchHours) return props.detail.dispatchHours
  // 根据时间自动计算
  const [startH, startM] = props.detail.startTime.split(':').map(Number)
  const [endH, endM] = props.detail.endTime.split(':').map(Number)
  let hours = endH - startH + (endM - startM) / 60
  // 处理跨天情况（如晚班 16:00-24:00）
  if (hours < 0) hours += 24
  return Math.round(hours * 10) / 10
})

/** 格式化日期显示 */
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** 确认接收任务 */
function handleConfirm(): void {
  if (props.detail) {
    emit('confirm', props.detail)
  }
}

/** 查看路线 */
function handleViewRoute(): void {
  if (props.detail) {
    emit('view-route', props.detail)
  }
}
</script>

<style scoped lang="scss">
// ================================================================
//  el-drawer 背景覆盖（确保深色/浅色模式均正确）
// ================================================================
:deep(.el-drawer) {
  background-color: var(--fts-bg-page);
  transition: background-color 0.3s ease;
}

:deep(.el-drawer__body) {
  background-color: transparent;
  padding: 0;
}

// ================================================================
//  抽屉内容区
// ================================================================
.shift-detail-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--fts-space-4);
  background-color: var(--fts-bg-card);
  border-left: 1px solid var(--fts-border-primary);
}

/* ===== 头部 ===== */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fts-space-5);
  flex-shrink: 0;
}

.drawer-title {
  margin: 0;
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.close-btn {
  color: var(--fts-text-tertiary);
  padding: 4px;

  &:hover {
    color: var(--fts-text-primary);
  }
}

/* ===== 员工信息 ===== */
.employee-section {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-5);
  flex-shrink: 0;
}

.employee-avatar {
  flex-shrink: 0;
}

.avatar-img,
.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-on-primary, #ffffff);
  background-color: var(--fts-primary);
}

.employee-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.employee-name {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.employee-position {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

/* ===== 信息卡片（带边框确保深色模式可见） ===== */
.info-card {
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md, 8px);
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  flex-shrink: 0;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-2) 0;

  &:not(:last-child) {
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.info-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  flex-shrink: 0;
}

.info-value {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

/* ===== 备注 ===== */
.remark-section {
  margin-bottom: var(--fts-space-4);
  flex-shrink: 0;
}

.remark-label {
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  color: var(--fts-text-tertiary);
  margin-bottom: var(--fts-space-1);
}

.remark-content {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;
  padding: var(--fts-space-2) var(--fts-space-3);
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-sm, 4px);
}

/* ===== 分隔线 + 按钮 ===== */
.divider {
  height: 1px;
  background-color: var(--fts-border-primary);
  margin-bottom: var(--fts-space-4);
  flex-shrink: 0;
}

.empty-detail {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

// ================================================================
//  深色模式增强
// ================================================================
.drawer-dark {
  .info-card {
    border-color: var(--fts-border-hover);
    box-shadow: 0 1px 3px var(--fts-bg-overlay, rgba(0, 0, 0, 0.2));
  }

  .info-row {
    &:not(:last-child) {
      border-bottom-color: var(--fts-border-secondary);
    }
  }

  .divider {
    background-color: var(--fts-border-primary);
  }
}

/* ===== 多行信息行 ===== */
.info-row--multiline {
  align-items: flex-start;
  flex-direction: column;
  gap: var(--fts-space-1);

  .info-label {
    flex-shrink: unset;
  }

  .info-value--reason {
    text-align: left;
    word-break: break-word;
    line-height: 1.6;
  }
}

/* ===== 操作按钮组 ===== */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  margin-top: auto;
}

.action-btn {
  width: 100%;

  &--primary {
    // 使用 Element Plus 默认 primary 样式
  }

  &--secondary {
    --el-button-text-color: var(--fts-text-secondary);
    --el-button-bg-color: var(--fts-bg-tertiary);
    --el-button-border-color: var(--fts-border-primary);

    &:hover {
      --el-button-text-color: var(--fts-primary);
      --el-button-border-color: var(--fts-primary-light);
      --el-button-bg-color: var(--fts-primary-light);
    }
  }
}
</style>
