<script setup lang="ts">
/**
 * TaskPublishPage - 发布任务（员工端）
 *
 * 支持双层接收人模型：
 *   targetType: individual(指定个人) / department(指定部门)
 *   assignmentMode: designated(指派) / open_pickup(公开接取)
 *
 * 已拆分为3个子组件：
 *   - TaskBasicForm: 基础表单（标题/描述/类别/优先级/截止时间/类别专属字段）
 *   - TaskAssigneeSection: 接收人设置（员工选择器/组织单元选择/跨部门协作）
 *   - TaskFormActions: 操作按钮（取消/发布）
 */
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PageContainer } from '@/components/core'

// 子组件导入
import TaskBasicForm from './TaskPublishPage/components/TaskBasicForm.vue'
import TaskAssigneeSection from './TaskPublishPage/components/TaskAssigneeSection.vue'
import TaskFormActions from './TaskPublishPage/components/TaskFormActions.vue'

const router = useRouter()
const submitting = ref(false)

// ==================== 类型定义 ====================
type TargetType = 'individual' | 'department' | 'store'
type AssignmentMode = 'designated' | 'open_pickup'
type CategoryType = '' | 'daily' | 'training' | 'business_trip' | 'inventory'

// ==================== 类别规则配置 ====================
interface CategoryRule {
  label: string
  allowedTargets: TargetType[]
  defaultTarget: TargetType
  defaultMode: AssignmentMode
  forceSingle?: boolean
  recommendCrossDept?: boolean
  assigneeHint: string
}

const CATEGORY_RULES: Record<string, CategoryRule> = {
  daily: {
    label: '日常指派',
    allowedTargets: ['individual', 'department'],
    defaultTarget: 'individual',
    defaultMode: 'designated',
    assigneeHint: '选择执行此任务的同事',
  },
  training: {
    label: '培训考核',
    allowedTargets: ['department'],
    defaultTarget: 'department',
    defaultMode: 'designated',
    assigneeHint: '选择需要参加培训的部门',
  },
  business_trip: {
    label: '出差任务',
    allowedTargets: ['individual'],
    defaultTarget: 'individual',
    defaultMode: 'designated',
    forceSingle: true,
    assigneeHint: '选择出差人员（仅限1人）',
  },
  inventory: {
    label: '盘点任务',
    allowedTargets: ['department', 'store'],
    defaultTarget: 'department',
    defaultMode: 'open_pickup',
    recommendCrossDept: true,
    assigneeHint: '选择需要盘点的部门或门店，全体成员将自动接收任务',
  },
}

// ==================== 表单数据 ====================
const form = ref({
  title: '',
  description: '',
  category: '' as CategoryType,
  priority: 'medium' as 'high' | 'medium' | 'low',
  deadline: '',
  targetType: 'individual' as TargetType,
  targetId: '' as string,
  assignmentMode: 'designated' as AssignmentMode,
  allowCrossDept: false,
  assigneeIds: [] as string[],
  // ---- 类别专属字段 ----
  destination: '',
  tripStartDate: '',
  tripEndDate: '',
  expenseBudget: '',
  inventoryScope: '' as '' | 'full_store' | 'kitchen' | 'dining_area' | 'warehouse',
})

// ==================== 类别选项 ====================
const categoryOptions = [
  { value: 'daily', label: '日常指派', desc: '指派日常工作给同事' },
  { value: 'training', label: '培训考核', desc: '部门级培训学习任务' },
  { value: 'business_trip', label: '出差任务', desc: '指定人员外出工作' },
  { value: 'inventory', label: '盘点任务', desc: '部门/门店库存盘点' },
]

const priorityOptions = [
  { value: 'high', label: '高优先级' },
  { value: 'medium', label: '普通' },
  { value: 'low', label: '低优先级' },
]

const inventoryScopeOptions = [
  { value: 'full_store', label: '全店盘点' },
  { value: 'kitchen', label: '后厨区域' },
  { value: 'dining_area', label: '前厅区域' },
  { value: 'warehouse', label: '仓储区域' },
]

