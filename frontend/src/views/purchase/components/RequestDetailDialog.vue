<script setup lang="ts">
/**
 * 采购申请详情对话框（共用组件）
 *
 * 采购申请页与采购订单详情（步骤条"采购申请"节点）复用同一详情展示，
 * 保证信息一致、维护单一。
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import { purchaseRequestApi } from '@/api/purchase/request'
import { purchaseRequestConverter } from '@/api/purchase/converters'
import type { PurchaseRequest } from '@/types/purchase-request'

const visible = ref(false)
const loading = ref(false)
const detail = ref<PurchaseRequest | null>(null)

interface Props {
  modelValue: boolean
  /** 申请ID */
  bizId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

async function loadDetail(): Promise<void> {
  if (!props.bizId) return
  loading.value = true
  try {
    const res = await purchaseRequestApi.getById(props.bizId)
    detail.value = res
  } catch {
    ElMessage.error('加载申请详情失败')
    detail.value = null
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

function getPriorityLabel(priority: string): string {
  const map: Record<string, string> = {
    low: '低',
    normal: '普通',
    high: '高',
    urgent: '紧急',
  }
  return map[priority] || priority
}

function getPriorityColor(priority: string): string {
  const map: Record<string, string> = {
    low: 'info',
    normal: 'primary',
    high: 'warning',
    urgent: 'error',
  }
  return map[priority] || 'info'
}

function formatTime(iso?: string): string {
  if (!iso) return '-'
  return iso.replace('T', ' ').slice(0, 16)
}

function formatAmount(amount: number): string {
  return purchaseRequestConverter.formatYuan(amount)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="采购申请详情"
    width="1100px"
    class="fts-dialog--wide"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号">{{ detail.requestNo }}</el-descriptions-item>
          <el-descriptions-item label="申请标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.createByName }}</el-descriptions-item>
          <el-descriptions-item label="申请部门">{{ detail.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <StatusTag
              :status="getPriorityColor(detail.priority)"
              :label="getPriorityLabel(detail.priority)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="purchaseRequestConverter.toStatusTagStatus(detail.status)"
              :label="purchaseRequestConverter.toStatusLabel(detail.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="期望到货日期">{{ detail.expectedDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span class="detail-amount">¥{{ formatAmount(detail.totalAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ detail.approvedTime ? formatTime(detail.approvedTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="拒绝原因" :span="2">
            <span class="reject-reason">{{ detail.rejectReason }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 明细列表 -->
        <el-divider content-position="left">采购明细</el-divider>
        <el-table v-if="detail.items?.length" :data="detail.items" border>
          <el-table-column prop="materialName" label="物料名称" min-width="140" />
          <el-table-column prop="specification" label="规格" min-width="100" />
          <el-table-column prop="unit" label="单位" width="70" align="center" />
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column label="预估单价(元)" width="110" align="right">
            <template #default="{ row }">{{ formatAmount(row.estimatedPrice) }}</template>
          </el-table-column>
          <el-table-column label="小计(元)" width="110" align="right">
            <template #default="{ row }">{{ formatAmount(row.subtotalAmount) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="用途/备注" min-width="120" />
        </el-table>
        <el-empty v-else description="暂无明细" :image-size="60" />
      </template>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-amount {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.reject-reason {
  color: var(--fts-error);
}
</style>
