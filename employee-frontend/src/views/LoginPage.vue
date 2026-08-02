<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useEmployeeStore } from '@/stores/employee'
import { usePermissionStore } from '@/stores/permission'
import { UserLevel } from '@/types/permission'
import { UserRole } from '@/types/user'
import { authApi } from '@/api'
import { useAuthContext } from '@/composables/useAuthContext'
import { useScreenSecurity } from '@/composables/useScreenSecurity'

const router = useRouter()
const employeeStore = useEmployeeStore()
const permissionStore = usePermissionStore()
const auth = useAuthContext()

const loading = ref(false)
const username = ref('')
const password = ref('')
const focusedField = reactive({ username: false, password: false })

/** 开发/APP打包模式均显示演示登录按钮（测试部署需要） */
const isDev = import.meta.env.VITE_APP_MODE === '1' || import.meta.env.DEV

async function passwordLogin() {
  if (!username.value || !password.value) return
  loading.value = true
  try {
    const result = await authApi.login({ username: username.value, password: password.value })
    handleLoginSuccess({
      userId: result.userInfo.employeeId || result.userInfo.fullName,
      username: result.userInfo.fullName,
      fullName: result.userInfo.fullName,
      departmentName: result.userInfo.storeName || '',
      storeName: result.userInfo.storeName || '',
      roles: (result.userInfo.roles || ['employee']) as string[],
      token: result.token,
      refreshToken: result.refreshToken,
      expiresIn: result.expiresIn,
    })
  } catch {
    ElMessage.error('登录失败，请检查账号或密码')
  } finally {
    loading.value = false
  }
}

function demoLogin() {
  handleLoginSuccess({
    userId: 'demo-001',
    username: '演示员工',
    fullName: auth.userName.value,
    departmentName: '后厨部',
    storeName: '示例餐厅',
    roles: ['employee'],
    level: UserLevel.STAFF,
    token: 'demo_token_' + Date.now(),
    refreshToken: 'demo_refresh_' + Date.now(),
    expiresIn: 30 * 60,
  })
}

function managerDemoLogin() {
  handleLoginSuccess({
    userId: 'mgr-001',
    username: '管理者演示',
    fullName: '王店长',
    departmentName: '门店管理',
    storeName: '朝阳大悦城店',
    roles: ['store_manager'],
    level: UserLevel.SUPERVISOR,
    token: 'mgr_token_' + Date.now(),
    refreshToken: 'mgr_refresh_' + Date.now(),
    expiresIn: 30 * 60,
  })
}

/** 经理演示登录（L3层级） */
function managerLevelDemoLogin() {
  handleLoginSuccess({
    userId: 'mgr-002',
    username: '经理演示',
    fullName: '陈运营',
    departmentName: '运营部',
    storeName: '',
    roles: ['ops_director'],
    level: UserLevel.MANAGER,
    token: 'manager_token_' + Date.now(),
    refreshToken: 'manager_refresh_' + Date.now(),
    expiresIn: 30 * 60,
  })
}

function handleLoginSuccess(info: {
  userId: string
  username: string
  fullName: string
  departmentName: string
  storeName: string
  roles: string[]
  level?: UserLevel
  token: string
  refreshToken?: string
  expiresIn?: number
}) {
  employeeStore.setUserInfo({
    userId: info.userId,
    username: info.username,
    fullName: info.fullName,
    departmentName: info.departmentName,
    storeName: info.storeName,
    roles: info.roles as UserRole[],
    // 传递层级信息到 employeeStore（用于后续同步到 permissionStore）
  })
  // 同步设置用户层级到权限Store
  if (info.level) {
    permissionStore.setUserLevel(info.level)
  }
  localStorage.setItem('token', info.token)
  if (info.refreshToken) {
    localStorage.setItem('refreshToken', info.refreshToken)
  }
  if (info.expiresIn) {
    localStorage.setItem('tokenExpiresAt', String(Date.now() + info.expiresIn * 1000))
  }
  localStorage.setItem('login_source', 'password')
  router.replace('/home')
}

/** [M10] 登录页为敏感页面，启用截屏/录屏防护 */
onMounted(() => {
  useScreenSecurity(true)
})