const targetTypeOptions = [
  { value: 'individual', label: '指定个人', icon: 'User' },
  { value: 'department', label: '指定部门', icon: 'OfficeBuilding' },
  { value: 'store', label: '指定门店', icon: 'Team' },
]

// ==================== 全量数据（模拟后端API返回）==========
interface Employee {
  id: string
  name: string
  deptId: string
  deptName: string
  storeId: string
  storeName: string
  role: 'store_manager' | 'dept_head' | 'staff'
  avatar?: string
}

interface OrgUnit {
  id: string
  name: string
  type: 'department' | 'store'
  memberCount: number
  headName: string
  headId: string
  parentStoreId?: string
}

/** 当前登录用户（模拟从 userStore / API 获取） */
const currentUser = ref<Employee>({
  id: 'emp-001',
  name: '张三',
  deptId: 'dept-001',
  deptName: '后厨部',
  storeId: 'store-001',
  storeName: '朝阳门店',
  role: 'store_manager',
})

// ---- 全量员工池（后续由API提供）----
const allEmployees: Employee[] = [
  // 本店 - 后厨部
  { id: 'emp-001', name: '张三',   deptId: 'dept-001', deptName: '后厨部', storeId: 'store-001', storeName: '朝阳门店', role: 'store_manager' },
  { id: 'emp-003', name: '王五',   deptId: 'dept-001', deptName: '后厨部', storeId: 'store-001', storeName: '朝阳门店', role: 'dept_head' },
  { id: 'emp-008', name: '孙九',   deptId: 'dept-001', deptName: '后厨部', storeId: 'store-001', storeName: '朝阳门店', role: 'staff' },
  { id: 'emp-009', name: '周十',   deptId: 'dept-001', deptName: '后厨部', storeId: 'store-001', storeName: '朝阳门店', role: 'staff' },
  // 本店 - 前厅部
  { id: 'emp-002', name: '李四',   deptId: 'dept-002', deptName: '前厅部', storeId: 'store-001', storeName: '朝阳门店', role: 'dept_head' },
  { id: 'emp-004', name: '赵六',   deptId: 'dept-002', deptName: '前厅部', storeId: 'store-001', storeName: '朝阳门店', role: 'staff' },
  { id: 'emp-010', name: '吴十一', deptId: 'dept-002', deptName: '前厅部', storeId: 'store-001', storeName: '朝阳门店', role: 'staff' },
  // 本店 - 仓储部
  { id: 'emp-005', name: '钱七',   deptId: 'dept-003', deptName: '仓储部', storeId: 'store-001', storeName: '朝阳门店', role: 'staff' },
  // 其他门店（跨店场景）
  { id: 'emp-101', name: '郑十二', deptId: 'dept-101', deptName: '后厨部', storeId: 'store-002', storeName: '海淀门店', role: 'dept_head' },
  { id: 'emp-102', name: '冯十三', deptId: 'dept-102', deptName: '前厅部', storeId: 'store-002', storeName: '海淀门店', role: 'staff' },
]

// ---- 全量组织单元（部门+门店）----
const allOrgUnits: OrgUnit[] = [
  // 本店部门
  { id: 'dept-001', name: '后厨部', type: 'department', memberCount: 4, headName: '王五', headId: 'emp-003', parentStoreId: 'store-001' },
  { id: 'dept-002', name: '前厅部', type: 'department', memberCount: 3, headName: '李四', headId: 'emp-002', parentStoreId: 'store-001' },
  { id: 'dept-003', name: '仓储部', type: 'department', memberCount: 1, headName: '钱七', headId: 'emp-005', parentStoreId: 'store-001' },
  // 本店
  { id: 'store-001', name: '朝阳门店', type: 'store', memberCount: 8, headName: '张三', headId: 'emp-001' },
  // 其他门店（仅店长可见）
  { id: 'store-002', name: '海淀门店', type: 'store', memberCount: 6, headName: '陈店长', headId: 'emp-200' },
]

// ==================== 基于角色的权限计算 ====================

/** 用户角色标签 */
const userRoleLabel = computed(() => {
  const labels: Record<string, string> = {
    store_manager: '店长',
    dept_head: '部门主管',
    staff: '员工',
  }
  return labels[currentUser.value.role] || '员工'
})

