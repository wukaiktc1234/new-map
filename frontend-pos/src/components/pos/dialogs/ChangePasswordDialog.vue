<script setup lang="ts">
/**
 * 修改密码对话框（收银端）
 *
 * 走已有后端端点 PUT /v1/auth/password（ChangePasswordRequest{oldPassword,newPassword,confirmPassword}），
 * 前端只做表单校验与交互，不改后端校验规则：
 *   - 长度 6-32（与登录页表单一致）；
 *   - 新密码不能与原密码相同；
 *   - 两次输入必须一致；
 *   - 服务端错误原样透出，不吞消息。
 * 修改成功后由父组件决定是否强制重新登录（旧 token 与旧密码已不匹配）。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/api/request'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverError = ref('')

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 个字符', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value && value === form.oldPassword) {
          callback(new Error('新密码不能与当前密码相同'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const canSubmit = computed(
  () => !!form.oldPassword && !!form.newPassword && !!form.confirmPassword && !submitting.value,
)

/**
 * 把任意失败形态收敛成「给收银员看的一句话」+「是否属于接口不可用」。
 *
 * 后端/RuoYi 风格返回可能是 { message }、axios 的 response.status，也可能是 HTML 错误页；
 * 404/501 或网关错误一律按「接口暂不可用」处理——收银员不需要懂 HTTP，
 * 但必须知道"这不是我输错了"。
 */
function describePasswordError(error: unknown): { message: string; unavailable: boolean } {
  const status =
    (error as { status?: number })?.status ??
    (error as { response?: { status?: number } })?.response?.status ??
    (error as { originalError?: { response?: { status?: number } } })?.originalError?.response?.status

  const backendMessage =
    (error as { originalError?: { response?: { data?: { message?: string } } } })?.originalError?.response
      ?.data?.message ?? (error as { message?: string })?.message

  const isUnavailable =
    status === 404 || status === 405 || status === 501 || status === 502 || status === 503 ||
    (typeof backendMessage === 'string' &&
      /not\s*found|no handler|404|暂不可用|未实现/i.test(backendMessage))

  if (isUnavailable) {
    return { message: '接口暂不可用：修改密码服务未就绪（POST /v1/auth/password）', unavailable: true }
  }
  if (typeof backendMessage === 'string' && backendMessage.trim()) {
    return { message: backendMessage.trim(), unavailable: false }
  }
  return { message: '修改密码失败，请稍后重试', unavailable: false }
}

function resetForm(): void {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  serverError.value = ''
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) resetForm()
  },
)

function close(): void {
  if (submitting.value) return
  emit('update:modelValue', false)
}

async function submit(): Promise<void> {
  if (submitting.value || !formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  serverError.value = ''
  try {
    await request.put('/v1/auth/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码已修改')
    emit('success')
    emit('update:modelValue', false)
  } catch (error) {
    // 绝不静默失败：任何失败都必须有可见反馈（内联 + Toast 双通道）
    const { message, unavailable } = describePasswordError(error)
    serverError.value = message
    if (unavailable) {
      ElMessage.error('接口暂不可用：修改密码服务未就绪，请联系管理员')
    } else {
      ElMessage.error(message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="修改密码"
    width="480px"
    :close-on-click-modal="false"
    :append-to-body="true"
    data-test="change-password-dialog"
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      @submit.prevent="submit"
    >
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          autocomplete="current-password"
          data-test="old-password-input"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          autocomplete="new-password"
          data-test="new-password-input"
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password"
          data-test="confirm-password-input"
          @keyup.enter="submit"
        />
      </el-form-item>
    </el-form>

    <el-alert
      v-if="serverError"
      :title="serverError"
      type="error"
      :closable="false"
      show-icon
      data-test="change-password-error"
    />
    <p class="change-pwd__tip">修改成功后需重新登录，当前会话的登录凭证将失效。</p>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button
        type="primary"
        :disabled="!canSubmit"
        :loading="submitting"
        data-test="change-password-submit"
        @click="submit"
      >确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.change-pwd__tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
}
</style>
