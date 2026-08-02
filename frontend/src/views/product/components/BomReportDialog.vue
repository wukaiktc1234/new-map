<script setup lang="ts">
/**
 * BOM 库存检查报告弹窗
 *
 * 【层级】L5 - 模块组件层(View/Product)
 * 【职责】展示菜品 BOM 物料库存可用性分析报告
 * 【依赖】L3(StatusTag/EmptyState)
 *
 * 使用方式：
 * <BomReportDialog v-model:visible="visible" :data="data" :loading="loading" />
 */
import { computed } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import type { BomCheckResult, BomInsufficientItem } from '@/api/product/bom-check'

// ==================== Props 定义 ====================

interface Props {
  /** 弹窗显隐（v-model:visible） */
  visible: boolean
  /** BOM 检查结果数据 */
  data: BomCheckResult | null
  /** 加载中状态 */
  loading: boolean
}

const props = defineProps<Props>()

// ==================== Emits 定义 ====================

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

// ==================== 计算属性 ====================

/** 弹窗标题 */
const dialogTitle = computed<string>(() => {
  const name = props.data?.dishName || '未知菜品'
  return `BOM 库存检查报告 - ${name}`
})

/** 缺料项数 */
const insufficientCount = computed<number>(() => {
  return props.data?.insufficientItems?.length || 0
})

/** 是否存在缺料明细 */
const hasInsufficientItems = computed<boolean>(() => insufficientCount.value > 0)

/** 可制作状态卡片样式类 */
const canMakeClass = computed<string>(() => {
  if (!props.data) return 'stat-card--neutral'
  return props.data.canMake ? 'stat-card--success' : 'stat-card--error'
})

/** 可制作状态图标 */
const canMakeSymbol = computed<string>(() => {
  return props.data?.canMake ? '✓' : '✗'
})

/** 可制作状态文案 */
const canMakeText = computed<string>(() => {
  if (props.data === null) return '-'
  return props.data.canMake ? '可制作' : '不可制作'
})

// ==================== 方法 ====================

/**
 * 格式化数字显示（保留2位小数，null 显示为 -）
 * @param value - 数值
 * @returns 格式化后的字符串
 */
function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  return Number(value).toFixed(2)
}

/**
 * 格式化百分比显示
 * @param value - 百分比值（0-100）
 * @returns 格式化后的字符串
 */
function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  return `${Number(value).toFixed(1)}%`
}

/**
 * 紧急程度映射到 StatusTag 的 status 值
 * 后端返回 low/medium/high，对应 NORMAL/WARNING/CRITICAL
 * @param urgency - 紧急程度原始值
 * @returns StatusTag 的 status 属性值
 */
function getUrgencyStatus(urgency: string | null | undefined): string {
  const map: Record<string, string> = {
    high: 'error',    // CRITICAL → danger(红色)
    medium: 'warning', // WARNING → warning(黄色)
    low: 'info',      // NORMAL → info(灰色)
  }
  return map[urgency || ''] || 'info'
}

/**
 * 紧急程度映射到中文标签
 * @param urgency - 紧急程度原始值
 * @returns 中文标签
 */
function getUrgencyLabel(urgency: string | null | undefined): string {
  const map: Record<string, string> = {
    high: '紧急',
    medium: '警告',
    low: '正常',
  }
  return map[urgency || ''] || '正常'
}

/**
 * 关闭弹窗
 */
function handleClose(): void {
  emit('update:visible', false)
}

/**
 * 处理 el-dialog 显隐变化（支持点击遮罩/ESC 关闭）
 * @param value - 新的显隐值
 */
function handleVisibleChange(value: boolean): void {
  emit('update:visible', value)
}

/**
 * 表格行 key 取值
 * @param row - 行数据
 * @returns 行唯一标识
 */
