<script setup lang="ts">
import { ref, reactive, watch, computed, nextTick, onUnmounted, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { Plus, UserFilled, Delete } from '@element-plus/icons-vue'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { useLocationOptions } from '@/composables/useLocationOptions'
import { positionApi } from '@/api/hr/position'
import type { PositionItem } from '@/api/hr/position'

interface Employee {
  id?: number | string
  name: string
  employeeNo: string
  gender?: string
  department: string
  departmentId?: string
  position: string
  positionId?: string
  phone: string
  email: string
  entryDate: string
  birthday: string
  education: string
  emergencyContact: string
  status: string
  salary?: number
  employmentType?: string  // full_time/part_time/intern/dispatch/over_age
  lifecycleStatus?: string // candidate/onboarding/probation/active/over_age_employed/resigning/resigned/retired
  retirementDate?: string  // 退休日期（超龄返聘时填写）
  reemploymentDate?: string // 返聘日期
  agreementNo?: string     // 劳务协议编号
  locationId?: number      // C5 (D2)：归属位置ID（locations.location_id；N6：number ↔ 后端 Long）
  workLocationName?: string // 工作归属显示名称
}

interface Props {
  modelValue: boolean
  editData: Employee | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [data: Employee]
  close: []
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const avatarUrl = ref('')
const avatarFile = ref<File | null>(null)

const formData = reactive<Employee>({
  name: '',
  employeeNo: '',
  gender: 'other',
  department: '',
  departmentId: '',
  position: '',
  phone: '',
  email: '',
  entryDate: '',
  birthday: '',
  education: '',
  emergencyContact: '',
  status: 'active',
  salary: undefined,
  employmentType: 'full_time',
  retirementDate: '',
  reemploymentDate: '',
  agreementNo: '',
  locationId: undefined,
})

const statusOptions = [
  { label: '在职', value: 'active' },
  { label: '试用期', value: 'probation' },
  { label: '已离职', value: 'inactive' },
]

const genderOptions = [
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
  { label: '其他', value: 'other' },
]

/** C5 (D2)：归属位置单一数据源 GET /v1/locations（复用 useLocationOptions，门店侧已收敛）；
 *  U9：TRANSIT 禁作员工归属 → 前端先行过滤（后端 C2b 对 TRANSIT 亦 400 拒绝） */
const { locationOptions } = useLocationOptions(true)
const employeeLocationOptions = computed(() =>
  locationOptions.value.filter(l => l.locationType !== 'TRANSIT'),
)

/** 位置类型中文标签（locations.location_type → 展示文案） */
const locationTypeLabels: Record<string, string> = {
  STORE: '门店',
  CENTRAL: '中央仓',
  DEPOT: '仓储仓',
  TRANSIT: '在途',
}

/** C5 (D2)：「归属类型」降级为只读展示，值由所选 location.locationType 推导（不再先选类型再选实体） */
const derivedLocationTypeLabel = computed(() => {
  const loc = locationOptions.value.find(l => l.locationId === formData.locationId)
  if (!loc) return '总部（未选位置 = 总部职能）'
  return locationTypeLabels[loc.locationType] || loc.locationType
})

const positionOptions = ref<PositionItem[]>([])

/** 根据已选部门过滤岗位选项 */
const filteredPositionOptions = computed(() => {
  if (!formData.departmentId) return positionOptions.value
  return positionOptions.value.filter(p => p.departmentId === formData.departmentId)
})

// 部门选项（接入真实后端 API）
const { departmentOptions, getDepartmentById } = useDepartmentOptions(true)

const isEditMode = computed(() => props.editData !== null)
const dialogTitle = computed(() => isEditMode.value ? '编辑员工' : '新增员工')

const rules: FormRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度2-20个字符', trigger: 'blur' },
  ],
  employeeNo: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9-]+$/, message: '工号只能包含字母、数字和连字符', trigger: 'blur' },
  ],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  entryDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
  salary: [
    { required: true, message: '请输入薪资', trigger: 'blur' },
    {
      validator: (_rule, value: number, callback: (error?: Error) => void) => {
        if (value == null) { callback(new Error('请输入薪资')); return }
        if (value < 0) { callback(new Error('薪资不能小于0')); return }
        if (value > 9999999.99) { callback(new Error('薪资超出范围')); return }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  formData.name = ''
  formData.employeeNo = ''
  formData.gender = 'other'
  formData.department = ''
  formData.departmentId = ''
  formData.position = ''
  formData.positionId = ''
  formData.phone = ''
  formData.email = ''
  formData.entryDate = ''
  formData.birthday = ''
  formData.education = ''
  formData.emergencyContact = ''
  formData.status = 'active'
  formData.salary = undefined
  formData.employmentType = 'full_time'
  formData.retirementDate = ''
  formData.reemploymentDate = ''
  formData.agreementNo = ''
  formData.locationId = undefined
  avatarUrl.value = ''
  avatarFile.value = null
  formRef.value?.resetFields()
}

function fillEditData(data: Employee) {
  formData.name = data.name
  formData.employeeNo = data.employeeNo
  formData.gender = data.gender || 'other'
  formData.department = data.department
  formData.departmentId = data.departmentId || ''
  formData.position = data.position
  formData.positionId = data.positionId || ''
  formData.phone = data.phone
  formData.email = data.email
  formData.entryDate = data.entryDate
  formData.birthday = data.birthday
  formData.education = data.education
  formData.emergencyContact = data.emergencyContact
  formData.status = data.status
  formData.salary = data.salary
  formData.employmentType = data.employmentType || 'full_time'
  formData.retirementDate = data.retirementDate || ''
  formData.reemploymentDate = data.reemploymentDate || ''
  formData.agreementNo = data.agreementNo || ''
  formData.locationId = data.locationId ?? undefined
  avatarUrl.value = ''
}

function handleOpen() {
  nextTick(() => {
    if (isEditMode.value && props.editData) fillEditData(props.editData)
    else resetForm()
  })
}

function handleClose() {
  emit('update:modelValue', false)
  emit('close')
  resetForm()
}

function handleCancel() { handleClose() }

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: Employee = { ...formData }
    if (avatarFile.value) { /* 实际项目中上传头像: avatarApi.upload(avatarFile.value) */ }

    emit('submit', submitData)
    ElMessage.success(isEditMode.value ? '编辑成功' : '新增成功')
    handleClose()
  } finally {
    submitLoading.value = false
  }
}

function handleAvatarChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) return

  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!allowedTypes.includes(file.type)) { ElMessage.error('仅支持 JPG、PNG、GIF 格式'); return }

  const maxSize = 2 * 1024 * 1024
  if (file.size > maxSize) { ElMessage.error('图片大小不能超过2MB'); return }

  avatarFile.value = file
  avatarUrl.value = URL.createObjectURL(file)
}

function handleRemoveAvatar() {
  if (avatarUrl.value && avatarUrl.value.startsWith('blob:')) URL.revokeObjectURL(avatarUrl.value)
  avatarUrl.value = ''
  avatarFile.value = null
}

function handlePositionChange(positionId: string) {
  const pos = positionOptions.value.find(p => p.positionId === positionId)
  formData.position = pos ? pos.positionName : ''
}

/** 加载岗位选项 */
async function loadPositionOptions(): Promise<void> {
  try {
    positionOptions.value = await positionApi.getAll()
  } catch {
    positionOptions.value = []
  }
}

// 监听 departmentId 变化，同步 department（部门名称）以兼容 HREmployee.vue 对 name 字段的依赖
watch(
  () => formData.departmentId,
  (newId) => {
    if (!newId) {
      formData.department = ''
      return
    }
    const dept = getDepartmentById(newId)
    formData.department = dept?.name || ''
    // 如果已选岗位不属于新部门，清空岗位选择
    if (formData.positionId) {
      const pos = positionOptions.value.find(p => p.positionId === formData.positionId)
      if (pos && pos.departmentId !== newId) {
        formData.positionId = ''
        formData.position = ''
      }
    }
  },
)

watch(
  () => props.editData,
  (newVal) => { if (props.modelValue && newVal) nextTick(() => fillEditData(newVal)) },
  { deep: true },
)

onMounted(() => {
  loadPositionOptions()
})

