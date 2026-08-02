<script setup lang="ts">
/**
 * 精美动画登录页组件
 * 基于旧frontend设计重构，适配新frontend架构
 *
 * 核心特性：
 * - 浮动形状背景动画（4个半透明圆形缓慢浮动）
 * - 卡片式布局（900x600px圆角卡片）
 * - 登录/注册切换动画（overlay覆盖层滑动过渡）
 * - 浮动标签输入框（Material Design风格）
 * - 渐变按钮 + hover效果
 * - Canvas验证码绘制
 * - 登录页独立浅色主题（不受全局深色模式影响）
 */
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  User,
  UserFilled,
  Lock,
  Message,
  Phone,
  Ticket,
  Search,
  CircleCheckFilled,
  CircleCloseFilled,
  Key,
  Loading,
} from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'
import { authApi } from '@/api/auth'
import type { LoginResult } from '@/api/auth'

// ========== 路由和Store ==========
const router = useRouter()
const route = useRoute()
const permissionStore = usePermissionStore()

// ========== 动画状态机 ==========
/**
 * 动画状态类型定义
 * login → cover-expand → cover-shrink → register (登录→注册，1400ms)
 * register → uncover-expand → uncover-shrink → (注册→登录，1400ms)
 */
type AnimState =
  | 'login'
  | 'cover-expand'
  | 'cover-shrink'
  | 'register'
  | 'uncover-expand'
  | 'uncover-shrink'

const animState = ref<AnimState>('login')

// ========== 表单数据 ==========
const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaId: '',
  rememberMe: false,
})

const registerForm = reactive({
  username: '',
  name: '',
  email: '',
  phone: '',
  registrationCode: '',
  password: '',
  confirmPassword: '',
  captcha: '',
  captchaId: '',
})

// ========== 状态管理 ==========
const loading = ref(false)
const registerLoading = ref(false)
const verifyingCode = ref(false)
const codeVerified = ref(false)
const codeInvalid = ref(false)
const verifiedName = ref('')

/** 开发环境自动登录中状态（临时，测试完成后移除） */
const devAutoLoggingIn = ref(false)

// 输入框聚焦状态（用于浮动标签）
const loginUsernameFocused = ref(false)
const loginPasswordFocused = ref(false)
const regCodeFocused = ref(false)
const regUsernameFocused = ref(false)
const regNameFocused = ref(false)
const regPhoneFocused = ref(false)
const regEmailFocused = ref(false)
const regPasswordFocused = ref(false)
const regConfirmPasswordFocused = ref(false)

// 表单引用
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// 验证码相关（统一使用后端验证码）
const captchaImage = ref('')
const captchaLoading = ref(false)
/** 是否需要验证码（失败次数≥3时后端返回461触发） */
const requireCaptcha = ref(false)

// 注册页验证码（注册必须输入验证码）
const registerCaptchaImage = ref('')
const registerCaptchaLoading = ref(false)

// ========== 动画状态计算属性 ==========

/** overlay覆盖层样式类 */
const overlayClass = computed(() => {
  if (animState.value === 'cover-expand') return 'expand-from-left'
  if (animState.value === 'cover-shrink') return 'shrink-to-right'
  if (animState.value === 'uncover-expand') return 'expand-from-right'
  if (animState.value === 'uncover-shrink') return 'shrink-to-left'
  if (animState.value === 'register') return 'position-right'
  return 'position-left'
})

/** 登录页面可见性 */
const loginPageClass = computed(() => {
  if (animState.value === 'register') return 'hidden'
  return ''
})

/** 注册页面可见性 */
const registerPageClass = computed(() => {
  if (animState.value === 'login') return 'hidden'
  return ''
})

/** 登录页欢迎面板动画类 */
const loginWelcomeClass = computed(() => {
  if (animState.value === 'cover-expand') return 'slide-out-left'
  if (animState.value === 'cover-shrink') return 'hidden'
  if (animState.value === 'register') return 'hidden'
  if (animState.value === 'uncover-expand') return 'prepare-left'
  if (animState.value === 'uncover-shrink') return 'slide-in-from-left'
  return ''
})

/** 注册页欢迎面板动画类 */
const registerWelcomeClass = computed(() => {
  if (animState.value === 'uncover-expand') return 'slide-out-right'
  if (animState.value === 'uncover-shrink') return 'hidden'
  if (animState.value === 'login') return 'hidden'
  if (animState.value === 'cover-expand') return 'prepare-right'
  if (animState.value === 'cover-shrink') return 'slide-in-from-right'
  if (animState.value === 'register') return ''
  return 'hidden'
})

/** 登录页表单面板动画类 */
const loginFormClass = computed(() => {
  if (animState.value === 'cover-expand') return 'slide-out-right'
  if (animState.value === 'cover-shrink') return 'hidden'
  if (animState.value === 'register') return 'hidden'
  if (animState.value === 'uncover-expand') return 'prepare-right'
  if (animState.value === 'uncover-shrink') return 'slide-in-from-right'
  return ''
})

