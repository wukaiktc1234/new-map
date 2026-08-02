<script setup lang="ts">
/**
 * 学习记录页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】查看员工培训学习记录，支持按员工、课程、完成状态筛选
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, computed, onMounted } from 'vue'
import { Search, Refresh, Reading, CircleCheck, Timer, DataAnalysis } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { trainingApi } from '@/api/hr'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  StudyRecord,
  StudyRecordQueryParams,
} from '@/types/hr/training'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
}

// ==================== 响应式数据 ====================

const queryForm = ref({
  keyword: '',
  completed: '' as '' | 'true' | 'false',
  startDate: '',
  endDate: '',
})

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<StudyRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: StudyRecordQueryParams & { page?: number; size?: number } = {
        page: params.page,
        pageSize: params.size,
      }
      if (params.keyword) queryParams.keyword = params.keyword
      if (params.completed) queryParams.completed = params.completed === 'true'
      if (params.startDate) queryParams.startDate = params.startDate
      if (params.endDate) queryParams.endDate = params.endDate
      return trainingApi.getStudyRecords(queryParams)
    },
  } as unknown as CrudApi<StudyRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = computed(() => {
  const records = tableData.value
  const completed = records.filter(r => r.completed).length
  const totalDuration = records.reduce((sum, r) => sum + (r.durationSeconds || 0), 0)
  const avgScore = records.length > 0
    ? Math.round(records.reduce((sum, r) => sum + (r.score || 0), 0) / records.length)
    : 0
  return {
    total: records.length,
    completed,
    totalDuration: Math.round(totalDuration / 3600 * 10) / 10,
    avgScore,
  }
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'employeeName', label: '员工姓名', minWidth: 100 },
  { prop: 'employeeCode', label: '员工编号', minWidth: 120 },
  { prop: 'departmentName', label: '部门', minWidth: 120 },
  { prop: 'courseTitle', label: '课程名称', minWidth: 180, ellipsis: true },
  { prop: 'articleTitle', label: '关联文章', minWidth: 180, ellipsis: true },
  { prop: 'completed', label: '完成状态', minWidth: 100, slot: 'completed' },
  { prop: 'score', label: '得分', minWidth: 80, slot: 'score' },
  { prop: 'durationSeconds', label: '学习时长', minWidth: 110, slot: 'durationSeconds' },
  { prop: 'startTime', label: '开始时间', minWidth: 160, slot: 'startTime' },
  { prop: 'endTime', label: '完成时间', minWidth: 160, slot: 'endTime' },
])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.completed = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  refresh()
}

function formatDuration(seconds: number): string {
  if (!seconds) return '-'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0) return `${h}小时${m > 0 ? `${m}分` : ''}`
  return `${m}分钟`
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  refresh()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="学习记录" description="查看员工培训课程学习进度与完成情况" />

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Reading" label="学习记录总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Timer" label="累计学习时长(小时)" :value="String(statistics.totalDuration)" color-type="warning" variant="bordered" />
      <StatCard icon="DataAnalysis" label="平均得分" :value="String(statistics.avgScore)" color-type="info" variant="bordered" />
    </section>

    <!-- 搜索面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索员工姓名/课程名称/文章标题"
            clearable
            style="width: 280px"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="queryForm.completed" placeholder="完成状态" clearable style="width: 140px">
            <el-option label="已完成" value="true" />
            <el-option label="学习中" value="false" />
          </el-select>

          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期起"
            value-format="YYYY-MM-DD"
            style="width: 150px"
          />

          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="开始日期止"
            value-format="YYYY-MM-DD"
            style="width: 150px"
          />

          <el-button type="primary" size="default" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button size="default" @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :stripe="true"
        :border="true"
        row-key="id"
      >
        <template #completed="{ row }">
          <StatusTag :status="row.completed ? 'active' : 'pending'" :label="row.completed ? '已完成' : '学习中'" />
        </template>

        <template #score="{ row }">
          <span v-if="row.completed" :class="{ 'score-high': row.score >= 90, 'score-pass': row.score >= 60 && row.score < 90, 'score-fail': row.score < 60 }">
            {{ row.score }}分
          </span>
          <span v-else>-</span>
        </template>

        <template #durationSeconds="{ row }">
          {{ formatDuration(row.durationSeconds) }}
        </template>

        <template #startTime="{ row }">
          {{ formatTime(row.startTime) }}
        </template>

        <template #endTime="{ row }">
          {{ formatTime(row.endTime) }}
        </template>
      </DataTable>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="refresh"
          @current-change="refresh"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.score-high {
  color: var(--fts-success);
  font-weight: 600;
}

.score-pass {
  color: var(--fts-warning);
}

.score-fail {
  color: var(--fts-error);
}
</style>
