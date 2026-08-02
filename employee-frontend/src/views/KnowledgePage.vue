<template>
  <PageContainer :title="isLearningMode ? (isReviewMode ? '复习课程' : '课程学习') : '知识库'" :sticky-footer="false">
    <!-- 搜索栏 + 分类Tab + 文章列表（仅非学习模式显示） -->
    <template v-if="!isLearningMode">
      <!-- 搜索栏 -->
      <section class="page-section">
        <div class="kb-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章标题或内容..."
            clearable
            :prefix-icon="Search"
            size="large"
            @input="handleSearch"
            @clear="handleSearch"
          />
        </div>

        <!-- 分类筛选 Tab -->
        <FilterTabs v-model="activeCategory" :options="categoryOptions" variant="pill" />
      </section>
    </template>

    <!-- 文章列表（仅非学习模式显示） -->
    <section v-if="!isLearningMode" class="page-section">
      <div v-loading="loading" class="kb-list">
        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && filteredArticles.length === 0"
          title="暂无相关文章"
          description="试试其他关键词或分类"
        />

      <!-- 文章卡片列表 -->
      <article
        v-for="article in filteredArticles"
        :key="article.id"
        class="kb-article"
        :class="{ 'kb-article--active': selectedArticle?.id === article.id }"
        @click="handleArticleClick(article)"
      >
        <div class="kb-article__header">
          <StatusTag :status="categoryStatusMap[article.category]?.status" :label="categoryStatusMap[article.category]?.label" size="small" variant="light" />
          <span class="kb-article__date">{{ formatDate(article.publishTime ?? '') }}</span>
        </div>

        <h3 class="kb-article__title">{{ article.title }}</h3>
        <p class="kb-article__summary">{{ article.summary }}</p>

        <div class="kb-article__footer">
          <span class="kb-article__meta">
            <el-icon :size="12"><View /></el-icon>
            {{ article.viewCount }}
          </span>
          <span class="kb-article__author">{{ article.author }}</span>

          <!-- 收藏按钮 -->
          <button
            class="kb-favorite-btn"
            :class="{ 'kb-favorite-btn--active': article.isFavorited }"
            @click.stop="handleToggleFavorite(article.id)"
          >
            <el-icon :size="14">
              <component :is="article.isFavorited ? StarFilled : Star" />
            </el-icon>
          </button>
        </div>
      </article>
    </div>
    </section>

    <!-- 文章详情面板（抽屉式） -->
    <KnowledgeDetail
      :article="selectedArticle"
      :is-learning-mode="isLearningMode"
      :is-review-mode="isReviewMode"
      :learning-phase="learningPhase"
      :current-article="currentArticle"
      :read-progress="readProgress"
      :has-met-min-time="hasMetMinTime"
      :formatted-read-time="formattedReadTime"
      :formatted-required-time="formattedRequiredTime"
      :is-passed="isPassed"
      :learning-result="learningResult"
      :fail-reason="failReason"
      :rendered-content="renderedContent"
      :estimated-read-time="estimatedReadTime"
      :category-status-map="categoryStatusMap"
      :current-popup-question="currentPopupQuestion"
      :popup-answer="popupAnswer"
      :popup-submitted="popupSubmitted"
      :popup-is-correct="popupIsCorrect"
      :is-current-required="isCurrentRequired"
      :answered-count="answeredCount"
      :total-quiz-count="totalQuizCount"
      :pass-threshold="PASS_THRESHOLD"
      @close="closeDetail"
      @toggle-favorite="handleToggleFavorite"
      @begin-learning="beginLearning"
      @abort-confirm="handleAbortWithConfirm"
      @complete-learning="handleCompleteLearning"
      @retry-learning="retryLearning"
      @return-training="handleReturnToTraining"
      @popup-answer="handlePopupAnswer"
      @popup-submit-next="handlePopupSubmitAndNext"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, View, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PageContainer, StatusTag, EmptyState, FilterTabs } from '@/components/core'
import KnowledgeDetail from './knowledge/components/KnowledgeDetail.vue'
import { knowledgeApi } from '@/api/knowledge'
import { trainingApi } from '@/api/training'
import { useKnowledgeReading } from '@/composables/useKnowledgeReading'
import type { KnowledgeArticle, KnowledgeCategory } from '@/types/knowledge'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()