onUnmounted(() => {
  useScreenSecurity(false)
})
</script>

<template>
  <div class="fl-page">
    <!-- 左侧品牌展示区（PC端显示） -->
    <aside class="fl-brand-panel">
      <div class="fl-glow fl-glow--1"></div>
      <div class="fl-glow fl-glow--2"></div>
      <div class="fl-glow fl-glow--3"></div>

      <div class="fl-brand__inner">
        <div class="fl-card-scene">
          <div class="fl-glass-card fl-glass-card--1">
            <div class="glass-icon glass-icon--calendar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="3"/><path d="M16 2v4M8 2v4M3 10h18"/><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01"/></svg>
            </div>
            <span class="glass-label">排班</span>
          </div>

          <div class="fl-glass-card fl-glass-card--2">
            <div class="glass-icon glass-icon--wallet">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="5" width="20" height="14" rx="3"/><path d="M2 10h20"/></svg>
            </div>
            <span class="glass-label">工资</span>
          </div>

          <div class="fl-glass-card fl-glass-card--3">
            <div class="glass-icon glass-icon--team">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="9" cy="7" r="3"/><path d="M3 21v-2a4 4 0 014-4h4"/><circle cx="16" cy="7" r="3"/><path d="M22 21v-2a4 4 0 00-3-3.87"/></svg>
            </div>
            <span class="glass-label">团队</span>
          </div>
        </div>

        <div class="fl-brand-text">
          <h1 class="fl-brand__title">员工自助门户</h1>
          <p class="fl-brand__subtitle">Food Traceability System</p>
        </div>

        <ul class="fl-features">
          <li class="fl-features__item">排班管理</li>
          <li class="fl-features__item">工资查询</li>
          <li class="fl-features__item">请假审批</li>
          <li class="fl-features__item">培训记录</li>
        </ul>
      </div>
    </aside>

    <!-- 右侧表单区 -->
    <main class="fl-form-panel">
      <div class="fl-form-card">
        <div class="fl-mobile-brand">
          <span class="fl-mobile-brand__text">员工自助门户</span>
        </div>

        <div v-if="loading" class="fl-loading-overlay">
          <div class="fl-spinner"></div>
          <p class="fl-loading-text">正在验证...</p>
        </div>

        <template v-if="!loading">
          <header class="fl-card-header">
            <h2 class="fl-card-title">欢迎回来</h2>
            <p class="fl-card-desc">请输入您的账号和密码以继续</p>
          </header>

          <form @submit.prevent="passwordLogin" class="fl-password-form">
            <div :class="['fl-field', { 'fl-field--focused': focusedField.username }, { 'fl-field--filled': !!username }]">
              <label class="fl-field__label" for="fl-username">员工账号</label>
              <div class="fl-field__input-wrap">
                <span class="fl-field__icon">
                  <svg viewBox="0 0 20 20" fill="none"><circle cx="10" cy="7" r="3.5" stroke="currentColor" stroke-width="1.5"/><path d="M4 17c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                </span>
                <input id="fl-username" v-model="username" type="text" autocomplete="username" autocapitalize="off" class="fl-field__input" @focus="focusedField.username = true" @blur="focusedField.username = false"/>
              </div>
            </div>

            <div :class="['fl-field', { 'fl-field--focused': focusedField.password }, { 'fl-field--filled': !!password }]">
              <label class="fl-field__label" for="fl-password">登录密码</label>
              <div class="fl-field__input-wrap">
                <span class="fl-field__icon">
                  <svg viewBox="0 0 20 20" fill="none"><rect x="4" y="9" width="12" height="8" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M7 9V6a3 3 0 116 0v3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="10" cy="13" r="1" fill="currentColor"/></svg>
                </span>
                <input id="fl-password" v-model="password" type="password" autocomplete="current-password" class="fl-field__input" @focus="focusedField.password = true" @blur="focusedField.password = false"/>
              </div>
            </div>

            <button type="submit" class="fl-btn fl-btn--primary" :disabled="!username || !password || loading">
              <span v-if="!loading">登 录</span>
              <span v-else class="fl-btn__loading"><span class="fl-btn__spinner"></span>登录中...</span>
            </button>

            <div class="fl-form-footer">
              <div class="fl-divider"><span class="fl-divider__text">或</span></div>
              <button v-if="isDev" type="button" class="fl-btn fl-btn--demo" @click="demoLogin">演示模式进入</button>
              <button v-if="isDev" type="button" class="fl-btn fl-btn--manager" @click="managerDemoLogin">管理者演示登录</button>
              <button v-if="isDev" type="button" class="fl-btn fl-btn--manager-level" @click="managerLevelDemoLogin">经理演示登录</button>
              <p v-if="isDev" class="fl-demo-hint">无需账号，直接体验系统功能</p>
            </div>
          </form>
        </template>
      </div>

      <footer class="fl-page-footer">
        <p>&copy; 2025 食品溯源系统 &middot; 员工端 v1.0</p>
      </footer>
    </main>
  </div>
