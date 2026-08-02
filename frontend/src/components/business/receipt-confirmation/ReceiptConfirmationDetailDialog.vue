<script setup lang="ts">
/**
 * 收货确认单详情对话框
 */
import { computed } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import type { ReceiptConfirmationInfo } from '@/types/purchase-arrival'
import { QualityCheckResultOptions } from '@/types/purchase-arrival'

interface Props {
  modelValue: boolean
  data: ReceiptConfirmationInfo | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const statusConfig = computed(() => {
  const status = props.data?.status
  if (status == null) return { status: 'info', label: '-' }
  if (status === 1) return { status: 'success', label: '已确认' }
  if (status === 2) return { status: 'error', label: '已取消' }
  return { status: 'info', label: String(status) }
})

const qualityCheckLabel = computed(() => {
  const value = props.data?.qualityCheckResult
  if (value == null) return '-'
  return QualityCheckResultOptions.find((o) => o.value === value)?.label || String(value)
})

function formatDate(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="收货确认单详情"
    width="850px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="data">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="确认单号">
          {{ data.confirmationCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="关联到货单">
          {{ data.arrivalId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="确认时间">
          {{ formatDate(data.confirmTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="确认数量">
          {{ data.totalQuantity }}
        </el-descriptions-item>
        <el-descriptions-item label="确认金额">
          {{ data.totalAmount.toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="statusConfig.status" :label="statusConfig.label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="质检结果">
          {{ qualityCheckLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="质检备注">
          {{ data.qualityRemark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ data.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="data.items && data.items.length" class="detail-items">
        <div class="detail-items__title">确认明细</div>
        <el-table :data="data.items" border size="small">
          <el-table-column label="物料名称/规格/单位" min-width="180">
            <template #default="{ row }">
              <div class="material-cell">
                <span class="material-cell__name">{{ row.materialName }}</span>
                <span v-if="row.specification" class="material-cell__spec">{{ row.specification }}</span>
                <span v-if="row.unit" class="material-cell__unit">（{{ row.unit }}）</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="confirmedQuantity" label="确认数量" min-width="100" align="right" />
          <el-table-column prop="rejectedQuantity" label="拒收数量" min-width="100" align="right" />
          <el-table-column prop="unitPrice" label="单价" min-width="100" align="right" />
          <el-table-column prop="amount" label="金额" min-width="100" align="right" />
          <el-table-column prop="batchNo" label="批次号" min-width="120" />
          <el-table-column prop="productionDate" label="生产日期" min-width="120" />
          <el-table-column prop="expiryDate" label="有效期至" min-width="120" />
          <el-table-column prop="remark" label="备注" min-width="140" />
        </el-table>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-items {
  margin-top: var(--fts-space-5);

  &__title {
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.material-cell {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &__name {
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-medium);
  }

  &__spec,
  &__unit {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
  }
}
</style>
