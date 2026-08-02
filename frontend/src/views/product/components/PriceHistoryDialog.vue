<template>
  <el-dialog v-model="visible" title="价格历史记录" width="950px" :close-on-click-modal="false" destroy-on-close>
    <div class="history-header">
      <span>菜品</span>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe size="small" style="width: 100%">
      <el-table-column prop="oldPrice" label="原价格(元)" width="110" align="right">
        <template #default="{ row }">{{ Number(row.oldPrice).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="newPrice" label="新价格(元)" width="110" align="right">
        <template #default="{ row }">
          <span :class="{ 'price-up': Number(row.newPrice) > Number(row.oldPrice), 'price-down': Number(row.newPrice) < Number(row.oldPrice) }">
            {{ Number(row.newPrice).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变化" width="100" align="center">
        <template #default="{ row }">
          <span :class="getChangeClass(row)">
            {{ getChangeText(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="adjustType" label="调价类型" width="95" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.adjustType === 'manual' ? 'active' : 'warning'" :label="row.adjustType === 'manual' ? '手动调价' : '批量调价'" size="small" variant="light" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="调价时间" width="165" align="center" />
      <el-table-column prop="operatorName" label="操作" width="100" align="center" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        :total="total"
        layout="prev, pager, next"
        small
        background
        @current-change="loadData"
      />
    </div>

    <div v-if="!loading && !tableData.length" class="empty-state">
      <el-empty description="暂无价格历史记录" :image-size="80" />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { pricingApi } from '@/api/product/pricing'
import StatusTag from '@/components/core/StatusTag.vue'
import type { PricingRecord } from '@/types/product'

const visible = ref(false)
const loading = ref(false)
const tableData = ref<PricingRecord[]>([])
const dishName = ref('')
const total = ref(0)
const page = ref(1)
let currentProductId = ''

function open(productId: string, name?: string): void {
  currentProductId = productId
  dishName.value = name || ''
  page.value = 1
  tableData.value = []
  visible.value = true
  loadData()
}

function close(): void {
  visible.value = false
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const res = await pricingApi.queryHistory({
      productId: Number(currentProductId),
      page: page.value,
      size: 20,
    })
    tableData.value = res.records || []
    total.value = res.total || 0
    if (!dishName.value && res.records?.length) {
      dishName.value = res.records[0].productName || ''
    }
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function getChangeClass(row: PricingRecord): string {
  const diff = Number(row.newPrice) - Number(row.oldPrice)
  if (diff > 0) return 'text-danger'
  if (diff < 0) return 'text-success'
  return 'text-info'
}

function getChangeText(row: PricingRecord): string {
  const diff = Number(row.newPrice) - Number(row.oldPrice)
  if (Math.abs(diff) < 0.01) return '-'
  const percent = ((diff / Number(row.oldPrice)) * 100).toFixed(1)
  return `${diff > 0 ? '+' : ''}${diff.toFixed(2)} (${percent}%)`
}

defineExpose({ open, close })
</script>

<style scoped lang="scss">
.history-header {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--fts-bg-secondary);
  border-radius: var(--el-border-radius-small);
  font-size: 14px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.price-up {
  color: var(--fts-error);
  font-weight: 500;
}

.price-down {
  color: var(--fts-success);
  font-weight: 500;
}

.text-success {
  color: var(--fts-success);
}

.text-danger {
  color: var(--fts-error);
}

.text-info {
  color: var(--fts-text-secondary);
}
</style>
