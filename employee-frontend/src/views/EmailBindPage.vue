<script setup lang="ts">
/**
 * EmailBindPage - 邮箱绑定页
 *
 * 输入邮箱 → 发送验证码 → 绑定流程
 * Mock实现，后续对接真实邮件服务
 */
import { ref, reactive, computed } from 'vue'
import { Phone, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'

const currentStep = ref<'input' | 'verify' | 'done'>('input')

const form = reactive({
  email: '',
  code: '',
})

const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

/** 绑定信息存储key（与PasswordRecoveryPage共享） */
const BIND_STORAGE_KEY = 'emp_bound_contact'
const MOCK_CODE = '123456'

/** 是否可发送 */
const canSendCode = computed(() => countdown.value === 0 && form.email.trim() !== '')

/** 邮箱格式校验 */
function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

/** 发送验证码 */
async function handleSendCode() {
  if (!form.email.trim()) {
    ElMessage.warning('请输入邮箱地址')
    return
  }
  if (!isValidEmail(form.email)) {
    ElMessage.error('邮箱格式不正确')
    return
  }

  ElMessage.success(`验证码已发送至 ${form.email}（Mock：${MOCK_CODE}）`)
  currentStep.value = 'verify'

  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, POLL_INTERVAL)
}

/** 确认绑定 */
function handleBind() {
  if (!form.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (form.code !== MOCK_CODE) {
    ElMessage.error('验证码错误')
    return
  }

  // 保存绑定信息到localStorage（供密码找回等安全功能使用）
  try {
    const existingRaw = localStorage.getItem(BIND_STORAGE_KEY)
    const existing = existingRaw ? JSON.parse(existingRaw) : {}
    localStorage.setItem(BIND_STORAGE_KEY, JSON.stringify({
      ...existing,
      email: form.email,
      bindTime: new Date().toISOString(),
    }))
  } catch {
    // localStorage 写入失败不应阻断流程
  }

  // Mock：绑定成功
  currentStep.value = 'done'
  ElMessage.success('邮箱绑定成功')
}

/** 完成 */
function handleDone() {
  if (countdownTimer) clearInterval(countdownTimer)
  // 可选：返回上一页或刷新用户信息
}
</script>

<template>
  <PageContainer title="邮箱绑定">
    <div class="email-bind-page">
      <!-- 输入邮箱 -->
      <section v-if="currentStep === 'input'" class="bind-card">
        <div class="card-icon card-icon--email">
          <el-icon :size="28"><Phone /></el-icon>
        </div>
        <h3 class="card-title">绑定邮箱</h3>
        <p class="card-desc">
          绑定邮箱后可用于密码找回、安全验证等重要操作
        </p>

        <div class="form-group">
          <label class="form-label">邮箱地址</label>
          <el-input
            v-model="form.email"
            type="email"
            placeholder="请输入您的邮箱地址"
            size="large"
            autocomplete="email"
            @keyup.enter="handleSendCode"
          />
        </div>

        <button class="primary-btn" @click="handleSendCode">
          发送验证码
        </button>
      </section>

      <!-- 输入验证码 -->
      <section v-if="currentStep === 'verify'" class="bind-card">
        <div class="card-icon card-icon--verify">
          <el-icon :size="28"><Phone /></el-icon>
        </div>
        <h3 class="card-title">验证邮箱</h3>
        <p class="card-desc">
          验证码已发送至 <strong>{{ form.email }}</strong>
        </p>

        <div class="form-group">
          <label class="form-label">验证码</label>
          <el-input
            v-model="form.code"
            placeholder="请输入6位验证码"
            maxlength="6"
            size="large"
            autocomplete="one-time-code"
          />
        </div>

        <div class="resend-row">
          <span v-if="countdown > 0" class="countdown-text">
            {{ countdown }}秒后可重新发送
          </span>
          <button v-else class="link-btn" @click="handleSendCode">
            重新发送
          </button>
        </div>

        <button class="primary-btn" @click="handleBind">
          确认绑定
        </button>
      </section>

      <!-- 绑定成功 -->
      <section v-if="currentStep === 'done'" class="bind-card bind-card--center">
        <div class="success-icon">
          <el-icon :size="40" style="color: var(--fts-success)"><CircleCheckFilled /></el-icon>
        </div>
        <h3 class="card-title">绑定成功</h3>
        <p class="card-desc">{{ form.email }} 已成功绑定</p>
        <button class="primary-btn" @click="handleDone">
          完成
        </button>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.email-bind-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--fts-space-6) 0;
}

.bind-card {
  width: 100%;
  max-width: 380px;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-6) var(--fts-space-5);
  box-shadow: var(--fts-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);

  &--center {
    align-items: center;
    text-align: center;
  }
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--fts-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: center;

  &--email {
    background: rgba(var(--fts-primary-rgb), 0.1);
    color: var(--fts-primary);
  }

  &--verify {
    background: rgba(var(--fts-warning-rgb), 0.1);
    color: var(--fts-warning);
  }
}

.card-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
  text-align: center;
}

.card-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
  line-height: 1.5;
  text-align: center;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.form-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
}

.primary-btn {
  width: 100%;
  padding: var(--fts-space-3) 0;
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: #fff;
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.98); }
}

.resend-row {
  display: flex;
  justify-content: flex-end;
}

.countdown-text {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

.link-btn {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;

  &:hover { opacity: 0.8; }
}

.success-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(var(--fts-success-rgb), 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--fts-space-3);
}
</style>
