<script setup lang="ts">
/**
 * PasswordChangePage - 密码修改页
 *
 * 从 SecuritySettingsPage 提取的独立密码修改功能
 * 支持旧密码验证、新密码设置、截屏防护
 */
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'
import { profileApi } from '@/api'
import { useScreenSecurity } from '@/composables/useScreenSecurity'

const changingPassword = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

/** 修改密码：调用后端 API */
async function handleChangePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await profileApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已更新，请重新登录')
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    // 后端返回的错误由响应拦截器统一处理，此处仅兜底提示
    ElMessage.error('密码修改失败，请稍后重试')
  } finally {
    changingPassword.value = false
  }
}

// 截屏防护
onMounted(() => {
  useScreenSecurity(true)
})

onUnmounted(() => {
  useScreenSecurity(false)
})
</script>

<template>
  <PageContainer title="修改密码">
    <div class="password-change-page">
      <section class="section-card">
        <!-- 页面头部 -->
        <div class="section-header">
          <div class="section-icon section-icon--password">
            <el-icon :size="18"><Lock /></el-icon>
          </div>
          <div class="header-text">
            <h3 class="section-title">登录密码</h3>
            <p class="section-desc">定期更换密码可保障账号安全</p>
          </div>
        </div>

        <!-- 表单区域 -->
        <div class="form-area">
          <div class="form-group">
            <label class="form-label">当前密码</label>
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入当前密码"
              show-password
              autocomplete="current-password"
            />
          </div>

          <div class="form-group">
            <label class="form-label">新密码</label>
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码（至少6位）"
              show-password
              autocomplete="new-password"
            />
          </div>

          <div class="form-group">
            <label class="form-label">确认新密码</label>
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              autocomplete="new-password"
            />
          </div>

          <button
            class="submit-btn"
            :disabled="changingPassword"
            @click="handleChangePassword"
          >
            {{ changingPassword ? '更新中...' : '更新密码' }}
          </button>
        </div>

        <!-- 安全提示 -->
        <div class="security-tips">
          <p class="tips-title">安全建议</p>
          <ul class="tips-list">
            <li>密码长度不少于6位，建议包含字母和数字</li>
            <li>避免使用生日、手机号等易猜测信息</li>
            <li>不要与其他平台使用相同密码</li>
          </ul>
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.password-change-page {
  display: flex;
  flex-direction: column;
}

/* ===== 卡片 ===== */
.section-card {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

/* ===== 头部 ===== */
.section-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-5);
}

.section-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--password {
    background: rgba(var(--fts-warning-rgb), 0.1);
    color: var(--fts-warning);
  }
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.section-title {
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.section-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin: 0;
}

/* ===== 表单 ===== */
.form-area {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-bottom: var(--fts-space-5);
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

.submit-btn {
  width: 100%;
  padding: var(--fts-space-3) 0;
  margin-top: var(--fts-space-3);
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: #fff;
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }
}

/* ===== 安全提示 ===== */
.security-tips {
  background: rgba(var(--fts-primary-rgb), 0.04);
  border: 1px solid rgba(var(--fts-primary-rgb), 0.1);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3) var(--fts-space-4);
}

.tips-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-primary);
  margin: 0 0 var(--fts-space-2);
}

.tips-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.tips-list li {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  padding-left: var(--fts-space-3);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 7px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background-color: var(--fts-primary);
  }
}
</style>
