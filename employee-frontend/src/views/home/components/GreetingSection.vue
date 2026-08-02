<template>
  <div class="greeting-section">
    <!-- 左侧：用户信息 -->
    <div class="greeting-left">
      <div class="user-avatar">
        <el-avatar :size="44" :src="avatarUrl">
          <el-icon :size="22"><User /></el-icon>
        </el-avatar>
      </div>
      <div class="greeting-content">
        <h1 class="greeting-text">{{ greetingMessage }}</h1>
        <p class="date-text">{{ currentDate }}</p>
        <p class="user-info">
          <span class="user-name">{{ userName }}</span>
          <span class="separator">·</span>
          <span class="store-name">{{ storeName }}</span>
        </p>

        <!-- 排班信息（紧凑行，非独立卡片） -->
        <div v-if="schedule && !schedule.isDayOff && scheduleDisplayText" class="schedule-info">
          <el-icon :size="14"><Clock /></el-icon>
          <span>今日 {{ scheduleDisplayText }}</span>
        </div>
        <div v-else-if="schedule?.isDayOff" class="schedule-info schedule-info--dayoff">
          <el-icon :size="14"><Coffee /></el-icon>
          <span>今日休息</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Clock, Coffee } from '@element-plus/icons-vue'

interface Props {
  userName: string
  storeName: string
  avatarUrl?: string
  schedule?: { isDayOff: boolean; shifts?: Array<{ name: string; startTime: string; endTime: string }> } | null
}

const props = withDefaults(defineProps<Props>(), {
  avatarUrl: ''
})

/** 排班信息展示文本（处理 schedule 可能为空的情况） */
const scheduleDisplayText = computed(() => {
  if (!props.schedule || props.schedule.isDayOff) return ''
  const shiftNames = props.schedule.shifts?.map(s => s.name).join('、') ?? ''
  const startTime = props.schedule.shifts?.[0]?.startTime ?? ''
  const endTime = props.schedule.shifts?.[0]?.endTime ?? ''
  return `${shiftNames} ${startTime}-${endTime}`.trim()
})

const greetingMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 6 && hour < 11) return '早上好'
  if (hour >= 11 && hour < 14) return '中午好'
  if (hour >= 14 && hour < 18) return '下午好'
  if (hour >= 18 && hour < 24) return '晚上好'
  return '夜深了'
})

/** 当前日期格式：2026年5月31日 星期六 */
const currentDate = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekDay = weekDays[now.getDay()]
  return `${year}年${month}月${day}日 ${weekDay}`
})
</script>

<style scoped lang="scss">
.greeting-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-6) var(--fts-space-6);
  background: var(--fts-gradient-primary);
  border-radius: var(--fts-radius-xl);
  color: var(--fts-text-on-primary);
  position: relative;
  overflow: hidden;
  box-shadow: var(--fts-shadow-lg);

  /* 装饰性背景元素 */
  &::before {
    content: '';
    position: absolute;
    top: -20%;
    right: -10%;
    width: 260px;
    height: 260px;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -10%;
    left: 10%;
    width: 180px;
    height: 180px;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
  }
}

.greeting-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4, 16px);
  position: relative;
  z-index: 1;
}

.user-avatar {
  flex-shrink: 0;

  :deep(.el-avatar) {
    border: 2px solid rgba(255, 255, 255, 0.4);
    background-color: rgba(255, 255, 255, 0.2);
    color: var(--fts-text-on-primary);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.greeting-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.greeting-text {
  margin: 0;
  font-size: var(--fts-font-size-xl);
  font-weight: 700;
  line-height: 1.2;
  color: inherit;
  letter-spacing: -0.3px;
}

.date-text {
  margin: 0;
  font-size: var(--fts-font-size-xs);
  opacity: 0.8;
  font-weight: 500;
}

.user-info {
  margin: 0;
  font-size: var(--fts-font-size-xs);
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: 2px;
}

.user-name {
  font-weight: var(--fts-font-weight-medium, 500);
}

.separator {
  opacity: 0.6;
}

.store-name {
  opacity: 0.9;
}

.schedule-info {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  margin-top: var(--fts-space-2);
  padding: 6px 12px;
  background-color: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-2xs);
  font-weight: 600;
  line-height: 1;
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  &--dayoff {
    background-color: rgba(255, 255, 255, 0.1);
    opacity: 0.8;
  }
}



/* 响应式适配 */
@media (max-width: 768px) {
  .greeting-section {
    padding: var(--fts-space-4, 16px);
    flex-direction: column;
    align-items: flex-start;
    gap: var(--fts-space-3, 12px);
  }

  .greeting-left {
    width: 100%;
  }

  .greeting-right {
    position: absolute;
    top: var(--fts-space-4, 16px);
    right: var(--fts-space-4, 16px);
  }

  .greeting-text {
    font-size: var(--fts-font-size-lg, 18px);
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .greeting-section::before,
  .greeting-section::after {
    transition: none;
  }
}
</style>