/** 注册页表单面板动画类 */
const registerFormClass = computed(() => {
  if (animState.value === 'uncover-expand') return 'slide-out-left'
  if (animState.value === 'uncover-shrink') return 'hidden'
  if (animState.value === 'login') return 'hidden'
  if (animState.value === 'cover-expand') return 'prepare-left'
  if (animState.value === 'cover-shrink') return 'slide-in-from-left'
  if (animState.value === 'register') return ''
  return 'hidden'
})

// ========== 表单校验规则 ==========

/** 确认密码校验器 */
const validateConfirmPassword = (
  rule: unknown,
  value: string,
  callback: (error?: Error) => void,
): void => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/** 登录表单校验规则（验证码规则按需启用） */
const loginRules = computed<FormRules>(() => {
  const base: FormRules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 32, message: '密码长度为 6 到 32 个字符', trigger: 'blur' },
    ],
  }
  // 仅在后端要求验证码时启用验证码校验
  if (requireCaptcha.value) {
    base.captcha = [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { min: 4, max: 6, message: '验证码长度不正确', trigger: 'blur' },
    ]
  }
  return base
})

/** 注册表单校验规则 */
const registerRules: FormRules = {
  registrationCode: [{ required: true, message: '请输入邀请码', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名长度不能超过50个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度必须在8-20个字符之间', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/, message: '密码必须包含至少一个字母和一个数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度不正确', trigger: 'blur' },
  ],
}

// ========== 页面切换动画 ==========

/** 切换到注册页面（触发overlay从左展开动画） */
const goToRegister = (): void => {
  animState.value = 'cover-expand'
  setTimeout(() => {
    animState.value = 'cover-shrink'
  }, 700)
  setTimeout(() => {
    animState.value = 'register'
    // 切换到注册页后获取验证码
    fetchRegisterCaptcha()
  }, 1400)
}

/** 切换到登录页面（触发overlay从右收缩动画） */
const goToLogin = (): void => {
  animState.value = 'uncover-expand'
  setTimeout(() => {
    animState.value = 'uncover-shrink'
  }, 700)
  setTimeout(() => {
    animState.value = 'login'
  }, 1400)
}

// ========== 验证码功能（统一使用后端验证码） ==========

/**
 * 从后端获取验证码图片
 * 后端返回Base64编码的验证码图片和captchaId
 */
async function fetchCaptcha(): Promise<void> {
  captchaLoading.value = true
  try {
    const result = await authApi.getCaptcha()
    if (result?.captchaId && result?.captchaImage) {
      loginForm.captchaId = result.captchaId
      captchaImage.value = result.captchaImage
      loginForm.captcha = ''
    }
  } catch (error) {
    console.error('[Login] 获取验证码失败:', error)
  } finally {
    captchaLoading.value = false
  }
}

/** 点击验证码刷新 */
function refreshCaptcha(): void {
  fetchCaptcha()
}

/**
 * 从后端获取注册验证码图片
 * 注册必须输入验证码，与登录验证码独立
 */
async function fetchRegisterCaptcha(): Promise<void> {
  registerCaptchaLoading.value = true
  try {
    const result = await authApi.getCaptcha()
    if (result?.captchaId && result?.captchaImage) {
      registerForm.captchaId = result.captchaId
      registerCaptchaImage.value = result.captchaImage
      registerForm.captcha = ''
    }
  } catch (error) {
    console.error('[Register] 获取验证码失败:', error)
  } finally {
    registerCaptchaLoading.value = false
  }
}

/** 点击注册验证码刷新 */
function refreshRegisterCaptcha(): void {
  fetchRegisterCaptcha()
}

// ========== 邀请码验证 ==========

/**
 * 用户修改邀请码输入时重置校验状态
 * 避免显示陈旧的"有效/无效"提示，要求重新点击验证按钮
 */
const resetCodeVerification = (): void => {
  if (codeVerified.value || codeInvalid.value) {
    codeVerified.value = false
    codeInvalid.value = false
    verifiedName.value = ''
  }
}

/**
 * 验证邀请码（调用后端真实校验接口）
 * 后端会校验邀请码是否存在、未使用、未过期
 * 校验通过显示绿色提示并允许继续注册，校验失败显示红色提示
 */
const verifyInvitationCode = async (): Promise<void> => {
  if (!registerForm.registrationCode) {
    ElMessage.warning('请输入邀请码')
    return
  }

  try {
    verifyingCode.value = true
    // 调用后端真实校验接口（公开接口，无需认证）
    const result = await authApi.validateInvitationCode(registerForm.registrationCode.trim())

    if (result && result.valid) {
      codeVerified.value = true
      codeInvalid.value = false
      verifiedName.value = result.boundName || '新用户'
      ElMessage.success('邀请码验证成功')
    } else {
      codeVerified.value = false
      codeInvalid.value = true
      verifiedName.value = ''
      ElMessage.error(result?.message || '邀请码无效或已被使用')
    }
  } catch (error: unknown) {
    codeVerified.value = false
    codeInvalid.value = true
    verifiedName.value = ''
    const errorMessage = error instanceof Error ? error.message : '邀请码验证失败'
    ElMessage.error(errorMessage)
  } finally {
    verifyingCode.value = false
  }
}

// ========== 登录处理 ==========

/** 提交登录（调用后端真实API获取合法JWT Token） */
const handleLogin = async (): Promise<void> => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  // 当后端要求验证码时，前端校验非空（实际校验由后端完成）
  if (requireCaptcha.value && !loginForm.captcha) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true

  try {
    // 调用后端真实登录API（携带验证码字段）
    const response = await authApi.login({
      username: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe,
      captcha: requireCaptcha.value ? loginForm.captcha : undefined,
      captchaId: requireCaptcha.value ? loginForm.captchaId : undefined,
    })

    // 处理461响应：后端要求验证码（失败次数≥3）
    // 响应拦截器已将461的data返回（非reject），此处通过requireCaptcha字段识别
    if (response && (response as LoginResult).requireCaptcha) {
      requireCaptcha.value = true
      loginForm.captchaId = (response as LoginResult).captchaId || loginForm.captchaId
      captchaImage.value = (response as LoginResult).captchaImage || captchaImage.value
      loginForm.captcha = ''
      ElMessage.warning('登录失败次数过多，请输入验证码')
      loading.value = false
      return
    }

    // 存储后端签发的合法JWT Token
    localStorage.setItem('token', response.token)
    if (response.refreshToken) {
      localStorage.setItem('refresh_token', response.refreshToken)
    }
    localStorage.setItem('user_id', response.userInfo.id)
    localStorage.setItem('username', response.userInfo.username)

    // 处理"记住我"功能
    if (loginForm.rememberMe) {
      localStorage.setItem('rememberedUsername', loginForm.username)
    } else {
      localStorage.removeItem('rememberedUsername')
    }

    // 初始化用户信息
    permissionStore.initFromToken()

    // 清除上次会话残留的标签页
    localStorage.removeItem('tab-bar-tabs')

    ElMessage.success('登录成功')

    // 跳转到重定向地址或首页
    const redirect = (route.query.redirect as string) || '/home'
    await nextTick()
    router.push(redirect)
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '登录失败，请重试'
    ElMessage.error(errorMessage)
    // 登录失败后刷新验证码（如果已在验证码模式）
    if (requireCaptcha.value) {
      fetchCaptcha()
    }
  } finally {
    loading.value = false
  }
}

