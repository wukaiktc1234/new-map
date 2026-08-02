<script setup lang="ts">
/**
 * 到货单详情对话框（数据密集型样板）
 * 采用分组信息卡 + 横向滚动明细表格，适配字段较多的详情场景
 */
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import type { PurchaseArrivalInfo } from '@/types/purchase-arrival'
import {
  ReceiverTypeOptions,
  ShipmentStatusOptions,
  TransportModeOptions,
  VehicleTypeOptions,
} from '@/types/purchase-arrival'
import { purchaseArrivalApi } from '@/api/purchase/arrival'

interface Props {
  modelValue: boolean
  data: PurchaseArrivalInfo | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
}>()

/** 操作中状态 */
const actionLoading = ref(false)

/** 是否待收货（可质检/确认入库） */
const isPending = computed(() => props.data?.status === 'pending')
/** 质检是否已通过 */
const isQcPassed = computed(() => props.data?.qualityCheckResult === 1)

/** 质检操作 */
async function handleQualityCheck(pass: boolean): Promise<void> {
  if (!props.data) return
  let remark = ''
  if (!pass) {
    const { value } = await ElMessageBox.prompt('请输入质检失败原因', '质检不通过', {
      inputPlaceholder: '如：货物破损、数量不符等',
      confirmButtonText: '确认不通过',
      cancelButtonText: '取消',
    }).catch(() => ({ value: '' }))
    if (!value?.trim()) {
      ElMessage.warning('请填写质检失败原因')
      return
    }
    remark = value
  }
  actionLoading.value = true
  try {
    await purchaseArrivalApi.qualityCheck(props.data.arrivalId, pass ? 1 : 2, remark)
    ElMessage.success(pass ? '质检通过' : '已标记质检不通过')
    emit('refresh')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '质检操作失败')
  } finally {
    actionLoading.value = false
  }
}

