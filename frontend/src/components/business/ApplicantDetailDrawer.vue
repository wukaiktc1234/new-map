<template>
  <el-drawer
    :model-value="visible"
    :title="drawerTitle"
    direction="rtl"
    size="500px"
    class="applicant-detail-drawer"
    :before-close="handleClose"
    @open="handleOpen"
  >
    <!-- 自定义头部 -->
    <template #header>
      <div class="drawer-custom-header">
        <!-- 标题栏 -->
        <div class="header-title-bar">
          <el-icon class="header-type-icon"><User /></el-icon>
          <span class="header-title-text">求职者档案</span>
        </div>

        <!-- 紧凑信息区：头像+核心信息+进度 -->
        <div class="header-compact">
          <div class="avatar-circle" :style="{ background: avatarColor }">
            {{ avatarLetter }}
          </div>
          <div class="compact-info">
            <div class="compact-row">
              <span class="compact-name">{{ applicant?.name || '未知' }}</span>
              <StatusTag v-if="applicant?.status" :status="applicant.status" size="small" />
              <span v-if="positionName" class="position-tag">{{ positionName }}</span>
            </div>
            <div v-if="interviewTimeline && interviewTimeline.totalRounds > 0" class="compact-progress">
              <el-progress
                :percentage="interviewTimeline.overallProgress"
                :stroke-width="6"
                :show-text="false"
                color="var(--fts-success)"
              />
              <span class="progress-text">
                <strong>{{ completedInterviews }}</strong>/{{ interviewTimeline.totalRounds }}轮
                <span v-if="interviewTimeline.currentRound">· 第{{ interviewTimeline.currentRound }}轮</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>
    <!-- 加载状态 -->
    <div v-if="loading" v-loading="loading" class="drawer-loading">
      <div class="loading-placeholder">
        正在加载应聘者信息...
      </div>
    </div>

    <!-- 内容区 -->
    <div v-else class="drawer-content">
      <!-- 区块1：基本信息 -->
      <div class="detail-section">
        <h3 class="section-title">
          <el-icon><User /></el-icon>
          基本信息
        </h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">性别</span>
            <span class="value">{{ formatGender(applicant?.gender) }}</span>
          </div>
          <div class="info-item">
            <span class="label">联系电话</span>
            <span class="value">
              {{ applicant?.phone ? maskPhone(applicant.phone) : '-' }}
              <el-button
                v-if="applicant?.phone"
                link
                type="primary"
                size="small"
                @click="togglePhoneVisibility"
              >
                {{ showFullPhone ? '隐藏' : '显示' }}
              </el-button>
            </span>
          </div>
          <div class="info-item info-item-full">
            <span class="label">应聘日期</span>
            <span class="value">{{ formatDate(applicant?.submitTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 区块2：应聘岗位 -->
      <div class="detail-section">
        <h3 class="section-title">
          <el-icon><Briefcase /></el-icon>
          应聘岗位
        </h3>
        <div class="info-grid">
          <div class="info-item info-item-full">
            <span class="label">目标岗位</span>
            <span class="value value-highlight">{{ positionName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">期望薪资</span>
            <span class="value salary-value">{{ applicant?.expectedSalary || '面议' }}</span>
          </div>
          <div class="info-item">
            <span class="label">到岗时间</span>
            <span class="value">{{ applicant?.availableDate || '随时' }}</span>
          </div>
          <div class="info-item info-item-full" v-if="matchScore !== null">
            <span class="label">岗位匹配度</span>
            <span class="value">
              <el-progress
                :percentage="matchScore"
                :status="getMatchStatus(matchScore)"
                :stroke-width="8"
              />
            </span>
          </div>
        </div>
      </div>

      <!-- 区块3：工作经验 -->
      <div class="detail-section">
        <h3 class="section-title">
          <el-icon><Medal /></el-icon>
          工作经验
        </h3>
        <div class="experience-content">
          <div class="experience-summary">
            <div class="summary-item">
              <span class="summary-label">总工作年限</span>
              <span class="summary-value">{{ formatExperience(applicant?.experience) }}</span>
            </div>
          </div>

          <!-- 自我介绍/详细经验 -->
          <div v-if="applicant?.introduction" class="introduction-box">
            <div
              class="introduction-text"
              :class="{ 'text-collapsed': !showFullIntro }"
            >
              {{ applicant.introduction }}
            </div>
            <el-button
              v-if="isIntroLong"
              link
              type="primary"
              size="small"
              class="expand-btn"
              @click="toggleIntroExpanded"
            >
              {{ showFullIntro ? '收起 ↑' : '展开全部 ↓' }}
            </el-button>
          </div>

          <!-- 空状态 -->
          <el-empty
            v-else
            description="暂无工作经验信息"
            :image-size="60"
          />
        </div>
      </div>

      <!-- 区块4：补充信息 -->
      <div class="detail-section" v-if="hasSupplementaryInfo">
        <h3 class="section-title">
          <el-icon><Document /></el-icon>
          补充信息
        </h3>
        <div class="info-grid">
          <div class="info-item" v-if="applicant?.education">
            <span class="label">学历</span>
            <span class="value">{{ applicant.education }}</span>
          </div>
          <div class="info-item" v-if="applicant?.certificates">
            <span class="label">相关证书</span>
            <span class="value">{{ applicant.certificates }}</span>
          </div>
          <div class="info-item info-item-full" v-if="applicant?.remark">
            <span class="label">备注</span>
            <span class="value remark-text">{{ applicant.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 面试记录时间线 -->
      <div class="detail-section" v-if="interviewTimeline">
        <InterviewHistoryTimeline :timeline="interviewTimeline" />
      </div>

      <!-- 操作时间线（预留） -->
      <div class="detail-section" v-if="operationHistory.length > 0">
        <h3 class="section-title">
          <el-icon><Clock /></el-icon>
          操作记录
        </h3>
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in operationHistory"
            :key="index"
            :timestamp="formatDate(record.time)"
            placement="top"
            :type="getTimelineType(record.action)"
          >
            <div class="timeline-content">
              <strong>{{ record.actionName }}</strong>
              <p v-if="record.detail">{{ record.detail }}</p>
              <span class="operator">操作人：{{ record.operator }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <!-- 底部操作栏（预留，当前仅查看） -->
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleClose">关闭</el-button>
        <slot name="actions" :applicant="applicant" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
/**
 * ApplicantDetailDrawer - 应聘者详情抽屉组件
 *
 * 功能：
 * - 展示应聘者完整信息（4个区块）
 * - 支持手机号脱敏/显示切换
 * - 自我介绍展开/收起
 * - 匹配度进度条展示
 * - 操作记录时间线（预留）
 * - 底部插槽支持自定义操作按钮
 */

import { ref, computed, watch } from 'vue'
import {
  User,
  Briefcase,
  Medal,
  Document,
  Clock,
} from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import InterviewHistoryTimeline from './interview/InterviewHistoryTimeline.vue'
import type { InterviewTimeline } from './interview/types'

// ==================== Props & Emits ====================

interface Props {
  visible: boolean
  applicant: any | null
  positionName?: string
  matchScore?: number | null
  interviewTimeline?: InterviewTimeline | null
  operationHistory?: Array<{
    action: string
    actionName: string
    time: string
    detail?: string
    operator?: string
  }>
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  applicant: null,
  positionName: '',
  matchScore: null,
  interviewTimeline: null,
  operationHistory: () => [],
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
}>()

// ==================== 响应式状态 ====================

const loading = ref(false)
const showFullPhone = ref(false)
const showFullIntro = ref(false)
const INTRO_MAX_LENGTH = 100

// ==================== 计算属性 ====================

const drawerTitle = computed(() => {
  if (!props.applicant) return '应聘者详情'
  return `${props.applicant.name} 的应聘详情`
})

const avatarLetter = computed(() => {
  if (!props.applicant?.name) return '?'
  return props.applicant.name.charAt(0).toUpperCase()
})

const avatarColor = computed(() => {
  const colors = [
    'var(--fts-primary)',
    'var(--fts-success)',
    'var(--fts-warning)',
    'var(--fts-error)'
  ]
  if (!props.applicant?.name) return colors[0]
  const index = props.applicant.name.charCodeAt(0) % colors.length
  return colors[index]
})

const completedInterviews = computed(() => {
  if (!props.interviewTimeline?.records) return 0
  return props.interviewTimeline.records.filter(r => r.result === 'passed').length
})

const isIntroLong = computed(() => {
  if (!props.applicant?.introduction) return false
  return props.applicant.introduction.length > INTRO_MAX_LENGTH
})

const hasSupplementaryInfo = computed(() => {
  const a = props.applicant
  return a?.education || a?.certificates || a?.remark
})

// ==================== 方法 ====================

/** 关闭抽屉 */
function handleClose() {
  emit('update:visible', false)
  emit('close')
  // 重置状态
  showFullPhone.value = false
  showFullIntro.value = false
}

/** 打开时的回调 */
function handleOpen() {
  loading.value = false
  showFullPhone.value = false
  showFullIntro.value = false
}

/** 切换手机号显示/隐藏 */
function togglePhoneVisibility() {
  showFullPhone.value = !showFullPhone.value
}

/** 切换自我介绍展开/收起 */
function toggleIntroExpanded() {
  showFullIntro.value = !showFullIntro.value
}

// ==================== 格式化函数 ====================

/** 手机号脱敏（隐藏中间4位） */
function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/** 性别格式化 */
function formatGender(gender: string): string {
  const map: Record<string, string> = {
    male: '男',
    female: '女',
    other: '其他',
  }
  return map[gender] || gender || '-'
}

/** 工作经验格式化 */
function formatExperience(exp: number): string {
  if (exp === null || exp === undefined) return '-'
  if (exp === 0) return '无经验'
  if (exp <= 1) return '1年以下'
  if (exp <= 3) return '1-3年'
  if (exp <= 5) return '3-5年'
  return `${exp}年以上`
}

/** 日期格式化 */
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}

/** 匹配度进度条状态 */
function getMatchStatus(score: number): '' | 'success' | 'warning' | 'exception' | 'danger' {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  if (score >= 40) return ''
  return 'exception'
}

/** 时间线类型映射 */
function getTimelineType(action: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    apply: 'primary',
    interview: 'warning',
    offer: 'success',
    onboard: 'success',
    reject: 'danger',
  }
  return typeMap[action] || 'info'
}
</script>

<style lang="scss" scoped>
/* var() fallback为防御性编码，确保CSS变量未定义时有默认样式 */
.applicant-detail-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 16px;
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-drawer__body) {
    padding: 0 20px;
    overflow-y: auto;
  }

  :deep(.el-drawer__footer) {
    border-top: 1px solid var(--el-border-color-lighter);
    padding: 12px 20px;
  }
}

// ==================== 自定义头部样式 ====================

.drawer-custom-header {
  width: 100%;
}

.header-title-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

.header-type-icon {
  font-size: 16px;
  color: var(--fts-primary);
}

.header-title-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--fts-text-primary);
}

// 紧凑信息区
.header-compact {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
}

.avatar-circle {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--fts-text-inverse);
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.compact-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.compact-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

.compact-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--fts-text-primary);
  line-height: 1.3;
}

