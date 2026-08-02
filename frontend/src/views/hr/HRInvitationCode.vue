<script setup lang="ts">
/**
 * 邀请码管理页面
 * TODO: 待对接 invitationCodeApi（路径 /v1/hr/invitation-codes/*），当前 silentGet 直接调用字符串路径，违反"前端 API 规范"第六条
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
const searchForm = ref({ status: '', keyword: '' })

/* ===== 数据类型定义 ===== */
interface InviteCode {
  code: string
  department: string
  position: string
  generateTime: string
  expireTime: string
  usedBy: string
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

/* ===== Mock 数据已清理（2026-06-30） =====
 * 原为 catch 块降级到本地 mockRecords，违反"前端禁止 mock 数据"规范，已删除
 * 后端 API 不可用时改为标准错误提示
 */

/* ===== 表格数据 ===== */
const tableData = ref<InviteCode[]>([])

/* ===== 统计卡片激活状态（点击筛选） ===== */
const activeStatKey = ref<string>('')

/* ===== 统计卡片数据（基于列表数据计算） ===== */
const statsCards = computed(() => [
  {
    key: 'active',
    icon: 'Key',
    label: '生效中',
    value: tableData.value.filter(r => r.status === 'active').length,
    colorType: 'success' as const,
    filterField: 'status',
    filterValue: 'active'
  },
  {
    key: 'used',
    icon: 'UserFilled',
    label: '已使用',
    value: tableData.value.filter(r => r.status === 'used').length,
    colorType: 'primary' as const,
    filterField: 'status',
    filterValue: 'used'
  },
  {
    key: 'expired',
    icon: 'CircleCloseFilled',
    label: '已过期',
    value: tableData.value.filter(r => r.status === 'expired').length,
    colorType: 'error' as const,
    filterField: 'status',
    filterValue: 'expired'
  },
  {
    key: 'total',
    icon: 'Calendar',
    label: '本月生成',
    value: tableData.value.length,
    colorType: 'info' as const
  }
])

/* ===== 表格列定义 ===== */
const columns = [
  { prop: 'code', label: '邀请码', minWidth: 150 },
  { prop: 'department', label: '分配部门', minWidth: 120 },
  { prop: 'position', label: '分配岗位', minWidth: 120 },
  { prop: 'generateTime', label: '生成时间', minWidth: 140 },
  { prop: 'expireTime', label: '截止时间', minWidth: 140 },
  { prop: 'usedBy', label: '使用者', minWidth: 100 },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status' }
]

/* ===== 数据加载（真实后端优先，错误时显示标准提示） ===== */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.value.status) params.status = searchForm.value.status
    if (searchForm.value.keyword) params.keyword = searchForm.value.keyword
    const res = await silentGet<PageResponse<InviteCode>>('/v1/hr/invitation-codes', params)
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } catch (error: unknown) {
    // 后端 API 不可用时显示标准错误提示（不再降级到 mock 数据）
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载邀请码列表失败')
    } else {
      ElMessage.error('加载邀请码列表失败')
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
  searchForm.value = { status: '', keyword: '' }
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
    <PageHeader title="邀请码管理" description="员工注册邀请码生成与分配" />
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
      <el-select v-model="searchForm.status" placeholder="邀请码状态" clearable style="width:140px" size="default">
        <el-option label="生效中" value="active" /><el-option label="已使用" value="used" /><el-option label="已过期" value="expired" />
      </el-select>
      <el-input v-model="searchForm.keyword" placeholder="搜索邀请码/部门" clearable style="width:190px" size="default" @keyup.enter="handleSearch" />
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
        <template #status="{ row }"><StatusTag :status="row.status === 'active' ? 'success' : row.status === 'used' ? 'primary' : 'error'" :label="row.status === 'active' ? '生效中' : row.status === 'used' ? '已使用' : '已过期'" size="small" /></template>
        <template #actions><el-button link type="primary" size="small">生成邀请码</el-button><el-button link type="danger" size="small">作废</el-button></template>
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
