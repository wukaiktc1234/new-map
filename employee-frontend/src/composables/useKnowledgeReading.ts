import { ref, computed, onBeforeUnmount } from 'vue'
import type { KnowledgeArticle, ReadingSession, QuizQuestion } from '@/types/knowledge'
import type { QuizDifficulty } from '@/types/knowledge'

// ==================== 常量配置 ====================

/** 短文最小时长（秒） */
const MIN_READ_SHORT = 60
/** 长文最小时长（秒） */
const MIN_READ_LONG = 120
/** 判定长短文的内容长度阈值（字符数） */
const CONTENT_LENGTH_THRESHOLD = 500
/** 首次弹题等待比例（阅读达到最小时长的此比例后弹出第一题） */
const FIRST_POPUP_RATIO = 0.4
/** 后续弹题间隔（秒，答完一题后多久弹出下一题） */
const POPUP_INTERVAL = 25
/** 每次学习从题池中随机抽取的最少题目数 */
const QUIZ_PICK_MIN = 3
/** 每次学习从题池中随机抽取的最多题目数 */
const QUIZ_PICK_MAX = 4
/** 通过阈值（普通题目正确率要求，如 0.75 = 75%） */
const PASS_THRESHOLD = 0.75
/** 学习时间记录的localStorage键名 */
const LS_KEY_STUDY_RECORDS = 'fts_study_time_records'

// ==================== 模块级工具函数 ====================

/**
 * 从localStorage读取某篇文章的学习时间汇总（模块级导出，可供任意组件调用）
 * @param articleId - 文章ID
 */
export function getStudySummary(articleId: string): {
  lastStudyTime: string | null
  totalStudyMinutes: number
  studyCount: number
  lastScore: number | null
  bestScore: number | null
} | null {
  try {
    const raw = localStorage.getItem(LS_KEY_STUDY_RECORDS)
    if (!raw) return null
    const records: StudyTimeRecord[] = JSON.parse(raw)
    const filtered = records.filter(r => r.articleId === articleId)
    if (filtered.length === 0) return null

    const sorted = [...filtered].sort((a, b) => b.endTime.localeCompare(a.endTime))
    const last = sorted[0]
    const totalSec = filtered.reduce((sum, r) => sum + r.durationSeconds, 0)
    const completedRecords = filtered.filter(r => r.completed)

    return {
      lastStudyTime: last.startTime,
      totalStudyMinutes: Math.round(totalSec / 60),
      studyCount: filtered.length,
      lastScore: last.completed ? last.score : null,
      bestScore: completedRecords.length > 0
        ? Math.max(...completedRecords.map(r => r.score))
        : null,
    }
  } catch {
    return null
  }
}

// ==================== 类型定义 ====================

/** 学习阶段枚举 */
export type LearningPhase = 'idle' | 'reading' | 'result' | 'completed' | 'abandoned' | 'reviewing'

/** 学习模式 */
export type LearningMode = 'test' | 'review'

/** 单道题的答题详情（用于结果展示） */
export interface AnswerDetail {
  /** 题目ID */
  questionId: string
  /** 题目文本（截断显示） */
  questionText: string
  /** 是否为必会题 */
  isRequired: boolean
  /** 是否答对 */
  correct: boolean
}

/** 学习结果 */
export interface LearningResult {
  /** 是否通过（必会题全对 + 普通题达到阈值） */
  passed: boolean
  /** 未通过原因 */
  failReason?: 'required_missed' | 'normal_threshold'
  /** 答对题目数 */
  correctCount: number
  /** 总题目数 */
  totalCount: number
  /** 正确率百分比（0-100） */
  score: number
  /** 必会题总数 */
  requiredTotal: number
  /** 必会题答对数 */
  requiredCorrect: number
  /** 必会题是否全部正确 */
  allRequiredPassed: boolean
  /** 每道题的答题详情 */
  answerDetails: AnswerDetail[]
  /** 阅读时长（秒） */
  readSeconds: number
}