// ========== 注册处理 ==========

/** 提交注册（调用后端真实API） */
const handleRegister = async (): Promise<void> => {
  if (!registerFormRef.value) return

  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }

  if (!codeVerified.value) {
    ElMessage.warning('请先验证邀请码')
    return
  }

  registerLoading.value = true

  try {
    // 调用后端真实注册API（邀请制）
    await authApi.register({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      fullName: registerForm.name,
      email: registerForm.email,
      phone: registerForm.phone,
      invitationCode: registerForm.registrationCode,
      captcha: registerForm.captcha,
      captchaId: registerForm.captchaId,
    })

    ElMessage.success('注册成功，请登录')
    goToLogin()
    // 重置注册表单
    codeVerified.value = false
    codeInvalid.value = false
    verifiedName.value = ''
    registerForm.username = ''
    registerForm.name = ''
    registerForm.email = ''
    registerForm.phone = ''
    registerForm.registrationCode = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.captcha = ''
    registerForm.captchaId = ''
    registerCaptchaImage.value = ''
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '注册失败'
    ElMessage.error(errorMessage)
    // 注册失败后刷新验证码
    fetchRegisterCaptcha()
  } finally {
    registerLoading.value = false
  }
}

// ========== 开发环境临时自动登录 ==========

/**
 * 开发环境自动登录（临时，测试完成后移除）
 * 使用 admin / admin123 自动换取 token，避免每次手动输入。
 * 仅当 Vite 开发模式且未禁用自动登录时触发。
 */
async function tryDevAutoLogin(): Promise<boolean> {
  devAutoLoggingIn.value = true
  try {
    const response = await authApi.login({
      username: 'admin',
      password: 'admin123',
      rememberMe: true,
    })

    // 后端要求验证码时回退到手动登录
    if (response && (response as LoginResult).requireCaptcha) {
      ElMessage.info('开发自动登录需要验证码，请手动登录')
      return false
    }

    localStorage.setItem('token', response.token)
    if (response.refreshToken) {
      localStorage.setItem('refresh_token', response.refreshToken)
    }
    localStorage.setItem('user_id', response.userInfo.id)
    localStorage.setItem('username', response.userInfo.username)

    permissionStore.initFromToken()
    localStorage.removeItem('tab-bar-tabs')

    ElMessage.success('开发环境已自动登录')
    await nextTick()
    router.push('/home')
    return true
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '自动登录失败'
    ElMessage.warning(`开发自动登录失败：${message}，请手动登录`)
    return false
  } finally {
    devAutoLoggingIn.value = false
  }
}

// ========== 忘记密码 ==========

