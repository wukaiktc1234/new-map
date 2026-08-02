<script setup lang="ts">
/**
 * 采购链路追溯对话框
 *
 * 【层级】模块层组件（views/purchase/components）
 * 【职责】在采购订单页面内按订单号展示采购→入库→库存/资产完整链路
 * 【依赖】core/DataTable、core/StatusTag、core/EmptyState
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { useLayoutStore } from '@/stores/layout'
import { inventoryTraceApi } from '@/api/warehouse/inventory-trace'
import type { ProcurementTraceInfo } from '@/types/procurement-trace'
import type { DataTableColumn } from '@/components/core/DataTable.vue'

const layoutStore = useLayoutStore()

const props = defineProps<{
  visible: boolean
  orderNo: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const dialogVisible = computed<boolean>({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const loading = ref(false)
const traceList = ref<ProcurementTraceInfo[]>([])
const currentTrace = ref<ProcurementTraceInfo | null>(null)
const detailVisible = ref(false)

const columns = computed<DataTableColumn[]>(() => [
  { prop: 'orderNo', label: '采购订单号', minWidth: 150, slot: 'orderNo' },
  { prop: 'requestNo', label: '采购申请号', minWidth: 150, slot: 'requestNo' },
  { prop: 'supplierName', label: '供应商', minWidth: 140, slot: 'supplierName' },
  { prop: 'orderAmount', label: '订单金额', minWidth: 120, align: 'right', slot: 'orderAmount' },
  { prop: 'orderStatusText', label: '订单状态', minWidth: 100, slot: 'orderStatusText' },
  { prop: 'stockinCodes', label: '入库单号', minWidth: 160, slot: 'stockinCodes' },
  { prop: 'warehouseName', label: '入库仓库', minWidth: 120, slot: 'warehouseName' },
  { prop: '_operation', label: '操作', width: 120, fixed: 'right', slot: 'operation' },
])

/** 按采购订单号加载追溯链路 */
async function loadTrace(): Promise<void> {
  if (!props.orderNo) return

  loading.value = true
  try {
    const res = await inventoryTraceApi.getTraceByOrderNo(props.orderNo)
    traceList.value = res ? [res] : []
    if (traceList.value.length === 0) {
      ElMessage.info('未找到相关追溯数据')
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '查询失败')
    }
    traceList.value = []
  } finally {
    loading.value = false
  }
}

/** 查看详情 */
function handleViewDetail(row: ProcurementTraceInfo): void {
  currentTrace.value = row
  detailVisible.value = true
}

/** 格式化金额 */
function formatAmount(amount?: number): string {
  return amount != null ? amount.toFixed(2) : '0.00'
}

/** 格式化日期时间 */
function formatTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 关闭对话框时重置状态 */
function handleClosed(): void {
  traceList.value = []
  currentTrace.value = null
  detailVisible.value = false
}

