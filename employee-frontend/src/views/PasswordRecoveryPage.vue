<script setup lang="ts">
/**
 * PasswordRecoveryPage - 密码找回页
 *
 * 安全流程：
 * 1. 检查是否已绑定邮箱/手机 → 未绑定则引导先绑定
 * 2. 仅向已绑定的联系方式发送验证码（禁止输入新邮箱）
 * 3. 验证码校验 + 设置新密码
 * 4. 完成
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Message, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'
import { POLL_INTERVAL } from '@/config/timing'

const router = useRouter()

/** 绑定信息存储key */
const BIND_STORAGE_KEY = 'emp_bound_contact'

interface BoundContact {
  email?: string
  phone?: string
  bindTime: string
}

/** 当前步骤：0-未绑定引导 1-选择方式 2-已发送 3-设置密码 4-完成 */
const currentStep = ref(0)

/** 已绑定的联系信息 */
const boundContact = ref<BoundContact | null>(null)

/** 用户选择的找回方式 */
const recoverMethod = ref<'email' | 'phone'>('email')

const emailForm = reactive({
  email: '',
})

const verifyForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

/** 倒计时秒数 */
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

/** 是否可以发送验证码 */
const canSendCode = computed(() => countdown.value === 0)

/** Mock验证码（固定为123456） */
const MOCK_CODE = '123456'

/** 邮箱格式校验 */
function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

/** 手机格式校验 */
function isValidPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

/** 初始化：检查是否已绑定联系方式 */
onMounted(() => {
  try {
    const raw = localStorage.getItem(BIND_STORAGE_KEY)
    if (raw) {
      boundContact.value = JSON.parse(raw) as BoundContact
      // 有绑定信息直接进入选择方式步骤
      if (boundContact.value.email || boundContact.value.phone) {
        currentStep.value = 1
      }
    }
  } catch {
    // 解析失败视为未绑定
  }
})

/** 步骤0：跳转到绑定页面 */
function handleGoBind() {
  router.push('/settings/security/email-bind')
}

/** 步骤1：确认发送验证码 */
async function handleSendCode() {
  const target = recoverMethod.value === 'email'
    ? boundContact.value?.email
    : boundContact.value?.phone

  if (!target) {
    ElMessage.error('未找到绑定的联系方式，请先绑定')
    return
  }

  // Mock：模拟发送成功（实际应调用后端API，仅向已绑定地址发送）
  const methodLabel = recoverMethod.value === 'email' ? '邮箱' : '手机'
  ElMessage.success(`验证码已发送至${methodLabel} ${maskContact(target)}（Mock：${MOCK_CODE}）`)

  currentStep.value = 2

  // 启动60秒倒计时
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, POLL_INTERVAL)
}

/** 重新发送验证码 */
function handleResendCode() {
  handleSendCode()
}

