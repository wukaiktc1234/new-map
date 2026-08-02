<script setup lang="ts">
/**
 * 印章新增/编辑对话框
 */
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { sealApi } from '@/api/seal'
import {
  type SealFormData,
  type SealInfo,
  type SealType,
  type SealScene,
  SealTypeOptions,
  SealSceneOptions,
} from '@/types/seal'

const props = defineProps<{
  visible: boolean
  editData: SealInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive<SealFormData>({
  sealName: '',
  sealType: 'official',
  sealImage: '',
  keeper: '',
  authorizedUsers: [],
  authorizedScenes: [],
  remark: '',
})

const isEdit = computed(() => !!props.editData)

const rules: FormRules = {
  sealName: [{ required: true, message: '请输入印章名称', trigger: 'blur' }],
  sealType: [{ required: true, message: '请选择印章类型', trigger: 'change' }],
  sealImage: [{ required: true, message: '请上传印章图片', trigger: 'change' }],
  keeper: [{ required: true, message: '请输入保管人', trigger: 'blur' }],
}

/** 监听 visible 变化，初始化表单 */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      if (props.editData) {
        Object.assign(formData, {
          sealName: props.editData.sealName,
          sealType: props.editData.sealType,
          sealImage: props.editData.sealImage,
          keeper: props.editData.keeper,
          authorizedUsers: [...props.editData.authorizedUsers],
          authorizedScenes: [...props.editData.authorizedScenes],
          remark: props.editData.remark || '',
        })
      } else {
        Object.assign(formData, {
          sealName: '',
          sealType: 'official',
          sealImage: '',
          keeper: '',
          authorizedUsers: [],
          authorizedScenes: [],
          remark: '',
        })
      }
      formRef.value?.clearValidate()
    }
  },
)

/** 印章图片上传处理（转 Base64） */
async function handleSealChange(file: UploadFile) {
  if (!file.raw) return
  // 校验文件类型和大小
  const isImage = file.raw.type === 'image/png' || file.raw.type === 'image/jpeg'
  if (!isImage) {
    ElMessage.error('印章图片仅支持 PNG/JPG 格式')
    return
  }
  const isLt2M = file.raw.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('印章图片大小不能超过 2MB')
    return
  }
  // 转 Base64
  const reader = new FileReader()
  reader.onload = (e) => {
    formData.sealImage = e.target?.result as string
    formRef.value?.validateField('sealImage')
  }
  reader.readAsDataURL(file.raw)
}

/** 移除印章图片 */
function handleRemoveSeal() {
  formData.sealImage = ''
  formRef.value?.validateField('sealImage')
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && props.editData) {
      await sealApi.update(props.editData.sealId, formData)
      ElMessage.success('更新成功')
    } else {
      await sealApi.create(formData)
      ElMessage.success('创建成功')
    }
    emit('success')
    emit('update:visible', false)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

/** 关闭对话框 */
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑印章' : '新增印章'"
    width="640px"
    destroy-on-close
    append-to-body
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="印章名称" prop="sealName">
            <el-input v-model="formData.sealName" placeholder="如：公司公章" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="印章类型" prop="sealType">
            <el-select v-model="formData.sealType" placeholder="请选择" style="width:100%" :teleported="false">
              <el-option v-for="opt in SealTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="印章图片" prop="sealImage">
        <div class="seal-upload-area">
          <div v-if="formData.sealImage" class="seal-preview">
            <img :src="formData.sealImage" alt="印章预览" class="seal-img" />
            <el-button link type="danger" size="small" @click="handleRemoveSeal">移除</el-button>
          </div>
          <el-upload
            v-else
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/png,image/jpeg"
            :on-change="handleSealChange"
          >
            <div class="seal-upload-trigger">
              <el-icon :size="28"><Plus /></el-icon>
              <span>点击上传印章</span>
            </div>
          </el-upload>
          <div class="seal-upload-tip">支持 PNG/JPG，建议透明背景，不超过 2MB</div>
        </div>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="保管人" prop="keeper">
            <el-input v-model="formData.keeper" placeholder="请输入保管人" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="授权场景">
        <el-checkbox-group v-model="formData.authorizedScenes">
          <el-checkbox v-for="opt in SealSceneOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.seal-upload-area {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.seal-preview {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.seal-img {
  width: 96px;
  height: 96px;
  object-fit: contain;
  border: 1px dashed var(--fts-border-color);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-1);
  background: var(--fts-bg-secondary);
}

.seal-upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border: 1px dashed var(--fts-border-color);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  color: var(--fts-text-tertiary);
  gap: var(--fts-space-1);

  &:hover {
    border-color: var(--fts-primary);
    color: var(--fts-primary);
  }

  span {
    font-size: var(--fts-font-size-xs);
  }
}

.seal-upload-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