onUnmounted(() => {
  if (avatarUrl.value && avatarUrl.value.startsWith('blob:')) URL.revokeObjectURL(avatarUrl.value)
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="960px"
    class="fts-dialog--lg employee-form-dialog"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    :lock-scroll="false"
    @open="handleOpen"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="90px"
      label-position="right"
      class="employee-form"
    >
      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          action=""
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleAvatarChange"
          accept="image/jpeg,image/png,image/gif"
        >
          <div v-if="avatarUrl" class="avatar-preview-wrapper">
            <img :src="avatarUrl" alt="头像预览" class="avatar-preview" />
            <div class="avatar-overlay" @click.stop>
              <el-button type="danger" circle size="small" :icon="Delete" @click.stop="handleRemoveAvatar" />
            </div>
          </div>
          <div v-else class="avatar-placeholder">
            <el-icon :size="28"><UserFilled /></el-icon>
            <span class="avatar-text">点击上传头像</span>
            <span class="avatar-hint">JPG/PNG/GIF, 最大2MB</span>
          </div>
        </el-upload>
      </div>

      <div class="form-fields">
        <!-- ===== 基础信息 ===== -->
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="formData.name" placeholder="请输入姓名" maxlength="20" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工号" prop="employeeNo">
              <el-input v-model="formData.employeeNo" placeholder="请输入工号" maxlength="20" clearable :disabled="isEditMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formData.gender">
                <el-radio v-for="opt in genderOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="departmentId">
              <el-select
                v-model="formData.departmentId"
                placeholder="请选择部门"
                clearable
                class="form-select"
                :teleported="false"
              >
                <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.label" :value="dept.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="positionId">
              <el-select
                v-model="formData.positionId"
                placeholder="请选择岗位"
                clearable
                class="form-select"
                :teleported="false"
                @change="handlePositionChange"
              >
                <el-option
                  v-for="pos in filteredPositionOptions"
                  :key="pos.positionId"
                  :label="pos.positionName"
                  :value="pos.positionId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ===== 联系与状态 ===== -->
        <el-divider content-position="left">联系与状态</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用工类型" prop="employmentType">
              <el-select v-model="formData.employmentType" placeholder="请选择用工类型" clearable class="form-select" :teleported="false">
                <el-option label="全职" value="full_time" />
                <el-option label="兼职" value="part_time" />
                <el-option label="实习生" value="intern" />
                <el-option label="劳务派遣" value="dispatch" />
                <el-option label="超龄返聘" value="over_age" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ===== 工作归属 ===== -->
        <!-- C5 (D2)：合并为单一位置下拉（locations 数据源）；「归属类型」降级为只读展示，由所选位置类型推导。
             workLocationType / storeId / warehouseId 停止发送——后端 C2b 从 location_type 推导（U9/U10）为权威来源 -->
        <el-divider content-position="left">工作归属</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="归属位置" prop="locationId">
              <el-select v-model="formData.locationId" placeholder="请选择归属位置（不选 = 总部职能）" clearable class="form-select" :teleported="true">
                <el-option
                  v-for="opt in employeeLocationOptions"
                  :key="opt.locationId"
                  :label="`${opt.locationName}（${locationTypeLabels[opt.locationType] || opt.locationType}）`"
                  :value="opt.locationId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属类型">
              <el-input :model-value="derivedLocationTypeLabel" readonly placeholder="由所选位置类型推导" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ===== 超龄返聘专属信息 ===== -->
        <template v-if="formData.employmentType === 'over_age'">
          <el-divider content-position="left">超龄返聘专属信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="退休日期" prop="retirementDate">
                <el-date-picker v-model="formData.retirementDate" type="date" placeholder="请选择退休日期" value-format="YYYY-MM-DD" class="form-date-picker" :teleported="false" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="返聘日期" prop="reemploymentDate">
                <el-date-picker v-model="formData.reemploymentDate" type="date" placeholder="请选择返聘日期" value-format="YYYY-MM-DD" class="form-date-picker" :teleported="false" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="劳务协议编号">
                <el-input v-model="formData.agreementNo" placeholder="请输入劳务协议编号" clearable />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- ===== 用工信息 ===== -->
        <el-divider content-position="left">用工信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker
                v-model="formData.entryDate"
                type="date"
                placeholder="请选择入职日期"
                value-format="YYYY-MM-DD"
                class="form-date-picker"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪资" prop="salary">
              <el-input-number
                v-model="formData.salary"
                :min="0"
                :max="9999999.99"
                :precision="2"
                :step="100"
                placeholder="请输入薪资"
                class="form-input-number"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.employee-form-dialog {
  :deep(.el-dialog__body) {
    padding: var(--fts-space-5) var(--fts-space-6);
  }

  :deep(.el-dialog__header) {
    padding: var(--fts-space-4) var(--fts-space-6);
    border-bottom: 1px solid var(--fts-border-primary);
    margin-right: 0;
  }

  :deep(.el-dialog__title) {
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  :deep(.el-dialog__footer) {
    padding: var(--fts-space-4) var(--fts-space-6);
    border-top: 1px solid var(--fts-border-primary);
  }
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: var(--fts-space-6);
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 2px dashed var(--fts-border-primary);
    border-radius: var(--fts-page-radius);
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color var(--fts-duration-fast) var(--fts-easing-default);
    width: 100px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover { border-color: var(--fts-primary); }
  }
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-2);
  text-align: center;
  color: var(--fts-text-tertiary);
  gap: 2px;

  .avatar-text {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    white-space: nowrap;
  }

  .avatar-hint {
    font-size: 10px;
    color: var(--fts-text-tertiary);
    line-height: 1.2;
    max-width: 80px;
    word-break: break-all;
  }
}

.avatar-preview-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;

  &:hover .avatar-overlay { opacity: 1; }
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: color-mix(in srgb, var(--fts-text-primary) 40%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--fts-duration-fast) var(--fts-easing-default);
  border-radius: 50%;
}

.form-fields {
  .el-row {
    margin-bottom: var(--fts-space-1);
  }
}

.employee-form {
  :deep(.el-form-item__label) {
    color: var(--fts-text-secondary);
    font-weight: var(--fts-font-weight-medium);
  }
}

.form-select { width: 100%; }
.form-date-picker { width: 100%; }
.form-input-number { width: 100%; }

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--fts-space-3);
}

@media (max-width: 768px) {
  .employee-form-dialog.el-dialog {
    width: 100%;
    margin: 0;

    :deep(.el-dialog__body) { padding: var(--fts-space-4); }
  }

  .form-fields .el-col {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>