/** 确认入库 */
async function handleConfirmArrival(): Promise<void> {
  if (!props.data) return
  try {
    await ElMessageBox.confirm(
      '确认入库后将增加库存（仓库+门店）、生成应付账款并创建原料追溯码，是否继续？',
      '确认入库',
      { confirmButtonText: '确认入库', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await purchaseArrivalApi.confirmArrival(props.data.arrivalId)
    ElMessage.success('确认入库成功')
    emit('refresh')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '确认入库失败')
  } finally {
    actionLoading.value = false
  }
}

interface StatusTagConfig {
  status: string
  label: string
}

const statusConfigMap: Record<string, StatusTagConfig> = {
  pending: { status: 'pending', label: '待收货' },
  receiving: { status: 'pending', label: '收货中' },
  partial_received: { status: 'warning', label: '部分收货' },
  received: { status: 'success', label: '已收货' },
  closed: { status: 'inactive', label: '已关闭' },
}

const statusConfig = computed(() => {
  const status = props.data?.status
  if (!status) return { status: 'info', label: '-' }
  return statusConfigMap[status] ?? { status: 'info', label: status }
})

function getReceiverTypeLabel(type: string): string {
  return ReceiverTypeOptions.find((o) => o.value === type)?.label || type
}

function getShipmentStatusLabel(status?: string): string {
  if (!status) return '-'
  return ShipmentStatusOptions.find((o) => o.value === status)?.label || status
}

function getTransportModeLabel(mode?: string): string {
  if (!mode) return '-'
  return TransportModeOptions.find((o) => o.value === mode)?.label || mode
}

function getVehicleTypeLabel(type?: string): string {
  if (!type) return '-'
  return VehicleTypeOptions.find((o) => o.value === type)?.label || type
}

function getReceiverName(data: PurchaseArrivalInfo): string {
  const typeLabel = getReceiverTypeLabel(data.receiverType)
  const name = data.storeId || data.warehouseId || ''
  return name ? `${typeLabel}(${name})` : typeLabel
}

function formatDate(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatMoney(value?: number): string {
  if (value == null) return '-'
  return value.toFixed(2)
}

const itemSummary = computed(() => {
  const items = props.data?.items || []
  return {
    count: items.length,
    expectedQuantity: items.reduce((sum, item) => sum + (item.expectedQuantity || 0), 0),
    receivedQuantity: items.reduce((sum, item) => sum + (item.receivedQuantity || 0), 0),
    amount: items.reduce((sum, item) => sum + (item.amount || 0), 0),
  }
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="到货单详情"
    width="1100px"
    class="data-intensive-dialog arrival-detail-dialog fts-dialog--wide"
    destroy-on-close
    lock-scroll="false"
    align-center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="data">
      <!-- 基本信息 -->
      <section class="detail-section">
        <div class="section-header">基本信息</div>
        <el-descriptions :column="4" border class="compact-descriptions">
          <el-descriptions-item label="到货单号" :span="1">
            {{ data.arrivalCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="采购订单号" :span="1">
            {{ data.orderCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="供应商" :span="2">
            {{ data.supplierName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货方" :span="2">
            {{ getReceiverName(data) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态" :span="2">
            <StatusTag
              :status="statusConfig.status"
              :label="statusConfig.label"
              size="small"
            />
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <!-- 物流信息 -->
      <section class="detail-section">
        <div class="section-header">物流信息</div>
        <el-descriptions :column="4" border class="compact-descriptions">
          <el-descriptions-item label="发货状态">
            {{ getShipmentStatusLabel(data.shipmentStatus) }}
          </el-descriptions-item>
          <el-descriptions-item label="物流单号">
            {{ data.logisticsNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="物流公司">
            {{ data.logisticsCompany || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="运输方式">
            {{ getTransportModeLabel(data.transportMode) }}
          </el-descriptions-item>
          <el-descriptions-item label="车辆类型">
            {{ getVehicleTypeLabel(data.vehicleType) }}
          </el-descriptions-item>
          <el-descriptions-item label="车牌号">
            {{ data.vehiclePlateNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="司机" :span="2">
            {{ data.driverName || data.driverPhone ? `${data.driverName || ''} ${data.driverPhone || ''}`.trim() : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <!-- 时间数量 -->
      <section class="detail-section">
        <div class="section-header">时间数量</div>
        <el-descriptions :column="4" border class="compact-descriptions">
          <el-descriptions-item label="预计到货日期">
            {{ formatDate(data.estimatedArrivalDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="实际到货日期">
            {{ formatDate(data.actualArrivalDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="到货数量">
            {{ data.totalQuantity }}
          </el-descriptions-item>
          <el-descriptions-item label="已收数量">
            {{ data.receivedQuantity }}
          </el-descriptions-item>
          <el-descriptions-item label="运费（元）" :span="2">
            {{ formatMoney(data.freightAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="关闭原因" :span="2">
            {{ data.closeReason || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="4">
            {{ data.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <!-- 到货明细 -->
      <section v-if="data.items && data.items.length" class="detail-section">
        <div class="section-header">
          到货明细
          <span class="detail-section__count">共 {{ itemSummary.count }} 项</span>
        </div>
        <div class="detail-table-wrapper">
          <el-table
            :data="data.items"
            border
            size="small"
            class="detail-table"
            :summary-method="() => ['', '合计', '', '', itemSummary.expectedQuantity, itemSummary.receivedQuantity, '', itemSummary.amount, '']"
            show-summary
          >
            <el-table-column type="index" label="序号" width="55" fixed="left" />
            <el-table-column prop="materialName" label="物料名称" min-width="160" fixed="left" show-overflow-tooltip />
            <el-table-column prop="specification" label="规格" min-width="120" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" min-width="80" />
            <el-table-column prop="expectedQuantity" label="预计数量" min-width="110" align="right" />
            <el-table-column prop="receivedQuantity" label="已收数量" min-width="110" align="right" />
            <el-table-column prop="unitPrice" label="单价（元）" min-width="120" align="right">
              <template #default="{ row }">
                {{ formatMoney(row.unitPrice) }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额（元）" min-width="120" align="right">
              <template #default="{ row }">
                {{ formatMoney(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          </el-table>
        </div>
      </section>

      <!-- 质检/入库操作区 -->
      <section v-if="isPending" class="detail-section arrival-actions">
        <div class="section-header">入库操作</div>
        <div class="arrival-actions__buttons">
          <el-button
            type="success"
            :loading="actionLoading"
            :disabled="isQcPassed"
            @click="handleQualityCheck(true)"
          >
            {{ isQcPassed ? '质检已通过' : '质检通过' }}
          </el-button>
          <el-button
            type="danger"
            :loading="actionLoading"
            :disabled="isQcPassed"
            @click="handleQualityCheck(false)"
          >
            质检不通过
          </el-button>
          <el-button
            type="primary"
            :loading="actionLoading"
            :disabled="!isQcPassed"
            @click="handleConfirmArrival"
          >
            确认入库
          </el-button>
        </div>
      </section>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-section {
  margin-bottom: var(--fts-space-5);

  &__count {
    font-weight: var(--fts-font-weight-normal);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }
}

.detail-table-wrapper {
  width: 100%;
  overflow-x: auto;
  border-radius: var(--fts-radius-base);
}

.detail-table {
  min-width: 900px;
}

.arrival-actions {
  &__buttons {
    display: flex;
    gap: var(--fts-space-3);
  }
}
</style>
