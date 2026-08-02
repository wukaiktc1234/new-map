<script setup lang="ts">
/**
 * SecurityLockPage - 安全锁定页
 *
 * 设置/修改安全密码（6位纯数字PIN码）
 * 用于敏感操作的二次验证
 * Mock实现：localStorage存储
 */
import { ref, reactive, onMounted } from 'vue'
import { Lock, CircleCheckFilled, View, Hide } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'

/** 当前模式：set=设置 / change=修改 / confirm=确认 */
const mode = ref<'set' | 'change' | 'confirm'>('set')

const setForm = reactive({
  pin: '',
  confirmPin: '',
})

const changeForm = reactive({
  oldPin: '',
  newPin: '',
  confirmPin: '',
})

const showPin = ref({
  set: false,
  setConfirm: false,
  changeOld: false,
  changeNew: false,
  changeConfirm: false,
})

/** localStorage key */
const STORAGE_KEY = 'emp_security_pin_hash'

/** 检查是否已设置安全密码 */
onMounted(() => {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    mode.value = 'change'
  }
})

/** 简单的PIN校验（Mock：仅检查长度和数字）*/
function isValidPin(pin: string): boolean {
  return /^\d{6}$/.test(pin)
}

/** 设置安全密码 */
function handleSetPin() {
  if (!isValidPin(setForm.pin)) {
    ElMessage.warning('安全密码为6位纯数字')
    return
  }
  if (setForm.pin !== setForm.confirmPin) {
    ElMessage.error('两次输入的安全密码不一致')
    return
  }

  // Mock：存储简单hash（实际应调用后端API）
  localStorage.setItem(STORAGE_KEY, btoa(setForm.pin))
  ElMessage.success('安全密码设置成功')
  mode.value = 'confirm'

  // 清空表单
  setForm.pin = ''
  setForm.confirmPin = ''
}

/** 修改安全密码 */
function handleChangePin() {
  if (!isValidPin(changeForm.oldPin)) {
    ElMessage.warning('请输入正确的当前安全密码')
    return
  }
  if (!isValidPin(changeForm.newPin)) {
    ElMessage.warning('新安全密码为6位纯数字')
    return
  }
  if (changeForm.newPin === changeForm.oldPin) {
    ElMessage.error('新安全密码不能与当前相同')
    return
  }
  if (changeForm.newPin !== changeForm.confirmPin) {
    ElMessage.error('两次输入的新安全密码不一致')
    return
  }

  // Mock：更新存储
  localStorage.setItem(STORAGE_KEY, btoa(changeForm.newPin))
  ElMessage.success('安全密码修改成功')
  mode.value = 'confirm'

  // 清空表单
  changeForm.oldPin = ''
  changeForm.newPin = ''
  changeForm.confirmPin = ''
}

/** 切换到修改模式 */
function switchToChangeMode() {
  mode.value = 'change'
}
</script>

