<script setup lang="ts">
/**
 * 个人中心页面
 * 功能：基本信息展示、修改密码、偏好设置（主题/界面）
 * 数据来源：usePermissionStore（用户信息）、useLayoutStore（界面偏好）
 * 后端对接：修改密码调用 PUT /v1/auth/password
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Setting, Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import { usePermissionStore, UserRole, OrgLevel, DataScope } from '@/stores/permission'
import { useLayoutStore, type ThemeMode } from '@/stores/layout'
import { authApi } from '@/api/auth'

const permissionStore = usePermissionStore()
const layoutStore = useLayoutStore()

// ==================== 角色显示名映射 ====================
const roleDisplayMap: Record<UserRole, string> = {
  [UserRole.OWNER]: '超级管理员',
  [UserRole.ADMIN]: '超级管理员',
  [UserRole.OPS_DIRECTOR]: '运营总监',
  [UserRole.FINANCE_DIRECTOR]: '财务总监',
  [UserRole.HR_DIRECTOR]: 'HR总监',
  [UserRole.STORE_MANAGER]: '门店经理',
  [UserRole.TEAM_LEADER]: '组长',
  [UserRole.SCHEDULER]: '排班员',
  [UserRole.REGION_MANAGER]: '区域经理',
  [UserRole.AUDITOR]: '稽查专员',
  [UserRole.EMPLOYEE]: '普通员工',
}

const orgLevelDisplayMap: Record<OrgLevel, string> = {
  [OrgLevel.OWNER]: '超级管理员',
  [OrgLevel.COMPANY]: '公司级管理者',
  [OrgLevel.DEPARTMENT]: '部门级管理者',
  [OrgLevel.STORE_MGR]: '门店级管理者',
  [OrgLevel.STORE_STAFF]: '门店执行层',
}

const dataScopeDisplayMap: Record<DataScope, string> = {
  [DataScope.ALL]: '全部数据',
  [DataScope.COMPANY]: '本公司数据',
  [DataScope.DEPARTMENT]: '本部门数据',
  [DataScope.STORE]: '本门店数据',
  [DataScope.STORES]: '指定门店',
  [DataScope.SELF]: '仅本人数据',
}

// ==================== 基本信息 ====================
const userInfo = computed(() => permissionStore.userInfo)

/** 用户名首字母（用于头像显示） */
const avatarText = computed(() => {
  const name = userInfo.value?.username || ''
  return name ? name.charAt(0).toUpperCase() : ''
})

/** 角色显示文本 */
const roleText = computed(() => {
  if (!userInfo.value?.roles?.length) return '未分配角色'
  return userInfo.value.roles.map(r => roleDisplayMap[r] || r).join('、')
})

/** 组织层级显示文本 */
const orgLevelText = computed(() => {
  const level = userInfo.value?.orgLevel
  if (!level) return '—'
  return orgLevelDisplayMap[level] || level
})

/** 数据范围显示文本 */
const dataScopeText = computed(() => {
  const scope = userInfo.value?.dataScope
  if (!scope) return '—'
  return dataScopeDisplayMap[scope] || scope
})

// ==================== 修改密码 ====================
const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordSubmitting = ref(false)

/** 密码强度校验：至少8位，包含字母和数字 */
function validateNewPassword(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }
  if (value.length < 8) {
    callback(new Error('密码长度至少8位'))
    return
  }
  if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码需包含字母和数字'))
    return
  }
  if (value === passwordForm.oldPassword) {
    callback(new Error('新密码不能与旧密码相同'))
    return
  }
  callback()
}

/** 确认密码校验 */
function validateConfirmPassword(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
}

function openPasswordDialog() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

async function handlePasswordSubmit() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordSubmitting.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '密码修改失败，请检查旧密码是否正确')
    } else {
      ElMessage.error('密码修改失败，请稍后重试')
    }
  } finally {
    passwordSubmitting.value = false
  }
}

// ==================== 偏好设置 ====================
const themeOptions: { label: string; value: ThemeMode; icon: typeof Sunny }[] = [
  { label: '浅色', value: 'light', icon: Sunny },
  { label: '深色', value: 'dark', icon: Moon },
  { label: '跟随系统', value: 'auto', icon: Monitor },
]

function handleThemeChange(mode: ThemeMode) {
  layoutStore.setThemeMode(mode)
  ElMessage.success(`已切换为${mode === 'light' ? '浅色' : mode === 'dark' ? '深色' : '跟随系统'}主题`)
}

/** 圆角调整 */
const borderRadiusValue = computed(() => layoutStore.borderRadius)
function handleBorderRadiusChange(val: number | number[]) {
  const num = Array.isArray(val) ? val[0] : val
  layoutStore.setBorderRadius(num)
}

/** 色弱模式 */
const colorWeakMode = computed({
  get: () => layoutStore.colorWeakMode,
  set: (val: boolean) => layoutStore.setColorWeakMode(val),
})

function handleColorWeakChange(val: boolean | string | number) {
  layoutStore.setColorWeakMode(Boolean(val))
  ElMessage.success(`已${Boolean(val) ? '开启' : '关闭'}色弱模式`)
}
</script>