// ==================== 学习模式检测 ====================
/** 是否处于培训联动学习模式（测试） */
const isLearningMode = computed(() => {
  const mode = route.query.mode as string
  return mode === 'learning' || mode === 'review'
})
/** 是否为复习模式 */
const isReviewMode = computed(() => route.query.mode === 'review')
/** 来源课程ID */
const sourceCourseId = computed(() => route.query.courseId as string | undefined)

// ==================== 阅读学习 Composable ====================
const {
  // 阶段与结果
  learningPhase,
  learningResult,
  isPassed,
  failReason,
  scorePercent,
  // 必会题统计
  isCurrentRequired,
  // 状态
  currentArticle,
  popupAnswer,
  popupSubmitted,
  popupIsCorrect,
  // 计算属性
  hasMetMinTime,
  readProgress,
  formattedReadTime,
  formattedRequiredTime,
  // 弹题相关
  currentPopupQuestion,
  answeredCount,
  totalQuizCount,
  // 生命周期方法（核心）
  initLearning,
  beginLearning,
  abortLearning,
  retryLearning,
  confirmComplete,
  // 弹题交互
  handlePopupAnswer,
  handlePopupSubmitAndNext,
  // 兼容旧接口
  stopReading,
} = useKnowledgeReading()

/** 通过阈值常量（用于模板显示） */
const PASS_THRESHOLD = 0.75

// ==================== 数据 ====================
const loading = ref(false)
const articles = ref<KnowledgeArticle[]>([])
const searchKeyword = ref('')
const activeCategory = ref<KnowledgeCategory | 'all'>('all')
const selectedArticle = ref<KnowledgeArticle | null>(null)

// 分类选项
const categories = knowledgeApi.getCategories()

/** FilterTabs 选项（从 categories 映射） */
const categoryOptions = categories.map(cat => ({ value: cat.key, label: cat.label }))

/** 分类 → StatusTag status + label 映射 */
const categoryStatusMap: Record<KnowledgeCategory | string, { status: string; label: string }> = {
  safety: { status: 'error', label: '安全' },
  service: { status: 'success', label: '服务' },
  manual: { status: 'info', label: '操作' },
  policy: { status: 'processing', label: '制度' },
}

// ==================== 计算属性 ====================
const filteredArticles = computed(() => {
  let result = articles.value

  // 分类筛选
  if (activeCategory.value !== 'all') {
    result = result.filter(a => a.category === activeCategory.value)
  }

  // 关键词搜索
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim()
    result = result.filter(
      a =>
        a.title.toLowerCase().includes(kw) ||
        a.summary.toLowerCase().includes(kw),
    )
  }

  return result
})

/** 阅读时长估算（按中文字符数：约300字/分钟） */
const estimatedReadTime = computed(() => {
  if (!selectedArticle.value) return '0'
  const textLen = selectedArticle.value.content.length
  const minutes = Math.max(1, Math.ceil(textLen / 300))
  return `${minutes} 分钟`
})

// ==================== Markdown 渲染配置 ====================

/** 配置 marked 解析器（启用 GFM 表格 + 安全选项） */
marked.setOptions({
  gfm: true,
  breaks: false,
})

/** 渲染 Markdown 内容为 HTML（使用 marked 库 + DOMPurify 净化，完整支持 GFM 并防御 XSS） */
const renderedContent = computed(() => {
  if (!selectedArticle.value) return ''
  const rawHtml = marked.parse(selectedArticle.value.content) as string
  return DOMPurify.sanitize(rawHtml)
})

// ==================== 方法 ====================

async function fetchArticles() {
  loading.value = true
  try {
    const res = await knowledgeApi.getList()
    articles.value = res.records

    // 学习模式：立即打开指定文章（无需延迟）
    if (isLearningMode.value && route.query.articleId) {
      const targetArticle = articles.value.find(
        a => a.id === route.query.articleId,
      )
      if (targetArticle) {
        nextTick(() => {
          handleArticleClick(targetArticle!)
        })
      }
    }
  } catch {
    articles.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  // 响应式计算，无需额外处理（filteredArticles 自动更新）
}

function handleArticleClick(article: KnowledgeArticle) {
  selectedArticle.value = article
  document.body.style.overflow = 'hidden'

  // 学习/复习模式：初始化（准备题池，但不自动开始计时）
  if (isLearningMode.value) {
    const mode = isReviewMode.value ? 'review' : 'test'
    initLearning(article, mode)
  }
}

/** 带确认的退出学习（防止误操作） */
function handleAbortWithConfirm(): void {
  ElMessageBox.confirm(
    '确定要退出本次学习吗？已记录的阅读进度和答题将不会保存。',
    '退出学习',
    {
      confirmButtonText: '确定退出',
      cancelButtonText: '继续学习',
      type: 'warning',
    },
  ).then(() => {
    abortLearning()
  }).catch(() => {
    // 用户取消，不做任何操作
  })
}

function closeDetail() {
  stopReading()

  selectedArticle.value = null
  currentArticle.value = null
  document.body.style.overflow = ''

  // 学习模式下关闭详情时返回培训页面
  if (isLearningMode.value) {
    router.push('/training')
  }
}

async function handleToggleFavorite(id: string) {
  try {
    await knowledgeApi.toggleFavorite(id)
    // 更新本地状态
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.isFavorited = !article.isFavorited
    }
    // 如果详情面板打开的是这篇文章，同步更新
    if (selectedArticle.value?.id === id) {
      selectedArticle.value.isFavorited = !selectedArticle.value.isFavorited
    }
  } catch {
    // 静默失败
  }
}