<template>
  <PageContainer title="安全锁定">
    <div class="security-lock-page">
      <!-- 设置模式 -->
      <section v-if="mode === 'set'" class="lock-card">
        <div class="card-header">
          <div class="header-icon header-icon--lock">
            <el-icon :size="24"><Lock /></el-icon>
          </div>
          <div>
            <h3 class="card-title">设置安全密码</h3>
            <p class="card-desc">用于敏感操作的二次验证</p>
          </div>
        </div>

        <div class="pin-info">
          <span class="info-icon">i</span>
          <span class="info-text">安全密码为6位纯数字，请勿与登录密码相同</span>
        </div>

        <div class="form-group">
          <label class="form-label">安全密码</label>
          <div class="input-with-toggle">
            <el-input
              v-model="setForm.pin"
              :type="showPin.set ? 'text' : 'password'"
              placeholder="请输入6位数字安全密码"
              maxlength="6"
              size="large"
              autocomplete="new-password"
            />
            <button class="toggle-btn" @click="showPin.set = !showPin.set">
              <el-icon :size="16"><View v-if="!showPin.set" /><Hide v-else /></el-icon>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">确认安全密码</label>
          <div class="input-with-toggle">
            <el-input
              v-model="setForm.confirmPin"
              :type="showPin.setConfirm ? 'text' : 'password'"
              placeholder="请再次输入安全密码"
              maxlength="6"
              size="large"
              autocomplete="new-password"
            />
            <button class="toggle-btn" @click="showPin.setConfirm = !showPin.setConfirm">
              <el-icon :size="16"><View v-if="!showPin.setConfirm" /><Hide v-else /></el-icon>
            </button>
          </div>
        </div>

        <button class="primary-btn" @click="handleSetPin">
          设置安全密码
        </button>
      </section>

      <!-- 修改模式 -->
      <section v-if="mode === 'change'" class="lock-card">
        <div class="card-header">
          <div class="header-icon header-icon--lock">
            <el-icon :size="24"><Lock /></el-icon>
          </div>
          <div>
            <h3 class="card-title">修改安全密码</h3>
            <p class="card-desc">需要先验证当前安全密码</p>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">当前安全密码</label>
          <div class="input-with-toggle">
            <el-input
              v-model="changeForm.oldPin"
              :type="showPin.changeOld ? 'text' : 'password'"
              placeholder="请输入当前安全密码"
              maxlength="6"
              size="large"
            />
            <button class="toggle-btn" @click="showPin.changeOld = !showPin.changeOld">
              <el-icon :size="16"><View v-if="!showPin.changeOld" /><Hide v-else /></el-icon>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">新安全密码</label>
          <div class="input-with-toggle">
            <el-input
              v-model="changeForm.newPin"
              :type="showPin.changeNew ? 'text' : 'password'"
              placeholder="请输入新安全密码"
              maxlength="6"
              size="large"
              autocomplete="new-password"
            />
            <button class="toggle-btn" @click="showPin.changeNew = !showPin.changeNew">
              <el-icon :size="16"><View v-if="!showPin.changeNew" /><Hide v-else /></el-icon>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">确认新安全密码</label>
          <div class="input-with-toggle">
            <el-input
              v-model="changeForm.confirmPin"
              :type="showPin.changeConfirm ? 'text' : 'password'"
              placeholder="请再次输入新安全密码"
              maxlength="6"
              size="large"
              autocomplete="new-password"
            />
            <button class="toggle-btn" @click="showPin.changeConfirm = !showPin.changeConfirm">
              <el-icon :size="16"><View v-if="!showPin.changeConfirm" /><Hide v-else /></el-icon>
            </button>
          </div>
        </div>

        <button class="primary-btn" @click="handleChangePin">
          修改安全密码
        </button>
      </section>

      <!-- 已设置/确认模式 -->
      <section v-if="mode === 'confirm'" class="lock-card lock-card--center">
        <div class="success-icon">
          <el-icon :size="40" style="color: var(--fts-success)"><CircleCheckFilled /></el-icon>
        </div>
        <h3 class="card-title">安全密码已设置</h3>
        <p class="card-desc">在进行敏感操作时将要求输入安全密码进行验证</p>

        <div class="security-features">
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>修改密码时需验证</span>
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>解除设备绑定时需验证</span>
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>申诉提交时可选择安全密码验证</span>
          </div>
        </div>

        <button class="secondary-btn" @click="switchToChangeMode">
          修改安全密码
        </button>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.security-lock-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--fts-space-4) 0;
}

.lock-card {
  width: 100%;
  max-width: 420px;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5) var(--fts-space-5);
  box-shadow: var(--fts-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);

  &--center {
    align-items: center;
    text-align: center;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--lock {
    background: rgba(var(--fts-warning-rgb), 0.1);
    color: var(--fts-warning);
  }
}

.card-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.card-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: var(--fts-space-1) 0 0;
}

.pin-info {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-info-rgb), 0.06);
  border-radius: var(--fts-radius-sm);
}

.info-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--fts-info);
  color: var(--fts-text-on-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-style: italic;
  font-weight: 700;
  flex-shrink: 0;
  line-height: 1;
}

.info-text {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  line-height: 1.5;
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

.input-with-toggle {
  position: relative;
  display: flex;
  align-items: center;

  .el-input {
    flex: 1;
    padding-right: 40px;
  }
}

.toggle-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--fts-text-quaternary);
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    color: var(--fts-text-secondary);
  }
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
}

.secondary-btn {
  padding: var(--fts-space-3) var(--fts-space-6);
  font-size: var(--fts-font-size-base);
  font-weight: 500;
  color: var(--fts-primary);
  background: rgba(var(--fts-primary-rgb), 0.08);
  border: 1px solid rgba(var(--fts-primary-rgb), 0.2);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    background: rgba(var(--fts-primary-rgb), 0.12);
  }

  &:active {
    transform: scale(0.98);
  }
}

/* ===== 成功/确认状态 ===== */
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

.security-features {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  margin: var(--fts-space-2) 0 var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.feature-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: var(--fts-success);
  flex-shrink: 0;
}
</style>
