<!--
  OnboardingWizard - 新员工入职引导向导（商用级）

  6步引导流程：欢迎 → 功能导览 → 入门必做 → 团队介绍 → 培训计划 → 完成
  使用 Element Plus 真实图标，符合项目设计规范
-->
<script setup lang="ts">
/**
 * OnboardingWizard - 新员工入职引导向导组件（商用级）
 *
 * 基于行业最佳实践的6步入职引导：
 *   Step 1: 欢迎页 — 个性化欢迎 + 企业品牌
 *   Step 2: 功能导览 — 核心功能模块介绍
 *   Step 3: 入门必做 — 首日任务清单
 *   Step 4: 团队与组织 — 直属上级/部门信息
 *   Step 5: 培训计划 — 必修课程路径
 *   Step 6: 完成页 — 准备就绪
 *
 * @last-modified 2026-06-05
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight, Check, CircleCheck, Grid, List,
  UserFilled, Reading, Trophy,
  // 功能卡片图标
  AlarmClock, Tickets, Document, Wallet,
  // 必做清单图标
  Edit, Clock, Reading as TrainingIcon,
  // 团队图标
  OfficeBuilding, User as UserIcon,
  // 培训图标
  CollectionTag, DataLine,
} from '@element-plus/icons-vue'
import { useOnboarding } from '@/composables/useOnboarding'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const permission = usePermissionStore()
const {
  currentStep,
  currentStepIndex,
  totalSteps,
  isLastStep,
  isFirstStep,
  progress,
  steps,
  visible,
  nextStep,
  prevStep,
  complete,
  skip,
} = useOnboarding()

// ========== 图标映射表 ==========

/** 步骤主图标映射 */
const stepIconMap: Record<string, any> = {
  CircleCheck,
  Grid,
  List,
  UserFilled,
  Reading,
  Trophy,
}

const currentStepIcon = computed(() => stepIconMap[currentStep.value.icon] || CircleCheck)

// ========== 功能卡片数据（Step 2） ==========

interface FeatureCard {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- Element Plus 图标组件类型
  icon: any
  iconBg: string
  label: string
  desc: string
}

const featureCards: FeatureCard[] = [
  {
    icon: AlarmClock,
    iconBg: 'rgba(var(--fts-primary-rgb), 0.1)',
    label: '考勤打卡',
    desc: 'GPS+WiFi双重验证',
  },
  {
    icon: Reading,
    iconBg: 'rgba(var(--fts-warning-rgb), 0.1)',
    label: '在线培训',
    desc: '必修/选修课程体系',
  },
  {
    icon: Tickets,
    iconBg: 'rgba(var(--fts-success-rgb), 0.1)',
    label: '审批流程',
    desc: '请假/加班/报销等',
  },
  {
    icon: Document,
    iconBg: 'rgba(var(--fts-info-rgb), 0.1)',
    label: '知识库',
    desc: '食品安全/操作规范',
  },
]

// ========== 必做清单数据（Step 3） ==========

interface ChecklistItem {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- Element Plus 图标组件类型
  icon: any
  label: string
  desc: string
  route: string
}

const checklistItems: ChecklistItem[] = [
  {
    icon: Edit,
    label: '完善个人资料',
    desc: '填写基本信息以便同事识别',
    route: '/settings/account',
  },
  {
    icon: AlarmClock,
    label: '完成首次打卡',
    desc: '设置打卡位置并完成签到',
    route: '/attendance',
  },
  {
    icon: TrainingIcon,
    label: '开始入职培训',
    desc: '完成必修课程获取证书',
    route: '/training',
  },
]

// ========== 团队信息数据（Step 4） ==========

const teamInfo = computed(() => ({
  department: permission.userInfo.department || '--',
  position: permission.userInfo.position || permission.getCurrentRoleLabel(),
  storeName: permission.userInfo.storeName || '--',
  supervisorName: permission.userInfo.supervisorName || '--',
}))

// ========== 培训计划数据（Step 5） ==========

const trainingPlan = [
  { title: '食品安全基础知识', status: 'required', duration: '30分钟' },
  { title: '服务操作规范', status: 'required', duration: '45分钟' },
  { title: '消防安全与应急处理', status: 'required', duration: '25分钟' },
  { title: '企业文化与价值观', status: 'optional', duration: '20分钟' },
]

// ========== 操作方法 ==========

function handlePrimaryAction() {
  if (isLastStep.value) {
    complete()
    router.push('/home')
  } else {
    nextStep()
  }
}

