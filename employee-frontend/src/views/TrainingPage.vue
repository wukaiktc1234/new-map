<script lang="ts">
// ── 模块级变量（非setup，跨组件实例持久化） ──
/** TrainingPage 导航前保存的滚动位置，从知识库返回时恢复 */
let _savedScrollY = 0
</script>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Reading, Trophy, Link, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer, StatusTag, FilterTabs, StatCard, EmptyState } from '@/components/core'
import { trainingApi } from '@/api/training'
import type { TrainingCourse } from '@/api/training'
import { getStudySummary } from '@/composables/useKnowledgeReading'

type CourseStatus = 'not_started' | 'in_progress' | 'completed'
type CourseType = 'required' | 'elective'
type CourseTab = 'all' | 'required' | 'elective'

interface CourseItem extends TrainingCourse {
  id: string
  /** 完成时间（ISO字符串，用于计算证书到期） */
  completedAt?: string
  /** 上次学习时间（ISO字符串，从localStorage读取） */
  lastStudyTime?: string
  /** 累计学习时长（分钟） */
  totalStudyMinutes?: number
  /** 学习次数 */
  studyCount?: number
  /** 最近一次测验得分 */
  lastScore?: number | null
}

const router = useRouter()
const courses = ref<CourseItem[]>([])

async function fetchCourses() {
  try {
    const data = await trainingApi.getCourses()
    courses.value = data.map(c => {
      const item: CourseItem = { ...c, id: c.id }
      // 从localStorage加载学习时间记录
      if (c.relatedArticleId) {
        const summary = getStudySummary(c.relatedArticleId)
        if (summary) {
          item.lastStudyTime = summary.lastStudyTime ?? undefined
          item.totalStudyMinutes = summary.totalStudyMinutes
          item.studyCount = summary.studyCount
          item.lastScore = summary.lastScore ?? undefined
        }
      }
      return item
    })
  } catch {
    courses.value = []
  }
}

/** 格式化上次学习时间为友好显示 */
function formatLastStudyTime(isoString: string): string {
  const date = new Date(isoString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}周前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// ========== Tab分类 ==========
const activeTab = ref<CourseTab>('all')

const courseTabs: { key: CourseTab; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'required', label: '必修' },
  { key: 'elective', label: '选修' },
]

/** FilterTabs 选项（从 courseTabs 映射） */
const categoryOptions = courseTabs.map(tab => ({ value: tab.key, label: tab.label }))

const filteredCourses = computed(() => {
  if (activeTab.value === 'all') return courses.value
  return courses.value.filter(c => c.type === activeTab.value)
})

// ========== 学习统计 ==========
const totalCourses = computed(() => courses.value.length)
const completedCount = computed(() => courses.value.filter(c => c.status === 'completed').length)
const inProgressCount = computed(() => courses.value.filter(c => c.status === 'in_progress').length)

/** 本月需完成的必修课程数（未完成 + 有截止日期在本月或之前） */
const monthlyDueCount = computed(() => {
  const now = new Date()
  const currentMonth = (now.getMonth() + 1).toString().padStart(2, '0')
  return courses.value.filter(c =>
    c.type === 'required' &&
    c.status !== 'completed' &&
    c.deadline &&
    c.deadline <= `${currentMonth}-31`,
  ).length
})

/** 已获得证书的课程数 */
const certificateCount = computed(() =>
  courses.value.filter(c => c.status === 'completed' && c.certificateEligible).length,
)

// ========== 课程操作（联动知识库） ==========

/**
 * 开始/继续学习（测试模式）：跳转到知识库文章进行阅读+测试
 */
function handleStartLearning(course: CourseItem) {
  if (course.relatedArticleId) {
    // 保存当前滚动位置（模块级变量，跨导航持久化）
    _savedScrollY = window.scrollY
    router.push({
      path: '/knowledge',
      query: {
        articleId: course.relatedArticleId,
        mode: 'learning',
        courseId: course.id,
      },
    })
  } else {
    ElMessage.info('该课程的学习材料正在准备中，请稍后再试')
  }
}

/** 复习模式：已完成课程可随时复习巩固，无测验压力 */
function handleReviewLearning(course: CourseItem) {
  if (course.relatedArticleId) {
    _savedScrollY = window.scrollY
    router.push({
      path: '/knowledge',
      query: {
        articleId: course.relatedArticleId,
        mode: 'review',
        courseId: course.id,
      },
    })
  } else {
    ElMessage.info('该课程的复习材料正在准备中')
  }
}

