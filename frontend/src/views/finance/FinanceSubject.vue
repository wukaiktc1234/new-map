<script setup lang="ts">
/**
 * 会计科目管理页面
 * 数据来源：/v1/subjects
 *
 * 功能：
 * - 科目树形展示（支持展开/收起）
 * - 新增科目（顶级或子科目）
 * - 编辑科目
 * - 启用/禁用科目
 * - 按编码/名称搜索、按类型/状态筛选
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { subjectApi } from '@/api/finance'
import type {
  SubjectTreeNode,
  SubjectFormData,
  SubjectType,
} from '@/types/finance'

defineOptions({ name: 'FinanceSubject' })

/** 科目类型配置：label + StatusTag 对应状态色 */
const subjectTypeConfig: Record<SubjectType, { label: string; status: string }> = {
  asset: { label: '资产', status: 'info' },
  liability: { label: '负债', status: 'warning' },
  equity: { label: '权益', status: 'success' },
  cost: { label: '成本', status: 'error' },
  income: { label: '收入', status: 'success' },
  profit: { label: '利润', status: 'pending' },
}

/** 科目类型下拉选项 */
const subjectTypeOptions: Array<{ value: SubjectType; label: string }> = [
  { value: 'asset', label: '资产' },
  { value: 'liability', label: '负债' },
  { value: 'equity', label: '权益' },
  { value: 'cost', label: '成本' },
  { value: 'income', label: '收入' },
  { value: 'profit', label: '利润' },
]

/** 余额方向配置 */
const directionConfig: Record<string, string> = {
  debit: '借方',
  credit: '贷方',
}

/* ===== 数据状态 ===== */
const loading = ref(false)
const subjectTree = ref<SubjectTreeNode[]>([])
const searchForm = ref({
  keyword: '',
  subjectType: '' as '' | SubjectType,
  status: '' as '' | 'active' | 'inactive',
})

/* ===== 对话框状态 ===== */
const dialogVisible = ref(false)
const dialogTitle = ref('新增科目')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()

/** 表单数据 */
const formData = ref<SubjectFormData>({
  subjectCode: '',
  subjectName: '',
  subjectType: 'asset',
  balanceDirection: 'debit',
  level: 0,
  parentId: undefined,
  status: 'active',
  remark: '',
})

