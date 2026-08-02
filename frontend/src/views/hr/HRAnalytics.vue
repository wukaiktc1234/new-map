<script setup lang="ts">
/**
 * 人事分析页面
 * 注意：人事分析为汇总数据，统计卡片不支持点击筛选
 * TODO: 待对接 hrAnalyticsApi（路径 /v1/hr/analytics/*），当前 silentGet 直接调用字符串路径，违反"前端 API 规范"第六条
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { silentGet } from '@/api/request'

/* ===== 分页与加载状态 ===== */
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

/* ===== 搜索表单 ===== */
const searchForm = ref({ department: '', period: '' })

/* ===== 部门选项（接入真实后端 API） ===== */
const { departmentOptions } = useDepartmentOptions(true)

/* ===== 数据类型定义 ===== */
interface DepartmentStats {
  department: string
  total: number
  mcount: number
  fcount: number
  avgSalary: string
  newThisMonth: number
  turnover: number
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
const tableData = ref<DepartmentStats[]>([])

/* ===== 统计卡片数据（基于列表数据汇总计算，不支持点击筛选） ===== */
const statsCards = computed(() => {
  const totalEmployees = tableData.value.reduce((sum, r) => sum + r.total, 0)
  const totalNew = tableData.value.reduce((sum, r) => sum + r.newThisMonth, 0)
  const totalTurnover = tableData.value.reduce((sum, r) => sum + r.turnover, 0)
  // 人均薪资：基于部门人数加权平均，无法精确计算时显示 '-'
  let avgSalary: string | number = '-'
  if (tableData.value.length > 0 && totalEmployees > 0) {
    // 解析薪资字符串中的数字进行加权平均
    let totalSalary = 0
    let validCount = 0
    for (const r of tableData.value) {
      const num = parseFloat(r.avgSalary.replace(/[^0-9.]/g, ''))
      if (!isNaN(num) && r.total > 0) {
        totalSalary += num * r.total
        validCount += r.total
      }
    }
    if (validCount > 0) {
      avgSalary = `¥${Math.round(totalSalary / validCount).toLocaleString()}`
    }
  }
  return [
    { key: 'total', icon: 'User', label: '在职员工', value: totalEmployees, colorType: 'primary' as const },
    { key: 'newThisMonth', icon: 'TrendCharts', label: '本月新增', value: totalNew, colorType: 'success' as const },
    { key: 'turnover', icon: 'Switch', label: '本月离职', value: totalTurnover, colorType: 'error' as const },
    { key: 'avgSalary', icon: 'BankCard', label: '人均薪资', value: avgSalary, colorType: 'info' as const }
  ]
})

/* ===== 表格列定义 ===== */
const columns: DataTableColumn[] = [
  { prop: 'department', label: '部门', minWidth: 100 },
  { prop: 'total', label: '总人数', minWidth: 80, align: 'center' },
  { prop: 'mcount', label: '男', minWidth: 60, align: 'center' },
  { prop: 'fcount', label: '女', minWidth: 60, align: 'center' },
  { prop: 'avgSalary', label: '人均薪资', minWidth: 100, align: 'right' },
  { prop: 'newThisMonth', label: '本月新增', minWidth: 85, align: 'center' },
  { prop: 'turnover', label: '本月离职', minWidth: 85, align: 'center' }
]

/* ===== 数据加载（真实后端优先，错误时显示标准提示） ===== */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.value.department) params.department = searchForm.value.department
    if (searchForm.value.period) params.period = searchForm.value.period
    const res = await silentGet<PageResponse<DepartmentStats>>('/v1/hr/analytics/departments', params)
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } catch (error: unknown) {
    // 后端 API 不可用时显示标准错误提示（不再降级到 mock 数据）
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载人事分析数据失败')
    } else {
      ElMessage.error('加载人事分析数据失败')
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
  searchForm.value = { department: '', period: '' }
  currentPage.value = 1
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
    <PageHeader title="人事分析" description="人力结构分析与关键指标" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="searchForm.department" placeholder="选择部门" clearable style="width:140px" size="default">
        <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.label" :value="dept.id" />
      </el-select>
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
        <template #actions><el-button link type="primary" size="small">详情</el-button></template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
  </div>
</template>