<template>
  <div class="modern-page personal-center">
    <PageHeader title="个人中心" description="管理个人信息、密码与界面偏好" />

    <div class="personal-content">
      <!-- ==================== 基本信息 ==================== -->
      <section class="content-card profile-card">
        <div class="card-header">
          <h2 class="card-title">
            <el-icon><User /></el-icon>
            基本信息
          </h2>
        </div>
        <div class="profile-body">
          <!-- 头像 -->
          <div class="avatar-section">
            <el-avatar :size="80" class="user-avatar">
              <span v-if="avatarText" class="avatar-text">{{ avatarText }}</span>
              <el-icon v-else :size="36"><User /></el-icon>
            </el-avatar>
            <div class="user-meta">
              <div class="user-name">{{ userInfo?.fullName || userInfo?.username || '未登录' }}</div>
              <div class="user-account">@{{ userInfo?.username || '—' }}</div>
              <div class="user-roles">{{ roleText }}</div>
            </div>
          </div>

          <!-- 信息表格 -->
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">用户ID</span>
              <span class="info-value">{{ userInfo?.userId || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ userInfo?.email || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所属门店</span>
              <span class="info-value">{{ userInfo?.storeName || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所属部门</span>
              <span class="info-value">{{ userInfo?.departmentName || '—' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">组织层级</span>
              <span class="info-value">{{ orgLevelText }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">数据范围</span>
              <span class="info-value">{{ dataScopeText }}</span>
            </div>
          </div>

          <!-- 操作 -->
          <div class="profile-actions">
            <el-button type="primary" :icon="Lock" @click="openPasswordDialog">修改密码</el-button>
          </div>
        </div>
      </section>

      <!-- ==================== 偏好设置 ==================== -->
      <section class="content-card preference-card">
        <div class="card-header">
          <h2 class="card-title">
            <el-icon><Setting /></el-icon>
            界面偏好
          </h2>
        </div>
        <div class="preference-body">
          <!-- 主题模式 -->
          <div class="preference-row">
            <div class="preference-label">
              <span class="label-text">主题模式</span>
              <span class="label-desc">切换浅色/深色主题，或跟随系统设置</span>
            </div>
            <div class="preference-control">
              <el-radio-group :model-value="layoutStore.themeMode" @change="handleThemeChange">
                <el-radio-button v-for="opt in themeOptions" :key="opt.value" :value="opt.value">
                  <el-icon class="theme-icon"><component :is="opt.icon" /></el-icon>
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <el-divider />

          <!-- 页面圆角 -->
          <div class="preference-row">
            <div class="preference-label">
              <span class="label-text">页面圆角</span>
              <span class="label-desc">调整卡片、按钮等元素的圆角大小</span>
            </div>
            <div class="preference-control">
              <el-slider
                :model-value="borderRadiusValue"
                :min="0"
                :max="20"
                :step="1"
                style="width: 220px"
                @change="handleBorderRadiusChange"
              />
              <span class="slider-value">{{ borderRadiusValue }}px</span>
            </div>
          </div>

          <el-divider />

          <!-- 色弱模式 -->
          <div class="preference-row">
            <div class="preference-label">
              <span class="label-text">色弱模式</span>
              <span class="label-desc">为色觉障碍用户提供高对比度界面</span>
            </div>
            <div class="preference-control">
              <el-switch :model-value="colorWeakMode" @change="handleColorWeakChange" />
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- ==================== 修改密码对话框 ==================== -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="460px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="90px"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="至少8位，包含字母和数字"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handlePasswordSubmit">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.personal-center {
  padding-bottom: var(--fts-space-6);
}

.personal-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
  padding: var(--fts-space-5) var(--fts-space-6) 0;
}

/* 复用项目统一样式：content-card */
.content-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-4) var(--fts-space-5);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.card-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin: 0;
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

/* ===== 基本信息卡片 ===== */
.profile-body {
  padding: var(--fts-space-5);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  padding-bottom: var(--fts-space-5);
  border-bottom: 1px solid var(--fts-border-secondary);
  margin-bottom: var(--fts-space-5);
}

.user-avatar {
  background: linear-gradient(135deg, var(--fts-primary), var(--fts-primary-hover));
  flex-shrink: 0;
}

.avatar-text {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-inverse);
  line-height: 1;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.user-name {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.user-account {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.user-roles {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin-top: var(--fts-space-1);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-4) var(--fts-space-6);
  margin-bottom: var(--fts-space-5);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.info-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.info-value {
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  word-break: break-all;

  &--warning {
    color: var(--fts-warning);
  }
}

.profile-actions {
  display: flex;
  gap: var(--fts-space-3);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);
}

/* ===== 偏好设置卡片 ===== */
.preference-body {
  padding: var(--fts-space-5);
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-5);
}

.preference-label {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.label-text {
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.label-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.preference-control {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.theme-icon {
  margin-right: var(--fts-space-1);
  vertical-align: middle;
}

.slider-value {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  min-width: 36px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .preference-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