function handleChecklistItem(item: ChecklistItem) {
  skip()
  router.push(item.route)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="onboard-fade">
      <div v-if="visible" class="onboarding-overlay" @click.self="skip">
        <div class="onboarding-wizard" role="dialog" aria-modal="true" :aria-label="currentStep.title">
          <!-- 顶部进度条 -->
          <div class="wizard-progress">
            <div class="wizard-progress__bar" :style="{ width: `${progress}%` }" />
          </div>
          <!-- 进度点 -->
          <div class="wizard-dots">
            <button
              v-for="(step, idx) in steps"
              :key="step.id"
              type="button"
              class="wizard-dot"
              :class="{
                'wizard-dot--active': idx === currentStepIndex,
                'wizard-dot--done': idx < currentStepIndex,
              }"
              :aria-label="`第${idx + 1}步：${step.title}`"
              :disabled="idx > currentStepIndex"
              @click="idx < currentStepIndex ? (currentStepIndex = idx) : undefined"
            />
          </div>

          <!-- 内容区域 -->
          <div class="wizard-body">
            <!-- ====== Step 1: 欢迎页 ====== -->
            <template v-if="currentStep.id === 'welcome'">
              <div class="wizard-icon wizard-icon--welcome">
                <el-icon :size="44"><component :is="currentStepIcon" /></el-icon>
              </div>
              <h2 class="wizard-title">{{ currentStep.title }}</h2>
              <p class="wizard-desc">{{ currentStep.description }}</p>
              <div class="welcome-badge">
                <el-icon :size="14"><OfficeBuilding /></el-icon>
                <span>{{ teamInfo.storeName || '连锁餐饮门店' }}</span>
              </div>
            </template>

            <!-- ====== Step 2: 功能导览 ====== -->
            <template v-if="currentStep.id === 'features'">
              <h2 class="wizard-title">{{ currentStep.title }}</h2>
              <p class="wizard-desc">{{ currentStep.description }}</p>
              <div class="feature-grid">
                <div
                  v-for="card in featureCards"
                  :key="card.label"
                  class="feature-card"
                >
                  <span class="feature-card__icon" :style="{ background: card.iconBg }">
                    <el-icon :size="22"><component :is="card.icon" /></el-icon>
                  </span>
                  <span class="feature-card__label">{{ card.label }}</span>
                  <span class="feature-card__desc">{{ card.desc }}</span>
                </div>
              </div>
            </template>

            <!-- ====== Step 3: 入门必做 ====== -->
            <template v-if="currentStep.id === 'checklist'">
              <h2 class="wizard-title">{{ currentStep.title }}</h2>
              <p class="wizard-desc">{{ currentStep.description }}</p>
              <ul class="checklist">
                <li
                  v-for="item in checklistItems"
                  :key="item.label"
                  class="checklist-item"
                  @click="handleChecklistItem(item)"
                >
                  <span class="checklist-item__icon">
                    <el-icon :size="18"><component :is="item.icon" /></el-icon>
                  </span>
                  <div class="checklist-item__body">
                    <span class="checklist-item__label">{{ item.label }}</span>
                    <span class="checklist-item__desc">{{ item.desc }}</span>
                  </div>
                  <el-icon :size="14" class="checklist-item__arrow"><ArrowRight /></el-icon>
                </li>
              </ul>
            </template>

            <!-- ====== Step 4: 团队与组织 ====== -->
            <template v-if="currentStep.id === 'team'">
              <div class="wizard-icon wizard-icon--team">
                <el-icon :size="40"><UserFilled /></el-icon>
              </div>
              <h2 class="wizard-title">{{ currentStep.title }}</h2>
              <p class="wizard-desc">{{ currentStep.description }}</p>
              <div class="team-info-grid">
                <div class="team-info-card">
                  <span class="team-info-card__label">所属门店</span>
                  <span class="team-info-card__value">
                    <el-icon :size="14"><OfficeBuilding /></el-icon>
                    {{ teamInfo.storeName }}
                  </span>
                </div>
                <div class="team-info-card">
                  <span class="team-info-card__label">所在部门</span>
                  <span class="team-info-card__value">{{ teamInfo.department }}</span>
                </div>
                <div class="team-info-card">
                  <span class="team-info-card__label">职位</span>
                  <span class="team-info-card__value">{{ teamInfo.position }}</span>
                </div>
                <div class="team-info-card">
                  <span class="team-info-card__label">直属上级</span>
                  <span class="team-info-card__value">
                    <el-icon :size="14"><UserIcon /></el-icon>
                    {{ teamInfo.supervisorName }}
                  </span>
                </div>
              </div>
            </template>

            <!-- ====== Step 5: 培训计划 ====== -->
            <template v-if="currentStep.id === 'training'">
              <div class="wizard-icon wizard-icon--training">
                <el-icon :size="40"><Reading /></el-icon>
              </div>
              <h2 class="wizard-title">{{ currentStep.title }}</h2>
              <p class="wizard-desc">{{ currentStep.description }}</p>
              <ul class="training-list">
                <li v-for="course in trainingPlan" :key="course.title" class="training-item">
                  <span class="training-item__status" :class="`training-item__status--${course.status}`">
                    {{ course.status === 'required' ? '必修' : '选修' }}
                  </span>
                  <span class="training-item__title">{{ course.title }}</span>
                  <span class="training-item__duration">
                    <el-icon :size="12"><Clock /></el-icon>
                    {{ course.duration }}
                  </span>
                </li>
              </ul>
            </template>

            <!-- ====== Step 6: 完成页 ====== -->
            <template v-if="currentStep.id === 'complete'">
              <div class="wizard-icon wizard-icon--complete">
                <el-icon :size="48"><Trophy /></el-icon>
              </div>
              <h2 class="wizard-title">{{ currentStep.title }}</h2>
              <p class="wizard-desc">{{ currentStep.description }}</p>
              <div class="complete-stats">
                <div class="complete-stat">
                  <span class="complete-stat__num">4</span>
                  <span class="complete-stat__label">核心功能</span>
                </div>
                <div class="complete-stat">
                  <span class="complete-stat__num">3</span>
                  <span class="complete-stat__label">必做任务</span>
                </div>
                <div class="complete-stat">
                  <span class="complete-stat__num">4</span>
                  <span class="complete-stat__label">培训课程</span>
                </div>
              </div>
            </template>
          </div>

          <!-- 底部操作栏 -->
          <div class="wizard-footer">
            <button
              v-if="!isFirstStep && !isLastStep"
              type="button"
              class="wizard-btn wizard-btn--ghost"
              @click="prevStep"
            >
              上一步
            </button>
            <button
              v-if="currentStep.secondaryAction"
              type="button"
              class="wizard-btn wizard-btn--ghost"
              @click="skip"
            >
              {{ currentStep.secondaryAction.text }}
            </button>
            <div class="wizard-footer__spacer" />
            <button
              type="button"
              class="wizard-btn wizard-btn--primary"
              @click="handlePrimaryAction"
            >
              {{ currentStep.actionText }}
              <el-icon v-if="!isLastStep" :size="14"><ArrowRight /></el-icon>
              <el-icon v-else :size="14"><Check /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
