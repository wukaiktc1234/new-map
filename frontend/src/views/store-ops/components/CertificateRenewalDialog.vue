<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { Upload, Delete, Document, InfoFilled } from '@element-plus/icons-vue'
import type { Certificate, CreateExpenseDTO } from '@/types/store-operation'

interface Props {
  modelValue: boolean
  certificate: Certificate | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [data: RenewalData]
  /** 费用记录生成事件（当 renewCost > 0 时触发） */
  expenseCreated: [expenseData: CreateExpenseDTO]
  close: []
}>()

/** 续期表单数据接口 */
export interface RenewalData {
  certificateId: string
  newExpiryDate: string
  newCertNumber?: string
  renewCost?: number
  remark?: string
  newFileUrl?: string
  newFile?: File
}

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const fileList = ref<UploadFile[]>([])
const previewImageUrl = ref('')

/** 续期表单 */
const renewalForm = reactive<RenewalData>({
  certificateId: '',
  newExpiryDate: '',
  newCertNumber: '',
  renewCost: undefined,
  remark: '',
  newFileUrl: '',
  newFile: undefined,
})

/** 表单校验规则 */
const rules: FormRules = {
  newExpiryDate: [
    { required: true, message: '请选择新的到期日期', trigger: 'change' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error('请选择到期日期'))
          return
        }
        const selectedDate = new Date(value)
        const minDate = new Date()
        minDate.setDate(minDate.getDate() + 30)
        if (selectedDate < minDate) {
          callback(new Error('新到期日期至少要比当前日期晚30天'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  renewCost: [
    {
      validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
        if (value !== undefined && value !== null) {
          if (value < 0) {
            callback(new Error('费用不能为负数'))
            return
          }
          if (value > 999999.99) {
            callback(new Error('费用超出范围'))
            return
          }
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

/** 是否为编辑模式 */
const isEditMode = computed(() => props.certificate !== null)

/** 对话框标题 */
const dialogTitle = computed(() => {
  if (!props.certificate) return '办理续期'
  return `办理续期 - ${props.certificate.certName}`
})

/** 当前证件信息摘要 */
const certSummary = computed(() => {
  if (!props.certificate) return null
  const cert = props.certificate
  return {
    name: cert.certName,
    currentExpiry: cert.expiryDate,
    certNumber: cert.certNumber || '未填写',
    holderName: cert.holderName,
  }
})

/** 监听对话框打开事件，初始化表单数据 */
watch(
  () => props.modelValue,
  (val) => {
    if (val && props.certificate) {
      resetForm()
      renewalForm.certificateId = props.certificate.certificateId
      // 默认新到期日期为当前日期+1年
      const defaultDate = new Date()
      defaultDate.setFullYear(defaultDate.getFullYear() + 1)
      renewalForm.newExpiryDate = defaultDate.toISOString().split('T')[0]
    }
  }
)

/** 重置表单 */
function resetForm() {
  Object.assign(renewalForm, {
    certificateId: '',
    newExpiryDate: '',
    newCertNumber: '',
    renewCost: undefined,
    remark: '',
    newFileUrl: '',
    newFile: undefined,
  })
  fileList.value = []
  previewImageUrl.value = ''
  formRef.value?.resetFields()
}

/** 处理文件上传变化 */
function handleFileChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) return

  // 文件类型验证
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('只支持 JPG/PNG/GIF/PDF 格式')
    return
  }

  // 文件大小验证（最大5MB）
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过5MB')
    return
  }

  renewalForm.newFile = file
  fileList.value = [uploadFile]

  // 如果是图片，生成预览URL
  if (file.type.startsWith('image/')) {
    const reader = new FileReader()
    reader.onload = (e) => {
      previewImageUrl.value = e.target?.result as string
    }
    reader.readAsDataURL(file)
  }
}

/** 移除已选文件 */
function handleRemoveFile() {
  fileList.value = []
  renewalForm.newFile = undefined
  renewalForm.newFileUrl = ''
  previewImageUrl.value = ''
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    // 构建提交数据
    const submitData: RenewalData = {
      certificateId: renewalForm.certificateId,
      newExpiryDate: renewalForm.newExpiryDate,
      newCertNumber: renewalForm.newCertNumber || undefined,
      renewCost: renewalForm.renewCost,
      remark: renewalForm.remark || undefined,
      newFile: renewalForm.newFile,
    }

    // 触发提交事件
    emit('submit', submitData)

    // P0阶段：若填写了费用，自动生成待报销记录
    if (renewalForm.renewCost && renewalForm.renewCost > 0 && props.certificate) {
      const expenseData: CreateExpenseDTO = {
        certificateId: props.certificate.certificateId,
        certName: props.certificate.certName,
        renewalRecordId: `RENEW_${Date.now()}`,
        amount: renewalForm.renewCost,
        remark: renewalForm.remark || undefined,
      }
      emit('expenseCreated', expenseData)
      // 费用记录创建提示（在对话框关闭前显示）
      ElMessage.success(`续期申请已提交，已生成 ¥${renewalForm.renewCost.toFixed(2)} 待报销记录`)
    } else {
      ElMessage.success('续期申请已提交')
    }

    emit('update:modelValue', false)
  } catch (error) {
    console.error('[CertificateRenewal] 提交失败:', error)
    ElMessage.error('提交失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

/** 关闭对话框 */
function handleClose() {
  resetForm()
  emit('update:modelValue', false)
  emit('close')
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="680px"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    class="certificate-renewal-dialog"
    @close="handleClose"
  >
    <!-- 当前证件信息卡片 -->
    <div v-if="certSummary" class="current-cert-info">
      <div class="info-header">
        <el-icon><InfoFilled /></el-icon>
        <span>当前证件信息</span>
      </div>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="证件名称">{{ certSummary.name }}</el-descriptions-item>
        <el-descriptions-item label="持有人">{{ certSummary.holderName }}</el-descriptions-item>
        <el-descriptions-item label="当前到期日">{{ certSummary.currentExpiry }}</el-descriptions-item>
        <el-descriptions-item label="证件编号">{{ certSummary.certNumber }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 续期表单 -->
    <el-form
      ref="formRef"
      :model="renewalForm"
      :rules="rules"
      label-width="120px"
      label-position="right"
      class="renewal-form"
    >
      <el-form-item label="新到期日期" prop="newExpiryDate">
        <el-date-picker
          v-model="renewalForm.newExpiryDate"
          type="date"
          placeholder="选择新的到期日期"
          style="width: 100%"
          value-format="YYYY-MM-DD"
          :teleported="false"
          :disabled-date="(time: Date) => {
            const minDate = new Date()
            minDate.setDate(minDate.getDate() + 30)
            return time.getTime() < minDate.getTime()
          }"
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="新证件编号" prop="newCertNumber">
            <el-input
              v-model="renewalForm.newCertNumber"
              placeholder="如证件编号有变更请填写"
              maxlength="50"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="续期费用（元）" prop="renewCost">
            <el-input-number
              v-model="renewalForm.renewCost"
              :min="0"
              :max="999999.99"
              :precision="2"
              :step="100"
              controls-position="right"
              placeholder="选填"
              style="width: 100%"
            />
            <!-- 费用提示文字 -->
            <div v-if="renewalForm.renewCost && renewalForm.renewCost > 0" class="cost-hint-text">
              续期费用将自动生成待报销记录
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="上传新证件" prop="newFile">
        <div class="upload-section">
          <el-upload
            action=""
            :auto-upload="false"
            :file-list="fileList"
            :show-file-list="true"
            :on-change="handleFileChange"
            :on-remove="handleRemoveFile"
            accept="image/jpeg,image/png,image/gif,application/pdf"
            :limit="1"
            list-type="text"
            class="certificate-uploader"
          >
            <el-button type="primary" plain>
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="upload-tip">
                支持 JPG/PNG/GIF/PDF 格式，单个文件不超过5MB
              </div>
            </template>
          </el-upload>

          <!-- 图片预览 -->
          <div v-if="previewImageUrl" class="image-preview">
            <el-image
              :src="previewImageUrl"
              :preview-src-list="[previewImageUrl]"
              fit="contain"
              class="preview-image"
            >
              <template #error>
                <div class="image-error">加载失败</div>
              </template>
            </el-image>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="renewalForm.remark"
          type="textarea"
          :rows="3"
          placeholder="可选填续期相关信息，如办理渠道、注意事项等"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
        确认续期
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.certificate-renewal-dialog {
  --dlg-bg: var(--el-bg-color);
  --dlg-text: var(--el-text-color-primary);
  --dlg-border: var(--el-border-color-light);
}

/* 当前证件信息卡片 */
.current-cert-info {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3);
  background: var(--el-fill-color-lighter, #f5f7fa);
  border-radius: var(--fts-border-radius-base);
  border: 1px solid var(--dlg-border);
}

.info-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-secondary, #606266);
}

.info-header .el-icon {
  color: var(--fts-primary);
}

/* 续期表单样式 */
.renewal-form {
  padding-top: var(--fts-space-2);
}

/* 上传区域样式 */
.upload-section {
  width: 100%;
}

.certificate-uploader {
  width: 100%;
}

.upload-tip {
  margin-top: var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  color: var(--el-text-color-placeholder, #c0c4cc);
  line-height: 1.4;
}

/* 图片预览区域 */
.image-preview {
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3);
  background: var(--el-fill-color-lighter, #f5f7fa);
  border-radius: var(--fts-border-radius-base);
  border: 1px dashed var(--dlg-border);
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: var(--fts-border-radius-sm);
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--el-text-color-placeholder, #c0c4cc);
  font-size: var(--fts-font-size-sm);
}

/* 费用输入提示文字 */
.cost-hint-text {
  margin-top: var(--fts-space-1);
  font-size: var(--fts-font-size-xs);
  color: var(--el-text-color-placeholder, #c0c4cc);
  line-height: 1.4;
}
</style>