/** 学习模式：确认完成学习 → 标记completed → 更新课程进度并返回培训页 */
async function handleCompleteLearning(): Promise<void> {
  if (!isPassed.value || !sourceCourseId.value || !selectedArticle.value) return

  // 先标记本地状态为已完成
  confirmComplete()

  try {
    await trainingApi.updateProgress(sourceCourseId.value, 100)
    ElMessage.success(`恭喜！「${selectedArticle.value.title}」学习通过（正确率 ${scorePercent.value}%）`)
    router.push('/training')
  } catch {
    ElMessage.error('更新学习进度失败，请重试')
  }
}

/** 结果页"稍后再说" → 返回培训页面 */
function handleReturnToTraining(): void {
  router.push('/training')
}

function formatDate(isoStr: string): string {
  const d = new Date(isoStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}-${day}`
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchArticles()
})

// 监听路由变化（从学习模式返回时重新初始化）
watch(() => route.query, () => {
  if (!isLearningMode.value && !route.query.articleId) {
    stopReading()
    selectedArticle.value = null
    currentArticle.value = null
    document.body.style.overflow = ''
  }
})
</script>

<style scoped lang="scss">
/* ── 紧凑页面区块（列表页专用，最小化垂直间距）
 * 注意：page-body__content 已有 gap 控制，此处不再叠加 margin-bottom ── */
.page-section {
  margin-bottom: 0;
  /* 深色模式下继承页面底色，防止白色闪现 */
  background-color: transparent;

  &:last-child {
    margin-bottom: 0;
  }
}

/* 确保文章列表容器不产生白色背景 */
.kb-list {
  background-color: transparent;
}

/* 覆盖 PageContainer page-body__content 的默认 gap(space-5≈20px)，
 * 列表页改用紧凑间距 */
:deep(.page-body__content) {
  gap: var(--fts-space-3, 12px);
}

/* ── 搜索栏（复用全局 _element-overrides.scss 已覆盖EP样式） ── */
.kb-search {
  margin-bottom: var(--fts-space-3);
}

/* ── 分类筛选 Tab 间距 ── */
:deep(.ft) {
  margin-bottom: var(--fts-space-4);
}

/* ── 文章列表 ── */
.kb-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  min-height: 200px;
}

/* ── 文章卡片（对齐 TrainingPage .course-card 模式） ── */
.kb-article {
  padding: 12px 16px;
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: border-color var(--fts-duration-fast) var(--fts-easing-default),
              box-shadow var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
  }

  &:active {
    transform: scale(0.99);
  }

  &--active {
    border-color: var(--fts-primary);
  }
}

.kb-article__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fts-space-2, 8px);
}

.kb-article__date {
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-quaternary);
}

.kb-article__title {
  margin: 0 0 var(--fts-space-1, 4px);
  font-size: 15px;
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kb-article__summary {
  margin: 0 0 var(--fts-space-2, 8px);
  font-size: 14px;
  color: var(--fts-text-tertiary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kb-article__footer {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
}

.kb-article__meta {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-quaternary);
}

.kb-article__author {
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-quaternary);
  margin-left: auto;
}

/* 收藏按钮 */
.kb-favorite-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  color: var(--fts-text-quaternary);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    background-color: var(--fts-bg-tertiary);
    color: var(--fts-warning);
  }

  &--active {
    color: var(--fts-warning);
  }
}

/* ── 减少动画偏好 ── */
@media (prefers-reduced-motion: reduce) {
  .kb-article,
  .kb-favorite-btn {
    transition: none;
  }
}
</style>
