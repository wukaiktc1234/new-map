<script setup lang="ts">
/**
 * 库存入库页面
 * 处理仓库到货单的收货确认与历史确认单查询。
 */
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { useReceiptPage } from '@/composables/useReceiptPage'
import { PurchaseArrivalStatusOptions } from '@/types/purchase-arrival'
import ReceiptConfirmationFormDialog from '@/components/business/receipt-confirmation/ReceiptConfirmationFormDialog.vue'
import ReceiptConfirmationDetailDialog from '@/components/business/receipt-confirmation/ReceiptConfirmationDetailDialog.vue'
import ArrivalDetailDialog from '@/views/purchase/components/ArrivalDetailDialog.vue'

const layoutStore = useLayoutStore()
const receiverType = 'WAREHOUSE' as const

const {
  activeTab,
  arrivalQuery,
  arrivalList,
  arrivalLoading,
  refreshArrivals,
  arrivalPagination,
  confirmationQuery,
  confirmationList,
  confirmationLoading,
  refreshConfirmations,
  confirmationPagination,
  statistics,
  arrivalColumns,
  confirmationColumns,
  formDialogVisible,
  confirmingArrival,
  confirmDialogLoading,
  detailDialogVisible,
  detailMode,
  detailArrival,
  detailConfirmation,
  isOverdue,
  getStatusConfig,
  getConfirmationStatusConfig,
  getShipmentStatusLabel,
  formatVehicleInfo,
  canConfirm,
  openConfirmDialog,
  openArrivalDetail,
  openConfirmationDetail,
  handleConfirmationSuccess,
  formatDate,
} = useReceiptPage(receiverType)

function handleArrivalSearch(): void {
  arrivalPagination.current = 1
  refreshArrivals()
}

function handleArrivalReset(): void {
  arrivalQuery.value = {
    arrivalCode: '',
    orderCode: '',
    status: '',
    receiverType,
  }
  handleArrivalSearch()
}

function handleConfirmationSearch(): void {
  confirmationPagination.current = 1
  refreshConfirmations()
}