/** 跳转到忘记密码页面 */
const handleForgotPassword = (): void => {
  router.push('/forgot-password')
}

// ========== 初始化 ==========

onMounted(async () => {
  // 清除残留的认证状态（过期 token 会导致 401 干扰）
  localStorage.removeItem('token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('user_id')
  localStorage.removeItem('username')

  // 添加特殊class防止主题样式干扰
  document.body.classList.add('login-page-body')

  // TODO: 开发调试阶段关闭自动登录，便于从前端验证登录流程；前端/后端验收完成后恢复
  // if (import.meta.env.DEV) {
  //   const autoLoginSuccess = await tryDevAutoLogin()
  //   if (autoLoginSuccess) return
  // }

  // TODO: 开发调试阶段临时禁用登录验证码显示，仅当后端明确要求时再展示；验收完成后恢复预加载
  // fetchCaptcha()
  // requireCaptcha.value = true

  // 恢复记住的用户名
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    loginForm.username = rememberedUsername
    loginForm.rememberMe = true
  }
})

/** 组件卸载时清理body class，防止影响其他页面 */
onUnmounted(() => {
  document.body.classList.remove('login-page-body')
})
</script>

<template>
  <div class="auth-container">
    <!-- 浮动形状背景（4个半透明圆形） -->
    <div class="floating-shapes">
      <div class="shape shape-1" />
      <div class="shape shape-2" />
      <div class="shape shape-3" />
      <div class="shape shape-4" />
    </div>

    <!-- 主卡片容器 -->
    <div class="auth-card">
      <!-- Overlay覆盖层（用于切换动画） -->
      <div class="overlay" :class="overlayClass"></div>

      <!-- ==================== 登录页面 ==================== -->
      <div class="page login-page" :class="loginPageClass">
        <!-- 左侧欢迎面板 -->
        <div class="welcome-panel" :class="loginWelcomeClass">
          <div class="welcome-content">
            <h2>欢迎回来</h2>
            <p>登录您的账户，继续使用系统</p>
            <div class="divider" />
            <p class="hint">还没有账户？</p>
            <button class="toggle-btn" @click="goToRegister">
              <span>立即注册</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 右侧表单面板 -->
        <div class="form-panel" :class="loginFormClass">
          <div class="form-content">
            <!-- Logo图标 -->
            <div class="logo">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="12" cy="8" r="4" fill="currentColor" />
                <path
                  d="M6 21V19C6 17.9391 6.42143 16.9217 7.17157 16.1716C7.92172 15.4214 8.93913 15 10 15H14C15.0609 15 16.0783 15.4214 16.8284 16.1716C17.5786 16.9217 18 17.9391 18 19V21"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>

            <h1>用户登录</h1>
            <p class="subtitle">欢迎回来，请登录您的账户</p>

            <!-- 登录表单 -->
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="form">
              <!-- 用户名（浮动标签） -->
              <el-form-item prop="username">
                <div
                  class="float-label"
                  :class="{
                    'has-value': loginForm.username,
                    'has-prefix': true,
                    focused: loginUsernameFocused,
                  }"
                >
                  <span class="float-label-text">用户名</span>
                  <el-input
                    v-model="loginForm.username"
                    :prefix-icon="User"
                    size="large"
                    @focus="loginUsernameFocused = true"
                    @blur="loginUsernameFocused = false"
                  />
                </div>
              </el-form-item>

              <!-- 密码（浮动标签） -->
              <el-form-item prop="password">
                <div
                  class="float-label"
                  :class="{
                    'has-value': loginForm.password,
                    'has-prefix': true,
                    focused: loginPasswordFocused,
                  }"
                >
                  <span class="float-label-text">密码</span>
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    :prefix-icon="Lock"
                    show-password
                    size="large"
                    @focus="loginPasswordFocused = true"
                    @blur="loginPasswordFocused = false"
                    @keyup.enter="handleLogin"
                  />
                </div>
              </el-form-item>

              <!-- 验证码（仅在后端要求时显示，失败次数≥3触发） -->
              <el-form-item v-if="requireCaptcha" prop="captcha">
                <div class="captcha-row">
                  <div class="captcha-input float-label" :class="{ 'has-value': loginForm.captcha, 'has-prefix': true }">
                    <span class="float-label-text">验证码</span>
                    <el-input
                      v-model="loginForm.captcha"
                      :prefix-icon="Key"
                      size="large"
                      maxlength="6"
                      placeholder="请输入图中字符"
                      @keyup.enter="handleLogin"
                    />
                  </div>
                  <img
                    v-if="captchaImage"
                    :src="captchaImage"
                    class="captcha-canvas"
                    alt="验证码"
                    title="点击刷新验证码"
                    @click="refreshCaptcha"
                  />
                  <div v-else class="captcha-loading">
                    <el-icon class="is-loading"><Loading /></el-icon>
                  </div>
                </div>
              </el-form-item>

              <!-- 记住我 & 忘记密码 -->
              <el-form-item class="form-options">
                <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
                <el-link type="primary" @click="handleForgotPassword">忘记密码</el-link>
              </el-form-item>

              <!-- 登录按钮 -->
              <el-form-item>
                <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
                  登录
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <!-- ==================== 注册页面 ==================== -->
      <div class="page register-page" :class="registerPageClass">
        <!-- 左侧表单面板 -->
        <div class="form-panel" :class="registerFormClass">
          <div class="form-content">
            <!-- Logo图标 -->
            <div class="logo">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="12" cy="8" r="4" fill="currentColor" />
                <path
                  d="M6 21V19C6 17.9391 6.42143 16.9217 7.17157 16.1716C7.92172 15.4214 8.93913 15 10 15H14C15.0609 15 16.0783 15.4214 16.8284 16.1716C17.5786 16.9217 18 17.9391 18 19V21"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>

            <h1>用户注册</h1>
            <p class="subtitle">创建您的账户，开始使用系统</p>

            <!-- 注册表单 -->
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="form">
              <!-- 邀请码 -->
              <el-form-item prop="registrationCode">
                <div
                  class="float-label has-append"
                  :class="{
                    'has-value': registerForm.registrationCode,
                    'has-prefix': true,
                    focused: regCodeFocused,
                  }"
                >
                  <span class="float-label-text">邀请码</span>
                  <el-input
                    v-model="registerForm.registrationCode"
                    :prefix-icon="Ticket"
                    size="large"
                    @focus="regCodeFocused = true"
                    @blur="regCodeFocused = false"
                    @input="resetCodeVerification"
                  >
                    <template #append>
                      <el-button :icon="Search" @click="verifyInvitationCode" :loading="verifyingCode">
                        验证
                      </el-button>
                    </template>
                  </el-input>
                </div>
                <!-- 验证成功提示 -->
                <div v-if="codeVerified" class="code-hint">
                  <el-icon><CircleCheckFilled /></el-icon>
                  <span>邀请码有效，欢迎{{ verifiedName }}</span>
                </div>
                <!-- 验证失败提示 -->
                <div v-if="codeInvalid" class="code-hint code-hint--error">
                  <el-icon><CircleCloseFilled /></el-icon>
                  <span>邀请码无效或已被使用</span>
                </div>
              </el-form-item>

              <!-- 用户名 & 姓名（两列） -->
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item prop="username">
                    <div
                      class="float-label"
                      :class="{
                        'has-value': registerForm.username,
                        'has-prefix': true,
                        focused: regUsernameFocused,
                      }"
                    >
                      <span class="float-label-text">用户名</span>
                      <el-input
                        v-model="registerForm.username"
                        :prefix-icon="User"
                        size="large"
                        :disabled="!codeVerified"
                        @focus="regUsernameFocused = true"
                        @blur="regUsernameFocused = false"
                      />
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item prop="name">
                    <div
                      class="float-label"
                      :class="{
                        'has-value': registerForm.name,
                        'has-prefix': true,
                        focused: regNameFocused,
                      }"
                    >
                      <span class="float-label-text">姓名</span>
                      <el-input
                        v-model="registerForm.name"
                        :prefix-icon="UserFilled"
                        size="large"
                        :disabled="!codeVerified"
                        @focus="regNameFocused = true"
                        @blur="regNameFocused = false"
                      />
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- 手机号 & 邮箱（两列） -->
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item prop="phone">
                    <div
                      class="float-label"
                      :class="{
                        'has-value': registerForm.phone,
                        'has-prefix': true,
                        focused: regPhoneFocused,
                      }"
                    >
                      <span class="float-label-text">手机号</span>
                      <el-input
                        v-model="registerForm.phone"
                        :prefix-icon="Phone"
                        size="large"
                        :disabled="!codeVerified"
                        @focus="regPhoneFocused = true"
                        @blur="regPhoneFocused = false"
                      />
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item prop="email">
                    <div
                      class="float-label"
                      :class="{
                        'has-value': registerForm.email,
                        'has-prefix': true,
                        focused: regEmailFocused,
                      }"
                    >
                      <span class="float-label-text">邮箱</span>
                      <el-input
                        v-model="registerForm.email"
                        :prefix-icon="Message"
                        size="large"
                        :disabled="!codeVerified"
                        @focus="regEmailFocused = true"
                        @blur="regEmailFocused = false"
                      />
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- 密码 & 确认密码（两列） -->
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item prop="password">
                    <div
                      class="float-label"
                      :class="{
                        'has-value': registerForm.password,
                        'has-prefix': true,
                        focused: regPasswordFocused,
                      }"
                    >
                      <span class="float-label-text">密码</span>
                      <el-input
                        v-model="registerForm.password"
                        type="password"
                        :prefix-icon="Lock"
                        show-password
                        size="large"
                        :disabled="!codeVerified"
                        @focus="regPasswordFocused = true"
                        @blur="regPasswordFocused = false"
                      />
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item prop="confirmPassword">
                    <div
                      class="float-label"
                      :class="{
                        'has-value': registerForm.confirmPassword,
                        'has-prefix': true,
                        focused: regConfirmPasswordFocused,
                      }"
                    >
                      <span class="float-label-text">确认密码</span>
                      <el-input
                        v-model="registerForm.confirmPassword"
                        type="password"
                        :prefix-icon="Lock"
                        show-password
                        size="large"
                        :disabled="!codeVerified"
                        @keyup.enter="handleRegister"
                        @focus="regConfirmPasswordFocused = true"
                        @blur="regConfirmPasswordFocused = false"
                      />
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- 验证码 -->
              <el-form-item prop="captcha">
                <div class="captcha-row">
                  <div
                    class="captcha-input float-label"
                    :class="{
                      'has-value': registerForm.captcha,
                      'has-prefix': true,
                    }"
                  >
                    <span class="float-label-text">验证码</span>
                    <el-input
                      v-model="registerForm.captcha"
                      :prefix-icon="Key"
                      size="large"
                      maxlength="6"
                      placeholder="请输入图中字符"
                      :disabled="!codeVerified"
                      @keyup.enter="handleRegister"
                    />
                  </div>
                  <img
                    v-if="registerCaptchaImage"
                    :src="registerCaptchaImage"
                    class="captcha-canvas"
                    alt="验证码"
                    title="点击刷新验证码"
                    @click="refreshRegisterCaptcha"
                  />
                  <div v-else class="captcha-loading">
                    <el-icon class="is-loading"><Loading /></el-icon>
                  </div>
                </div>
              </el-form-item>

              <!-- 注册按钮 -->
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="submit-btn"
                  :loading="registerLoading"
                  :disabled="!codeVerified"
                  @click="handleRegister"
                >
                  注册
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 右侧欢迎面板 -->
        <div class="welcome-panel" :class="registerWelcomeClass">
          <div class="welcome-content">
            <h2>加入我们</h2>
            <p>创建账户，开启全新体验</p>
            <div class="divider" />
            <p class="hint">已有账户？</p>
            <button class="toggle-btn" @click="goToLogin">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              <span>立即登录</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// ========== 容器布局 ==========
