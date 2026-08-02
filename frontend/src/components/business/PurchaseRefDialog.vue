<script setup lang="ts">
/**
 * 关联单据详情弹窗（单据链互动）
 *
 * 在单据详情中点击来源单号（如采购申请）时弹出，展示该单据的关键信息，
 * 不跳转页面，保持当前操作上下文。
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import { purchaseRequestApi } from '@/api/purchase/request'
import { purchaseRequestConverter } from '@/api/purchase/converters'
import { purchaseArrivalApi } from '@/api/purchase/arrival'
import type { PurchaseRequest } from '@/types/purchase-request'
import type { PurchaseArrivalInfo } from '@/types/purchase-arrival'

const visible = ref(false)
const loading = ref(false)
const detail = ref<PurchaseRequest | null>(null)
const arrivalDetail = ref<PurchaseArrivalInfo | null>(null)

interface Props {
  modelValue: boolean
  /** 关联单据类型：purchase_request / purchase_arrival */
  bizType: string
  /** 关联单据业务ID（purchase_request 用） */
  bizId?: string
  /** 关联单据单号（purchase_arrival 用，按订单号查询） */
  bizNo?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

async function loadDetail(): Promise<void> {
  loading.value = true
  detail.value = null
  arrivalDetail.value = null
  try {
    if (props.bizType === 'purchase_request' && props.bizId) {
      const res = await purchaseRequestApi.getById(props.bizId)
      detail.value = res
    } else if (props.bizType === 'purchase_arrival' && props.bizNo) {
      const list = await purchaseArrivalApi.getList({ orderNo: props.bizNo, page: 1, size: 1 })
      arrivalDetail.value = list.records?.[0] ?? null
    }
  } catch {
    ElMessage.error('加载关联单据详情失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      loadDetail()
    }
  },
)

function formatTime(iso?: string): string {
  if (!iso) return '-'
  return iso.replace('T', ' ').slice(0, 16)
}

function formatAmount(amount: number): string {
  return Number(amount || 0).toFixed(2)
}

/** 到货单状态文案 */
const arrivalStatusLabel = (() => {
  const map: Record<string, string> = {
    pending: '待收货',
    receiving: '收货中',
    partial_received: '部分收货',
    received: '已收货',
    closed: '已关闭',
  }
  return (s: string | undefined) => map[s || ''] || s || '-'
})()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="关联单据详情"
    width="720px"
    class="fts-dialog--lg"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号">{{ detail.requestNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="purchaseRequestConverter.toStatusTagStatus(detail.status)"
              :label="purchaseRequestConverter.toStatusLabel(detail.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="申请标题" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="申请部门">{{ detail.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.createByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请日期">{{ formatTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="期望到货">{{ detail.expectedDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ formatAmount(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.items && detail.items.length" class="ref-items">
          <div class="ref-items__title">申请明细</div>
          <el-table :data="detail.items" size="small" border>
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="materialName" label="物料名称" min-width="130" />
            <el-table-column prop="specification" label="规格" min-width="90" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="quantity" label="数量" width="90" align="right" />
            <el-table-column label="预估单价(元)" width="110" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.estimatedPrice) }}</template>
            </el-table-column>
            <el-table-column label="小计(元)" width="110" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.subtotalAmount) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- 到货单详情 -->
      <template v-else-if="arrivalDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="到货单号">{{ arrivalDetail.arrivalCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="arrivalDetail.status"
              :label="arrivalStatusLabel(arrivalDetail.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ arrivalDetail.orderCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货仓库">{{ arrivalDetail.warehouseId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="到货数量">{{ arrivalDetail.totalQuantity }}</el-descriptions-item>
          <el-descriptions-item label="已收数量">{{ arrivalDetail.receivedQuantity }}</el-descriptions-item>
          <el-descriptions-item label="预计到货">{{ arrivalDetail.estimatedArrivalDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际到货">{{ arrivalDetail.actualArrivalDate || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="arrivalDetail.items && arrivalDetail.items.length" class="ref-items">
          <div class="ref-items__title">到货明细</div>
          <el-table :data="arrivalDetail.items" size="small" border>
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="materialName" label="物料名称" min-width="130" />
            <el-table-column prop="specification" label="规格" min-width="90" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="expectedQuantity" label="预计数量" width="100" align="right" />
            <el-table-column prop="receivedQuantity" label="已收数量" width="100" align="right" />
          </el-table>
        </div>
      </template>

      <el-empty v-if="!loading && !detail && !arrivalDetail" description="暂无关联单据" :image-size="80" />
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.ref-items {
  margin-top: var(--fts-space-4);

  &__title {
    font-weight: var(--fts-font-weight-semibold);
    margin-bottom: var(--fts-space-2);
  }
}
</style>