watch([() => props.visible, () => props.orderNo], ([visible, orderNo]) => {
  if (visible && orderNo) {
    loadTrace()
  }
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="采购链路追溯"
    width="960px"
    class="fts-dialog--lg"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
    @closed="handleClosed"
  >
    <div v-if="orderNo" class="trace-subtitle">
      <span class="trace-subtitle__label">采购订单号：</span>
      <span class="trace-subtitle__value">{{ orderNo }}</span>
    </div>

    <section class="table-section">
      <DataTable
        :data="traceList"
        :columns="columns"
        :loading="loading"
        :selectable="false"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
      >
        <template #orderNo="{ row }">
          <span class="code-text">{{ row.orderNo || '-' }}</span>
        </template>

        <template #requestNo="{ row }">
          <span class="code-text">{{ row.requestNo || '-' }}</span>
        </template>

        <template #supplierName="{ row }">
          <span class="secondary-text">{{ row.supplierName || '-' }}</span>
        </template>

        <template #orderAmount="{ row }">
          <span class="amount-text">¥{{ formatAmount(row.orderAmount) }}</span>
        </template>

        <template #orderStatusText="{ row }">
          <StatusTag
            :status="row.orderStatus || 'info'"
            :label="row.orderStatusText || row.orderStatus || '-'"
            size="small"
          />
        </template>

        <template #stockinCodes="{ row }">
          <div class="stockin-list">
            <span v-for="code in row.stockinCodes" :key="code" class="stockin-tag">{{ code }}</span>
            <span v-if="!row.stockinCodes?.length" class="secondary-text">-</span>
          </div>
        </template>

        <template #warehouseName="{ row }">
          <span class="secondary-text">{{ row.warehouseName || '-' }}</span>
        </template>

        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="small" @click.stop="handleViewDetail(row)">链路详情</el-button>
          </div>
        </template>

        <template #empty>
          <EmptyState description="暂无追溯数据" />
        </template>
      </DataTable>
    </section>

    <!-- 链路详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="采购链路详情"
      width="1200px"
      class="fts-dialog--xl"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
      append-to-body
    >
      <template v-if="currentTrace">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="采购订单号">{{ currentTrace.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购申请号">{{ currentTrace.requestNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请标题">{{ currentTrace.requestTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentTrace.createByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentTrace.supplierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ formatAmount(currentTrace.orderAmount) }}</el-descriptions-item>
          <el-descriptions-item label="入库仓库">{{ currentTrace.warehouseName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="质检结果">{{ currentTrace.qualityCheckResultText || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 物料明细 -->
        <el-divider content-position="left">采购物料明细</el-divider>
        <el-table v-if="currentTrace.materialItems?.length" :data="currentTrace.materialItems" size="small" border>
          <el-table-column prop="materialName" label="物料名称" min-width="140" />
          <el-table-column prop="specification" label="规格" min-width="100" />
          <el-table-column prop="unit" label="单位" width="80" align="center" />
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column prop="batchNo" label="批次号" min-width="120" />
          <el-table-column prop="expiryDate" label="有效期至" min-width="120" />
        </el-table>
        <el-empty v-else description="暂无物料明细" :image-size="60" />

        <!-- 生成资产 -->
        <template v-if="currentTrace.assets?.length">
          <el-divider content-position="left">生成资产</el-divider>
          <el-table :data="currentTrace.assets" size="small" border>
            <el-table-column prop="assetCode" label="资产编码" min-width="140" />
            <el-table-column prop="assetName" label="资产名称" min-width="140" />
            <el-table-column prop="statusText" label="状态" width="100" />
            <el-table-column prop="purchaseDate" label="购置日期" min-width="120" />
            <el-table-column prop="originalValue" label="原值（元）" width="120" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.originalValue) }}</template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 链路节点时间轴 -->
        <el-divider content-position="left">链路节点</el-divider>
        <el-timeline v-if="currentTrace.traceNodes?.length">
          <el-timeline-item
            v-for="node in currentTrace.traceNodes"
            :key="`${node.nodeType}-${node.nodeId}`"
            :type="node.status === 'error' ? 'danger' : node.status === 'success' ? 'success' : 'primary'"
            :timestamp="formatTime(node.operateTime)"
          >
            <div class="trace-node">
              <div class="node-title">{{ node.title }}</div>
              <div class="node-meta">
                <StatusTag
                  :status="node.status"
                  :label="node.statusText"
                  size="small"
                />
                <span v-if="node.operatorName" class="operator-text">操作人：{{ node.operatorName }}</span>
              </div>
              <div v-if="node.remark" class="node-remark">{{ node.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无链路节点" :image-size="60" />
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<style scoped lang="scss">
.trace-subtitle {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-md);

  &__label {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
  }

  &__value {
    font-family: var(--fts-font-family-mono);
    color: var(--fts-text-primary);
    font-variant-numeric: tabular-nums;
  }
}

.table-section {
  overflow-x: auto;

  :deep(.el-table) {
    width: 100%;
  }

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

.code-text {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.secondary-text {
  color: var(--fts-text-secondary);
}

.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.stockin-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-1);
}

.stockin-tag {
  display: inline-block;
  padding: 0 var(--fts-space-2);
  background: var(--fts-primary-bg);
  color: var(--fts-primary);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);
}

.trace-node {
  .node-title {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  .node-meta {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    margin-top: var(--fts-space-1);
  }

  .operator-text {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  .node-remark {
    margin-top: var(--fts-space-1);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }
}
</style>