/** 完成学习回调：由知识库页面通过路由状态或事件总线通知更新进度 */
function handleCourseAction(courseId: string, status: CourseStatus) {
  const course = courses.value.find(c => c.id === courseId)
  if (!course) return

  if (status === 'not_started') {
    handleStartLearning(course)
  } else if (status === 'in_progress') {
    handleStartLearning(course)
  }
}

function getButtonConfig(course: CourseItem) {
  if (course.status === 'completed') {
    return { type: 'success' as const, label: '已完成', disabled: true }
  }
  if (course.status === 'in_progress') {
    return { type: 'primary' as const, label: '继续学习', disabled: false }
  }
  return { type: 'primary' as const, label: '开始学习', disabled: false }
}

/** 证书有效期：90天（防遗忘机制） */
const CERTIFICATE_VALIDITY_DAYS = 90

/**
 * 根据完成时间计算证书到期信息
 * - 距到期>30天：显示"有效期至 YYYY-MM-DD"
 * - 距到期≤30天：显示"即将到期（还剩N天）"
 * - 已过期：显示"已过期，需重新认证"
 */
function formatExpiry(completedAt: string): string {
  const completed = new Date(completedAt)
  const expiry = new Date(completed.getTime() + CERTIFICATE_VALIDITY_DAYS * 24 * 60 * 60 * 1000)
  const now = new Date()
  const daysLeft = Math.ceil((expiry.getTime() - now.getTime()) / (24 * 60 * 60 * 1000))

  if (daysLeft < 0) return '已过期，需重新认证'
  if (daysLeft <= 30) return `即将到期（还剩${daysLeft}天）`
  return `有效期至 ${expiry.toISOString().slice(0, 10)}`
}

/** 保存滚动位置，从知识库返回时恢复（模块级变量） */
// 注意：使用 let _savedScrollY 而非 ref，因为 ref 在组件销毁时会重置

onMounted(() => {
  fetchCourses()
  // 从其他页面返回时恢复滚动位置
  if (_savedScrollY > 0) {
    nextTick(() => {
      window.scrollTo({ top: _savedScrollY, behavior: 'instant' })
      _savedScrollY = 0
    })
  }
})
</script>

<template>
  <PageContainer title="培训记录">
    <section class="page-section">
      <!-- 学习统计卡片 -->
      <div class="course-stats">
        <StatCard :value="totalCourses" label="总课程" variant="grid" compact />
        <StatCard :value="completedCount" label="已完成" variant="success" compact />
        <StatCard :value="inProgressCount" label="进行中" variant="warning" compact />
        <StatCard :value="monthlyDueCount" label="本月需完成" variant="error" compact />
      </div>

      <!-- Tab分类切换 -->
      <div class="course-filter-tabs">
        <FilterTabs v-model="activeTab" :options="categoryOptions" variant="pill" size="medium" />
      </div>

      <!-- 课程列表 -->
      <div v-if="filteredCourses.length > 0" class="course-list">
        <div
          v-for="course in filteredCourses"
          :key="course.id"
          class="course-card"
        >
          <div class="card-left">
            <div class="cover" :class="[`cover--${course.status}`]">
              <el-icon :size="24"><Reading /></el-icon>
            </div>
          </div>
          <div class="card-center">
            <div class="course-title-row">
              <h4 class="course-title">{{ course.title }}</h4>
              <span :class="['course-type-badge', `course-type-badge--${course.type}`]">
                {{ course.type === 'required' ? '必修' : '选修' }}
              </span>
            </div>
            <!-- 讲师 + 截止日期 / 证书到期 / 学习时间 -->
            <div class="course-meta-row">
              <span v-if="course.instructor" class="course-instructor">
                {{ course.instructor }}
              </span>
              <span v-if="course.deadline && course.status !== 'completed'" class="course-deadline">
                截止 {{ course.deadline }}
              </span>
              <span v-if="course.certificateEligible && course.status === 'completed'" class="course-cert">
                <el-icon :size="12"><Trophy /></el-icon>
                可获证书
              </span>
              <span v-if="course.status === 'completed' && course.completedAt" class="course-expire-info">
                {{ formatExpiry(course.completedAt) }}
              </span>
              <span v-if="course.lastStudyTime && course.status !== 'not_started'" class="course-study-time">
                <el-icon :size="11"><Clock /></el-icon>
                <template v-if="course.status === 'completed' && course.totalStudyMinutes">
                  累计{{ course.totalStudyMinutes }}分钟 · 学{{ course.studyCount }}次
                </template>
                <template v-else>
                  上次{{ formatLastStudyTime(course.lastStudyTime) }}
                </template>
              </span>
            </div>
            <p class="course-meta">{{ course.chapter }} · {{ course.duration }}</p>
            <!-- 课程描述（单行预览） -->
            <p v-if="course.description" class="course-desc">{{ course.description }}</p>
            <!-- 关联知识库提示 -->
            <div v-if="course.relatedArticleId" class="course-kb-link">
              <el-icon :size="12"><Link /></el-icon>
              <span>关联知识库文章</span>
            </div>
            <div class="progress-wrapper">
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :class="[`progress-fill--${course.status}`]"
                  :style="{ width: `${course.progress}%` }"
                ></div>
              </div>
              <span class="progress-text">{{ course.progress }}%</span>
            </div>
          </div>
          <div class="card-right">
            <!-- 已完成：双按钮（复习 + 查看证书） -->
            <template v-if="course.status === 'completed'">
              <el-button
                type="warning"
                size="small"
                round
                plain
                @click="handleReviewLearning(course)"
              >
                复习
              </el-button>
            </template>
            <!-- 进行中 / 未开始 -->
            <template v-else>
              <el-button
                :type="getButtonConfig(course).type"
                :disabled="getButtonConfig(course).disabled"
                size="small"
                round
                @click="handleCourseAction(course.id, course.status)"
              >
                {{ getButtonConfig(course).label }}
              </el-button>
            </template>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else
        title="暂无课程"
        description="该分类下暂无培训课程"
        :actionText="activeTab !== 'all' ? '查看全部课程' : ''"
        @action="activeTab = 'all'"
      />
    </section>

    <div class="footer-note">
      <p v-if="certificateCount > 0">
        已完成 {{ certificateCount }} 门可认证课程，完成全部必修课可获得「食品安全培训合格」证书
      </p>
      <p v-else>完成全部培训课程可获得「食品安全培训合格」证书</p>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.training-page {
  padding-bottom: 0;
}