/* ============================================
 * OnboardingWizard 样式（商用级）
 * 遵循项目 BEM + --fts-* CSS 变量规范
 * ============================================ */

// ---------- 遮罩层 ----------
.onboarding-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.55);
  padding: var(--fts-space-4);
  backdrop-filter: blur(4px);
}

// ---------- 向导卡片 ----------
.onboarding-wizard {
  width: 100%;
  max-width: 440px;
  max-height: 88vh;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-xl);
  box-shadow:
    0 24px 64px rgba(0, 0, 0, 0.28),
    0 8px 24px rgba(0, 0, 0, 0.16);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  @media (max-width: 480px) {
    max-width: 100%;
    max-height: 92vh;
    border-radius: var(--fts-radius-lg);
  }
}

// ---------- 进度条 ----------
.wizard-progress {
  height: 3px;
  background: var(--fts-bg-secondary);

  &__bar {
    height: 100%;
    background: linear-gradient(90deg, var(--fts-primary), var(--fts-gradient-primary-deep));
    border-radius: 0 2px 2px 0;
    transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

// ---------- 进度点 ----------
.wizard-dots {
  display: flex;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) 0 0;
}

.wizard-dot {
  width: 8px;
  height: 8px;
  border: none;
  border-radius: 50%;
  background: var(--fts-bg-tertiary);
  cursor: pointer;
  transition: all 0.25s ease;
  padding: 0;

  &--active {
    width: 24px;
    border-radius: 4px;
    background: var(--fts-primary);
  }

  &--done {
    background: var(--fts-primary-light, rgba(var(--fts-primary-rgb), 0.4));
  }

  &:disabled {
    cursor: default;
  }
}

// ---------- 内容区 ----------
.wizard-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--fts-space-5) var(--fts-space-6) var(--fts-space-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  @media (max-width: 480px) {
    padding: var(--fts-space-4) var(--fts-space-4) var(--fts-space-3);
  }
}

// ---------- 图标容器 ----------
.wizard-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--fts-radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--fts-space-4);

  &--welcome {
    background: linear-gradient(135deg, rgba(var(--fts-primary-rgb), 0.12), rgba(var(--fts-primary-rgb), 0.04));
    color: var(--fts-primary);
  }

  &--team {
    background: linear-gradient(135deg, rgba(var(--fts-info-rgb), 0.12), rgba(var(--fts-info-rgb), 0.04));
    color: var(--fts-info);
  }

  &--training {
    background: linear-gradient(135deg, rgba(var(--fts-warning-rgb), 0.12), rgba(var(--fts-warning-rgb), 0.04));
    color: var(--fts-warning);
  }

  &--complete {
    background: linear-gradient(135deg, rgba(var(--fts-success-rgb), 0.12), rgba(var(--fts-success-rgb), 0.04));
    color: var(--fts-success);
  }
}