/** 脱敏显示联系方式 */
function maskContact(contact: string): string {
  if (!contact) return ''
  if (contact.includes('@')) {
    // 邮箱：保留首字符和域名
    const [local, domain] = contact.split('@')
    return `${local[0]}***@${domain}`
  }
  // 手机：保留前3后4
  return contact.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/** 步骤3：验证并设置新密码 */
function handleVerifyAndReset() {
  if (!verifyForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (verifyForm.code !== MOCK_CODE) {
    ElMessage.error('验证码错误，请重新输入')
    return
  }
  if (!verifyForm.newPassword || verifyForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (verifyForm.newPassword !== verifyForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  // Mock：重置成功
  currentStep.value = 4
}

/** 完成：返回登录 */
function handleFinish() {
  // 清理定时器
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  ElMessage.success('密码已重置，请使用新密码登录')
  router.push('/login')
}
</script>

<template>
  <PageContainer title="密码找回">
    <div class="password-recovery-page">
      <!-- 步骤条 -->
      <div class="steps-bar">
        <div
          v-for="(step, index) in ['安全验证', '身份确认', '设置密码', '完成']"
          :key="index"
          class="step-item"
          :class="{
            'step-item--active': currentStep === index + 1,
            'step-item--done': currentStep > index + 1,
          }"
        >
          <span class="step-number">
            <template v-if="currentStep > index + 1">
              <el-icon :size="12"><CircleCheckFilled /></el-icon>
            </template>
            <template v-else>{{ index + 1 }}</template>
          </span>
          <span class="step-label">{{ step }}</span>
          <div v-if="index < 3" class="step-line" :class="{ 'step-line--done': currentStep > index + 1 }"></div>
        </div>
      </div>

      <!-- 步骤0：未绑定引导 -->
      <section v-if="currentStep === 0" class="step-card">
        <div class="card-header card-header--warning">
          <el-icon :size="24" style="color: var(--fts-warning)"><WarningFilled /></el-icon>
          <h3 class="card-title">需要先绑定联系方式</h3>
        </div>
        <p class="card-desc">
          为了保障账号安全，密码找回仅能通过已绑定的邮箱或手机号进行。
          您当前尚未绑定任何联系方式。
        </p>

        <div class="bind-requirement-list">
          <div class="bind-req-item">
            <span class="req-dot req-dot--email"></span>
            <span>绑定邮箱后可通过邮箱验证码重置密码</span>
          </div>
          <div class="bind-req-item">
            <span class="req-dot req-dot--phone"></span>
            <span>绑定手机后可通过短信验证码重置密码</span>
          </div>
        </div>

        <button class="primary-btn" @click="handleGoBind">
          去绑定联系方式
        </button>
      </section>

      <!-- 步骤1：选择已绑定的找回方式 -->
      <section v-if="currentStep === 1" class="step-card">
        <div class="card-header">
          <el-icon :size="24" style="color: var(--fts-primary)"><Message /></el-icon>
          <h3 class="card-title">选择验证方式</h3>
        </div>
        <p class="card-desc">验证码将发送至您已绑定的联系方式</p>

        <div class="method-options">
          <button
            v-if="boundContact?.email"
            :class="['method-option', { 'method-option--active': recoverMethod === 'email' }]"
            @click="recoverMethod = 'email'"
          >
            <span class="method-icon method-icon--email">
              <el-icon :size="20"><Message /></el-icon>
            </span>
            <div class="method-body">
              <span class="method-label">邮箱验证</span>
              <span class="method-value">{{ maskContact(boundContact.email!) }}</span>
            </div>
            <span v-if="recoverMethod === 'email'" class="method-check"></span>
          </button>

          <button
            v-if="boundContact?.phone"
            :class="['method-option', { 'method-option--active': recoverMethod === 'phone' }]"
            @click="recoverMethod = 'phone'"
          >
            <span class="method-icon method-icon--phone">
              <el-icon :size="20"><Message /></el-icon>
            </span>
            <div class="method-body">
              <span class="method-label">手机验证</span>
              <span class="method-value">{{ maskContact(boundContact.phone!) }}</span>
            </div>
            <span v-if="recoverMethod === 'phone'" class="method-check"></span>
          </button>
        </div>

        <button class="primary-btn" @click="handleSendCode" :disabled="!canSendCode && currentStep === 2">
          发送验证码
        </button>

        <button class="link-btn-center" @click="router.push('/settings/security/email-bind')">
          更换或新增绑定方式
        </button>
      </section>

      <!-- 步骤2：已发送提示 -->
      <section v-if="currentStep === 2" class="step-card">
        <div class="card-header">
          <el-icon :size="24" style="color: var(--fts-warning)"><Message /></el-icon>
          <h3 class="card-title">验证码已发送</h3>
        </div>
        <p class="card-desc">
          验证码已发送至
          <strong>{{ recoverMethod === 'email' ? maskContact(boundContact?.email || '') : maskContact(boundContact?.phone || '') }}</strong>
          ，请查收并输入
        </p>

        <div class="form-group">
          <label class="form-label">{{ recoverMethod === 'email' ? '邮箱' : '短信' }}验证码</label>
          <el-input
            v-model="verifyForm.code"
            :placeholder="`请输入6位验证码`"
            maxlength="6"
            size="large"
            autocomplete="one-time-code"
          />
        </div>

        <div class="resend-row">
          <span v-if="countdown > 0" class="countdown-text">
            {{ countdown }}秒后可重新发送
          </span>
          <button v-else class="link-btn" @click="handleResendCode">
            重新发送验证码
          </button>
        </div>

        <button class="primary-btn" @click="currentStep = 3">
          下一步
        </button>
      </section>

      <!-- 步骤3：设置新密码 -->
      <section v-if="currentStep === 3" class="step-card">
        <div class="card-header">
          <el-icon :size="24" style="color: var(--fts-primary)"><Lock /></el-icon>
          <h3 class="card-title">设置新密码</h3>
        </div>
        <p class="card-desc">请设置新的登录密码</p>

        <div class="form-group">
          <label class="form-label">新密码</label>
          <el-input
            v-model="verifyForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
            size="large"
            autocomplete="new-password"
          />
        </div>

        <div class="form-group">
          <label class="form-label">确认新密码</label>
          <el-input
            v-model="verifyForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            size="large"
            autocomplete="new-password"
          />
        </div>

        <button class="primary-btn" @click="handleVerifyAndReset">
          确认重置
        </button>
      </section>

      <!-- 步骤4：完成 -->
      <section v-if="currentStep === 4" class="step-card step-card--success">
        <div class="success-icon-wrap">
          <el-icon :size="48" style="color: var(--fts-success)"><CircleCheckFilled /></el-icon>
        </div>
        <h3 class="success-title">密码重置成功</h3>
        <p class="success-desc">您的新密码已生效，请使用新密码登录</p>
        <button class="primary-btn" @click="handleFinish">
          返回登录
        </button>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.password-recovery-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--fts-space-4) 0;
}

/* ===== 步骤条 ===== */
.steps-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 400px;
  margin-bottom: var(--fts-space-5);
  position: relative;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-1);
  position: relative;
  flex: 1;

  &:last-child .step-line {
    display: none;
  }
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  background: var(--fts-bg-tertiary);
  color: var(--fts-text-quaternary);
  border: 2px solid var(--fts-border-secondary);
  transition: all var(--fts-duration-normal) ease;

  .step-item--active & {
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);
    border-color: var(--fts-primary);
  }

  .step-item--done & {
    background: var(--fts-success);
    color: var(--fts-text-on-primary);
    border-color: var(--fts-success);
  }
}