// 登录页固定为浅色主题，不受全局深色模式影响
// 通过在 .auth-container 作用域内重新定义 CSS 变量为浅色主题固定值
// 内部所有使用这些变量的元素（文字、输入框、边框等）都会保持浅色主题的视觉效果
.auth-container {
  // 强制浅色主题变量（覆盖全局深色模式）
  --fts-text-primary: #1f2937;
  --fts-text-secondary: #4b5563;
  --fts-text-tertiary: #9ca3af;
  --fts-text-white: #ffffff;
  --fts-border-primary: #e5e7eb;
  --fts-border-secondary: #f3f4f6;
  --fts-bg-page: #f5f7fa;
  --fts-primary: #1a5fb4;
  --fts-primary-active: #104a8e;
  --fts-primary-light: #e8f0fe;
  --fts-success: #34a853;
  --fts-error: #ea4335;

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    var(--fts-bg-dark, #1a1a2e) 0%,
    var(--fts-primary-active, #16213e) 50%,
    var(--fts-primary, #0f3460) 100%
  );
  padding: 20px;
  position: relative;
  overflow: hidden;
}

// ========== 浮动形状背景动画 ==========
.floating-shapes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;

  .shape {
    position: absolute;
    border-radius: 50%;
    background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.1) 0%,
      rgba(255, 255, 255, 0.05) 100%
    );
    animation: float 20s infinite ease-in-out;
  }

  .shape-1 {
    width: 300px;
    height: 300px;
    top: -100px;
    left: -100px;
  }

  .shape-2 {
    width: 200px;
    height: 200px;
    top: 50%;
    right: -50px;
    animation-delay: -5s;
  }

  .shape-3 {
    width: 150px;
    height: 150px;
    bottom: -50px;
    left: 30%;
    animation-delay: -10s;
  }

  .shape-4 {
    width: 100px;
    height: 100px;
    top: 20%;
    left: 60%;
    animation-delay: -15s;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  25% {
    transform: translateY(-20px) rotate(5deg);
  }
  50% {
    transform: translateY(0) rotate(0deg);
  }
  75% {
    transform: translateY(20px) rotate(-5deg);
  }
}

