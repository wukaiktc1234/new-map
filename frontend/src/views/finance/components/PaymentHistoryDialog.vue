<script setup lang="ts">
/**
 * 付款历史对话框
 *
 * 功能：
 * - 展示某笔应付账款的所有付款记录
 * - 列表显示：付款单号、付款日期、付款金额(元)、付款方式、付款账户、凭证号、流水号、状态、备注
 * - 状态使用 StatusTag 组件显示
 * - 金额从分转为元显示
 */
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import { paymentApi, fenToYuanNumber } from '@/api/finance'
import type { FinancePayment, PaymentMethod, PaymentStatus } from '@/types/finance'

interface Props {
  /** 对话框可见性 */
  visible: boolean
  /** 应付账款ID */
  payableId: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'void-success': []
}>()

/** 对话框可见性双向绑定 */
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

/** 原始付款记录列表（金额单位：分） */
const rawRecords = ref<FinancePayment[]>([])
/** 加载中状态 */
const loading = ref(false)

/** 列定义 */
const columns = [
  { prop: 'paymentNo', label: '付款单号', minWidth: 150 },
  { prop: 'paymentDate', label: '付款日期', minWidth: 110 },
  { prop: 'orderNo', label: '采购订单号', minWidth: 150 },
  { prop: 'stockinNo', label: '入库单号', minWidth: 150 },
  { prop: 'paymentAmount', label: '付款金额(元)', minWidth: 130, align: 'right' },
  { prop: 'paymentMethod', label: '付款方式', minWidth: 100 },
  { prop: 'bankAccountName', label: '付款账户', minWidth: 140 },
  { prop: 'voucherNo', label: '凭证号', minWidth: 140 },
  { prop: 'fundFlowNo', label: '流水号', minWidth: 140 },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'remark', label: '备注', minWidth: 140 },
  { prop: 'actions', label: '操作', minWidth: 100, slot: 'actions', align: 'center' },
]

/** 付款方式映射 */
const paymentMethodMap: Record<PaymentMethod, string> = {
  bank_transfer: '银行转账',
  cash: '现金',
  check: '支票',
}

/** 付款状态映射（用于 StatusTag） */
const paymentStatusMap: Record<PaymentStatus, { status: string; label: string }> = {
  confirmed: { status: 'success', label: '已确认' },
  voided: { status: 'inactive', label: '已作废' },
}

/** 表格展示数据（金额分→元格式化，状态/方式语义化） */
const tableData = computed(() =>
  rawRecords.value.map(r => ({
    ...r,
    paymentAmount: fenToYuanNumber(r.paymentAmount).toFixed(2),
    paymentMethod: paymentMethodMap[r.paymentMethod] || r.paymentMethod,
    orderNo: r.orderNo || '-',
    stockinNo: r.stockinNo || '-',
    voucherNo: r.voucherNo || '-',
    fundFlowNo: r.fundFlowNo || '-',
    bankAccountName: r.bankAccountName || '-',
    remark: r.remark || '-',
  }))
)

/** 作废付款单 */
async function handleVoid(row: FinancePayment): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入作废备注（可选）', '作废付款单', {
      confirmButtonText: '确认作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入作废原因',
      confirmButtonClass: 'el-button--danger',
      type: 'warning',
    })
    await paymentApi.voidPayment(row.id, { remark: value || undefined })
    ElMessage.success('付款单已作废')
    emit('void-success')
    await loadHistory()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '作废失败')
    }
  }
}

/** 加载付款记录 */
async function loadHistory(): Promise<void> {
  if (!props.payableId) return
  loading.value = true
  try {
    const list = await paymentApi.getHistoryByPayableId(props.payableId)
    rawRecords.value = list || []
  } catch {
    rawRecords.value = []
    ElMessage.error('加载付款记录失败')
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
    if (visible && props.payableId) {
      loadHistory()
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="付款记录"
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
      empty-text="暂无付款记录"
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
            :status="paymentStatusMap[row.status as PaymentStatus]?.status || 'info'"
            :label="paymentStatusMap[row.status as PaymentStatus]?.label || row.status"
            size="small"
          />
        </template>
        <template v-else-if="col.slot === 'actions'" #default="{ row }">
          <el-button
            v-if="row.status === 'confirmed'"
            link
            type="danger"
            size="small"
            @click="handleVoid(row as FinancePayment)"
          >
            作废
          </el-button>
          <span v-else>-</span>
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