.step-label {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);

  .step-item--active &,
  .step-item--done & {
    color: var(--fts-text-secondary);
    font-weight: 500;
  }
}

.step-line {
  position: absolute;
  top: 14px;
  left: calc(50% + 18px);
  width: calc(100% - 36px);
  height: 2px;
  background: var(--fts-border-secondary);

  &--done {
    background: var(--fts-success);
  }
}

/* ===== 卡片通用 ===== */
.step-card {
  width: 100%;
  max-width: 420px;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5);
  box-shadow: var(--fts-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);

  &--success {
    align-items: center;
    text-align: center;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);

  &--warning {
    padding-bottom: var(--fts-space-3);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.card-title {
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.card-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
  line-height: 1.6;
}

/* ===== 未绑定引导 ===== */
.bind-requirement-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.bind-req-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.req-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &--email { background-color: var(--fts-primary); }
  &--phone { background-color: var(--fts-success); }
}

/* ===== 方式选择 ===== */
.method-options {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.method-option {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  width: 100%;
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1.5px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  background: transparent;
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.04);
  }
}

.method-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--email { background: rgba(var(--fts-primary-rgb), 0.1); color: var(--fts-primary); }
  &--phone { background: rgba(var(--fts-success-rgb), 0.1); color: var(--fts-success); }
}

.method-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.method-label {
  font-size: var(--fts-font-size-base);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.method-value {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.method-check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--fts-primary);
  flex-shrink: 0;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 5px;
    left: 5px;
    width: 8px;
    height: 4px;
    border-left: 2px solid var(--fts-text-on-primary);
    border-bottom: 2px solid var(--fts-text-on-primary);
    transform: rotate(-45deg);
  }
}

/* ===== 表单 ===== */
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
  color: var(--fts-text-on-primary);
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.98); }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
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

.link-btn-center {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--fts-space-2) 0;
  text-align: center;

  &:hover { color: var(--fts-primary); }
}

/* ===== 成功状态 ===== */
.success-icon-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(var(--fts-success-rgb), 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--fts-space-3);
}

.success-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2);
}

.success-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0 0 var(--fts-space-5);
}
</style>
