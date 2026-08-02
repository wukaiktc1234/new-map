<script setup lang="ts">
/**
 * HelpFeedbackPage - 帮助与反馈页面
 *
 * 提供常见问题解答、使用指南和意见反馈功能。
 */
import { ref, reactive } from 'vue'
import {
  QuestionFilled, ChatDotRound, Document, Phone,
  Promotion, CircleCheck,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'
import { UI_DELAY_MEDIUM } from '@/config/timing'

const activeFaq = ref<string | null>(null)

const faqList = ref([
  {
    id: '1',
    question: '如何查看我的排班？',
    answer: '在首页"我的排班"卡片或底部导航进入排班页面，可以按周查看个人班次安排。点击有班次的日期可查看详情。',
  },
  {
    id: '2',
    question: '如何发起请假/加班申请？',
    answer: '在"我的申请"页面点击"+"按钮，选择申请类型（请假、加班、出差、报销、物品申领），填写信息后提交即可。',
  },
  {
    id: '3',
    question: '如何申请换班？',
    answer: '在团队视图下点击同事的班次卡片，或从班次详情中点击"换班申请"按钮。对方确认后生效。',
  },
  {
    id: '4',
    question: '忘记密码怎么办？',
    answer: '在登录页点击"忘记密码"，通过手机验证码重置。也可以联系门店管理员协助处理。',
  },
  {
    id: '5',
    question: '如何切换深色模式？',
    answer: '在个人中心 → 外观主题 中可选择浅色、深色或跟随系统模式。',
  },
])

const feedbackForm = reactive({
  type: '' as string,
  content: '',
  contact: '',
})

const submitting = ref(false)

function toggleFaq(id: string) {
  activeFaq.value = activeFaq.value === id ? null : id
}

function handleSubmit() {
  if (!feedbackForm.type) {
    ElMessage.warning('请选择反馈类型')
    return
  }
  if (feedbackForm.content.length < 10) {
    ElMessage.warning('请详细描述您的问题（至少10个字）')
    return
  }
  submitting.value = true
  setTimeout(() => {
    ElMessage.success('感谢您的反馈，我们会尽快处理！')
    feedbackForm.type = ''
    feedbackForm.content = ''
    feedbackForm.contact = ''
    submitting.value = false
  }, UI_DELAY_MEDIUM)
}
</script>

<template>
  <PageContainer title="帮助与反馈">
    <div class="help-page">
      <!-- 快捷入口 -->
      <section class="quick-links">
        <button class="quick-link" @click="activeFaq = null">
          <el-icon :size="20"><Document /></el-icon>
          <span>常见问题</span>
        </button>
        <button class="quick-link" @click="$el?.querySelector('.feedback-section')?.scrollIntoView({ behavior: 'smooth' })">
          <el-icon :size="20"><ChatDotRound /></el-icon>
          <span>意见反馈</span>
        </button>
        <button class="quick-link" @click="ElMessage.info('客服热线：400-123-4567')">
          <el-icon :size="20"><Phone /></el-icon>
          <span>联系客服</span>
        </button>
      </section>

      <!-- 常见问题 -->
      <section class="faq-section">
        <h3 class="section-title">常见问题</h3>
        <div class="faq-list">
          <div
            v-for="faq in faqList"
            :key="faq.id"
            :class="['faq-item', { 'faq-item--open': activeFaq === faq.id }]"
            @click="toggleFaq(faq.id)"
          >
            <div class="faq-question">
              <span class="faq-icon">
                <el-icon><QuestionFilled /></el-icon>
              </span>
              <span class="faq-q-text">{{ faq.question }}</span>
              <el-icon :class="['faq-arrow', { 'faq-arrow--open': activeFaq === faq.id }]">
                <Promotion />
              </el-icon>
            </div>
            <transition name="faq-answer">
              <p v-if="activeFaq === faq.id" class="faq-answer">{{ faq.answer }}</p>
            </transition>
          </div>
        </div>
      </section>

      <!-- 意见反馈 -->
      <section class="feedback-section section-card">
        <h3 class="section-title">意见反馈</h3>
        <div class="form-group">
          <label class="form-label">反馈类型</label>
          <div class="type-options">
            <button
              v-for="t in ['功能建议', 'Bug报告', '使用咨询', '其他']"
              :key="t"
              :class="['type-option', { 'type-option--active': feedbackForm.type === t }]"
              @click="feedbackForm.type = t"
            >
              {{ t }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">详细描述</label>
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您的建议或遇到的问题..."
            maxlength="500"
            show-word-limit
          />
        </div>
        <div class="form-group">
          <label class="form-label">联系方式（选填）</label>
          <el-input
            v-model="feedbackForm.contact"
            placeholder="手机号或邮箱，方便我们联系您"
          />
        </div>
        <button
          class="submit-btn"
          :disabled="submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交反馈' }}
        </button>
      </section>

      <!-- 版本信息 -->
      <footer class="page-footer">
        <p>员工端 v1.0.0</p>
        <p>© 2026 食品溯源系统</p>
      </footer>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.help-page {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

/* ===== 快捷入口 ===== */
.quick-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-3);
}

.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-2);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-xs);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: var(--fts-primary);
    color: var(--fts-primary);

    .el-icon {
      background: rgba(var(--fts-primary-rgb), 0.08);
      color: var(--fts-primary);
    }
  }

  &:active {
    transform: scale(0.97);
  }

  .el-icon {
    width: 40px;
    height: 40px;
    border-radius: var(--fts-radius-md);
    background: var(--fts-bg-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s ease;
  }
}