/** 表单校验规则 */
const formRules = {
  subjectCode: [{ required: true, message: '请输入科目编码', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  subjectType: [{ required: true, message: '请选择科目类型', trigger: 'change' }],
  balanceDirection: [{ required: true, message: '请选择余额方向', trigger: 'change' }],
}

/* ===== 计算属性 ===== */

/** 扁平化科目树（用于父科目选择下拉） */
const flatSubjectList = computed(() => {
  const result: Array<SubjectTreeNode & { displayName: string }> = []
  const walk = (nodes: SubjectTreeNode[], prefix = '') => {
    for (const node of nodes) {
      result.push({ ...node, displayName: prefix + node.subjectName })
      if (node.children && node.children.length > 0) {
        walk(node.children, prefix + '　')
      }
    }
  }
  walk(subjectTree.value)
  return result
})

/** 父科目选项（编辑时排除自身及后代，防止循环引用） */
const parentOptions = computed(() => {
  if (!editingId.value) return flatSubjectList.value
  const excludeIds = new Set<string>()
  const currentNode = findNode(subjectTree.value, editingId.value)
  if (currentNode) {
    excludeIds.add(currentNode.id)
    collectDescendantIds(currentNode, excludeIds)
  }
  return flatSubjectList.value.filter(n => !excludeIds.has(n.id))
})

/** 过滤后的科目树（按关键词/类型/状态筛选，保留匹配节点的祖先链） */
const filteredTree = computed(() => {
  const { keyword, subjectType, status } = searchForm.value
  if (!keyword && !subjectType && !status) return subjectTree.value

  const kw = keyword.trim().toLowerCase()
  const predicate = (node: SubjectTreeNode): boolean => {
    if (kw && !node.subjectCode.toLowerCase().includes(kw) && !node.subjectName.toLowerCase().includes(kw)) {
      return false
    }
    if (subjectType && node.subjectType !== subjectType) return false
    if (status && node.status !== status) return false
    return true
  }
  return filterTree(subjectTree.value, predicate)
})

/* ===== 工具函数 ===== */

/** 递归过滤树节点（保留匹配节点的祖先链） */
function filterTree(nodes: SubjectTreeNode[], predicate: (node: SubjectTreeNode) => boolean): SubjectTreeNode[] {
  const result: SubjectTreeNode[] = []
  for (const node of nodes) {
    const children = node.children ? filterTree(node.children, predicate) : []
    if (predicate(node) || children.length > 0) {
      result.push({ ...node, children: children.length > 0 ? children : undefined })
    }
  }
  return result
}

/** 在树中查找节点 */
function findNode(nodes: SubjectTreeNode[], id: string): SubjectTreeNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 递归收集所有后代ID */
function collectDescendantIds(node: SubjectTreeNode, ids: Set<string>): void {
  if (node.children) {
    for (const child of node.children) {
      ids.add(child.id)
      collectDescendantIds(child, ids)
    }
  }
}

/** 根据父ID计算层级 */
function computeLevel(parentId: string | undefined): number {
  if (!parentId) return 0
  const parent = findNode(subjectTree.value, parentId)
  return parent ? parent.level + 1 : 0
}

/* ===== 数据加载 ===== */

/** 加载科目树 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const tree = await subjectApi.getTree()
    subjectTree.value = tree || []
  } catch {
    ElMessage.error('加载科目树失败')
    subjectTree.value = []
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */

/** 查询（filteredTree 为计算属性，自动响应筛选条件变化） */
function handleSearch(): void {
  // 筛选由 computed 自动处理，此处无需额外操作
}

/** 重置筛选条件 */
function handleReset(): void {
  searchForm.value = { keyword: '', subjectType: '', status: '' }
}

/* ===== 新增/编辑 ===== */

/** 打开新增顶级科目对话框 */
function handleCreate(): void {
  editingId.value = null
  dialogTitle.value = '新增科目'
  formData.value = {
    subjectCode: '',
    subjectName: '',
    subjectType: 'asset',
    balanceDirection: 'debit',
    level: 0,
    parentId: undefined,
    status: 'active',
    remark: '',
  }
  dialogVisible.value = true
}

/** 打开新增子科目对话框（预填上级科目信息） */
function handleAddChild(row: SubjectTreeNode): void {
  editingId.value = null
  dialogTitle.value = '新增子科目'
  formData.value = {
    subjectCode: '',
    subjectName: '',
    subjectType: row.subjectType,
    balanceDirection: row.balanceDirection,
    level: row.level + 1,
    parentId: row.id,
    status: 'active',
    remark: '',
  }
  dialogVisible.value = true
}

/** 打开编辑对话框（从后端拉取完整详情） */
async function handleEdit(row: SubjectTreeNode): Promise<void> {
  try {
    const detail = await subjectApi.getById(row.id)
    editingId.value = row.id
    dialogTitle.value = '编辑科目'
    formData.value = {
      id: detail.id,
      subjectCode: detail.subjectCode,
      subjectName: detail.subjectName,
      subjectType: detail.subjectType,
      balanceDirection: detail.balanceDirection,
      level: detail.level,
      parentId: detail.parentId,
      status: detail.status,
      remark: detail.remark || '',
    }
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载科目详情失败')
  }
}

/** 提交表单（新增/编辑） */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const submitData: SubjectFormData = {
      ...formData.value,
      level: computeLevel(formData.value.parentId),
    }
    if (editingId.value) {
      await subjectApi.update(editingId.value, submitData)
      ElMessage.success('更新成功')
    } else {
      await subjectApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(editingId.value ? '更新失败，请重试' : '创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

/* ===== 启用/禁用 ===== */

/** 切换科目启用/禁用状态 */
async function handleToggleStatus(row: SubjectTreeNode): Promise<void> {
  const action = row.status === 'active' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}科目"${row.subjectName}"吗？`,
      `${action}确认`,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await subjectApi.toggleStatus(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(`${action}失败，请重试`)
    }
  }
}

/* ===== 生命周期 ===== */
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="会计科目管理" description="管理会计科目体系，支持树形结构展示">
      <template #extra>
        <el-button type="primary" size="default" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>新增科目
        </el-button>
      </template>
    </PageHeader>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="searchForm.keyword"
            placeholder="科目编码/名称"
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="searchForm.subjectType"
            placeholder="科目类型"
            clearable
            style="width: 140px"
            size="default"
          >
            <el-option
              v-for="opt in subjectTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="searchForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
          >
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="table-section">
      <el-table
        :data="filteredTree"
        row-key="id"
        border
        default-expand-all
        v-loading="loading"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column prop="subjectCode" label="科目编码" width="160" />
        <el-table-column prop="subjectName" label="科目名称" min-width="200" />
        <el-table-column prop="subjectType" label="科目类型" width="100">
          <template #default="{ row }">
            <StatusTag
              :status="subjectTypeConfig[row.subjectType as SubjectType]?.status || 'info'"
              :label="subjectTypeConfig[row.subjectType as SubjectType]?.label || row.subjectType"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column prop="balanceDirection" label="余额方向" width="100">
          <template #default="{ row }">
            {{ directionConfig[row.balanceDirection] || row.balanceDirection }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="handleAddChild(row)">新增子科目</el-button>
            <el-button link type="primary" size="small" @click.stop="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'danger' : 'primary'"
              size="small"
              @click.stop="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑科目对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="上级科目">
          <el-select
            v-model="formData.parentId"
            :teleported="false"
            clearable
            placeholder="无（顶级科目）"
            style="width: 100%"
          >
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="`${item.subjectCode} ${item.displayName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目编码" prop="subjectCode">
          <el-input v-model="formData.subjectCode" placeholder="如：1001" />
        </el-form-item>
        <el-form-item label="科目名称" prop="subjectName">
          <el-input v-model="formData.subjectName" placeholder="如：库存现金" />
        </el-form-item>
        <el-form-item label="科目类型" prop="subjectType">
          <el-select
            v-model="formData.subjectType"
            :teleported="false"
            placeholder="请选择科目类型"
            style="width: 100%"
          >
            <el-option
              v-for="opt in subjectTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="余额方向" prop="balanceDirection">
          <el-radio-group v-model="formData.balanceDirection">
            <el-radio value="debit">借方</el-radio>
            <el-radio value="credit">贷方</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.table-section {
  :deep(.el-table) {
    --el-table-border-color: var(--fts-border-primary);
  }
}
</style>
