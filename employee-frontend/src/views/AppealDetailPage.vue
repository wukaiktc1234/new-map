<script setup lang="ts">
/**
 * AppealDetailPage - 申诉详情页
 *
 * 展示申诉完整信息、附件列表和处理时间轴
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Warning, ChatDotSquare, Document, CircleCheckFilled,
  CircleCloseFilled, InfoFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PageContainer, StatusTag, EmptyState } from '@/components/core'
import { appealApi, type AppealDetail } from '@/api/appeal'
import {
  appealTypeLabelMap,
  appealActionLabelMap,
  convertAppealStatus,
} from '@/api/converters/appeal-converters'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<AppealDetail | null>(null)

/** 加载详情 */
async function fetchDetail() {
  const id = route.params.id as string
  if (!id) return

  loading.value = true
  try {
    detail.value = await appealApi.getById(id)
  } catch {
    // 响应拦截器已处理错误
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 撤回申诉 */
async function handleWithdraw() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('确定要撤回此申诉吗？撤回后可重新提交', '确认撤回', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await appealApi.withdraw(detail.value.appealId)
    ElMessage.success('申诉已撤回')
    router.push('/appeals')
  } catch {
    // 用户取消或API错误
  }
}

/** 再次申诉 */
function handleReappeal() {
  if (!detail.value) return
  router.push(`/appeals/create/${detail.value.type}`)
}

/** 格式化文件大小（字节→可读字符串） */
function formatFileSize(bytes: number): string {
  if (bytes == null || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), units.length - 1)
  return `${(bytes / Math.pow(k, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]}`
}

/** 格式化时间（ISO字符串→友好格式） */
function formatTime(time: string): string {
  if (!time) return ''
  try {
    const d = new Date(time)
    return d.toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    })
  } catch {
    return time
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <PageContainer title="申诉详情" :loading="loading">
    <div v-if="detail" class="appeal-detail-page">
      <!-- 空状态保护 -->
      <EmptyState
        v-if="!detail.appealId"
        title="申诉不存在"
        description="该申诉可能已被删除"
      />

      <template v-else>
        <!-- 头部状态卡片 -->
        <section class="status-card">
          <div class="status-header">
            <span
              class="type-badge"
              :class="detail.type === 'penalty' ? 'type-badge--warning' : 'type-badge--error'"
            >
              {{ appealTypeLabelMap[detail.type] || detail.type }}
            </span>
            <StatusTag
              :status="convertAppealStatus(detail.status)"
              size="medium"
              variant="light"
            />
            <span v-if="detail.anonymousFlag" class="anonymous-badge">匿名</span>
          </div>
          <h2 class="detail-title">{{ detail.title }}</h2>
          <p class="detail-meta">
            编号：{{ detail.appealId?.slice(0, 8).toUpperCase() }}...
            · 提交于 {{ detail.createTime }}
          </p>
        </section>

        <!-- 详细描述 -->
        <section class="info-section">
          <h3 class="section-title">详细描述</h3>
          <p class="desc-text">{{ detail.description }}</p>
        </section>

        <!-- 关联处罚单号 -->
        <section v-if="detail.targetDecisionId" class="info-section">
          <h3 class="section-title">关联处罚单号</h3>
          <p class="desc-text">{{ detail.targetDecisionId }}</p>
        </section>

        <!-- 期望结果 -->
        <section v-if="detail.expectedResult" class="info-section">
          <h3 class="section-title">期望结果</h3>
          <p class="desc-text">{{ detail.expectedResult }}</p>
        </section>

        <!-- 附件列表 -->
        <section v-if="detail.attachments && detail.attachments.length > 0" class="info-section">
          <h3 class="section-title">证据附件 ({{ detail.attachments.length }})</h3>
          <div class="attachment-list">
            <div
              v-for="att in detail.attachments"
              :key="att.attachmentId"
              class="attachment-item"
            >
              <el-icon :size="18" style="color: var(--fts-primary)"><Document /></el-icon>
              <span class="att-name">{{ att.fileName }}</span>
              <span class="att-size">{{ formatFileSize(att.fileSize) }}</span>
            </div>
          </div>
        </section>

        <!-- 处理时间轴 -->
        <section class="timeline-section">
          <h3 class="section-title">处理进度</h3>

          <el-timeline v-if="detail.processLogs && detail.processLogs.length > 0">
            <el-timeline-item
              v-for="log in detail.processLogs"
              :key="log.logId"
              :timestamp="formatTime(log.createTime)"
              placement="top"
            >
              <div class="timeline-content">
                <span class="timeline-action">
                  {{ appealActionLabelMap[log.action] || log.action }}
                </span>
                <p v-if="log.comment" class="timeline-comment">{{ log.comment }}</p>
                <span v-if="log.operatorName" class="timeline-operator">— {{ log.operatorName }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>

          <!-- 无日志时显示初始提交记录 -->
          <el-timeline v-else>
            <el-timeline-item :timestamp="detail.createTime" placement="top">
              <div class="timeline-content">
                <span class="timeline-action">{{ appealActionLabelMap['submit'] || '提交申请' }}</span>
                <span class="timeline-operator">— 您</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>

        <!-- 操作栏 -->
        <section class="action-bar">
          <button
            v-if="detail.status === 'pending' || detail.status === 'processing'"
            class="action-btn action-btn--warning"
            @click="handleWithdraw"
          >
            撤回申诉
          </button>
          <button
            v-if="detail.status === 'resolved' || detail.status === 'closed' || detail.status === 'withdrawn'"
            class="action-btn action-btn--primary"
            @click="handleReappeal"
          >
            再次申诉
          </button>
        </section>
      </template>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.appeal-detail-page {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

/* ===== 状态卡片 ===== */
.status-card {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

.status-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

.type-badge {
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--fts-radius-full);

  &--warning { background: rgba(var(--fts-warning-rgb), 0.1); color: var(--fts-warning); }
  &--error { background: rgba(var(--fts-error-rgb), 0.08); color: var(--fts-error); }
}

.anonymous-badge {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-info);
  background: rgba(var(--fts-info-rgb), 0.08);
  padding: 2px 8px;
  border-radius: var(--fts-radius-full);
  margin-left: auto;
}

.detail-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-1);
}

.detail-meta {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  margin: 0;
}

/* ===== 信息区块 ===== */
.info-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
}

.section-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
}

.desc-text {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

/* ===== 附件列表 ===== */
.attachment-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.att-name {
  flex: 1;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.att-size {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
  flex-shrink: 0;
}

/* ===== 时间轴 ===== */
.timeline-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-action {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-primary);
}

.timeline-comment {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  line-height: 1.4;
  margin: 0;
}

.timeline-operator {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
}

/* ===== 操作栏 ===== */
.action-bar {
  display: flex;
  justify-content: center;
  gap: var(--fts-space-3);
  padding-top: var(--fts-space-4);
}

.action-btn {
  padding: var(--fts-space-2) var(--fts-space-5);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  border: 1px solid;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &--primary {
    color: var(--fts-primary);
    border-color: var(--fts-primary);
    background: transparent;

    &:hover { background: rgba(var(--fts-primary-rgb), 0.04); }
  }

  &--warning {
    color: var(--fts-warning);
    border-color: var(--fts-warning);
    background: transparent;

    &:hover { background: rgba(var(--fts-warning-rgb), 0.04); }
  }

  &:active { transform: scale(0.98); }
}
</style>
