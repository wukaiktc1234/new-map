<script setup lang="ts">
/**
 * 统计卡片组件 - 多种风格
 * 风格A: 极简数字（默认）
 * 风格B: 图标左侧
 * 风格C: 卡片带边框
 * 风格D: 迷你指标
 *
 * 颜色通过CSS类 + CSS变量控制，自动适配深色模式
 */
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

interface Props {
  icon: string
  label: string
  value: string | number
  colorType?: 'primary' | 'success' | 'warning' | 'error' | 'info'
  trend?: number
  variant?: 'default' | 'minimal' | 'bordered' | 'compact'
}

withDefaults(defineProps<Props>(), {
  colorType: 'primary',
  trend: 0,
  variant: 'default'
})
</script>

<template>
  <!-- 风格A: 默认（图标顶部，数字中部） -->
  <div v-if="variant === 'default'" class="stat-card stat-card--default" :class="`stat-card--color-${colorType}`">
    <div class="stat-card__header">
      <div class="stat-card__icon">
        <el-icon :size="20">
          <component :is="icon" />
        </el-icon>
      </div>
      <div v-if="trend !== 0" class="stat-card__trend" :class="trend > 0 ? 'trend-up' : 'trend-down'">
        <el-icon :size="12">
          <ArrowUp v-if="trend > 0" />
          <ArrowDown v-else />
        </el-icon>
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>
    <div class="stat-card__value">{{ value }}</div>
    <div class="stat-card__label">{{ label }}</div>
  </div>

  <!-- 风格B: 极简（纯数字+标签） -->
  <div v-else-if="variant === 'minimal'" class="stat-card stat-card--minimal" :class="`stat-card--color-${colorType}`">
    <div class="stat-card__value">{{ value }}</div>
    <div class="stat-card__label">{{ label }}</div>
    <div v-if="trend !== 0" class="stat-card__trend-mini" :class="trend > 0 ? 'trend-up' : 'trend-down'">
      {{ trend > 0 ? '+' : '' }}{{ trend }}%
    </div>
  </div>

  <!-- 风格C: 边框卡片（图标左侧，信息右侧） -->
  <div v-else-if="variant === 'bordered'" class="stat-card stat-card--bordered" :class="`stat-card--color-${colorType}`">
    <div class="stat-card__icon-box">
      <el-icon :size="18">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="stat-card__info">
      <div class="stat-card__label">{{ label }}</div>
      <div class="stat-card__value-row">
        <span class="stat-card__value">{{ value }}</span>
        <span v-if="trend !== 0" class="stat-card__trend-tag" :class="trend > 0 ? 'trend-up' : 'trend-down'">
          {{ trend > 0 ? '+' : '' }}{{ trend }}%
        </span>
      </div>
    </div>
  </div>

  <!-- 风格D: 紧凑指标（横向排列） -->
  <div v-else-if="variant === 'compact'" class="stat-card stat-card--compact" :class="`stat-card--color-${colorType}`">
    <div class="stat-card__content">
      <span class="stat-card__label">{{ label }}</span>
      <span class="stat-card__divider">·</span>
      <span class="stat-card__value">{{ value }}</span>
      <span v-if="trend !== 0" class="stat-card__trend-micro" :class="trend > 0 ? 'trend-up' : 'trend-down'">
        {{ trend > 0 ? '↑' : '↓' }} {{ Math.abs(trend) }}%
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.stat-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
}

.trend-up {
  color: var(--fts-success);
}

.trend-down {
  color: var(--fts-error);
}

// ========== 颜色主题 - 通过CSS变量自动适配深色模式 ==========
.stat-card--color-primary {
  .stat-card__icon { color: var(--fts-primary); }
  .stat-card__icon-box { background-color: var(--fts-primary-bg); color: var(--fts-primary); }
  .stat-card__value { color: var(--fts-primary); }
}

.stat-card--color-success {
  .stat-card__icon { color: var(--fts-success); }
  .stat-card__icon-box { background-color: var(--fts-success-bg); color: var(--fts-success); }
  .stat-card__value { color: var(--fts-success); }
}

.stat-card--color-warning {
  .stat-card__icon { color: var(--fts-warning); }
  .stat-card__icon-box { background-color: var(--fts-warning-bg); color: var(--fts-warning); }
  .stat-card__value { color: var(--fts-warning); }
}

.stat-card--color-error {
  .stat-card__icon { color: var(--fts-error); }
  .stat-card__icon-box { background-color: var(--fts-error-bg); color: var(--fts-error); }
  .stat-card__value { color: var(--fts-error); }
}

.stat-card--color-info {
  .stat-card__icon { color: var(--fts-info); }
  .stat-card__icon-box { background-color: var(--fts-info-bg); color: var(--fts-info); }
  .stat-card__value { color: var(--fts-info); }
}

// ========== 风格A: 默认 ==========
.stat-card--default {
  padding: var(--fts-space-4);

  .stat-card__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-2);
  }

  .stat-card__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--fts-radius-sm);
    background: var(--fts-bg-secondary);
  }

  .stat-card__trend {
    display: flex;
    align-items: center;
    gap: 2px;
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-medium);
  }

  .stat-card__value {
    font-size: var(--fts-font-size-2xl);
    font-weight: var(--fts-font-weight-bold);
    line-height: var(--fts-line-height-tight);
    margin-bottom: var(--fts-space-1);
  }

  .stat-card__label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// ========== 风格B: 极简 ==========
.stat-card--minimal {
  padding: var(--fts-space-3) var(--fts-space-4);
  text-align: center;

  .stat-card__value {
    font-size: var(--fts-font-size-2xl);
    font-weight: var(--fts-font-weight-bold);
    line-height: var(--fts-line-height-tight);
  }

  .stat-card__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    margin-top: var(--fts-space-1);
  }

  .stat-card__trend-mini {
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-medium);
    margin-top: var(--fts-space-1);
  }
}

// ========== 风格C: 边框卡片 ==========
.stat-card--bordered {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1px solid var(--fts-border-secondary);

  .stat-card__icon-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
    flex-shrink: 0;
  }

  .stat-card__info {
    flex: 1;
    min-width: 0;
  }

  .stat-card__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    margin-bottom: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .stat-card__value-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  .stat-card__value {
    font-size: var(--fts-font-size-xl);
    font-weight: var(--fts-font-weight-bold);
    line-height: var(--fts-line-height-tight);
  }

  .stat-card__trend-tag {
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-medium);
    padding: 1px 5px;
    border-radius: var(--fts-radius-sm);
    background: var(--fts-bg-secondary);
  }
}

// ========== 风格D: 紧凑指标 ==========
.stat-card--compact {
  padding: var(--fts-space-2) var(--fts-space-3);

  .stat-card__content {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-sm);
  }

  .stat-card__label {
    color: var(--fts-text-secondary);
  }

  .stat-card__divider {
    color: var(--fts-border-primary);
  }

  .stat-card__value {
    font-weight: var(--fts-font-weight-bold);
  }

  .stat-card__trend-micro {
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-medium);
  }
}
</style>
