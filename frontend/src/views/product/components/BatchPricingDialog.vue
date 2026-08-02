<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="resetForm"
  >
    <!-- 顶部提示与生效日期 -->
    <div class="batch-header">
      <div class="batch-header__tip">
        <el-icon><InfoFilled /></el-icon>
        <span>共 <strong>{{ rows.length }}</strong> 项待调价，请逐项设置新售价或调价策略</span>
      </div>
      <div class="batch-header__date">
        <span class="batch-header__label">生效日期：</span>
        <el-date-picker
          v-model="effectiveDate"
          type="date"
          placeholder="选择生效日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :teleported="false"
          size="default"
          style="width: 160px"
        />
      </div>
    </div>

    <!-- 批量调价表格 -->
    <el-table
      :data="rows"
      border
      size="small"
      style="width: 100%"
      max-height="420"
      class="batch-table"
    >
      <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip />
      <el-table-column label="当前售价(元)" width="110" align="right">
        <template #default="{ row }">
          <span class="price-current">{{ formatPrice(row.currentPrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成本(元)" width="100" align="right">
        <template #default="{ row }">
          <span class="price-cost">{{ formatPrice(row.costPrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="调价策略" width="140">
        <template #default="{ row, $index }">
          <el-select
            v-model="row.strategy"
            size="small"
            style="width: 100%"
            :popper-options="{ strategy: 'fixed' }"
            @change="handleStrategyChange($index)"
          >
            <el-option label="手动输入" value="MANUAL" />
            <el-option label="加价(按成本%)" value="MARKUP" />
            <el-option label="折扣(按售价%)" value="DISCOUNT" />
            <el-option label="取整(到0.5元)" value="ROUND" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="新售价(元)" width="150">
        <template #default="{ row, $index }">
          <!-- MANUAL: 直接输入新售价 -->
          <el-input-number
            v-if="row.strategy === 'MANUAL'"
            v-model="row.newPrice"
            :min="0"
            :precision="2"
            :step="1"
            :controls="false"
            size="small"
            style="width: 100%"
            placeholder="请输入新售价"
          />
          <!-- MARKUP / DISCOUNT: 输入百分比，自动计算新售价 -->
          <div v-else-if="row.strategy === 'MARKUP' || row.strategy === 'DISCOUNT'" class="percent-input">
            <el-input-number
              v-model="row.percent"
              :min="0"
              :max="row.strategy === 'DISCOUNT' ? 100 : 1000"
              :precision="1"
              :step="5"
              :controls="false"
              size="small"
              style="width: 70px"
              @input="calcNewPrice($index)"
            />
            <span class="percent-symbol">%</span>
            <span class="price-computed">{{ row.newPrice != null ? formatPrice(row.newPrice) : '--' }}</span>
          </div>
          <!-- ROUND: 自动计算，仅展示 -->
          <div v-else class="price-round-display">
            <span class="price-computed">{{ row.newPrice != null ? formatPrice(row.newPrice) : '--' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <el-input
            v-model="row.remark"
            size="small"
            placeholder="可选"
            maxlength="100"
            show-word-limit
          />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认调价</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 批量调价对话框
 *
 * 支持单条/批量调价，4 种调价策略：
 * - MANUAL  手动输入新售价
 * - MARKUP  按成本加价（新售价 = 成本 × (1 + 百分比/100)）
 * - DISCOUNT 按售价折扣（新售价 = 当前售价 × 百分比/100）
 * - ROUND   向上取整到 0.5 元（新售价 = ceil(当前售价 × 2) / 2）
 *
 * 弹窗内所有 popper 类组件（el-select/el-date-picker）统一设置 :teleported="false"，
 * 避免滚动时下拉面板漂移（参见项目规范：Dialog 内弹出组件定位规范）。
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { pricingApi } from '@/api/product/pricing'
import type {
  BatchPricingItem,
  PricingFormData,
  PricingProductType,
  PricingStrategy,
} from '@/types/product'

/** 表格行数据（在 BatchPricingItem 基础上增加调价交互字段） */
interface BatchPricingRow extends BatchPricingItem {
  /** 调价策略 */
  strategy: PricingStrategy
  /** 新售价（元，number） */
  newPrice: number | null
  /** 加价/折扣百分比（仅 MARKUP / DISCOUNT 策略使用） */
  percent: number | null
  /** 备注 */
  remark: string
}

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitting = ref(false)
/** 当前调价的产品类型（FOOD / COMBO） */
const productType = ref<PricingProductType>('FOOD')
/** 待调价项列表 */
const rows = ref<BatchPricingRow[]>([])
/** 生效日期（YYYY-MM-DD） */
const effectiveDate = ref<string>(formatToday())

const dialogTitle = computed(() => `批量调价 - 已选 ${rows.value.length} 项`)

/** 格式化金额显示（保留 2 位小数） */
function formatPrice(value: number | string | null | undefined): string {
  if (value == null || value === '') return '0.00'
  return Number(value).toFixed(2)
}

/** 获取今日日期字符串（YYYY-MM-DD） */
function formatToday(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 打开对话框
 * @param items 待调价产品列表
 * @param type  产品类型 FOOD / COMBO
 */
function open(items: BatchPricingItem[], type: PricingProductType): void {
  if (!items.length) {
    ElMessage.warning('未选择任何调价项')
    return
  }
  productType.value = type
  rows.value = items.map(item => ({
    ...item,
    strategy: 'MANUAL',
    newPrice: null,
    percent: null,
    remark: '',
  }))
  effectiveDate.value = formatToday()
  visible.value = true
}

function close(): void {
  visible.value = false
}

function resetForm(): void {
  rows.value = []
  submitting.value = false
}

/**
 * 切换调价策略时，重新计算新售价
 * - MARKUP / DISCOUNT: 需要用户输入百分比，先清空 newPrice 等待 input 事件
 * - ROUND: 立即计算向上取整到 0.5 元
 * - MANUAL: 清空 newPrice，由用户手动输入
 */
function handleStrategyChange(index: number): void {
  const row = rows.value[index]
  if (!row) return
  row.percent = null
  if (row.strategy === 'ROUND') {
    row.newPrice = roundToHalf(Number(row.currentPrice) || 0)
  } else if (row.strategy === 'MANUAL') {
    row.newPrice = null
  } else {
    row.newPrice = null
  }
}

/**
 * 根据百分比计算新售价（MARKUP / DISCOUNT 策略）
 * - MARKUP:  新售价 = 成本 × (1 + 百分比/100)
 * - DISCOUNT: 新售价 = 当前售价 × 百分比/100
 */
function calcNewPrice(index: number): void {
  const row = rows.value[index]
  if (!row || row.percent == null) {
    if (row) row.newPrice = null
    return
  }
  const cost = Number(row.costPrice) || 0
  const current = Number(row.currentPrice) || 0
  const pct = row.percent
  if (row.strategy === 'MARKUP') {
    row.newPrice = Number((cost * (1 + pct / 100)).toFixed(2))
  } else if (row.strategy === 'DISCOUNT') {
    row.newPrice = Number((current * pct / 100).toFixed(2))
  }
}

/** 向上取整到 0.5 元（如 12.3 → 12.5，12.7 → 13.0） */
function roundToHalf(price: number): number {
  return Math.ceil(price * 2) / 2
}

/** 提交批量调价 */
async function handleSubmit(): Promise<void> {
  // 校验：所有行必须填写新售价
  for (const row of rows.value) {
    if (row.newPrice == null || row.newPrice < 0) {
      ElMessage.warning(`请为「${row.productName}」设置有效的新售价`)
      return
    }
  }

  submitting.value = true
  try {
    // 构造 PricingFormData 列表，元→分转换在 converter 中完成
    const pricingList: PricingFormData[] = rows.value.map(row => ({
      productType: productType.value,
      productId: row.productId,
      productName: row.productName,
      newPrice: String(row.newPrice),
      costPrice: row.costPrice,
      pricingStrategy: row.strategy,
      remark: row.remark || undefined,
      effectiveDate: effectiveDate.value,
    }))

    await pricingApi.batchPricing(productType.value, pricingList)
    ElMessage.success(`成功调价 ${pricingList.length} 项`)
    emit('success')
    close()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '调价失败')
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ open, close })
</script>

<style scoped lang="scss">
.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);

  &__tip {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    color: var(--fts-text-secondary);

    .el-icon {
      color: var(--fts-primary);
    }

    strong {
      color: var(--fts-primary);
      font-weight: var(--fts-font-weight-semibold);
    }
  }

  &__date {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__label {
    color: var(--fts-text-secondary);
    white-space: nowrap;
  }
}

.batch-table {
  :deep(.el-table__row) {
    td {
      padding: var(--fts-space-2) var(--fts-space-2);
    }
  }
}

.price-current {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.price-cost {
  color: var(--fts-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.price-computed {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
  margin-left: var(--fts-space-2);
}

.percent-input {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
}

.percent-symbol {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

.price-round-display {
  display: flex;
  align-items: center;
}
</style>