/** 用户可管理的范围描述 */
const userScopeLabel = computed(() => {
  const u = currentUser.value
  if (u.role === 'store_manager') return `${u.storeName}（全店管理）`
  if (u.role === 'dept_head') return `${u.storeName} · ${u.deptName}`
  return `个人`
})

/**
 * 核心逻辑：根据当前用户角色，过滤可选择的员工列表
 */
const availableEmployees = computed<Employee[]>(() => {
  const u = currentUser.value
  switch (u.role) {
    case 'store_manager':
      return allEmployees.filter(e => e.storeId === u.storeId)
    case 'dept_head':
      return allEmployees.filter(e => e.deptId === u.deptId)
    case 'staff':
      return allEmployees.filter(e => e.id === u.id)
    default:
      return []
  }
})

/**
 * 根据当前用户角色，过滤可选择的组织单元（部门/门店）
 */
const availableOrgUnits = computed<OrgUnit[]>(() => {
  const u = currentUser.value
  switch (u.role) {
    case 'store_manager':
      return allOrgUnits.filter(
        ou => ou.id === u.storeId || ou.parentStoreId === u.storeId
      )
    case 'dept_head':
      return allOrgUnits.filter(ou => ou.id === u.deptId)
    case 'staff':
      return []
    default:
      return []
  }
})

/** 是否允许发起跨部门协作 */
const canInitiateCrossDept = computed(() =>
  ['store_manager', 'dept_head'].includes(currentUser.value.role)
)

/** 跨部门可选的其他部门 */
const crossDeptOptions = computed<OrgUnit[]>(() => {
  if (!canInitiateCrossDept.value) return []
  const u = currentUser.value
  return allOrgUnits.filter(ou =>
    ou.type === 'department' &&
    ou.id !== u.deptId &&
    (u.role === 'store_manager' ? ou.parentStoreId === u.storeId : true)
  )
})

// ==================== 计算属性 ====================

/** 当前类别的规则 */
const currentRule = computed<CategoryRule | undefined>(() =>
  form.value.category ? CATEGORY_RULES[form.value.category] : undefined
)

/** 可选的目标类型（受类别规则 + 用户角色双重限制） */
const availableTargetTypes = computed(() => {
  let options = targetTypeOptions
  if (currentRule.value) {
    options = options.filter(opt =>
      currentRule.value!.allowedTargets.includes(opt.value as TargetType)
    )
  }
  if (currentUser.value.role === 'staff') {
    options = options.filter(opt => opt.value === 'individual')
  }
  if (currentUser.value.role === 'dept_head') {
    options = options.filter(opt => opt.value !== 'store')
  }
  return options
})

/** 是否强制单选（出差任务） */
const isForceSingle = computed(() => currentRule.value?.forceSingle ?? false)

/** 接收人区域提示文字 */
const assigneeHintText = computed(() => {
  const base = currentRule.value?.assigneeHint ?? '选择接收人'
  const scope = userScopeLabel.value
  return `${base}（可选范围：${scope}）`
})

/** 是否显示跨部门协作区 */
const showCrossDeptSection = computed(() =>
  form.value.assignmentMode === 'open_pickup' &&
  canInitiateCrossDept.value &&
  crossDeptOptions.value.length > 0
)

// ==================== 方法 ====================

/** 切换类别时自动应用该类别的默认规则 */
function onCategoryChange() {
  const rule = CATEGORY_RULES[form.value.category]
  if (rule) {
    if (!rule.allowedTargets.includes(form.value.targetType)) {
      form.value.targetType = rule.defaultTarget
    }
    form.value.assignmentMode = rule.defaultMode
    if (rule.forceSingle) {
      form.value.assigneeIds = []
    }
    if (rule.recommendCrossDept) {
      form.value.allowCrossDept = true
    } else {
      form.value.allowCrossDept = false
    }
  }
}

/** 切换目标类型时重置关联字段 */
function onTargetTypeChange() {
  form.value.targetId = ''
  form.value.assigneeIds = []
}