function handleConfirmationReset(): void {
  confirmationQuery.value = {
    confirmationCode: '',
    receiverType,
  }
  handleConfirmationSearch()
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存入库" description="处理仓库到货单的入库确认与历史查询" />

    <section class="stats-section">
      <StatCard icon="Box" label="待收货" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="今日已确认" :value="String(statistics.today)" color-type="success" variant="bordered" />
      <StatCard icon="Calendar" label="本月已确认" :value="String(statistics.month)" color-type="primary" variant="bordered" />
      <StatCard icon="Warning" label="超期待收" :value="String(statistics.overdue)" color-type="error" variant="bordered" />
    </section>

    <el-tabs v-model="activeTab" type="border-card" class="receipt-tabs">
      <el-tab-pane label="待收货" name="pending">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-input
                v-model="arrivalQuery.arrivalCode"
                placeholder="到货单号"
                clearable
                style="width: 150px"
                size="default"
                @keyup.enter="handleArrivalSearch"
                @clear="handleArrivalSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-input
                v-model="arrivalQuery.orderCode"
                placeholder="采购订单号"
                clearable
                style="width: 150px"
                size="default"
                @keyup.enter="handleArrivalSearch"
                @clear="handleArrivalSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-select
                v-model="arrivalQuery.status"
                placeholder="状态"
                clearable
                style="width: 130px"
                size="default"
                @change="handleArrivalSearch"
              >
                <el-option
                  v-for="opt in PurchaseArrivalStatusOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleArrivalSearch">查询</el-button>
              <el-button size="default" @click="handleArrivalReset">
                <el-icon :size="14"><Refresh /></el-icon>重置
              </el-button>
            </div>
          </div>
        </div>

        <section class="table-section">
          <DataTable
            :data="arrivalList"
            :columns="arrivalColumns"
            :loading="arrivalLoading"
            :selectable="true"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            :actions-width="160"
          >
          <template #arrivalCode="{ row }">
            <span class="code-text">{{ row.arrivalCode || '-' }}</span>
          </template>
          <template #orderCode="{ row }">
            <span class="code-text">{{ row.orderCode || '-' }}</span>
          </template>
          <template #supplierName="{ row }">
            <span class="secondary-text">{{ row.supplierName || '-' }}</span>
          </template>
          <template #shipmentStatus="{ row }">
            <span class="secondary-text">{{ getShipmentStatusLabel(row.shipmentStatus) }}</span>
          </template>
          <template #logisticsNo="{ row }">
            <span class="secondary-text">{{ row.logisticsNo || '-' }}</span>
          </template>
          <template #logisticsCompany="{ row }">
            <span class="secondary-text">{{ row.logisticsCompany || '-' }}</span>
          </template>
          <template #vehicleInfo="{ row }">
            <span class="secondary-text">{{ formatVehicleInfo(row) }}</span>
          </template>
          <template #estimatedArrivalDate="{ row }">
            <span :class="['date-text', { 'date-text--overdue': isOverdue(row) }]">
              {{ formatDate(row.estimatedArrivalDate) }}
            </span>
          </template>
          <template #quantity="{ row }">
            <span class="quantity-text">{{ row.totalQuantity }} / {{ row.receivedQuantity }}</span>
          </template>
          <template #status="{ row }">
            <StatusTag
              :status="getStatusConfig(row.status).status"
              :label="getStatusConfig(row.status).label"
              size="small"
            />
          </template>
          <template #actions="{ row }">
            <div class="action-text">
              <el-button link type="primary" size="small" @click.stop="openArrivalDetail(row)">详情</el-button>
              <el-button
                v-if="canConfirm(row)"
                link
                type="primary"
                size="small"
                :loading="confirmDialogLoading"
                @click.stop="openConfirmDialog(row)"
              >
                确认收货
              </el-button>
            </div>          </template>
          </DataTable>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="arrivalPagination.current"
              v-model:page-size="arrivalPagination.pageSize"
              :total="arrivalPagination.total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="refreshArrivals"
              @size-change="refreshArrivals"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="已确认" name="confirmed">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-input
                v-model="confirmationQuery.confirmationCode"
                placeholder="确认单号"
                clearable
                style="width: 200px"
                size="default"
                @keyup.enter="handleConfirmationSearch"
                @clear="handleConfirmationSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleConfirmationSearch">查询</el-button>
              <el-button size="default" @click="handleConfirmationReset">
                <el-icon :size="14"><Refresh /></el-icon>重置
              </el-button>
            </div>
          </div>
        </div>

        <section class="table-section">
          <DataTable
            :data="confirmationList"
            :columns="confirmationColumns"
            :loading="confirmationLoading"
            :selectable="true"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            :actions-width="120"
          >
          <template #confirmationCode="{ row }">
            <span class="code-text">{{ row.confirmationCode || '-' }}</span>
          </template>
          <template #arrivalId="{ row }">
            <span class="code-text">{{ row.arrivalId || '-' }}</span>
          </template>
          <template #confirmTime="{ row }">
            <span class="date-text">{{ formatDate(row.confirmTime) }}</span>
          </template>
          <template #totalAmount="{ row }">
            <span class="quantity-text">{{ row.totalAmount.toFixed(2) }}</span>
          </template>
          <template #status="{ row }">
            <StatusTag
              :status="getConfirmationStatusConfig(row.status).status"
              :label="getConfirmationStatusConfig(row.status).label"
              size="small"
            />
          </template>
          <template #actions="{ row }">
            <div class="action-text">
              <el-button link type="primary" size="small" @click.stop="openConfirmationDetail(row)">详情</el-button>
            </div>
          </template>
        </DataTable>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="confirmationPagination.current"
            v-model:page-size="confirmationPagination.pageSize"
            :total="confirmationPagination.total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="refreshConfirmations"
            @size-change="refreshConfirmations"
          />
        </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <ReceiptConfirmationFormDialog
      v-model="formDialogVisible"
      :arrival="confirmingArrival"
      :receiver-type="receiverType"
      @success="handleConfirmationSuccess"
    />

    <ArrivalDetailDialog v-if="detailMode === 'arrival'" v-model="detailDialogVisible" :data="detailArrival" />
    <ReceiptConfirmationDetailDialog
      v-if="detailMode === 'confirmation'"
      v-model="detailDialogVisible"
      :data="detailConfirmation"
    />
  </div>
</template>

<style scoped lang="scss">
.code-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.secondary-text {
  color: var(--fts-text-secondary);
}

.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);

  &--overdue {
    color: var(--fts-error);
    font-weight: var(--fts-font-weight-semibold);
  }
}

.quantity-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}
</style>
