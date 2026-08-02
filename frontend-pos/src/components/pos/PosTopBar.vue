<template>
  <div class="pos-top-bar">
    <div class="store-info">
      <div class="pos-logo">
        <div class="pos-logo-icon">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12H15V22" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="pos-logo-text">
          <h1>快餐收银系统</h1>
        </div>
      </div>
      <div class="time-display">
        <span class="time">{{ currentTime.split(' ')[1] }}</span>
        <span class="date-sep">|</span>
        <span class="date">{{ currentTime.split(' ')[0] }}</span>
      </div>
    </div>

    <div class="quick-actions">
      <!-- 收银核心操作 -->
      <button class="action-btn hold-order-btn" @click="$emit('hold')">
        <el-icon><Document /></el-icon>
        <span>挂单</span>
      </button>

      <el-badge :value="heldCount" :hidden="heldCount === 0" :max="99">
        <button class="action-btn retrieve-order-btn" @click="$emit('retrieve')">
          <el-icon><FolderOpened /></el-icon>
          <span>取单</span>
        </button>
      </el-badge>

      <!-- 叫号系统入口 -->
      <el-badge :value="callNumberPendingCount" :hidden="callNumberPendingCount === 0" type="danger" :max="99">
        <button class="action-btn call-number-btn" @click="$emit('callNumber')">
          <el-icon><Bell /></el-icon>
          <span>叫号</span>
        </button>
      </el-badge>

      <!-- 高频辅助操作 -->
      <button class="action-btn coupon-btn" @click="$emit('coupon')">
        <el-icon><Ticket /></el-icon>
        <span>券码</span>
      </button>

      <button class="action-btn query-btn" @click="$emit('orderQuery')">
        <el-icon><Search /></el-icon>
        <span>订单</span>
      </button>

      <!-- 桌台管理 -->
      <button class="action-btn table-btn" @click="$emit('tables')">
        <el-icon><Grid /></el-icon>
        <span>桌台</span>
      </button>

      <!-- YOLO 辅助收银（多商品快速结算） -->
      <button class="action-btn yolo-btn" @click="$emit('yolo')" title="YOLO 视觉识别辅助收银">
        <el-icon><View /></el-icon>
        <span>视觉识别</span>
      </button>

      <!-- 交班 -->
      <button class="action-btn shift-handover-btn" @click="$emit('shiftHandover')">
        <el-icon><SwitchButton /></el-icon>
        <span>交班</span>
      </button>

      <!-- 主题切换 -->
      <button class="action-btn theme-btn" @click="$emit('toggleTheme')">
        <el-icon v-if="isDark"><Sunny /></el-icon>
        <el-icon v-else><Moon /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Document,
  FolderOpened,
  Search,
  Ticket,
  Bell,
  Grid,
  SwitchButton,
  Sunny,
  Moon,
  View
} from '@element-plus/icons-vue'

interface Props {
  currentTime: string
  heldCount: number
  callNumberPendingCount: number
  isDark: boolean
}

defineProps<Props>()

defineEmits<{
  (e: 'hold'): void
  (e: 'retrieve'): void
  (e: 'orderQuery'): void
  (e: 'coupon'): void
  (e: 'callNumber'): void
  (e: 'tables'): void
  (e: 'yolo'): void
  (e: 'shiftHandover'): void
  (e: 'toggleTheme'): void
}>()
</script>

<style scoped>
.pos-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: var(--pos-glass-bg, rgba(255, 255, 255, 0.8));
  backdrop-filter: blur(10px);
  border-bottom: 2px solid var(--pos-border-color, #e5e7eb);
}

.store-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.pos-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pos-logo-icon {
  width: 40px;
  height: 40px;
  background: var(--pos-primary-gradient, linear-gradient(135deg, #ea580c, #f97316));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pos-logo-icon svg {
  width: 24px;
  height: 24px;
}

.pos-logo-text h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--pos-text-primary, #1f2937);
}

.time-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
  border-left: 2px solid var(--pos-border-color, #e5e7eb);
}

.time-display .time {
  font-size: 18px;
  font-weight: 600;
  color: var(--pos-text-primary, #1f2937);
  font-family: 'Courier New', monospace;
  letter-spacing: 0.5px;
}

.time-display .date-sep {
  color: #d4d4d8;
  font-size: 14px;
}

.time-display .date {
  font-size: 13px;
  color: var(--pos-text-muted, #6b7280);
}

.quick-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1.5px solid var(--pos-border-color, #e5e7eb);
  background: var(--pos-bg-secondary, #f9fafb);
  color: var(--pos-text-primary, #1f2937);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  user-select: none;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(234, 88, 12, 0.15);
  border-color: var(--pos-primary, #ea580c);
  color: var(--pos-primary, #ea580c) !important;
}

.action-btn:active {
  transform: translateY(0);
}

.action-btn .el-icon {
  font-size: 16px;
}

.hold-order-btn {
  border-color: var(--pos-primary, #ea580c);
  color: var(--pos-primary, #ea580c);
}

.hold-order-btn:hover {
  background: var(--pos-primary, #ea580c) !important;
  color: #ffffff !important;
  border-color: var(--pos-primary, #ea580c) !important;
}

.retrieve-order-btn {
  border-color: #0891b2;
  color: #0e7490;
}

.retrieve-order-btn:hover {
  background: #0891b2 !important;
  color: #ffffff !important;
  border-color: #0891b2 !important;
}

.call-number-btn {
  background: var(--pos-primary-gradient, linear-gradient(135deg, #ea580c, #f97316)) !important;
  border-color: transparent !important;
  color: #ffffff !important;
}

.call-number-btn:hover {
  box-shadow: 0 4px 16px rgba(234, 88, 12, 0.4) !important;
  opacity: 0.95 !important;
}

.coupon-btn {
  border-color: #10b981;
  color: #059669;
}

.coupon-btn:hover {
  background: #10b981 !important;
  color: #ffffff !important;
  border-color: #10b981 !important;
}

.query-btn {
  border-color: #64748b;
  color: #475569;
}

.query-btn:hover {
  background: #64748b !important;
  color: #ffffff !important;
  border-color: #64748b !important;
}

.table-btn {
  border-color: #0891b2;
  color: #0e7490;
}

.table-btn:hover {
  background: #0891b2 !important;
  color: #ffffff !important;
  border-color: #0891b2 !important;
}

.yolo-btn {
  border-color: #7c3aed;
  color: #6d28d9;
}

.yolo-btn:hover {
  background: #7c3aed !important;
  color: #ffffff !important;
  border-color: #7c3aed !important;
}

.shift-handover-btn {
  border-color: var(--pos-warning, #f59e0b);
  color: var(--pos-warning, #f59e0b);
}

.shift-handover-btn:hover {
  background: var(--pos-warning, #f59e0b) !important;
  color: #ffffff !important;
  border-color: var(--pos-warning, #f59e0b) !important;
}

.theme-btn {
  width: 40px;
  padding: 0;
  min-width: 40px;
}

.theme-btn .el-icon {
  font-size: 18px;
}

@media (max-width: 1200px) {
  .quick-actions {
    gap: 6px;
  }

  .action-btn {
    padding: 0 10px;
    font-size: 13px;
  }

  .action-btn span:not(.el-icon) {
    display: none;
  }

  .hold-order-btn span:not(.el-icon),
  .retrieve-order-btn span:not(.el-icon),
  .call-number-btn span:not(.el-icon),
  .coupon-btn span:not(.el-icon),
  .query-btn span:not(.el-icon) {
    display: inline;
  }
}
</style>