.position-tag {
  font-size: 13px;
  color: var(--fts-text-secondary, #606266);
  background: var(--fts-bg-tertiary, #f5f7fa);
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: 500;
}

// 紧凑进度条
.compact-progress {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  :deep(.el-progress) {
    flex: 1;
    max-width: 120px;
    margin: 0;
  }

  :deep(.el-progress-bar__outer) {
    border-radius: 3px;
  }
}

.progress-text {
  font-size: 13px;
  color: var(--fts-text-secondary, #606266);
  white-space: nowrap;

  strong {
    color: var(--fts-success, #67C23A);
    font-weight: 600;
  }
}

.drawer-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.loading-placeholder {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.drawer-content {
  padding-bottom: 20px;
}

// ==================== 区块样式 ====================

.detail-section {
  margin-bottom: 16px;
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary, #fafafa);
  border-radius: var(--fts-radius-sm);
  border: 1px solid var(--fts-border-secondary, #f0f0f0);

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--fts-border-primary);

  .el-icon {
    color: var(--fts-primary);
    font-size: 16px;
  }
}

// ==================== 信息网格 ====================

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;

  .label {
    font-size: 13px;
    color: var(--el-text-color-secondary, #909399);
    line-height: 1.4;
    font-weight: 500;
  }

  .value {
    font-size: 14px;
    color: var(--el-text-color-primary, #303133);
    line-height: 1.5;
    word-break: break-all;

    &.value-primary {
      font-size: 15px;
      font-weight: 600;
      color: var(--fts-color-primary);
    }

    &.value-highlight {
      font-weight: 600;
      color: var(--el-color-success);
    }

    &.salary-value {
      color: var(--el-color-warning);
      font-weight: 500;
    }

    &.remark-text {
      color: var(--el-text-color-regular);
      background: var(--el-fill-color-lighter);
      padding: 8px 10px;
      border-radius: 4px;
      line-height: 1.6;
    }
  }

  &.info-item-full {
    grid-column: 1 / -1;
  }
}

// ==================== 工作经验 ====================

.experience-content {
  .experience-summary {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    padding: 12px;
    background: var(--el-fill-color-extra-light);
    border-radius: 6px;
  }

  .summary-item {
    display: flex;
    align-items: baseline;
    gap: 8px;

    .summary-label {
      font-size: 14px;
      color: var(--el-text-color-secondary, #606266);
      font-weight: 500;
    }

    .summary-value {
      font-size: 16px;
      font-weight: 600;
      color: var(--fts-color-primary);
    }
  }

  .introduction-box {
    .introduction-text {
      font-size: 14px;
      line-height: 1.7;
      color: var(--el-text-color-regular);
      white-space: pre-wrap;
      word-break: break-word;

      &.text-collapsed {
        max-height: 4.08em; /* 约3行 */
        overflow: hidden;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          right: 0;
          width: 60px;
          height: 1.36em;
          background: linear-gradient(to right, transparent, white);
        }
      }
    }

    .expand-btn {
      margin-top: 8px;
      font-size: 12px;
    }
  }
}

// ==================== 操作时间线 ====================

.timeline-content {
  strong {
    font-size: 14px;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
    line-height: 1.5;
  }

  .operator {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

// ==================== 底部操作栏 ====================

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

// ==================== 响应式适配 ====================

@media screen and (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .experience-summary {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
