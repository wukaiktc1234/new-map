<script setup lang="ts">
/**
 * 导出任务抽屉
 *
 * 功能：
 * - 显示已创建的导出任务列表
 * - 自动轮询未完成任务状态（每 3s）
 * - 已完成任务提供下载按钮
 * - 支持移除单个任务和清空所有任务
 */
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Delete, RefreshRight, Document } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { operationsReportApi } from '@/api/operations/report'
import type { ExportTaskResponse, ExportTaskStatus } from '../types/report'
import logger from '@/utils/logger'

/** 导出任务项（扩展 ExportTaskResponse + 创建元数据 + 轮询状态） */
export interface ExportTaskItem extends ExportTaskResponse {
  /** 任务类型：excel/pdf */
  taskKind: 'excel' | 'pdf'
  /** 报表类型 */
  reportKind: string
  /** 创建时间（ISO 字符串） */
  createdAt: string
  // 以下字段来自 getExportTaskStatus 轮询
  fileName?: string
  fileSize?: number
  rowCount?: number
  errorMessage?: string
  completedTime?: string
}

interface Props {
  /** 抽屉可见性（v-model） */
  modelValue: boolean
  /** 任务列表 */
  tasks: ExportTaskItem[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'update-task', taskId: number, status: ExportTaskStatus): void
  (e: 'remove', taskId: number): void
  (e: 'clear-all'): void
  (e: 'retry', taskId: number): void
}>()

// ========== 状态映射 ==========

const STATUS_MAP: Record<number, { label: string; status: string }> = {
  1: { label: '待处理', status: 'pending' },
  2: { label: '处理中', status: 'processing' },
  3: { label: '已完成', status: 'completed' },
  4: { label: '失败', status: 'error' },
}

function getStatusInfo(status: number) {
  return STATUS_MAP[status] || { label: '未知', status: 'default' }
}

// ========== 报表类型映射 ==========

const REPORT_LABELS: Record<string, string> = {
  daily: '日报',
  weekly: '周报',
  monthly: '月报',
  quarterly: '季报',
  yearly: '年报',
  profit: '利润分析',
}

function getReportLabel(kind: string): string {
  return REPORT_LABELS[kind] || kind
}

// ========== 轮询 ==========

let pollTimer: ReturnType<typeof setInterval> | null = null

/** 未完成任务（待处理/处理中） */
const pendingTasks = computed(() =>
  props.tasks.filter(t => t.status === 1 || t.status === 2)
)

async function pollTaskStatus(): Promise<void> {
  if (pendingTasks.value.length === 0) return
  for (const task of pendingTasks.value) {
    try {
      const status = await operationsReportApi.getExportTaskStatus(task.taskId)
      emit('update-task', task.taskId, status)
    } catch (error: unknown) {
      logger.error('EXPORT_TASK', `轮询任务状态失败: taskId=${task.taskId}`, error instanceof Error ? error : new Error(String(error)))
    }
  }
}

function startPolling(): void {
  if (pollTimer) return
  pollTaskStatus()
  pollTimer = setInterval(pollTaskStatus, 3000)
}

function stopPolling(): void {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 抽屉打开时启动轮询，关闭时停止
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      startPolling()
    } else {
      stopPolling()
    }
  }
)

// 没有未完成任务时停止轮询
watch(
  pendingTasks,
  (tasks) => {
    if (tasks.length === 0) {
      stopPolling()
    } else if (props.modelValue && !pollTimer) {
      startPolling()
    }
  },
  { deep: true }
)

onUnmounted(() => {
  stopPolling()
})

// ========== 下载 ==========

const downloadingIds = ref<Set<number>>(new Set())

async function handleDownload(task: ExportTaskItem): Promise<void> {
  if (downloadingIds.value.has(task.taskId)) return
  downloadingIds.value.add(task.taskId)
  try {
    const blob = await operationsReportApi.downloadExportFile(task.taskId)
    // 触发浏览器下载
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = task.fileName || `导出文件_${task.taskNo}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('文件下载已开始')
  } catch (error: unknown) {
    logger.error('EXPORT_TASK', `下载文件失败: taskId=${task.taskId}`, error instanceof Error ? error : new Error(String(error)))
    if (error instanceof Error && error.message) {
      ElMessage.error(`下载失败：${error.message}`)
    } else {
      ElMessage.error('下载文件失败')
    }
  } finally {
    downloadingIds.value.delete(task.taskId)
  }
}

function isDownloading(taskId: number): boolean {
  return downloadingIds.value.has(taskId)
}

// ========== 移除 / 清空 ==========

async function handleRemove(task: ExportTaskItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认移除任务 ${task.taskNo}？移除后不可恢复。`,
      '移除任务',
      { type: 'warning' }
    )
    emit('remove', task.taskId)
  } catch {
    // 用户取消
  }
}

