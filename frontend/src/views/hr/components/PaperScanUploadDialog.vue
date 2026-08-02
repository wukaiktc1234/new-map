<script setup lang="ts">
/**
 * 纸质合同扫描件上传对话框
 *
 * 【功能】
 * - 上传纸质合同扫描件（PDF/JPG/PNG，单文件10MB以内）
 * - 填写档案编号、存放位置、备注
 * - 上传成功后调用 contractApi.uploadPaperScan
 * - emit success 事件
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile, UploadUserFile } from 'element-plus'
import { contractApi } from '@/api/hr/contract'
import type { EmployeeContract } from '@/types/hr/contract'
import { ContractTypeOptions } from '@/types/hr/contract'

const props = defineProps<{
  /** 对话框可见性 */
  visible: boolean
  /** 合同信息 */
  contract: EmployeeContract | null
}>()

const emit = defineEmits<{
  /** 更新可见性 */
  (e: 'update:visible', val: boolean): void
  /** 上传成功 */
  (e: 'success'): void
}>()

/* ===== 表单状态 ===== */
const fileList = ref<UploadUserFile[]>([])
const formData = ref({
  archiveNumber: '',
  archiveLocation: '',
  remark: '',
})
const uploading = ref(false)
const uploadProgress = ref(0)

/* ===== 监听 visible 变化，重置表单 ===== */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      resetForm()
    }
  }
)

function resetForm() {
  fileList.value = []
  formData.value = {
    archiveNumber: '',
    archiveLocation: '',
    remark: '',
  }
  uploading.value = false
  uploadProgress.value = 0
}

/* ===== 文件选择校验 ===== */
function handleFileChange(file: UploadFile) {
  if (!file.raw) return
  // 校验文件类型
  const allowedTypes = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg']
  if (!allowedTypes.includes(file.raw.type)) {
    ElMessage.error('仅支持 PDF/JPG/PNG 格式')
    fileList.value = []
    return
  }
  // 校验文件大小（10MB）
  if (file.raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    fileList.value = []
    return
  }
  fileList.value = [file]
}

function handleRemove() {
  fileList.value = []
}

/* ===== 提交上传 ===== */
async function handleSubmit() {
  if (!props.contract) return
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择扫描件文件')
    return
  }
  if (!formData.value.archiveLocation) {
    ElMessage.warning('请填写纸质合同存放位置')
    return
  }

  uploading.value = true
  uploadProgress.value = 0

  // 模拟上传进度（确保在任何路径下都清理定时器，防止资源泄漏）
  const progressInterval = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += 10
    }
  }, 200)

  try {
    const file = fileList.value[0]
    if (!file.raw) throw new Error('文件无效')

    // 将文件转为 Base64 URL（Mock 模式）
    const scanFileUrl = await fileToBase64(file.raw)
    const scanFileName = file.name

    await contractApi.uploadPaperScan(props.contract.id, {
      scanFileUrl,
      scanFileName,
      archiveLocation: formData.value.archiveLocation,
      archiveNumber: formData.value.archiveNumber,
      remark: formData.value.remark,
    })

    uploadProgress.value = 100

    ElMessage.success('扫描件上传成功')
    emit('success')
    handleClose()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '上传失败')
  } finally {
    clearInterval(progressInterval)
    uploading.value = false
  }
}

/* ===== 文件转 Base64 ===== */
function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target?.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

/* ===== 关闭对话框 ===== */
function handleClose() {
  emit('update:visible', false)
}

/* ===== 获取合同类型标签 ===== */
function getContractTypeLabel(type?: string): string {
  if (!type) return '-'
  return ContractTypeOptions.find(o => o.value === type)?.label || type
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="上传纸质合同扫描件"
    width="680px"
    class="fts-dialog--md"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="handleClose"
  >
    <div v-if="contract" class="paper-scan-dialog">
      <!-- 合同基本信息 -->
      <el-descriptions :column="2" border size="small" style="margin-bottom: var(--fts-space-4);">
        <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="员工姓名">{{ contract.employeeName }}</el-descriptions-item>
        <el-descriptions-item label="合同类型">{{ getContractTypeLabel(contract.contractType) }}</el-descriptions-item>
        <el-descriptions-item label="签订日期">{{ contract.signDate || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-width="100px">
        <!-- 扫描件上传 -->
        <el-form-item label="扫描件" required>
          <el-upload
            v-model:file-list="fileList"
            class="scan-uploader"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleRemove"
            :limit="1"
            accept="application/pdf,image/png,image/jpeg,image/jpg"
            drag
          >
            <div class="upload-dragger-content">
              <el-icon :size="40"><UploadFilled /></el-icon>
              <div class="upload-text">将扫描件拖到此处，或点击上传</div>
              <div class="upload-hint">支持 PDF/JPG/PNG 格式，单文件不超过 10MB</div>
            </div>
          </el-upload>
          <el-progress
            v-if="uploading"
            :percentage="uploadProgress"
            :stroke-width="6"
            style="margin-top: var(--fts-space-2);"
          />
        </el-form-item>

        <!-- 档案编号 -->
        <el-form-item label="档案编号">
          <el-input
            v-model="formData.archiveNumber"
            placeholder="请填写纸质合同档案编号（如 HR-2024-001）"
            style="width: 100%;"
          />
        </el-form-item>

        <!-- 存放位置 -->
        <el-form-item label="存放位置" required>
          <el-input
            v-model="formData.archiveLocation"
            placeholder="请填写纸质合同存放位置（如 档案柜A-3-02）"
            style="width: 100%;"
          />
        </el-form-item>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="可填写备注信息（选填）"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
      >
        <template #title>
          <span>上传扫描件后，纸质合同将与电子合同统一管理，可在合同详情中查看和下载扫描件。</span>
        </template>
      </el-alert>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleSubmit">确认上传</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.paper-scan-dialog {
  min-height: 300px;
}

.scan-uploader {
  width: 100%;

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: var(--fts-space-5);
    border: 1px dashed var(--fts-border-primary);
    border-radius: var(--fts-radius-base);
    background: var(--fts-bg-card);
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--fts-primary);
    }
  }
}

.upload-dragger-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  color: var(--fts-text-secondary);
}

.upload-text {
  font-size: var(--fts-font-size-lg);
  color: var(--fts-text-primary);
}

.upload-hint {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}
</style>
