<script setup lang="ts">
/**
 * 菜品库存决策看板（方案D+/E）
 *
 * D+：基于近 N 天订单销量 + 门店库存流水自校准"实际每份用量"（含损耗），
 *     预测 可做份数 / 理论份数 / 预计售罄 / 建议补货。
 * E：  原料理论消耗 vs 实际消耗差异分析，揪出超耗/浪费。
 */
import { ref, computed, watch } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { bomCheckApi } from '@/api/product/bom-check'
import type { StockForecastResult, VarianceItem } from '@/api/product/bom-check'

const props = defineProps<{
  modelValue: boolean
  forecast: StockForecastResult | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const activeTab = ref<'items' | 'variance'>('items')

// ==================== 差异分析（方案E） ====================

const varianceLoading = ref(false)
const varianceList = ref<VarianceItem[]>([])

async function loadVariance(): Promise<void> {
  varianceLoading.value = true
  try {
    varianceList.value = await bomCheckApi.varianceAnalysis(7, 20)
  } catch {
    varianceList.value = []
  } finally {
    varianceLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) loadVariance()
  },
)

// ==================== 展示辅助 ====================

const percent = (v?: number): string => {
  if (v == null) return '-'
  return `${(v * 100).toFixed(1)}%`
}

const daysText = (v?: number): string => {
  if (v == null || v <= 0) return '—'
  if (v < 1) return `${Math.round(v * 24)} 小时`
  return `${v.toFixed(1)} 天`
}

function formatQty(v?: number): string {
  if (v == null) return '-'
  return Number.isInteger(v) ? String(v) : Number(v).toFixed(3)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="库存决策看板"
    width="880px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="forecast">
      <!-- 顶部决策指标 -->
      <el-row :gutter="12" class="stats-row">
        <el-col :span="6">
          <div class="stat-card" :class="forecast.canMake ? 'stat-card--ok' : 'stat-card--warn'">
            <div class="stat-card__label">可做份数</div>
            <div class="stat-card__value">{{ forecast.maxServings }}</div>
            <div class="stat-card__sub">瓶颈：{{ forecast.bottleneckMaterialName || '-' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-card__label">理论份数（无损耗）</div>
            <div class="stat-card__value">{{ forecast.theoreticalServings }}</div>
            <div class="stat-card__sub">差异体现隐含损耗</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-card__label">预计售罄</div>
            <div class="stat-card__value">{{ daysText(forecast.daysToSellout) }}</div>
            <div class="stat-card__sub">日均销量 {{ forecast.avgDailySales }} 份</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-card__label">建议补货</div>
            <div class="stat-card__value">{{ forecast.suggestedRestock }}</div>
            <div class="stat-card__sub">瓶颈原料补至安全库存</div>
          </div>
        </el-col>
      </el-row>

      <el-alert
        class="source-note"
        :type="forecast.hasSufficientData ? 'info' : 'warning'"
        :closable="false"
        show-icon
        :title="`实际每份用量基于近 ${forecast.dataWindowDays} 天订单销量+库存流水自校准（含洗捡配切损耗）`"
        :description="forecast.hasSufficientData
          ? '数据充足：可做份数已按实际用量（含损耗）计算。'
          : '销量数据不足，暂按理论BOM用量估算（无损耗）。随订单积累自动校准。'"
      />

      <el-tabs v-model="activeTab" class="forecast-tabs">
        <el-tab-pane label="原料明细" name="items">
          <el-table :data="forecast.items" size="small" border max-height="320">
            <el-table-column prop="materialName" label="原料" min-width="120" />
            <el-table-column label="可用库存" min-width="90" align="right">
              <template #default="{ row }">{{ formatQty(row.availableStock) }} {{ row.unit || '' }}</template>
            </el-table-column>
            <el-table-column label="理论每份" min-width="80" align="right">
              <template #default="{ row }">{{ formatQty(row.recipeQty) }}</template>
            </el-table-column>
            <el-table-column label="实际每份" min-width="80" align="right">
              <template #default="{ row }">
                <b>{{ formatQty(row.actualQtyPerServing) }}</b>
              </template>
            </el-table-column>
            <el-table-column label="隐含损耗" min-width="90" align="right">
              <template #default="{ row }">
                <StatusTag
                  v-if="row.impliedLossRate > 0"
                  status="warning"
                  :label="percent(row.impliedLossRate)"
                  size="small"
                />
                <StatusTag v-else status="info" label="0%" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="可做份数" min-width="90" align="right">
              <template #default="{ row }">
                <span :class="{ 'bottleneck-text': row.isBottleneck }">
                  {{ row.maxServingsByThis }}{{ row.isBottleneck ? ' ⚠' : '' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="安全库存" min-width="80" align="right">
              <template #default="{ row }">{{ formatQty(row.safetyStock) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="损耗差异分析" name="variance">
          <div v-loading="varianceLoading">
            <el-table :data="varianceList" size="small" border max-height="320" v-if="varianceList.length">
              <el-table-column prop="materialName" label="原料" min-width="130" />
              <el-table-column label="理论消耗" min-width="90" align="right">
                <template #default="{ row }">{{ formatQty(row.theoreticalConsumed) }} {{ row.unit || '' }}</template>
              </el-table-column>
              <el-table-column label="实际消耗" min-width="90" align="right">
                <template #default="{ row }">{{ formatQty(row.actualConsumed) }}</template>
              </el-table-column>
              <el-table-column label="差异量" min-width="90" align="right">
                <template #default="{ row }">
                  <span :class="row.varianceQty > 0 ? 'variance-pos' : 'variance-neg'">
                    {{ row.varianceQty > 0 ? '+' : '' }}{{ formatQty(row.varianceQty) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="差异率" min-width="90" align="right">
                <template #default="{ row }">
                  <StatusTag
                    :status="row.varianceRate > 0.15 ? 'error' : row.varianceRate > 0.05 ? 'warning' : 'success'"
                    :label="percent(row.varianceRate)"
                    size="small"
                  />
                </template>
              </el-table-column>
              <el-table-column label="售出份数" min-width="80" align="right">
                <template #default="{ row }">{{ formatQty(row.soldServings) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!varianceLoading" description="近 7 天暂无销售数据，无差异可分析" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts">
export default { name: 'StockForecastDialog' }
</script>

<style scoped lang="scss">
.stats-row {
  margin-bottom: var(--fts-space-3);
}

.stat-card {
  padding: var(--fts-space-3);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);
  text-align: center;

  &--ok {
    border-color: var(--fts-success);
  }
  &--warn {
    border-color: var(--fts-warning);
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }
  &__value {
    font-size: 22px;
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin: var(--fts-space-1) 0;
  }
  &__sub {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

.source-note {
  margin-bottom: var(--fts-space-3);
}

.forecast-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: var(--fts-space-3);
  }
}

.bottleneck-text {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-semibold);
}

.variance-pos {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}
.variance-neg {
  color: var(--fts-success);
}
</style>