function getRowKey(row: BomInsufficientItem): string {
  return row.materialId || row.materialName || Math.random().toString(36)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="720px"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <div v-loading="loading" class="bom-report">
      <!-- 顶部统计卡片区域 -->
      <div class="bom-report__stats">
        <!-- 可制作状态卡片 -->
        <div class="stat-card" :class="canMakeClass">
          <div class="stat-card__icon">{{ canMakeSymbol }}</div>
          <div class="stat-card__content">
            <div class="stat-card__label">可制作状态</div>
            <div class="stat-card__value">{{ canMakeText }}</div>
          </div>
        </div>

        <!-- 总需求数量卡片 -->
        <div class="stat-card stat-card--neutral">
          <div class="stat-card__content">
            <div class="stat-card__label">总需求数量</div>
            <div class="stat-card__value">{{ data?.totalRequired ?? 0 }}</div>
          </div>
        </div>

        <!-- 缺料项数卡片 -->
        <div class="stat-card" :class="hasInsufficientItems ? 'stat-card--error' : 'stat-card--success'">
          <div class="stat-card__content">
            <div class="stat-card__label">缺料项数</div>
            <div class="stat-card__value">{{ insufficientCount }}</div>
          </div>
        </div>
      </div>

      <!-- 缺料明细区域 -->
      <div class="bom-report__section">
        <div class="section-title">缺料明细</div>

        <!-- 缺料明细表格 -->
        <el-table
          v-if="hasInsufficientItems"
          :data="data?.insufficientItems"
          :row-key="getRowKey"
          stripe
          border
          size="small"
          style="width: 100%"
        >
          <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.materialName || '-' }}</template>
          </el-table-column>
          <el-table-column label="需求数量" width="100" align="right">
            <template #default="{ row }">{{ formatNumber(row.requiredQty) }}</template>
          </el-table-column>
          <el-table-column label="可用数量" width="100" align="right">
            <template #default="{ row }">{{ formatNumber(row.availableQty) }}</template>
          </el-table-column>
          <el-table-column label="缺口数量" width="100" align="right">
            <template #default="{ row }">
              <span class="shortage-text">{{ formatNumber(row.shortageQty) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="缺口百分比" width="110" align="right">
            <template #default="{ row }">{{ formatPercent(row.shortagePercent) }}</template>
          </el-table-column>
          <el-table-column label="单位" width="70" align="center">
            <template #default="{ row }">{{ row.unit || '-' }}</template>
          </el-table-column>
          <el-table-column label="紧急程度" width="100" align="center">
            <template #default="{ row }">
              <StatusTag
                :status="getUrgencyStatus(row.urgency)"
                :label="getUrgencyLabel(row.urgency)"
                size="small"
                variant="light"
              />
            </template>
          </el-table-column>
        </el-table>

        <!-- 无缺料时显示空状态 -->
        <EmptyState
          v-else
          type="default"
          title="所有物料库存充足"
          description="当前菜品所需全部物料库存均可满足制作需求"
          :image-size="120"
        />
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.bom-report {
  min-height: 200px;
}

// 顶部统计卡片区域
.bom-report__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-5);

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

// 单个统计卡片
.stat-card {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius, var(--fts-radius-md, 8px));
  background: var(--fts-bg-card);
  transition: all var(--fts-duration-fast, 150ms) var(--fts-easing-default, ease-in-out);

  // 可制作状态卡片 - 成功（绿色）
  &--success {
    border-color: var(--fts-success);
    background: var(--fts-success-bg);
  }

  // 可制作状态卡片 - 失败（红色）
  &--error {
    border-color: var(--fts-error);
    background: var(--fts-error-bg);
  }

  // 中性卡片
  &--neutral {
    background: var(--fts-bg-card);
  }
}

// 卡片图标（可制作状态的 ✓ / ✗）
.stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 22px;
  font-weight: var(--fts-font-weight-bold, 700);
  flex-shrink: 0;

  .stat-card--success & {
    background: var(--fts-success);
    color: var(--fts-text-inverse, #ffffff);
  }

  .stat-card--error & {
    background: var(--fts-error);
    color: var(--fts-text-inverse, #ffffff);
  }
}

// 卡片内容
.stat-card__content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-card__label {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-secondary);
  line-height: 1.4;
}

.stat-card__value {
  font-size: var(--fts-font-size-lg, 16px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  line-height: 1.4;
  font-variant-numeric: tabular-nums;

  .stat-card--success & {
    color: var(--fts-success);
  }

  .stat-card--error & {
    color: var(--fts-error);
  }
}

// 缺料明细区域
.bom-report__section {
  margin-top: var(--fts-space-2);
}

.section-title {
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
}

// 缺口数量文字（红色强调）
.shortage-text {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium, 500);
  font-variant-numeric: tabular-nums;
}
</style>