</template>

<style scoped lang="scss">
$font-stack: -apple-system, 'SF Pro Display', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
$primary: #3370FF;
$text-primary: #1D2129;
$text-secondary: #4E5969;
$text-muted: #86909C;
$text-placeholder: #C9CDD4;
$border-color: #E5E6EB;
$border-light: #EFF0F4;
$bg-input: #F7F8FA;
$bg-white: #FFFFFF;
$bg-page: #F2F3F5;

.fl-page {
  display: flex;
  min-height: 100vh;
  font-family: $font-stack;
  background: $bg-page;
}

/* ========== 左侧品牌面板 — 玻璃拟态静态版 ========== */
.fl-brand-panel {
  position: relative;
  flex: 0 0 45%;
  max-width: 600px;
  background: linear-gradient(160deg, #0f1021 0%, #1a1b3a 35%, #161830 65%, #0d0e1f 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fl-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(80px);

  &--1 { width: 500px; height: 500px; top: -200px; right: -150px; background: radial-gradient(circle, rgba(51, 112, 255, 0.18) 0%, transparent 70%); }
  &--2 { width: 380px; height: 380px; bottom: -120px; left: -100px; background: radial-gradient(circle, rgba(99, 102, 241, 0.12) 0%, transparent 70%); }
  &--3 { width: 240px; height: 240px; top: 40%; left: 30%; background: radial-gradient(circle, rgba(51, 112, 255, 0.08) 0%, transparent 70%); filter: blur(60px); }
}

.fl-brand__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 40px;
  gap: 36px;
}

/* 玻璃拟态卡片 */
.fl-card-scene {
  position: relative;
  width: 280px;
  height: 220px;
  margin-bottom: 8px;
}

.fl-glass-card {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border-radius: 16px;
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.08);

  animation: glass-float 5s ease-in-out infinite;
  transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-4px) rotate(var(--rot, 0deg));
    border-color: rgba(255, 255, 255, 0.28);
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.22), 0 0 32px rgba(51, 112, 255, 0.08);
    .glass-icon { transform: scale(1.1) rotate(-5deg); }
  }

  &--1 { top: 0; left: 8px; width: 100px; height: 90px; --rot: -4deg; animation-delay: 0s; }
  &--2 { top: 30px; right: 0; width: 110px; height: 95px; --rot: 3deg; animation-delay: -1.6s; }
  &--3 { bottom: 0; left: 50%; --rot: 1deg; transform: translateX(-50%) rotate(1deg); width: 105px; height: 88px; animation-delay: -3.2s;

    &:hover { transform: translateX(-50%) translateY(-4px) rotate(1deg); }
  }
}

@keyframes glass-float {
  0%, 100% { transform: translateY(0) rotate(var(--rot, 0deg)); }
  50% { transform: translateY(-8px) rotate(var(--rot, 0deg)); }
}