async function handleClearAll(): Promise<void> {
  if (props.tasks.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确认清空所有 ${props.tasks.length} 条导出任务记录？`,
      '清空任务',
      { type: 'warning' }
    )
    emit('clear-all')
  } catch {
    // 用户取消
  }
}

// ========== 格式化 ==========

function formatFileSize(bytes?: number): string {
  if (!bytes || bytes === 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatTime(iso?: string): string {
  if (!iso) return '-'
  try {
    return new Date(iso).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return iso
  }
}

// ========== 抽屉关闭 ==========

function handleClose(): void {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="导出任务"
    direction="rtl"
    size="520px"
    :with-header="true"
    :lock-scroll="false"
    @update:model-value="handleClose"
  >
    <div class="export-task-drawer">
      <!-- 工具栏 -->
      <div class="toolbar" v-if="tasks.length > 0">
        <span class="toolbar__count">共 {{ tasks.length }} 个任务</span>
        <el-button
          link
          type="danger"
          size="small"
          :icon="Delete"
          @click="handleClearAll"
        >
          清空全部
        </el-button>
      </div>

      <!-- 任务列表 -->
      <div v-if="tasks.length === 0" class="empty-state">
        <el-icon :size="48"><Document /></el-icon>
        <p class="empty-state__text">暂无导出任务</p>
        <p class="empty-state__hint">点击页面上的"导出 Excel"按钮创建任务</p>
      </div>

      <div v-else class="task-list">
        <div
          v-for="task in tasks"
          :key="task.taskId"
          class="task-card"
          :class="`task-card--${getStatusInfo(task.status).status}`"
        >
          <!-- 卡片头部 -->
          <div class="task-card__header">
            <div class="task-card__title">
              <span class="task-card__no">{{ task.taskNo }}</span>
              <StatusTag
                :status="getStatusInfo(task.status).status"
                :label="getStatusInfo(task.status).label"
                size="small"
              />
            </div>
            <el-button
              link
              type="danger"
              size="small"
              :icon="Delete"
              @click="handleRemove(task)"
            />
          </div>

          <!-- 卡片信息 -->
          <div class="task-card__body">
            <div class="task-card__row">
              <span class="task-card__label">类型</span>
              <span class="task-card__value">
                {{ task.taskKind === 'excel' ? 'Excel' : 'PDF' }}
                ·
                {{ getReportLabel(task.reportKind) }}
              </span>
            </div>
            <div class="task-card__row">
              <span class="task-card__label">创建时间</span>
              <span class="task-card__value">{{ formatTime(task.createdAt) }}</span>
            </div>
            <div v-if="task.fileName" class="task-card__row">
              <span class="task-card__label">文件名</span>
              <span class="task-card__value task-card__value--ellipsis" :title="task.fileName">
                {{ task.fileName }}
              </span>
            </div>
            <div v-if="task.fileSize" class="task-card__row">
              <span class="task-card__label">文件大小</span>
              <span class="task-card__value">{{ formatFileSize(task.fileSize) }}</span>
            </div>
            <div v-if="task.rowCount" class="task-card__row">
              <span class="task-card__label">数据行数</span>
              <span class="task-card__value">{{ task.rowCount }} 行</span>
            </div>
            <div v-if="task.completedTime" class="task-card__row">
              <span class="task-card__label">完成时间</span>
              <span class="task-card__value">{{ formatTime(task.completedTime) }}</span>
            </div>
            <div v-if="task.errorMessage" class="task-card__row task-card__row--error">
              <span class="task-card__label">错误信息</span>
              <span class="task-card__value task-card__value--error">{{ task.errorMessage }}</span>
            </div>
          </div>

          <!-- 卡片操作 -->
          <div class="task-card__footer">
            <el-button
              v-if="task.status === 3"
              type="primary"
              size="small"
              :icon="Download"
              :loading="isDownloading(task.taskId)"
              @click="handleDownload(task)"
            >
              下载文件
            </el-button>
            <span v-else-if="task.status === 1 || task.status === 2" class="task-card__pending">
              <el-icon class="task-card__pending-icon"><RefreshRight /></el-icon>
              <span>{{ task.status === 1 ? '等待处理' : '正在生成' }}...</span>
            </span>
            <span v-else-if="task.status === 4" class="task-card__failed">
              导出失败
            </span>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
.export-task-drawer {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-info, color-mix(in srgb, var(--fts-info) 8%, transparent));
  border-radius: var(--fts-radius-md, 8px);

  &__count {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-12) var(--fts-space-4);
  color: var(--fts-text-disabled);
  text-align: center;

  .el-icon {
    margin-bottom: var(--fts-space-3);
    color: var(--fts-border-hover, var(--fts-text-disabled));
  }

  &__text {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-secondary);
    margin: 0 0 var(--fts-space-1) 0;
  }

  &__hint {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-disabled);
    margin: 0;
  }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.task-card {
  padding: var(--fts-space-3);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 8px);
  border-left: 3px solid var(--fts-info);
  transition: border-color var(--fts-duration-fast);

  &--completed {
    border-left-color: var(--fts-success);
  }

  &--error {
    border-left-color: var(--fts-error);
  }

  &--processing,
  &--pending {
    border-left-color: var(--fts-warning);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-2);
  }

  &__title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    min-width: 0;
  }

  &__no {
    font-weight: 600;
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-1);
    margin-bottom: var(--fts-space-2);
  }

  &__row {
    display: flex;
    align-items: flex-start;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-xs);

    &--error {
      align-items: flex-start;
    }
  }

  &__label {
    flex-shrink: 0;
    color: var(--fts-text-disabled);
    min-width: 60px;
  }

  &__value {
    color: var(--fts-text-secondary);
    word-break: break-all;

    &--ellipsis {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &--error {
      color: var(--fts-error);
    }
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding-top: var(--fts-space-2);
    border-top: 1px dashed var(--fts-border-secondary);
  }

  &__pending {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
    color: var(--fts-warning);
    font-size: var(--fts-font-size-xs);
  }

  &__pending-icon {
    animation: task-spin 1.5s linear infinite;
  }

  &__failed {
    color: var(--fts-error);
    font-size: var(--fts-font-size-xs);
  }
}

@keyframes task-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