/** 选择/取消员工 */
function toggleEmployee(empId: string) {
  const idx = form.value.assigneeIds.indexOf(empId)
  if (idx >= 0) {
    form.value.assigneeIds.splice(idx, 1)
  } else {
    if (isForceSingle.value) {
      form.value.assigneeIds = [empId]
    } else {
      form.value.assigneeIds.push(empId)
    }
  }
}

/** 选择组织单元（部门/门店） */
function selectOrgUnit(orgId: string) {
  form.value.targetId = orgId
}

/** 跨部门协作：选择要邀请的部门 */
const crossDeptSelected = ref<string[]>([])

function toggleCrossDept(deptId: string) {
  const idx = crossDeptSelected.value.indexOf(deptId)
  if (idx >= 0) {
    crossDeptSelected.value.splice(idx, 1)
  } else {
    crossDeptSelected.value.push(deptId)
  }
}

// ==================== 表单验证 ====================
function validateForm(): boolean {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return false
  }
  if (!form.value.category) {
    ElMessage.warning('请选择任务类别')
    return false
  }
  if (!form.value.deadline) {
    ElMessage.warning('请设置截止时间')
    return false
  }
  // 接收人校验
  if (form.value.targetType === 'individual') {
    if (form.value.assigneeIds.length === 0) {
      ElMessage.warning('请选择至少一位接收人')
      return false
    }
  } else {
    if (!form.value.targetId) {
      ElMessage.warning('请选择目标部门或门店')
      return false
    }
  }
  return true
}

// ==================== 提交发布 ====================
async function handleSubmit() {
  if (!validateForm()) return

  submitting.value = true
  try {
    // Mock：模拟发布成功
    await new Promise(resolve => setTimeout(resolve, 800))

    ElMessage.success('任务已发布')
    router.push('/tasks')
  } catch {
    // 响应拦截器已统一处理错误提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PageContainer title="发布任务">
    <div class="publish-page">
      <form class="publish-form" @submit.prevent="handleSubmit">
        <!-- 基础表单 -->
        <TaskBasicForm
          v-model:title="form.title"
          v-model:description="form.description"
          v-model:category="form.category"
          v-model:priority="form.priority"
          v-model:deadline="form.deadline"
          v-model:destination="form.destination"
          v-model:expenseBudget="form.expenseBudget"
          v-model:tripStartDate="form.tripStartDate"
          v-model:tripEndDate="form.tripEndDate"
          v-model:inventoryScope="form.inventoryScope"
          @category-change="onCategoryChange"
        />

        <!-- 接收人设置 -->
        <TaskAssigneeSection
          :category="form.category"
          :target-type="form.targetType"
          :target-id="form.targetId"
          :assignment-mode="form.assignmentMode"
          :allow-cross-dept="form.allowCrossDept"
          :assignee-ids="form.assigneeIds"
          :available-employees="availableEmployees"
          :available-org-units="availableOrgUnits"
          :available-target-types="availableTargetTypes"
          :user-role-label="userRoleLabel"
          :assignee-hint-text="assigneeHintText"
          :is-force-single="isForceSingle"
          :show-cross-dept-section="showCrossDeptSection"
          :cross-dept-options="crossDeptOptions"
          :cross-dept-selected="crossDeptSelected"
          @update:target-type="(val: TargetType) => { form.targetType = val; onTargetTypeChange() }"
          @update:target-id="(val: string) => selectOrgUnit(val)"
          @update:assignment-mode="(val: AssignmentMode) => form.assignmentMode = val"
          @update:allow-cross-dept="(val: boolean) => form.allowCrossDept = val"
          @toggle-employee="(empId: string) => toggleEmployee(empId)"
          @toggle-cross-dept="(deptId: string) => toggleCrossDept(deptId)"
        />

        <!-- 操作按钮 -->
        <TaskFormActions
          :submitting="submitting"
          :category="form.category"
          @submit="handleSubmit"
          @cancel="router.push('/tasks')"
        />
      </form>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.publish-page {
  /* 为日期选择器弹出层预留空间（datetime-picker 面板约320px + 安全边距） */
  padding-bottom: calc(var(--fts-space-6) + 360px);
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}
</style>
