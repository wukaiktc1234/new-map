<script setup lang="ts">
/**
 * 组织表单对话框
 *
 * 从 HROrganization.vue 中提取，负责新增/编辑组织的表单渲染、
 * 上级组织选项计算以及提交（调用 departmentApi）。
 */
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { departmentApi } from '@/api/hr/department'
import type { OrgNode, OrgType, OrgFormData } from './org-types'

interface Props {
  modelValue: boolean
  title: string
  mode: 'add-top' | 'add-child' | 'edit'
  nodes: OrgNode[]
  editingNodeId?: string | null
  initialForm?: Partial<OrgFormData>
}

const props = withDefaults(defineProps<Props>(), {
  editingNodeId: null,
  initialForm: () => ({}),
})

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  success: []
  cancel: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const orgForm = ref<OrgFormData>({
  name: '',
  type: 'company',
  parentId: null,
  manager: '',
  phone: '',
  location: '',
  description: '',
})

const ORG_TYPE_OPTIONS = [
  { label: '公司', value: 'company' },
  { label: '部门', value: 'department' },
  { label: '门店', value: 'store' },
  { label: '仓库', value: 'warehouse' },
  { label: '小组', value: 'group' },
  { label: '办事处', value: 'office' },
  { label: '团队', value: 'team' },
]

const formRules: FormRules = {
  name: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
}

function resetOrgForm(type: OrgType = 'company') {
  orgForm.value = {
    name: '',
    type,
    parentId: null,
    manager: '',
    phone: '',
    location: '',
    description: '',
  }
}

function fillForm(data: Partial<OrgFormData>) {
  orgForm.value = {
    name: data.name ?? '',
    type: data.type ?? 'company',
    parentId: data.parentId ?? null,
    manager: data.manager ?? '',
    phone: data.phone ?? '',
    location: data.location ?? '',
    description: data.description ?? '',
  }
}

function handleOpen() {
  nextTick(() => {
    if (props.mode === 'edit' && props.initialForm) fillForm(props.initialForm)
    else resetOrgForm(props.initialForm?.type ?? 'company')
  })
}

function handleClose() {
  emit('update:modelValue', false)
  emit('cancel')
  resetOrgForm('company')
}

/** 收集指定节点及其所有子孙节点的ID */
function collectSelfAndDescendantIds(node: OrgNode): Set<string> {
  const ids = new Set<string>([node.id])
  if (node.children) {
    for (const child of node.children) {
      ids.add(child.id)
      const childIds = collectSelfAndDescendantIds(child)
      childIds.forEach(id => ids.add(id))
    }
  }
  return ids
}

function findNodeById(nodes: OrgNode[], id: string): OrgNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 递归收集所有可作为上级组织的节点选项 */
function collectParentOptions(
  nodes: OrgNode[],
  excludeIds: Set<string>,
  prefix: string = '',
): { label: string; value: string }[] {
  const options: { label: string; value: string }[] = []
  for (const node of nodes) {
    if (!excludeIds.has(node.id)) {
      options.push({ label: prefix + node.name, value: node.id })
      if (node.children) {
        options.push(...collectParentOptions(node.children, excludeIds, prefix + '　'))
      }
    }
  }
  return options
}

/** 上级组织下拉选项（顶级固定为 null） */
const parentOrgOptions = computed(() => {
  const topOption = { label: '无上级（顶级组织）', value: '' }
  const excludeIds = new Set<string>()
  if (props.mode === 'edit' && props.editingNodeId) {
    const editingNode = findNodeById(props.nodes, props.editingNodeId)
    if (editingNode) {
      collectSelfAndDescendantIds(editingNode).forEach(id => excludeIds.add(id))
    }
  }
  return [topOption, ...collectParentOptions(props.nodes, excludeIds)]
})

watch(
  () => props.modelValue,
  (visible) => { if (visible) handleOpen() },
)

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const selectedParentId = orgForm.value.parentId ? String(orgForm.value.parentId).trim() : ''
    const parentIdValue = selectedParentId ? Number(selectedParentId) : 0

    if (props.mode === 'edit' && props.editingNodeId) {
      const dto = {
        name: orgForm.value.name.trim(),
        type: orgForm.value.type,
        parentId: parentIdValue,
        manager: orgForm.value.manager.trim() || undefined,
        remark: orgForm.value.description.trim() || undefined,
      }
      await departmentApi.updateDepartment(props.editingNodeId, dto)
      ElMessage.success('更新成功')
    } else {
      const payload = {
        departmentName: orgForm.value.name.trim(),
        type: orgForm.value.type,
        managerName: orgForm.value.manager.trim() || undefined,
        description: orgForm.value.description.trim() || undefined,
        parentId: parentIdValue,
      }
      await departmentApi.addDepartment(payload)
      ElMessage.success('新增成功')
    }

    emit('update:modelValue', false)
    emit('success')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '操作失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="680px"
    class="fts-dialog--md"
    destroy-on-close
    lock-scroll="false"
    @open="handleOpen"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="orgForm" :rules="formRules" label-width="100px">
      <el-form-item label="组织名称" prop="name">
        <el-input v-model="orgForm.name" placeholder="请输入组织名称" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="组织类型" prop="type">
        <el-select
          v-model="orgForm.type"
          :teleported="false"
          placeholder="请选择组织类型"
          style="width: 100%"
        >
          <el-option v-for="opt in ORG_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="上级组织" prop="parentId">
        <el-select
          v-model="orgForm.parentId"
          :teleported="false"
          placeholder="请选择上级组织（留空则为顶级组织）"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="opt in parentOrgOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="负责人" prop="manager">
        <el-input v-model="orgForm.manager" placeholder="请输入负责人姓名" maxlength="20" />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="orgForm.phone" placeholder="请输入联系电话" maxlength="20" />
      </el-form-item>
      <el-form-item label="办公地点" prop="location">
        <el-input v-model="orgForm.location" placeholder="请输入办公地点" maxlength="100" />
      </el-form-item>
      <el-form-item :label="orgForm.type === 'company' ? '公司简介' : '组织职责'" prop="description">
        <el-input
          v-model="orgForm.description"
          type="textarea"
          :rows="4"
          placeholder="请输入部门职责或公司简介"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