/** 单次学习时间记录（用于localStorage持久化和培训页面展示） */
export interface StudyTimeRecord {
  /** 文章ID（与课程关联） */
  articleId: string
  /** 本次学习开始时间（ISO字符串） */
  startTime: string
  /** 本次学习结束时间（ISO字符串） */
  endTime: string
  /** 本次阅读时长（秒） */
  durationSeconds: number
  /** 是否完成（通过测验） */
  completed: boolean
  /** 测验得分（0-100，未完成则为0） */
  score: number
}

// ==================== 工具函数 ====================

function shuffleArray<T>(arr: T[]): T[] {
  const result = [...arr]
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

/**
 * 知识库阅读学习 Composable v3
 *
 * 核心特性：
 * 1. 题目分级：普通(normal) / 必会(required)，必会题错=直接OUT
 * 2. 双模式：测试模式(test) / 复习模式(review)
 * 3. 完整状态机：idle → reading → result → completed/abandoned
 * 4. 防遗忘：支持复习模式，避免"学完就忘"
 *
 * 通过条件（必须同时满足）：
 *   A) 所有「必会」题目全部答对（一票否决）
 *   B) 整体正确率 ≥ PASS_THRESHOLD（默认75%）
 */
export function useKnowledgeReading() {
  // ==================== 学习阶段与模式 ====================
  const learningPhase = ref<LearningPhase>('idle')
  const learningMode = ref<LearningMode>('test')

  // ==================== 阅读计时器 ====================
  let timerId: ReturnType<typeof setInterval> | null = null
  const readSeconds = ref(0)
  /** 本次学习会话开始时间戳（beginLearning调用时记录） */
  let sessionStartTimestamp: number | null = null
  const currentArticle = ref<KnowledgeArticle | null>(null)

  // ==================== 弹题状态 ====================
  const shuffledQuestions = ref<QuizQuestion[]>([])
  const pendingQuestionIndex = ref(0)
  let popupTimerId: ReturnType<typeof setTimeout> | null = null
  const popupAnswer = ref<string>('')
  const popupSubmitted = ref(false)
  const popupIsCorrect = ref(false)

  // ==================== 答题记录 ====================
  /** 每道题的答题结果（索引→是否正确） */
  const answerResults = ref<boolean[]>([])

  // ==================== 计算属性 ====================

  const minRequiredSeconds = computed(() => {
    if (!currentArticle.value) return MIN_READ_SHORT
    return currentArticle.value.content.length > CONTENT_LENGTH_THRESHOLD ? MIN_READ_LONG : MIN_READ_SHORT
  })

  const hasMetMinTime = computed(() => readSeconds.value >= minRequiredSeconds.value)
  const totalQuizCount = computed(() => shuffledQuestions.value.length)
  const answeredCount = computed(() => pendingQuestionIndex.value)

  /** 当前正在展示的弹题 */
  const currentPopupQuestion = computed<QuizQuestion | null>(() => {
    if (!shuffledQuestions.value.length || pendingQuestionIndex.value >= shuffledQuestions.value.length) return null
    return shuffledQuestions.value[pendingQuestionIndex.value]
  })

  /** 当前弹题是否为必会题 */
  const isCurrentRequired = computed<boolean>(() =>
    currentPopupQuestion.value?.difficulty === 'required',
  )

  /** 答对题目数 */
  const correctCount = computed(() => answerResults.value.filter(r => r).length)

  /** 通过所需最低正确数（普通题阈值） */
  const passRequiredCount = computed(() =>
    Math.ceil(totalQuizCount.value * PASS_THRESHOLD),
  )

  /** 正确率百分比 */
  const scorePercent = computed(() =>
    totalQuizCount.value > 0 ? Math.round((correctCount.value / totalQuizCount.value) * 100) : 0,
  )

  // ── 必会题统计 ──

  /** 当前抽取题目中的必会题列表（含索引） */
  const requiredQuestions = computed(() =>
    shuffledQuestions.value
      .map((q, idx) => ({ q, idx }))
      .filter(({ q }) => q.difficulty === 'required'),
  )

  /** 必会题总数 */
  const requiredTotal = computed(() => requiredQuestions.value.length)

  /** 已答对的必会题数量 */
  const requiredCorrect = computed(() =>
    requiredQuestions.value.filter(({ idx }) => idx < answerResults.value.length && answerResults.value[idx]).length,
  )

  /** 必会题是否全部答对（核心通过条件A） */
  const allRequiredPassed = computed(() =>
    requiredTotal.value === 0 || requiredCorrect.value >= requiredTotal.value,
  )

  // ── 最终通过判定 ──

  /**
   * 通过条件（AND逻辑）：
   *   A) 所有必会题全部答对（requiredCorrect >= requiredTotal）
   *   B) 整体正确率 ≥ PASS_THRESHOLD
   */
  const isPassed = computed(() => {
    if (learningMode.value === 'review') return true // 复习模式默认通过
    if (!allRequiredPassed.value) return false          // 条件A失败 → 直接OUT
    return correctCount.value >= passRequiredCount.value // 条件B
  })

  /** 未通过的具体原因 */
  const failReason = computed<'required_missed' | 'normal_threshold' | undefined>(() => {
    if (isPassed.value) return undefined
    if (!allRequiredPassed.value) return 'required_missed'
    return 'normal_threshold'
  })

  // ── 学习结果对象 ──

  const learningResult = computed<LearningResult | null>(() => {
    if (learningPhase.value !== 'result') return null

    const details: AnswerDetail[] = shuffledQuestions.value.map((q, idx) => ({
      questionId: q.id,
      questionText: q.question.length > 40 ? q.question.slice(0, 40) + '...' : q.question,
      isRequired: q.difficulty === 'required',
      correct: idx < answerResults.value.length ? answerResults.value[idx] : false,
    }))

    return {
      passed: isPassed.value,
      failReason: failReason.value,
      correctCount: correctCount.value,
      totalCount: totalQuizCount.value,
      score: scorePercent.value,
      requiredTotal: requiredTotal.value,
      requiredCorrect: requiredCorrect.value,
      allRequiredPassed: allRequiredPassed.value,
      answerDetails: details,
      readSeconds: readSeconds.value,
    }
  })

  const canCompleteLearning = computed(() =>
    learningPhase.value === 'result' && isPassed.value,
  )

  const completeDisabledReason = computed<string | null>(() => {
    if (learningPhase.value === 'reviewing') return null
    if (learningPhase.value === 'idle') return '请先开始测试'
    if (learningPhase.value === 'reading') {
      if (!hasMetMinTime.value) return `还需阅读 ${minRequiredSeconds.value - readSeconds.value} 秒`
      if (pendingQuestionIndex.value < totalQuizCount.value) return '请完成随堂测验'
      return null
    }
    if (learningPhase.value === 'result' && !isPassed.value) {
      if (failReason.value === 'required_missed') {
        return `必会题未全部答对（${requiredCorrect.value}/${requiredTotal.value}），请重新学习`
      }
      return `正确率不足 ${Math.round(PASS_THRESHOLD * 100)}%（${correctCount.value}/${totalQuizCount.value}），请重新学习`
    }
    if (learningPhase.value === 'completed') return '已完成学习'
    if (learningPhase.value === 'abandoned') return '已放弃本次学习'
    return null
  })

  const readProgress = computed(() =>
    Math.min(100, Math.round((readSeconds.value / minRequiredSeconds.value) * 100)),
  )

  const formattedReadTime = computed(() => {
    const t = readSeconds.value
    return `${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`
  })

  const formattedRequiredTime = computed(() => {
    const t = minRequiredSeconds.value
    return `${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`
  })

  const quizQuestions = computed<QuizQuestion[]>(() => shuffledQuestions.value)

  // ==================== 核心方法：生命周期 ====================

  /**
   * 初始化学习/复习
   * @param article 文章
   * @param mode 模式：'test'=测试模式(有弹题), 'review'=复习模式(无测验)
   */
  function initLearning(article: KnowledgeArticle, mode: LearningMode = 'test'): void {
    resetState()
    currentArticle.value = article
    learningMode.value = mode
    learningPhase.value = mode === 'review' ? 'reviewing' : 'idle'

    if (mode === 'test') {
      // 测试模式：准备题池
      const rawQuiz = article.quiz ?? []
      const allShuffled = shuffleArray(rawQuiz)
      const pickCount = Math.min(
        QUIZ_PICK_MAX,
        Math.max(QUIZ_PICK_MIN, Math.ceil(allShuffled.length * 0.5)),
      )
      shuffledQuestions.value = allShuffled.slice(0, Math.min(pickCount, allShuffled.length)).map(q => ({
        ...q,
        options: shuffleArray(q.options),
      }))
    }
    // review 模式：不准备题池
  }

  /** 开始测试（用户主动触发）：启动计时器 + 安排首次弹题 */
  function beginLearning(): void {
    if (!currentArticle.value || learningPhase.value !== 'idle') return

    learningPhase.value = 'reading'
    readSeconds.value = 0
    pendingQuestionIndex.value = 0
    answerResults.value = []
    sessionStartTimestamp = Date.now()

    timerId = setInterval(() => { readSeconds.value++ }, 1000)

    const firstDelay = Math.ceil(minRequiredSeconds.value * FIRST_POPUP_RATIO)
    scheduleNextPopup(firstDelay)
  }

  /** 放弃学习/退出 */
  function abortLearning(): void {
    saveCurrentSessionTime(false)
    stopAllTimers()
    learningPhase.value = 'abandoned'
  }

  /** 重新学习（未通过时重试）：重新抽题从头开始 */
  function retryLearning(): void {
    if (!currentArticle.value) return

    stopAllTimers()
    readSeconds.value = 0
    pendingQuestionIndex.value = 0
    answerResults.value = []
    popupAnswer.value = ''
    popupSubmitted.value = false
    popupIsCorrect.value = false

    // 重新从题池随机抽取新题目
    const rawQuiz = currentArticle.value.quiz ?? []
    const allShuffled = shuffleArray(rawQuiz)
    const pickCount = Math.min(
      QUIZ_PICK_MAX,
      Math.max(QUIZ_PICK_MIN, Math.ceil(allShuffled.length * 0.5)),
    )
    shuffledQuestions.value = allShuffled.slice(0, Math.min(pickCount, allShuffled.length)).map(q => ({
      ...q,
      options: shuffleArray(q.options),
    }))

    learningPhase.value = 'reading'
    timerId = setInterval(() => { readSeconds.value++ }, 1000)

    const firstDelay = Math.ceil(minRequiredSeconds.value * FIRST_POPUP_RATIO)
    scheduleNextPopup(firstDelay)
  }

  /** 确认完成学习 */
  function confirmComplete(): void {
    if (learningPhase.value !== 'result' || !isPassed.value) return
    stopAllTimers()
    learningPhase.value = 'completed'
  }

  // ==================== 弹题交互方法 ====================

  function handlePopupAnswer(optionId: string): { correct: boolean } {
    if (popupSubmitted.value || !currentPopupQuestion.value) return { correct: false }

    popupAnswer.value = optionId
    popupSubmitted.value = true
    popupIsCorrect.value = optionId === currentPopupQuestion.value.correctAnswerId

    return { correct: popupIsCorrect.value }
  }

  /** 提交当前题目答案并进入下一题（或进入 result 阶段） */
  function handlePopupSubmitAndNext(): void {
    if (!popupSubmitted.value || !currentPopupQuestion.value) return

    // 记录本题答题结果
    answerResults.value.push(popupIsCorrect.value)

    // 重置弹题显示状态
    popupAnswer.value = ''
    popupSubmitted.value = false
    popupIsCorrect.value = false

    // 推进索引
    pendingQuestionIndex.value++

    if (pendingQuestionIndex.value < shuffledQuestions.value.length) {
      scheduleNextPopup(POPUP_INTERVAL)
    } else {
      saveCurrentSessionTime(true)
      stopAllTimers()
      learningPhase.value = 'result'
    }
  }

  /** 点击弹题外部区域 / 退出按钮 */
  function handlePopupDismiss(): void {
    if (learningPhase.value !== 'reading') return
    abortLearning()
  }

  // ==================== 学习时间记录（localStorage持久化） ====================

  /**
   * 保存当前学习会话的时间记录到localStorage
   * @param completed - 是否完成测验通过
   */
  function saveCurrentSessionTime(completed: boolean): void {
    if (!currentArticle.value || !sessionStartTimestamp) return

    const now = new Date().toISOString()
    const record: StudyTimeRecord = {
      articleId: currentArticle.value.id,
      startTime: new Date(sessionStartTimestamp).toISOString(),
      endTime: now,
      durationSeconds: readSeconds.value,
      completed,
      score: completed ? scorePercent.value : 0,
    }

    try {
      const raw = localStorage.getItem(LS_KEY_STUDY_RECORDS)
      const records: StudyTimeRecord[] = raw ? JSON.parse(raw) : []
      records.push(record)
      if (records.length > 100) {
        records.splice(0, records.length - 100)
      }
      localStorage.setItem(LS_KEY_STUDY_RECORDS, JSON.stringify(records))
    } catch {
      // localStorage不可用时静默失败
    }

    sessionStartTimestamp = null
  }

  // ==================== 内部工具 ====================

  function resetState(): void {
    stopAllTimers()
    readSeconds.value = 0
    shuffledQuestions.value = []
    pendingQuestionIndex.value = 0
    answerResults.value = []
    popupAnswer.value = ''
    popupSubmitted.value = false
    popupIsCorrect.value = false
    learningPhase.value = 'idle'
    learningMode.value = 'test'
  }

  function stopReading(): void {
    if (timerId !== null) { clearInterval(timerId); timerId = null }
  }

  function clearPopupTimer(): void {
    if (popupTimerId !== null) { clearTimeout(popupTimerId); popupTimerId = null }
  }

  function stopAllTimers(): void {
    stopReading()
    clearPopupTimer()
  }

  function scheduleNextPopup(delaySeconds: number): void {
    clearPopupTimer()
    if (!currentArticle.value || pendingQuestionIndex.value >= shuffledQuestions.value.length) return

    popupTimerId = setTimeout(() => {
      if (
        pendingQuestionIndex.value < shuffledQuestions.value.length &&
        hasMetMinTime.value &&
        learningPhase.value === 'reading'
      ) {
        popupAnswer.value = ''
        popupSubmitted.value = false
        popupIsCorrect.value = false
      } else if (!hasMetMinTime.value && learningPhase.value === 'reading') {
        scheduleNextPopup(5)
      }
    }, delaySeconds * 1000)
  }

  /** 获取当前阅读会话快照 */
  function getSessionSnapshot(): ReadingSession | null {
    if (!currentArticle.value) return null
    return {
      isLearningMode: learningPhase.value !== 'idle',
      startedAt: Date.now() - readSeconds.value * 1000,
      readSeconds: readSeconds.value,
      minRequiredSeconds: minRequiredSeconds.value,
      hasMetMinTime: hasMetMinTime.value,
      quizCompleted: pendingQuestionIndex.value >= totalQuizCount.value,
      quizPassed: isPassed.value,
      answers: {},
    }
  }

  // ==================== 生命周期清理 ====================
  onBeforeUnmount(() => { stopAllTimers() })

  return {
    // ===== 阶段与结果 =====
    learningPhase,
    learningMode,
    learningResult,
    isPassed,
    failReason,
    correctCount,
    passRequiredCount,
    scorePercent,

    // ===== 必会题统计 =====
    isCurrentRequired,
    requiredTotal,
    requiredCorrect,
    allRequiredPassed,

    // ===== 状态 =====
    readSeconds,
    currentArticle,
    popupAnswer,
    popupSubmitted,
    popupIsCorrect,

    // ===== 计算属性 =====
    minRequiredSeconds,
    hasMetMinTime,
    quizQuestions,
    canCompleteLearning,
    completeDisabledReason,
    readProgress,
    formattedReadTime,
    formattedRequiredTime,

    // ===== 弹题相关 =====
    currentPopupQuestion,
    answeredCount,
    totalQuizCount,

    // ===== 生命周期方法 =====
    initLearning,
    beginLearning,
    abortLearning,
    retryLearning,
    confirmComplete,

    // ===== 弹题交互 =====
    handlePopupAnswer,
    handlePopupSubmitAndNext,
    handlePopupDismiss,
    getSessionSnapshot,

    // ===== 兼容旧接口 =====
    startReading: initLearning,
    stopReading,
  }
}