// ========== 主卡片容器 ==========
.auth-card {
  width: 900px;
  max-width: 100%;
  height: 600px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.35);
  position: relative;
  overflow: hidden;
  z-index: 1;
}

// ========== Overlay覆盖层（切换动画核心） ==========
.overlay {
  position: absolute;
  top: 0;
  height: 100%;
  background: linear-gradient(
    135deg,
    var(--fts-bg-dark, #1a1a2e) 0%,
    var(--fts-primary, #0f3460) 100%
  );
  z-index: 5;
  pointer-events: none;
  transition:
    left 0.6s cubic-bezier(0.645, 0.045, 0.355, 1),
    width 0.6s cubic-bezier(0.645, 0.045, 0.355, 1);

  &.position-left {
    left: 0;
    width: 50%;
  }

  &.position-right {
    left: 50%;
    width: 50%;
  }

  &.expand-from-left {
    left: 0;
    width: 100%;
  }

  &.shrink-to-right {
    left: 50%;
    width: 50%;
  }

  &.expand-from-right {
    left: 0;
    width: 100%;
  }

  &.shrink-to-left {
    left: 0;
    width: 50%;
  }
}

// ========== 页面基础样式 ==========
.page {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;

  &.hidden {
    opacity: 0;
    pointer-events: none;
    z-index: 1;
  }
}

.login-page {
  z-index: 15;

  .welcome-panel {
    width: 50%;
    background: transparent;
  }

  .form-panel {
    width: 50%;
    background: #ffffff;
  }
}

.register-page {
  z-index: 12;

  .form-panel {
    width: 50%;
    background: #ffffff;
  }

  .welcome-panel {
    width: 50%;
    background: transparent;
  }
}

// ========== 欢迎面板（深色区域） ==========
.welcome-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  pointer-events: auto;

  &.hidden {
    opacity: 0;
    pointer-events: none;
  }

  &.prepare-left {
    transform: translateX(-100%);
    opacity: 0;
  }

  &.prepare-right {
    transform: translateX(100%);
    opacity: 0;
  }

  &.slide-out-right {
    animation: welcomeSlideOutRight 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }

  &.slide-out-left {
    animation: welcomeSlideOutLeft 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }

  &.slide-in-from-left {
    animation: welcomeSlideInLeft 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }

  &.slide-in-from-right {
    animation: welcomeSlideInRight 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }
}

// 欢迎面板滑动动画关键帧
@keyframes welcomeSlideOutLeft {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(-100%);
    opacity: 0;
  }
}