/* ===== FAQ ===== */
.section-title {
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.faq-item {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  overflow: hidden;
  transition: border-color 0.15s ease;

  &:hover {
    border-color: var(--fts-border-hover);
  }

  &--open {
    border-color: rgba(var(--fts-primary-rgb), 0.25);
  }
}

.faq-question {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  cursor: pointer;
  user-select: none;
}

.faq-icon {
  color: var(--fts-primary);
  flex-shrink: 0;
}

.faq-q-text {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
  flex: 1;
}

.faq-arrow {
  color: var(--fts-text-quaternary);
  transition: transform 0.2s ease;
  flex-shrink: 0;

  &--open {
    transform: rotate(180deg);
    color: var(--fts-primary);
  }
}

.faq-answer {
  margin: 0;
  padding: 0 var(--fts-space-4) var(--fts-space-4);
  padding-left: calc(var(--fts-space-4) + 22px);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.7;
}

/* ===== 反馈表单 ===== */
.section-card {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &:not(:last-child) {
    margin-bottom: var(--fts-space-3);
  }
}

.form-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
}

.type-options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
}

.type-option {
  padding: var(--fts-space-1) var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  border: 1px solid var(--fts-border-hover);
  border-radius: var(--fts-radius-full);
  background: transparent;
  color: var(--fts-text-tertiary);
  cursor: pointer;
  transition: all 0.12s ease;

  &:hover:not(&--active) {
    border-color: var(--fts-primary);
    color: var(--fts-primary);
  }

  &--active {
    background: rgba(var(--fts-primary-rgb), 0.08);
    border-color: var(--fts-primary);
    color: var(--fts-primary);
    font-weight: 600;
  }
}

.submit-btn {
  width: 100%;
  padding: var(--fts-space-3) 0;
  margin-top: var(--fts-space-2);
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: #fff;
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover:not(:disabled) { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

/* ===== 页脚 ===== */
.page-footer {
  text-align: center;
  padding: var(--fts-space-6) 0 var(--fts-space-8);

  p {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-quaternary);
    margin: 2px 0;
  }
}

/* ===== 动画 ===== */
.faq-answer-enter-active {
  animation: slideDown 0.2s ease;
}

.faq-answer-leave-active {
  animation: slideUp 0.15s ease;
}

@keyframes slideDown {
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 200px; }
}

@keyframes slideUp {
  from { opacity: 1; max-height: 200px; }
  to { opacity: 0; max-height: 0; }
}
</style>