// ---------- 文字样式 ----------
.wizard-title {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2);
  line-height: 1.3;
}

.wizard-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.7;
  margin: 0 0 var(--fts-space-4);
  max-width: 340px;
}

// ---------- 欢迎页徽章 ----------
.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-1) var(--fts-space-3);
  border-radius: var(--fts-radius-full);
  background: var(--fts-bg-page);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);

  .el-icon {
    color: var(--fts-primary);
  }
}

// ---------- 功能卡片网格 ----------
.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-3);
  width: 100%;
  margin-top: var(--fts-space-1);
}

.feature-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-4) var(--fts-space-3);
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-page);
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.97);
  }

  &__icon {
    width: 42px;
    height: 42px;
    border-radius: var(--fts-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--fts-primary);
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  &__desc {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-tertiary);
  }
}

// ---------- 必做清单 ----------
.checklist {
  list-style: none;
  margin: var(--fts-space-1) 0 0;
  padding: 0;
  width: 100%;
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) ease;
  -webkit-tap-highlight-color: transparent;

  &:hover {
    background: var(--fts-bg-hover);
  }

  &:active {
    background: var(--fts-bg-page);
  }

  &__icon {
    width: 32px;
    height: 32px;
    border-radius: var(--fts-radius-sm);
    background: var(--fts-bg-secondary);
    color: var(--fts-text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__label {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    text-align: left;
  }

  &__desc {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-quaternary);
    text-align: left;
  }

  &__arrow {
    color: var(--fts-text-quaternary);
    flex-shrink: 0;
  }
}

// ---------- 团队信息网格 ----------
.team-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-3);
  width: 100%;
  margin-top: var(--fts-space-1);
}

.team-info-card {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-page);

  &__label {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-tertiary);
  }

  &__value {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);

    .el-icon {
      color: var(--fts-text-tertiary);
    }
  }
}

// ---------- 培训列表 ----------
.training-list {
  list-style: none;
  margin: var(--fts-space-1) 0 0;
  padding: 0;
  width: 100%;
}

.training-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child {
    border-bottom: none;
  }

  &__status {
    padding: 2px 8px;
    border-radius: var(--fts-radius-sm);
    font-size: var(--fts-font-size-2xs);
    font-weight: 600;
    flex-shrink: 0;

    &--required {
      background: rgba(var(--fts-error-rgb), 0.08);
      color: var(--fts-error);
    }

    &--optional {
      background: rgba(var(--fts-info-rgb), 0.08);
      color: var(--fts-info);
    }
  }

  &__title {
    flex: 1;
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    text-align: left;
  }

  &__duration {
    display: flex;
    align-items: center;
    gap: 2px;
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-quaternary);
    flex-shrink: 0;
  }
}

// ---------- 完成页统计 ----------
.complete-stats {
  display: flex;
  justify-content: center;
  gap: var(--fts-space-6);
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);
}

.complete-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;

  &__num {
    font-size: var(--fts-font-size-xl);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-primary);
  }

  &__label {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-tertiary);
  }
}

// ---------- 底部操作栏 ----------
.wizard-footer {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-5) var(--fts-space-5);
  border-top: 1px solid var(--fts-border-secondary);

  @media (max-width: 480px) {
    padding: var(--fts-space-3) var(--fts-space-4) var(--fts-space-4);
  }

  &__spacer {
    flex: 1;
  }
}

.wizard-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-2) var(--fts-space-4);
  border: none;
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-base);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;
  white-space: nowrap;
  -webkit-tap-highlight-color: transparent;

  &--primary {
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);

    &:hover { opacity: 0.9; }
    &:active { opacity: 0.82; transform: scale(0.98); }
  }

  &--ghost {
    background: transparent;
    color: var(--fts-text-secondary);
    padding: var(--fts-space-2) var(--fts-space-3);

    &:hover { color: var(--fts-text-primary); background: var(--fts-bg-hover); }
    &:active { transform: scale(0.98); }
  }
}

// ---------- 过渡动画 ----------
.onboard-fade-enter-active {
  transition: opacity 0.3s ease;
}

.onboard-fade-leave-active {
  transition: opacity 0.2s ease;
}

.onboard-fade-enter-from,
.onboard-fade-leave-to {
  opacity: 0;
}
</style>
