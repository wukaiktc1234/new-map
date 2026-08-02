<template>
  <div class="modern-page">
    <PageHeader
      title="套餐管理"
      description="管理菜品套餐组合，支持成本自动计算"
      icon-name="Grid"
    >
      <el-button type="primary" size="default" v-permission="'product:combo:create'" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增套餐
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard icon="Grid" label="套餐总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="启用中" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="已停用" :value="String(statistics.inactive)" color-type="warning" variant="bordered" />
    </section>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-input
            v-model="queryForm.keyword"
            placeholder="请输入套餐名称或编码"
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
          <el-button
            type="success"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchStatus('active')"
          >
            批量启用
          </el-button>
          <el-button
            type="warning"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchStatus('inactive')"
          >
            批量停用
          </el-button>
        </div>
      </div>
    </div>

    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="true"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        @selection-change="handleSelectionChange"
      >
        <template #comboName="{ row }">
          <div class="combo-name">{{ row.comboName }}</div>
        </template>

        <template #description="{ row }">
          <span class="description-text">{{ row.description || '-' }}</span>
        </template>

        <template #comboPrice="{ row }">
          <span class="price-text">{{ row.comboPrice ?? '0.00' }}</span>
        </template>

        <template #totalCost="{ row }">
          <span class="cost-text">¥{{ Number(row.totalCost ?? 0).toFixed(2) }}</span>
        </template>

        <template #profitRate="{ row }">
          <span :class="['profit-rate', getProfitRateClass(row.profitRate)]">
            {{ Number(row.profitRate ?? 0).toFixed(1) }}%
          </span>
        </template>

        <template #comboStatus="{ row }">
          <StatusTag :status="row.comboStatus === 'active' ? 'active' : 'inactive'" :label="row.comboStatus === 'active' ? '启用' : '停用'" size="small" />
        </template>

        <template #default>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="action-text">
                <el-button link type="primary" size="small" @click.stop="handleEdit(row)">编辑</el-button>
                <el-button
                  link
                  :type="row.comboStatus === 'active' ? 'warning' : 'primary'"
                  size="small"
                  @click.stop="handleComboStatusToggle(row)"
                >
                  {{ row.comboStatus === 'active' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" size="small" @click.stop="handleDeleteClick(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </template>
      </DataTable>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>

  <DishComboDialog ref="dialogRef" @submit="handleSubmit" />
</template>

<script setup lang="ts">
defineOptions({ name: 'DishCombo' })

import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import DishComboDialog from './components/DishComboDialog.vue'
import { useLayoutStore } from '@/stores/layout'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { comboApi } from '@/api/product/combo'
import type { DishComboRow, DishComboFormData, ProductStatus } from '@/types/product'

const layoutStore = useLayoutStore()
const dialogRef = ref<InstanceType<typeof DishComboDialog>>()

/** 当前选中的表格行（用于批量操作） */
const selectedRows = ref<DishComboRow[]>([])

const queryForm = ref({ keyword: '', status: undefined as number | undefined })

const { tableData, loading, pagination, refresh, handlePageChange, handleSizeChange, handleDelete } = useCrudTable<DishComboRow, typeof queryForm.value>({
  api: comboApi as unknown as CrudApi<DishComboRow, typeof queryForm.value>, queryForm, autoLoad: true,
})

/** 表格选择变更 */
function handleSelectionChange(rows: DishComboRow[]): void {
  selectedRows.value = rows
}

const columns = [{ prop: 'comboCode', label: '编码', minWidth: 110 },
  { prop: 'comboName', label: '套餐信息', minWidth: 180, slot: 'comboName' },
  { prop: 'description', label: '描述', minWidth: 200, slot: 'description', showOverflowTooltip: true },
  { prop: 'comboPrice', label: '售价(元)', minWidth: 100, slot: 'comboPrice' },
  { prop: 'totalCost', label: '成本(元)', minWidth: 100, slot: 'totalCost' },
  { prop: 'profitRate', label: '利润率', minWidth: 90, slot: 'profitRate' },
  { prop: 'comboStatus', label: '状态', minWidth: 85, slot: 'comboStatus' },
]

const statistics = computed(() => ({
  total: tableData.value.length,
  active: tableData.value.filter(item => item.comboStatus === 'active').length,
  inactive: tableData.value.filter(item => item.comboStatus === 'inactive').length,
}))

function handleSearch(): void { pagination.current = 1; refresh() }
function handleReset(): void { queryForm.value = { keyword: '', status: undefined }; pagination.current = 1; refresh() }
function handleCreate(): void { dialogRef.value?.openCreate() }
function handleEdit(row: DishComboRow): void { dialogRef.value?.openEdit(row as unknown as Record<string, unknown>) }

/** 根据利润率返回样式类名（使用 CSS 变量统一管理颜色） */
function getProfitRateClass(rate: number | undefined): string {
  const value = Number(rate ?? 0)
  if (value >= 30) return 'profit-rate--high'
  if (value >= 0) return 'profit-rate--mid'
  return 'profit-rate--low'
}

async function handleComboStatusToggle(row: DishComboRow): Promise<void> {
  const isActive = row.comboStatus === 'active'
  const text = isActive ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${text}套餐「${row.comboName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newStatus: ProductStatus = isActive ? 'inactive' : 'active'
    // 直接调用 API，由后端处理状态变更；不本地修改 tableData，避免与后端数据不一致
    await comboApi.updateStatus(Number(row.comboId), newStatus)
    ElMessage.success(`${text}成功`)
    // 调用 refresh() 重新拉取数据，保证 UI 与后端数据一致
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/**
 * 批量启用/停用套餐
 * 后端套餐暂未提供批量状态更新接口，循环调用单条 updateStatus
 * @param targetStatus - 目标状态：active 启用 / inactive 停用
 */
async function handleBatchStatus(targetStatus: ProductStatus): Promise<void> {
  if (selectedRows.value.length === 0) return
  const text = targetStatus === 'active' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要批量${text}选中的 ${selectedRows.value.length} 个套餐吗？`,
      '批量操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    // 后端无批量接口，循环调用单条状态更新；任意一条失败则终止并提示
    for (const row of selectedRows.value) {
      await comboApi.updateStatus(Number(row.comboId), targetStatus)
    }
    ElMessage.success(`批量${text}成功`)
    selectedRows.value = []
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : `批量${text}失败`)
    }
  }
}

async function handleDeleteClick(row: DishComboRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除套餐「${row.comboName}」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    // 直接调用 API 并 refresh，避免 useCrudTable.handleDelete 的二次确认弹窗
    await comboApi.delete(Number(row.comboId))
    ElMessage.success('删除成功')
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function handleSubmit(data: DishComboFormData): Promise<void> {
  try {
    if (data.id) { await comboApi.update(data.id, data); ElMessage.success('更新成功') }
    else { await comboApi.create(data); ElMessage.success('创建成功') }
    dialogRef.value?.close()
    refresh()
  } catch (error: unknown) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
}
</script>

<style scoped lang="scss">

.stats-section { grid-template-columns: repeat(3, 1fr); }

.combo-name { font-weight: var(--fts-font-weight-semibold); color: var(--fts-text-primary); }
.description-text { color: var(--fts-text-secondary); }
.price-text { font-weight: var(--fts-font-weight-semibold); color: var(--fts-success); font-variant-numeric: tabular-nums; }
.cost-text { color: var(--fts-text-secondary); font-variant-numeric: tabular-nums; }
.profit-rate { font-variant-numeric: tabular-nums; font-weight: var(--fts-font-weight-medium); }
.profit-rate--high { color: var(--fts-success); }
.profit-rate--mid { color: var(--fts-warning); }
.profit-rate--low { color: var(--fts-danger); }
.action-text { display: flex; gap: var(--fts-space-2); }
</style>