.page-section {
  margin-bottom: var(--fts-space-section-gap, 20px);

  &:last-child {
    margin-bottom: 0;
  }
}

// ========== 学习统计卡片 ==========
.course-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
}

// ========== Tab分类切换 ==========
.course-filter-tabs {
  margin-bottom: var(--fts-space-4);
}

// ========== 课程列表 ==========
.course-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.course-card {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: 12px 16px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  transition: border-color var(--fts-duration-fast) var(--fts-easing-default),
              box-shadow var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
  }
}

.card-left {
  flex-shrink: 0;
}

.cover {
  width: 42px; height: 42px; border-radius: var(--fts-radius-md);
  display: flex; align-items: center; justify-content: center;
  transition: all var(--fts-duration-normal) var(--fts-easing-smooth);

  &--completed { background: rgba(var(--fts-success-rgb), 0.1); color: var(--fts-success); }
  &--in_progress { background: rgba(var(--fts-warning-rgb), 0.1); color: var(--fts-warning); }
  &--not_started { background: var(--fts-bg-tertiary); color: var(--fts-text-tertiary); }
}

.card-center {
  flex: 1;
  min-width: 0;
}

.course-title-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-1);
}

.course-title {
  font-size: var(--fts-font-size-base); font-weight: 600;
  color: var(--fts-text-primary); margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-type-badge {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: var(--fts-radius-full);
  line-height: 1.5;

  &--required {
    background: rgba(var(--fts-danger-rgb), 0.1);
    color: var(--fts-danger);
  }

  &--elective {
    background: rgba(var(--fts-primary-rgb), 0.1);
    color: var(--fts-primary);
  }
}

.course-meta {
  font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary);
  margin: 0 0 var(--fts-space-2);
}

// ========== 讲师 / 截止日期 / 证书 行 ==========
.course-meta-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-1);
}

.course-instructor {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
}

.course-deadline {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-error);
  font-weight: 500;
}

.course-cert {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-warning-dark, #b45309);
  font-weight: 600;
}

.course-expire-info {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  font-weight: 500;
}

.course-study-time {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);

  .el-icon {
    flex-shrink: 0;
  }
}

// ========== 课程描述 ==========
.course-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin: 0 0 var(--fts-space-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

// ========== 关联知识库提示 ==========
.course-kb-link {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-primary);
  margin-bottom: var(--fts-space-2);
  cursor: default;

  .el-icon {
    flex-shrink: 0;
  }
}

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--fts-radius-full);
  transition: width 0.4s ease;

  &--completed { background: var(--fts-gradient-success); }
  &--in_progress { background: var(--fts-warning); }
  &--not_started { background: var(--fts-border-primary); }
}

.progress-text {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: 500;
  min-width: 28px;
  text-align: right;
}

.card-right {
  flex-shrink: 0;
}

.footer-note {
  margin-top: var(--fts-space-2, 8px);
  padding: 8px 14px;
  text-align: center;

  p {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin: 0;
  }
}
</style>
