<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-bg">
        <div class="bg-circle bg-circle-1"></div>
        <div class="bg-circle bg-circle-2"></div>
        <div class="bg-circle bg-circle-3"></div>
      </div>
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12H15V22" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="brand-title">食品溯源收银系统</h1>
        <p class="brand-desc">Food Traceability POS</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">⚡</span>
            <span>快速收银</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔗</span>
            <span>全程溯源</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📊</span>
            <span>智能分析</span>
          </div>
        </div>
      </div>
    </div>

    <div class="login-main">
      <div class="login-card">
        <div class="card-header">
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-subtitle">请输入您的账号信息登录系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="employeeId">
            <el-input
              ref="employeeIdInput"
              v-model="form.employeeId"
              placeholder="员工号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keydown="handleKeyDown"
            />
          </el-form-item>

          <el-alert
            v-if="error"
            :title="error"
            type="error"
            show-icon
            :closable="false"
            class="login-error"
          />

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        <div class="login-footer">
          <span>v1.0.0</span>
          <span>&copy; {{ currentYear }} 食品溯源系统</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'

interface LoginForm {
  employeeId: string
  password: string
}

interface LoginResponse {
  token: string
  userInfo: {
    employeeId: string
    name: string
    role: string
    shopName: string
  }
}

const router = useRouter()
const formRef = ref<FormInstance>()
const employeeIdInput = ref<HTMLInputElement>()

const form = reactive<LoginForm>({
  employeeId: '',
  password: ''
})

const loading = ref(false)
const error = ref('')

const currentYear = computed(() => new Date().getFullYear())

const rules: FormRules<LoginForm> = {
  employeeId: [
    { required: true, message: '请输入员工号', trigger: 'blur' },
    { min: 3, max: 20, message: '员工号长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32个字符', trigger: 'blur' }
  ]
}

function generateTerminalId(): string {
  const hostname = window.location.hostname || 'localhost'
  return `pos-${hostname}-${Date.now()}`
}

async function handleLogin(): Promise<void> {
  error.value = ''

  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (loading.value) return
  loading.value = true

  // 登录前清理旧凭证，避免旧 token 导致后续请求 401
  localStorage.removeItem('pos-token')
  localStorage.removeItem('pos-user')

  try {
    const payload = {
      employeeId: form.employeeId,
      password: form.password,
      terminalId: generateTerminalId()
    }

    const response = await request.post<LoginResponse>('/v1/pos/auth/login', payload) as any

    if (response?.token) {
      localStorage.setItem('pos-token', response.token)
      localStorage.setItem('pos-user', JSON.stringify(response.userInfo))
      ElMessage.success('登录成功')
      const redirect = router.currentRoute.value.query.redirect as string
      router.push(redirect || '/')
    } else {
      error.value = '登录响应异常，请联系管理员'
    }
  } catch (err: unknown) {
    const errorMsg = err && typeof err === 'object' && 'message' in err
      ? String((err as { message: unknown }).message)
      : '登录失败，请稍后重试'
    error.value = errorMsg
  } finally {
    loading.value = false
  }
}

function handleKeyDown(event: KeyboardEvent): void {
  if (event.key === 'Enter') {
    event.preventDefault()
    handleLogin()
  }
}

function focusEmployeeInput(): void {
  setTimeout(() => {
    const inputEl = employeeIdInput.value as unknown as { $el: HTMLElement } | null
    if (inputEl?.$el) {
      const nativeInput = inputEl.$el.querySelector('input') as HTMLInputElement | null
      nativeInput?.focus()
    }
  }, 100)
}

onMounted(() => {
  focusEmployeeInput()
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

/* ====== 左侧品牌区 ====== */
.login-brand {
  width: 420px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1a1a2e 0%, #16213e 40%, #1a1a2e 100%);
}

.brand-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.bg-circle-1 {
  width: 400px;
  height: 400px;
  right: -120px;
  top: -80px;
  background: #ea580c;
  filter: blur(60px);
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  left: -60px;
  bottom: -40px;
  background: #f97316;
  filter: blur(50px);
}

.bg-circle-3 {
  width: 180px;
  height: 180px;
  right: 40px;
  bottom: 100px;
  background: #fb923c;
  filter: blur(40px);
  animation: float 8s ease-in-out infinite alternate;
}

@keyframes float {
  0% { transform: translateY(0); }
  100% { transform: translateY(-20px); }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 40px;
}

.brand-logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ea580c, #f97316);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  box-shadow: 0 8px 32px rgba(234, 88, 12, 0.35);
}

.brand-logo svg {
  width: 36px;
  height: 36px;
}

.brand-title {
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 36px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 24px;
  border-radius: 50px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  transition: all 0.25s ease;
}

.feature-item:hover {
  background: rgba(234, 88, 12, 0.15);
  border-color: rgba(234, 88, 12, 0.25);
  color: #ffffff;
}

.feature-icon {
  font-size: 17px;
}

/* ====== 右侧登录区 ====== */
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.06);
  padding: 44px 36px 36px;
}

.card-header {
  margin-bottom: 32px;
}

.card-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.card-subtitle {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
  transition: all 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #ea580c inset, 0 0 0 3px rgba(234, 88, 12, 0.1);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-error {
  margin-bottom: 16px;
  border-radius: 8px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 3px;
  transition: all 0.25s ease;
  background: linear-gradient(135deg, #ea580c, #f97316);
  border: none;
  box-shadow: 0 4px 14px rgba(234, 88, 12, 0.3);
  margin-top: 4px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(234, 88, 12, 0.4);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-footer {
  margin-top: 28px;
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #d1d5db;
}

[data-theme='dark'] .login-brand {
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 40%, #0f172a 100%);
}

[data-theme='dark'] .login-main {
  background: #0f172a;
}

[data-theme='dark'] .login-card {
  background: #1e293b;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.2);
}

[data-theme='dark'] .card-title {
  color: #f1f5f9;
}

[data-theme='dark'] .card-subtitle {
  color: #64748b;
}

[data-theme='dark'] .login-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #334155 inset;
  background: #0f172a;
}

[data-theme='dark'] .login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #475569 inset;
}

[data-theme='dark'] .feature-item {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
}

@media (max-width: 900px) {
  .login-brand {
    display: none;
  }

  .login-main {
    padding: 20px;
  }

  .login-card {
    padding: 32px 24px 24px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 20px 20px;
  }

  .card-title {
    font-size: 20px;
  }

  .login-btn {
    height: 42px;
    font-size: 14px;
  }
}
</style>