.glass-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #FFFFFF;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  svg { width: 20px; height: 20px; }

  &--calendar { background: linear-gradient(135deg, #3370FF, #5B8FFF); }
  &--wallet   { background: linear-gradient(135deg, #00B365, #34D399); }
  &--team     { background: linear-gradient(135deg, #FF9000, #FFB84D); }
}

.glass-label { font-size: 11px; font-weight: 600; color: rgba(255, 255, 255, 0.7); letter-spacing: 0.5px; }

.fl-brand-text { text-align: center; }
.fl-brand__title { font-size: 32px; font-weight: 700; color: #FFFFFF; letter-spacing: 6px; margin: 0 0 8px; text-indent: 6px; line-height: 1.3; }
.fl-brand__subtitle { font-size: 12px; font-weight: 300; color: rgba(255, 255, 255, 0.4); letter-spacing: 3px; text-transform: uppercase; margin: 0; }

.fl-features {
  list-style: none; padding: 0; margin: 0;
  display: flex; flex-wrap: wrap; gap: 8px; justify-content: center;

  &__item {
    padding: 7px 18px;
    background: rgba(255, 255, 255, 0.06);
    backdrop-filter: blur(8px);
    border-radius: 999px;
    border: 1px solid rgba(255, 255, 255, 0.08);
    font-size: 12px;
    color: rgba(255, 255, 255, 0.65);
    letter-spacing: 0.5px;
    transition: all 0.25s ease;

    &:hover { background: rgba(255, 255, 255, 0.12); border-color: rgba(255, 255, 255, 0.16); color: rgba(255, 255, 255, 0.85); }
  }
}

/* ========== 右侧表单区 ========== */
.fl-form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.fl-form-card {
  width: 100%;
  max-width: 400px;
  background: $bg-white;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06), 0 2px 8px rgba(0, 0, 0, 0.03);
  padding: 40px 36px 32px;
  position: relative;
  overflow: hidden;
}

.fl-mobile-brand {
  display: none;
  text-align: center;
  margin-bottom: 28px;
  &__text { display: block; font-size: 22px; font-weight: 700; color: $text-primary; letter-spacing: 4px; }
}

.fl-loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  gap: 20px;
}
.fl-spinner {
  width: 40px; height: 40px;
  border: 3.5px solid $border-light;
  border-top-color: $primary;
  border-radius: 50%;
  animation: fl-spin 0.75s linear infinite;
}
@keyframes fl-spin { to { transform: rotate(360deg); } }
.fl-loading-text { font-size: 14px; color: $text-secondary; margin: 0; }

.fl-card-header { margin-bottom: 32px; }
.fl-card-title { font-size: 24px; font-weight: 700; color: $text-primary; margin: 0 0 8px; letter-spacing: 0.5px; }
.fl-card-desc { font-size: 14px; color: $text-muted; margin: 0; }

/* Float Label 输入框 */
.fl-field { margin-bottom: 24px; position: relative; }

.fl-field__label {
  position: absolute;
  left: 40px;
  top: 14px;
  font-size: 15px;
  color: $text-placeholder;
  pointer-events: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: left center;
}

.fl-field__input-wrap {
  display: flex;
  align-items: center;
  background: $bg-input;
  border: 1.5px solid transparent;
  border-radius: 12px;
  padding: 0 14px;
  height: 52px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.fl-field__icon {
  flex-shrink: 0;
  width: 20px; height: 20px;
  margin-right: 10px;
  color: $text-muted;
  transition: color 0.25s ease;
  svg { width: 100%; height: 100%; }
}

.fl-field__input {
  flex: 1;
  height: 100%;
  background: transparent;
  border: none;
  outline: none;
  font-size: 15px;
  color: $text-primary;
  font-family: $font-stack;
  &::placeholder { color: transparent; }
}

.fl-field--focused .fl-field__input-wrap,
.fl-field--filled .fl-field__input-wrap {
  background: $bg-white;
  border-color: $primary;
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.10);
}

.fl-field--focused .fl-field__icon,
.fl-field--filled .fl-field__icon { color: $primary; }

.fl-field--focused .fl-field__label,
.fl-field--filled .fl-field__label {
  top: -8px; left: 12px;
  font-size: 11px; font-weight: 600; color: $primary;
  background: $bg-white; padding: 0 6px;
}

/* 按钮 */
.fl-btn {
  cursor: pointer;
  border: none;
  outline: none;
  font-family: $font-stack;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.fl-btn--primary {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(90deg, #3370FF 0%, #5B8FFF 100%);
  color: #FFFFFF;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  padding-left: 6px;
  margin-top: 8px;
  box-shadow: 0 4px 14px rgba(51, 112, 255, 0.28);

  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 22px rgba(51, 112, 255, 0.38); }
  &:active:not(:disabled) { transform: scale(0.97); box-shadow: 0 2px 8px rgba(51, 112, 255, 0.22); }
  &:disabled { opacity: 0.45; cursor: not-allowed; box-shadow: none; }
}

.fl-btn__loading { display: inline-flex; align-items: center; gap: 8px; letter-spacing: 2px; }
.fl-btn__spinner {
  display: inline-block;
  width: 16px; height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #FFF;
  border-radius: 50%;
  animation: fl-spin 0.65s linear infinite;
}

.fl-btn--demo {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  background: transparent;
  border: 1.5px solid $border-color;
  color: $text-secondary;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  letter-spacing: 1px;
  transition: all 0.2s ease;

  &:hover { border-color: $primary; color: $primary; background: #EEF3FE; }
  &:active { transform: scale(0.98); }
}

.fl-btn--manager {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(90deg, #3370FF 0%, #5B8FFF 100%);
  border: none;
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  letter-spacing: 1px;
  transition: all 0.2s ease;

  &:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(51, 112, 255, 0.28); }
  &:active { transform: scale(0.98); }
}

.fl-btn--manager-level {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(90deg, #00B365 0%, #34D399 100%);
  border: none;
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  letter-spacing: 1px;
  transition: all 0.2s ease;

  &:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(0, 179, 101, 0.28); }
  &:active { transform: scale(0.98); }
}

.fl-form-footer { margin-top: 20px; text-align: center; }
.fl-divider { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.fl-divider::before, .fl-divider::after { content: ''; flex: 1; height: 1px; background: $border-light; }
.fl-divider__text { font-size: 12px; color: $text-placeholder; white-space: nowrap; }
.fl-demo-hint { font-size: 11px; color: $text-placeholder; margin: 10px 0 0; }

.fl-page-footer { margin-top: 32px; text-align: center; p { font-size: 12px; color: $text-placeholder; margin: 0; } }

@media (max-width: 767px) {
  .fl-page {
    flex-direction: column;
    background: var(--fts-bg-page);
  }

  /* 移动端：完全隐藏左侧品牌面板 */
  .fl-brand-panel { display: none !important; }

  /* 移动端表单区 — 全屏 App 风格 */
  .fl-form-panel {
    flex: 1;
    padding: 0 20px;
    justify-content: flex-start;
    padding-top: calc(env(safe-area-inset-top, 0px) + 48px); /* 为状态栏+导航预留 */
    padding-bottom: env(safe-area-inset-bottom, 0px);
  }

  .fl-form-card {
    max-width: 100%;
    border-radius: var(--fts-radius-xl, 16px);
    box-shadow: none;
    background: transparent;
    padding: 0;
  }

  /* 移动端品牌标题 — 更紧凑 */
  .fl-mobile-brand {
    display: block;
    margin-bottom: 32px;
    margin-top: 8px;

    &__text {
      font-size: 22px;
      font-weight: 700;
      color: var(--fts-text-primary);
      letter-spacing: 2px;
    }
  }

  .fl-card-header { margin-bottom: 24px; }
  .fl-card-title { font-size: 21px; letter-spacing: 0; }
  .fl-card-desc { font-size: 13px; }

  /* 输入框 — 全宽，更大触控区 */
  .fl-field { margin-bottom: 20px; }

  .fl-field__input-wrap {
    height: 48px;
    border-radius: var(--fts-radius-md, 12px);
  }

  .fl-field__icon { width: 18px; height: 18px; margin-right: 10px; }

  .fl-field__input { font-size: 16px; } /* iOS 防止自动缩放 */

  /* 登录按钮 — 全宽 */
  .fl-btn--primary {
    height: 48px;
    border-radius: var(--fts-radius-md, 12px);
    font-size: 16px;
    letter-spacing: 4px;
    margin-top: 4px;
  }

  .fl-form-footer { margin-top: 24px; }

  .fl-page-footer { display: none; } /* 移动端隐藏版权信息 */
}

@media (min-width: 1400px) {
  .fl-brand-panel { flex: 0 0 50%; max-width: 720px; }
  .fl-form-card { max-width: 420px; }
}

// ================================================================
//  深色模式适配（登录页 — 表单区域）
//  品牌区保持不变（本身已是深色设计）
// ================================================================
@media (prefers-color-scheme: dark) {
  .fl-page { background: #000000; }

  // 右侧表单区
  .fl-form-panel { background: var(--fts-bg-page, #0A0A0C); }
  .fl-form-card {
    background: var(--fts-bg-card, #1C1C1E);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  // 移动端品牌文字
  .fl-mobile-brand__text { color: var(--fts-text-primary, #F5F5F7); }

  // 加载状态
  .fl-loading-text { color: var(--fts-text-secondary, #EBEBF0); }
  .fl-spinner { border-color: var(--fts-border-secondary, rgba(255,255,255,0.08)); border-top-color: var(--fts-primary, #0A84FF); }

  // 卡片头部
  .fl-card-title { color: var(--fts-text-primary, #F5F5F7); }
  .fl-card-desc { color: var(--fts-text-tertiary, #B0B0B5); }

  // Float Label 输入框
  .fl-field__label { color: var(--fts-text-placeholder, #48484A); }
  .fl-field__input-wrap { background: var(--fts-bg-tertiary, #2C2C2E); }
  .fl-field__icon { color: var(--fts-text-quaternary, #8E8E93); }
  .fl-field__input { color: var(--fts-text-primary, #F5F5F7); }

  .fl-field--focused .fl-field__input-wrap,
  .fl-field--filled .fl-field__input-wrap {
    background: var(--fts-bg-secondary, #121214);
    border-color: var(--fts-primary, #0A84FF);
    box-shadow: 0 0 0 3px rgba(10, 132, 255, 0.12);
  }
  .fl-field--focused .fl-field__icon,
  .fl-field--filled .fl-field__icon { color: var(--fts-primary, #0A84FF); }
  .fl-field--focused .fl-field__label,
  .fl-field--filled .fl-field__label {
    color: var(--fts-primary, #0A84FF);
    background: var(--fts-bg-secondary, #121214);
  }

  // 按钮
  .fl-btn--primary {
    background: linear-gradient(90deg, #0A84FF 0%, #409CFF 100%);
    box-shadow: 0 4px 14px rgba(10, 132, 255, 0.35);
    &:hover:not(:disabled) { box-shadow: 0 6px 22px rgba(10, 132, 255, 0.45); }
  }
  .fl-btn--demo {
    border-color: var(--fts-border-primary, rgba(255,255,255,0.12));
    color: var(--fts-text-secondary, #EBEBF0);
    &:hover { border-color: var(--fts-primary, #0A84FF); color: var(--fts-primary, #0A84FF); background: rgba(10, 132, 255, 0.08); }
  }
  .fl-btn--manager {
    background: linear-gradient(90deg, #0A84FF 0%, #409CFF 100%);
    &:hover { box-shadow: 0 4px 14px rgba(10, 132, 255, 0.35); }
  }
  .fl-btn--manager-level {
    background: linear-gradient(90deg, #30D158 0%, #34D399 100%);
    &:hover { box-shadow: 0 4px 14px rgba(48, 209, 88, 0.35); }
  }

  // 分割线和提示文字
  .fl-divider::before, .fl-divider::after { background: var(--fts-border-secondary, rgba(255,255,255,0.08)); }
  .fl-divider__text { color: var(--fts-text-placeholder, #48484A); }
  .fl-demo-hint { color: var(--fts-text-placeholder, #48484A); }
  .fl-page-footer p { color: var(--fts-text-placeholder, #48484A); }

  // spinner 加载动画
  .fl-btn__spinner { border-color: rgba(255,255,255,0.25); border-top-color: #FFF; }
}
</style>
