<script setup lang="ts">
/**
 * 印章使用记录对话框组件
 *
 * 【层级】L5 - 组件层(Component)
 * 【职责】展示指定印章的使用记录列表
 * 【依赖】L3(DataTable/StatusTag)
 */
import { ref, watch, computed } from 'vue'
import { sealApi } from '@/api/seal'
import type { SealInfo, SealType } from '@/types/seal'
import { SealTypeLabelMap } from '@/types/seal'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'

// ==================== 类型定义 ====================

/** 印章使用记录 */
interface SealUsageRecord {
  /** 记录ID */
  recordId: string
  /** 印章ID */
  sealId: string
  /** 使用人 */
  userName: string
  /** 使用部门 */
  department: string
  /** 使用事由 */
  reason: string
  /** 使用日期 */
  useDate: string
  /** 预计归还日期 */
  expectedReturnDate: string
  /** 实际归还日期 */
  actualReturnDate?: string
  /** 使用文件 */
  documentName: string
  /** 状态：using-使用中 / returned-已归还 */
  status: 'using' | 'returned'
  /** 备注 */
  remark?: string
}

// ==================== Props & Emits ====================

const props = defineProps<{
  /** 对话框显示状态 */
  visible: boolean
  /** 印章ID */
  sealId: string
  /** 印章名称 */
  sealName: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'close'): void
}>()

// ==================== 响应式数据 ====================

const loading = ref(false)
const detailLoading = ref(false)
const sealInfo = ref<SealInfo | null>(null)
const records = ref<SealUsageRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// ==================== 表格列定义 ====================

const columns = computed<DataTableColumn[]>(() => [
  { prop: 'useDate', label: '使用日期', width: 110 },
  { prop: 'userName', label: '使用人', width: 100 },
  { prop: 'department', label: '使用部门', width: 110 },
  { prop: 'reason', label: '使用事由', minWidth: 150, showOverflowTooltip: true },
  { prop: 'documentName', label: '使用文件', minWidth: 140, showOverflowTooltip: true },
  { prop: 'actualReturnDate', label: '归还日期', width: 110, slot: 'returnDate' },
  { prop: 'status', label: '状态', width: 90, slot: 'status', ellipsis: false },
])

// ==================== 方法 ====================

/** 加载印章详情 */
async function loadSealDetail() {
  if (!props.sealId) return
  detailLoading.value = true
  try {
    const detail = await sealApi.getById(props.sealId)
    sealInfo.value = detail
  } catch {
    sealInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

/** 加载使用记录 */
async function loadRecords() {
  if (!props.sealId) return
  loading.value = true
  try {
    // TODO: 后端暂无印章使用记录 API（sealApi.getUsageRecords），待接口提供后替换为真实调用
    // const result = await sealApi.getUsageRecords(props.sealId, { page: currentPage.value, size: pageSize.value })
    // records.value = result.records || []
    // total.value = result.total || 0
    records.value = []
    total.value = 0
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 分页变化 */
function handlePageChange(page: number) {
  currentPage.value = page
  loadRecords()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadRecords()
}

/** 获取印章类型状态颜色 */
function getSealTypeStatus(sealType: SealType): string {
  const map: Record<SealType, string> = {
    official: 'primary',
    finance: 'warning',
    contract: 'success',
    legal: 'info',
    custom: 'info',
  }
  return map[sealType] || 'info'
}

/** 获取使用记录状态颜色 */
function getRecordStatus(status: string): string {
  return status === 'using' ? 'warning' : 'success'
}

/** 获取使用记录状态标签 */
function getRecordStatusLabel(status: string): string {
  return status === 'using' ? '使用中' : '已归还'
}

/** 关闭对话框 */
function handleClose() {
  emit('update:visible', false)
  emit('close')
}

// ==================== 监听器 ====================

/** 监听 visible 变化 */
watch(
  () => props.visible,
  (val) => {
    if (val && props.sealId) {
      currentPage.value = 1
      loadSealDetail()
      loadRecords()
    }
  },
)
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="印章使用记录"
    width="900px"
    destroy-on-close
    append-to-body
    @update:model-value="handleClose"
  >
    <div class="dialog-content">
      <!-- 印章基本信息 -->
      <div class="info-section" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="印章名称">
            {{ sealInfo?.sealName || sealName }}
          </el-descriptions-item>
          <el-descriptions-item label="印章类型">
            <StatusTag
              v-if="sealInfo"
              :status="getSealTypeStatus(sealInfo.sealType)"
              :label="SealTypeLabelMap[sealInfo.sealType]"
              size="small"
              variant="light"
            />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="保管人">
            {{ sealInfo?.keeper || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 使用记录表格 -->
      <div class="table-section">
        <div class="section-title">使用记录</div>
        <DataTable
          :data="records"
          :columns="columns"
          :loading="loading"
          :selectable="false"
          :stripe="true"
          :hover="true"
          :border="false"
        >
          <!-- 归还日期列 -->
          <template #returnDate="{ row }">
            <span class="date-text">{{ row.actualReturnDate || '-' }}</span>
          </template>

          <!-- 状态列 -->
          <template #status="{ row }">
            <StatusTag
              :status="getRecordStatus(row.status)"
              :label="getRecordStatusLabel(row.status)"
              size="small"
              variant="light"
            />
          </template>
        </DataTable>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// 基本信息区域
.info-section {
  :deep(.el-descriptions) {
    --el-descriptions-item-label-bg-color: var(--fts-bg-secondary);
  }

  :deep(.el-descriptions__label) {
    color: var(--fts-text-secondary);
    font-weight: var(--fts-font-weight-medium);
  }

  :deep(.el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
    color: var(--fts-text-primary);
  }
}

// 表格区域
.table-section {
  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

// 日期文字
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fts-space-4);
}
</style>