@keyframes welcomeSlideOutRight {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}

@keyframes welcomeSlideInLeft {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes welcomeSlideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

// 欢迎面板内容样式
.welcome-content {
  text-align: center;
  color: var(--fts-text-white, #ffffff);

  h2 {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 12px;
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  }

  p {
    font-size: 15px;
    opacity: 0.9;
    margin-bottom: 8px;
  }

  .divider {
    width: 60px;
    height: 2px;
    background: rgba(255, 255, 255, 0.3);
    margin: 30px auto;
  }

  .hint {
    margin-bottom: 16px;
    font-size: 14px;
    opacity: 0.7;
  }
}

// 切换按钮样式
.toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 36px;
  background: transparent;
  border: 2px solid rgba(255, 255, 255, 0.8);
  color: var(--fts-text-white, #ffffff);
  font-size: 15px;
  font-weight: 600;
  border-radius: 50px;
  cursor: pointer;
  transition:
    background-color 0.3s ease,
    color 0.3s ease,
    transform 0.3s ease;

  &:hover {
    background: #ffffff;
    color: var(--fts-text-primary);
    transform: scale(1.02);

    svg {
      width: 18px;
      height: 18px;
      transition: transform 0.3s ease;
    }
  }

  &:hover svg {
    transform: translateX(3px);
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

// ========== 表单面板（白色区域） ==========
.form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  pointer-events: auto;
  position: relative;
  // 内容超出时在面板内滚动，避免撑开卡片
  overflow-y: auto;

  &.hidden {
    opacity: 0;
    pointer-events: none;
  }

  &.prepare-left {
    transform: translateX(-100%);
    opacity: 0;
  }

  &.prepare-right {
    transform: translateX(100%);
    opacity: 0;
  }

  &.slide-out-right {
    animation: formSlideOutRight 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }

  &.slide-out-left {
    animation: formSlideOutLeft 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }

  &.slide-in-from-left {
    animation: formSlideInLeft 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }

  &.slide-in-from-right {
    animation: formSlideInRight 0.7s cubic-bezier(0.645, 0.045, 0.355, 1) forwards;
  }
}

// 表单面板滑动动画关键帧
@keyframes formSlideOutLeft {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(-100%);
    opacity: 0;
  }
}

@keyframes formSlideOutRight {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}

@keyframes formSlideInLeft {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes formSlideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

// 表单内容区
.form-content {
  width: 100%;
  max-width: 340px;
  text-align: center;
}

// Logo图标
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  color: var(--fts-text-primary);

  svg {
    width: 100%;
    height: 100%;
  }
}

// 标题样式
h1 {
  font-size: 24px;
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: var(--fts-text-secondary);
  margin: 0 0 20px 0;
}

// ========== 表单样式 ==========
.form {
  width: 100%;
  text-align: left;

  :deep(.el-form-item) {
    margin-bottom: 20px;
    position: relative;
  }

  :deep(.el-form-item__label) {
    display: none;
  }

  :deep(.el-input) {
    position: relative;
  }

  :deep(.el-input__wrapper) {
    background: #ffffff;
    box-shadow: none;
    border: 2px solid var(--fts-border-primary);
    border-radius: 8px;
    padding: 8px 16px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: var(--fts-text-tertiary);
    }

    &:focus-within {
      border-color: var(--fts-border-primary);
      box-shadow: 0 0 0 3px rgba(26, 26, 46, 0.1);
    }
  }

  :deep(.el-input__inner) {
    color: var(--fts-text-primary);
    font-size: 15px;
    height: 32px;
    padding-top: 8px;

    &::placeholder {
      color: transparent;
    }

    // 自动填充样式修复
    &:autofill,
    &:autofill:hover,
    &:autofill:focus,
    &:-webkit-autofill,
    &:-webkit-autofill:hover,
    &:-webkit-autofill:focus {
      -webkit-box-shadow: 0 0 0 1000px #ffffff inset;
      -webkit-text-fill-color: var(--fts-text-primary);
      box-shadow: 0 0 0 1000px #ffffff inset;
      transition: background-color 5000s ease-in-out 0s;
    }
  }

  :deep(.el-input__prefix) {
    color: var(--fts-text-secondary);
    margin-top: 4px;
  }

  // 输入组追加按钮样式
  :deep(.el-input-group__append) {
    background: var(--fts-bg-dark, #1a1a2e);
    border: none;
    border-radius: 0 6px 6px 0;
    color: var(--fts-text-white, #ffffff);
    padding: 0 16px;

    .el-button {
      color: var(--fts-text-white, #ffffff);
      font-weight: 500;
    }
  }

  // 两列表单项间距
  :deep(.el-col-12) .el-form-item {
    margin-bottom: 20px;
  }

  // 记住我 & 忘记密码行
  .form-options {
    margin-bottom: 20px;

    :deep(.el-form-item__content) {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    :deep(.el-checkbox__label) {
      color: var(--fts-text-secondary);
    }
  }

  // 邀请码验证成功提示
  .code-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 8px;
    font-size: 13px;
    color: var(--fts-success);

    // 验证失败提示（红色）
    &--error {
      color: var(--fts-error, #f56c6c);
    }
  }

  // 提交按钮（渐变效果）
  .submit-btn {
    width: 100%;
    height: 50px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 8px;
    background: linear-gradient(
      135deg,
      var(--fts-bg-dark, #1a1a2e) 0%,
      var(--fts-primary, #0f3460) 100%
    );
    border: none;
    transition: all 0.3s ease;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(26, 26, 46, 0.3);
    }

    &:disabled {
      background: var(--fts-border-primary);
      cursor: not-allowed;
    }
  }
}

// ========== 浮动标签输入框（Material Design风格） ==========
.float-label {
  position: relative;
  // margin由外层el-form-item统一控制，避免双重间距

  .float-label-text {
    position: absolute;
    left: 16px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 15px;
    color: var(--fts-text-tertiary);
    pointer-events: none;
    // 必须包含所有会变化的属性，否则动画跳变
    transition:
      top 0.25s cubic-bezier(0.4, 0, 0.2, 1),
      transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
      font-size 0.25s cubic-bezier(0.4, 0, 0.2, 1),
      color 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    background: #ffffff;
    padding: 0 4px;
    z-index: 10; // 确保标签在校验错误提示之上
    line-height: 1.2;
    white-space: nowrap; // 防止文字被裁剪
  }

  :deep(.el-input__wrapper) {
    padding: 12px 16px;
  }

  :deep(.el-input__inner) {
    height: 24px;
    padding-top: 0;
  }

  // 有前缀图标时标签偏移
  &.has-prefix .float-label-text {
    left: 44px;
  }

  // 有追加按钮时标签位置调整
  &.has-append .float-label-text {
    right: 80px;
    left: auto;
    transform: translateY(-50%);
  }

  &.has-append.has-prefix .float-label-text {
    left: 44px;
    right: auto;
  }

  // 聚焦或有值时标签上浮到输入框顶部边框
  &.focused .float-label-text,
  &.has-value .float-label-text {
    top: 0;
    transform: translateY(-50%);
    font-size: 11px;
    color: var(--fts-primary);
  }

  &.focused .float-label-text {
    color: var(--fts-text-primary);
  }
}

// ========== 验证码行 ==========
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-canvas {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid var(--fts-border-primary);
  transition: border-color 0.3s;

  &:hover {
    border-color: var(--fts-primary);
  }
}

.captcha-loading {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  border-radius: 6px;
  border: 2px solid var(--fts-border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-secondary);
  font-size: 18px;
}

// ========== 响应式设计（768px以下全宽卡片） ==========
@media (max-width: 768px) {
  .auth-card {
    width: 100%;
    height: auto;
    min-height: 100vh;
    border-radius: 0;
    box-shadow: none;
  }

  .floating-shapes {
    display: none;
  }

  .page {
    position: relative;
    width: 100%;
    height: auto;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  .login-page,
  .register-page {
    .welcome-panel,
    .form-panel {
      width: 100%;
    }

    .welcome-panel {
      padding: 30px 20px;
      min-height: 200px;
    }

    .form-panel {
      padding: 30px 20px;
    }
  }

  .overlay {
    display: none;
  }

  .welcome-panel,
  .form-panel {
    transform: none !important;
    animation: none !important;
  }

  .form-content {
    max-width: 100%;
  }
}
</style>
