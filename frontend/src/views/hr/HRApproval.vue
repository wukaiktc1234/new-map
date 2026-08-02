<script setup lang="ts">
/* mock 数据已清理（2026-06-30） */
/**
 * 审批管理页面
 * 对接后端 GET /v1/hr/approvals（后端模块待补充，调用失败显示标准错误）
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { silentGet } from '@/api/request'

/* ===== 分页与加载状态 ===== */
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

/* ===== 搜索表单 ===== */
const searchForm = ref({ status: '', type: '' })

/* ===== 数据类型定义 ===== */
interface ApprovalRecord {
  applicant: string
  type: string
  reason: string
  applyDate: string
  startDate: string
  endDate: string
  status: string
}

/** 分页响应类型 */
interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/* ===== 表格数据（后端 API 缺失，暂为空数组） ===== */
const tableData = ref<ApprovalRecord[]>([])

/* ===== 统计卡片激活状态（点击筛选） ===== */
const activeStatKey = ref<string>('')

/* ===== 统计卡片数据（基于列表数据计算） ===== */
const statsCards = computed(() => [
  {
    key: 'pending',
    icon: 'DocumentAdd',
    label: '待审批',
    value: tableData.value.filter(r => r.status === 'pending').length,
    colorType: 'warning' as const,
    filterField: 'status',
    filterValue: 'pending'
  },
  {
    key: 'approved',
    icon: 'CircleCheck',
    label: '已通过',
    value: tableData.value.filter(r => r.status === 'approved').length,
    colorType: 'success' as const,
    filterField: 'status',
    filterValue: 'approved'
  },
  {
    key: 'rejected',
    icon: 'CircleCloseFilled',
    label: '已驳回',
    value: tableData.value.filter(r => r.status === 'rejected').length,
    colorType: 'error' as const,
    filterField: 'status',
    filterValue: 'rejected'
  },
  {
    key: 'total',
    icon: 'List',
    label: '本月申请',
    value: tableData.value.length,
    colorType: 'primary' as const
  }
])

/* ===== 表格列定义 ===== */
const columns = [
  { prop: 'applicant', label: '申请人', minWidth: 90 },
  { prop: 'type', label: '类型', minWidth: 80 },
  { prop: 'reason', label: '申请事由', minWidth: 140, showOverflowTooltip: true },
  { prop: 'applyDate', label: '申请日期', minWidth: 115 },
  { prop: 'startDate', label: '开始日期', minWidth: 115 },
  { prop: 'endDate', label: '结束日期', minWidth: 115 },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status' }
]

/* ===== 数据加载（真实后端优先，失败显示标准错误） ===== */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.value.status) params.status = searchForm.value.status
    if (searchForm.value.type) params.type = searchForm.value.type
    const res = await silentGet<PageResponse<ApprovalRecord>>('/v1/hr/approvals', params)
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } catch (error: unknown) {
    // 后端 API 不可用时显示标准错误提示（不再降级到 mock 数据）
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载审批列表失败')
    } else {
      ElMessage.error('加载审批列表失败')
    }
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */
async function handleSearch(): Promise<void> {
  currentPage.value = 1
  await loadData()
}

async function handleReset(): Promise<void> {
  searchForm.value = { status: '', type: '' }
  activeStatKey.value = ''
  currentPage.value = 1
  await loadData()
}

/* ===== 统计卡片点击筛选（Toggle取消 + 多卡片互斥） ===== */
async function handleStatClick(
  statKey: string,
  filterField: string,
  filterValue: string | number
): Promise<void> {
  if (activeStatKey.value === statKey) {
    // 点击已激活卡片：取消筛选
    activeStatKey.value = ''
    ;(searchForm.value as Record<string, unknown>)[filterField] = ''
  } else {
    // 点击未激活卡片：设置筛选 + 激活
    activeStatKey.value = statKey
    ;(searchForm.value as Record<string, unknown>)[filterField] = filterValue
  }
  await loadData()
}

/* ===== 分页变更 ===== */
function handleSizeChange(size: number): void {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

function handleCurrentChange(page: number): void {
  currentPage.value = page
  loadData()
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="合规审批" description="请假/加班/外出等HR审批流程" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        :class="[
          stat.filterValue ? 'stat-clickable' : '',
          { 'stat-active': activeStatKey === stat.key }
        ]"
        variant="bordered"
        @click="stat.filterValue ? handleStatClick(stat.key, stat.filterField, stat.filterValue) : undefined"
      />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="searchForm.type" placeholder="申请类型" clearable style="width:130px" size="default">
        <el-option label="请假" value="请假" /><el-option label="加班" value="加班" /><el-option label="外出" value="外出" />
      </el-select>
      <el-select v-model="searchForm.status" placeholder="审批状态" clearable style="width:130px" size="default">
        <el-option label="待审批" value="pending" /><el-option label="已通过" value="approved" /><el-option label="已驳回" value="rejected" />
      </el-select>
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
        <template #status="{ row }"><StatusTag :status="row.status === 'approved' ? 'success' : row.status === 'rejected' ? 'error' : 'warning'" :label="row.status === 'approved' ? '已通过' : row.status === 'rejected' ? '已驳回' : '待审批'" size="small" /></template>
        <template #actions><el-button link type="primary" size="small">审批</el-button><el-button link type="primary" size="small">详情</el-button></template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 统计卡片可点击交互样式 */
:deep(.stat-clickable) {
  cursor: pointer;
  transition: transform var(--fts-duration-fast) var(--fts-easing-default),
              box-shadow var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--fts-shadow-md);
  }
}

:deep(.stat-active) {
  outline: 2px solid var(--fts-primary);
  outline-offset: -1px;
}
</style>
