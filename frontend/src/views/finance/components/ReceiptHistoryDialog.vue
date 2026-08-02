<script setup lang="ts">
/**
 * 收款历史对话框
 *
 * 功能：
 * - 展示某笔应收账款的所有收款记录
 * - 列表显示：收款单号、收款日期、收款金额(元)、收款方式、收款账户、凭证号、流水号、状态、备注
 * - 状态使用 StatusTag 组件显示
 * - 金额从分转为元显示
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import { receiptApi, fenToYuanNumber } from '@/api/finance'
import type { FinanceReceipt, ReceiptMethod, ReceiptStatus } from '@/types/finance'

interface Props {
  /** 对话框可见性 */
  visible: boolean
  /** 应收账款ID */
  receivableId: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

/** 对话框可见性双向绑定 */
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

/** 原始收款记录列表（金额单位：分） */
const rawRecords = ref<FinanceReceipt[]>([])
/** 加载中状态 */
const loading = ref(false)

/** 列定义 */
const columns = [
  { prop: 'receiptNo', label: '收款单号', minWidth: 150 },
  { prop: 'receiptDate', label: '收款日期', minWidth: 110 },
  { prop: 'receiptAmount', label: '收款金额(元)', minWidth: 130, align: 'right' },
  { prop: 'receiptMethod', label: '收款方式', minWidth: 100 },
  { prop: 'bankAccountName', label: '收款账户', minWidth: 140 },
  { prop: 'voucherNo', label: '凭证号', minWidth: 140 },
  { prop: 'fundFlowNo', label: '流水号', minWidth: 140 },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'remark', label: '备注', minWidth: 140 },
]

/** 收款方式映射 */
const receiptMethodMap: Record<ReceiptMethod, string> = {
  bank_transfer: '银行转账',
  cash: '现金',
  check: '支票',
}

/** 收款状态映射（用于 StatusTag） */
const receiptStatusMap: Record<ReceiptStatus, { status: string; label: string }> = {
  confirmed: { status: 'success', label: '已确认' },
  voided: { status: 'inactive', label: '已作废' },
}

/** 表格展示数据（金额分→元格式化，状态/方式语义化） */
const tableData = computed(() =>
  rawRecords.value.map(r => ({
    ...r,
    receiptAmount: fenToYuanNumber(r.receiptAmount).toFixed(2),
    receiptMethod: receiptMethodMap[r.receiptMethod] || r.receiptMethod,
    voucherNo: r.voucherNo || '-',
    fundFlowNo: r.fundFlowNo || '-',
    bankAccountName: r.bankAccountName || '-',
    remark: r.remark || '-',
  }))
)

/** 加载收款记录 */
async function loadHistory(): Promise<void> {
  if (!props.receivableId) return
  loading.value = true
  try {
    const list = await receiptApi.getHistoryByReceivableId(props.receivableId)
    rawRecords.value = list || []
  } catch {
    rawRecords.value = []
    ElMessage.error('加载收款记录失败')
  } finally {
    loading.value = false
  }
}

/** 对话框关闭时清空数据 */
function handleClosed(): void {
  rawRecords.value = []
}

// 监听对话框打开：加载数据
watch(
  () => props.visible,
  (visible) => {
    if (visible && props.receivableId) {
      loadHistory()
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="收款记录"
    width="800px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
      size="small"
      empty-text="暂无收款记录"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :min-width="col.minWidth"
        :align="(col.align as 'left' | 'right' | 'center') || 'left'"
      >
        <template v-if="col.slot === 'status'" #default="{ row }">
          <StatusTag
            :status="receiptStatusMap[row.status as ReceiptStatus]?.status || 'info'"
            :label="receiptStatusMap[row.status as ReceiptStatus]?.label || row.status"
            size="small"
          />
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
:deep(.el-table) {
  .el-table__cell {
    font-size: var(--fts-font-size-sm);
  }
}
</style>
