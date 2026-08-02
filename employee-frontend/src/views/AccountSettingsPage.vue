<script setup lang="ts">
/**
 * AccountSettingsPage - 账号设置页面（员工档案查看/编辑）
 *
 * 展示从管理端员工档案同步的完整字段信息。
 * 员工仅可编辑部分基础字段（姓名、手机号、邮箱），
 * 其余组织信息、入职信息等由管理端统一维护。
 *
 * @last-modified 2026-06-05
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'
import { usePermissionStore } from '@/stores/permission'
import { useScreenSecurity } from '@/composables/useScreenSecurity'
import { UI_DELAY_MEDIUM } from '@/config/timing'

const router = useRouter()
const permission = usePermissionStore()

/** [M10] 账号设置页为敏感页面，启用截屏/录屏防护 */
onMounted(() => {
  useScreenSecurity(true)
})

onUnmounted(() => {
  useScreenSecurity(false)
})

// ========== 表单数据 ==========
const form = reactive({
  name: permission.userInfo.name || '',
  phone: permission.userInfo.phone || '',
  email: permission.userInfo.email || '',
})

// ========== 字段配置（动态生成表单项） ==========

/** 字段定义：label / value / 是否可编辑 / 分组 */
interface FieldDef {
  key: string
  label: string
  value: string | undefined
  editable: boolean
  group: 'basic' | 'org' | 'employment'
}

/** 基础身份信息（员工可编辑部分） */
const basicFields = computed<FieldDef[]>(() => [
  { key: 'name', label: '姓名', value: form.name, editable: true, group: 'basic' },
  { key: 'employeeNo', label: '工号', value: permission.userInfo.employeeNo || '--', editable: false, group: 'basic' },
  { key: 'phone', label: '手机号', value: form.phone, editable: true, group: 'basic' },
  { key: 'email', label: '邮箱', value: form.email || '--', editable: true, group: 'basic' },
])

/** 组织信息（管理端维护，只读） */
const orgFields = computed<FieldDef[]>(() => [
  { key: 'department', label: '部门', value: permission.userInfo.department || '--', editable: false, group: 'org' },
  { key: 'position', label: '职位', value: permission.userInfo.position || permission.getCurrentRoleLabel(), editable: false, group: 'org' },
  { key: 'storeName', label: '所属门店', value: permission.userInfo.storeName || '--', editable: false, group: 'org' },
  { key: 'supervisorName', label: '直属上级', value: permission.userInfo.supervisorName || '--', editable: false, group: 'org' },
])

/** 入职信息（管理端维护，只读） */
const employmentFields = computed<FieldDef[]>(() => [
  { key: 'joinDate', label: '入职日期', value: permission.userInfo.joinDate || '--', editable: false, group: 'employment' },
  { key: 'probationEnd', label: '试用期至', value: permission.userInfo.probationEnd || '--', editable: false, group: 'employment' },
  { key: 'workStatus', label: '在职状态', value: formatWorkStatus(permission.userInfo.workStatus), editable: false, group: 'employment' },
])

/** 格式化在职状态显示 */
function formatWorkStatus(status: string): string {
  const map: Record<string, string> = {
    active: '在职',
    probation: '试用期',
    inactive: '已离职',
    resigned: '已辞职',
  }
  return map[status] || status || '--'
}

// ========== 操作 ==========

const saving = ref(false)

function handleSave() {
  saving.value = true
  // TODO: 对接员工档案更新 API
  setTimeout(() => {
    // 同步回 store（本地即时反馈）
    permission.userInfo.name = form.name
    permission.userInfo.phone = form.phone
    permission.userInfo.email = form.email
    ElMessage.success('账号信息已更新')
    saving.value = false
  }, UI_DELAY_MEDIUM)
}

function handleBack() {
  router.back()
}
</script>

<template>
  <PageContainer title="账号设置">
    <div class="account-settings">
      <!-- 头像区 -->
      <section class="avatar-section">
        <div class="avatar-wrap">
          <img
            v-if="permission.userInfo.avatar"
            :src="permission.userInfo.avatar"
            :alt="form.name"
            class="avatar-img"
          />
          <span v-else class="avatar-placeholder">
            <el-icon :size="28"><User /></el-icon>
          </span>
        </div>
        <p class="avatar-hint">点击更换头像</p>
      </section>

      <!-- 基础信息（可编辑） -->
      <section class="form-section">
        <h3 class="form-title">基本信息</h3>
        <div class="form-group" v-for="field in basicFields" :key="field.key">
          <label class="form-label">{{ field.label }}</label>
          <el-input
            v-if="field.editable"
            v-model="(form as any)[field.key]"
            :placeholder="`请输入${field.label}`"
            :maxlength="field.key === 'phone' ? 11 : field.key === 'name' ? 20 : 50"
            :type="field.key === 'phone' ? 'tel' : field.key === 'email' ? 'email' : 'text'"
          />
          <el-input v-else :model-value="field.value" disabled />
        </div>
      </section>

      <!-- 组织信息（只读） -->
      <section class="form-section">
        <h3 class="form-title">组织信息</h3>
        <div class="form-group" v-for="field in orgFields" :key="field.key">
          <label class="form-label">{{ field.label }}</label>
          <el-input :model-value="field.value" disabled />
        </div>
      </section>

      <!-- 入职信息（只读） -->
      <section class="form-section">
        <h3 class="form-title">入职信息</h3>
        <div class="form-group" v-for="field in employmentFields" :key="field.key">
          <label class="form-label">{{ field.label }}</label>
          <el-input :model-value="field.value" disabled />
        </div>
      </section>

      <!-- 保存按钮 -->
      <button class="save-btn" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中...' : '保存修改' }}
      </button>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.account-settings {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

/* ===== 头像区 ===== */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-6) 0;
}

.avatar-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background-color: var(--fts-bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: var(--fts-text-quaternary);
}

.avatar-hint {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin: 0;
}

/* ===== 表单区块 ===== */
.form-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

.form-title {
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &:not(:last-child) {
    margin-bottom: var(--fts-space-4);
  }
}

.form-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
}

/* ===== 保存按钮 ===== */
.save-btn {
  width: 100%;
  padding: var(--fts-space-3) 0;
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: #fff;
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:active:not(:disabled) {
    transform: scale(0.99);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}
</style>
