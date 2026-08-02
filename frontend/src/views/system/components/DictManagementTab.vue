<script setup lang="ts">
/**
 * 字典管理 Tab
 *
 * 左侧：字典类型列表（按分组筛选，支持新增/编辑/删除/启停）
 * 右侧：选中字典类型的字典项列表（支持新增/编辑/删除）
 *
 * 对应后端: /v1/dict (SysDictController)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import {
  sysDictApi,
  type DictTypeItem,
  type DictItem,
  type DictQueryForm,
  type DictItemQueryForm,
  type DictTypeCreateDTO,
  type DictTypeUpdateDTO,
  type DictItemCreateDTO,
  type DictItemUpdateDTO,
} from '@/api/system/sys-dict'

/* ===== 字典类型状态 ===== */
const dictTypes = ref<DictTypeItem[]>([])
const typeLoading = ref(false)
const groups = ref<string[]>([])
const selectedGroup = ref<string>('')
const typeSearchName = ref('')
const typeSearchCode = ref('')
const selectedDictId = ref<number | null>(null)

const selectedDict = computed<DictTypeItem | null>(() => {
  return dictTypes.value.find(d => d.dictId === selectedDictId.value) || null
})

/* ===== 字典项状态 ===== */
const dictItems = ref<DictItem[]>([])
const itemLoading = ref(false)
const itemSearchLabel = ref('')
const itemSearchStatus = ref<number | ''>('')

/* ===== 字典类型列定义 ===== */
const typeColumns: DataTableColumn[] = [
  { prop: 'dictName', label: '字典名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'dictCode', label: '编码', minWidth: 140, showOverflowTooltip: true },
  { prop: 'dictGroup', label: '分组', minWidth: 90, showOverflowTooltip: true },
  { prop: 'sortOrder', label: '排序', minWidth: 70, align: 'center' },
  { prop: 'status', label: '状态', minWidth: 80, align: 'center', slot: 'typeStatus' },
]

/* ===== 字典项列定义 ===== */
const itemColumns: DataTableColumn[] = [
  { prop: 'itemLabel', label: '标签', minWidth: 140, showOverflowTooltip: true },
  { prop: 'itemValue', label: '值', minWidth: 140, showOverflowTooltip: true },
  { prop: 'sortOrder', label: '排序', minWidth: 70, align: 'center' },
  { prop: 'isDefault', label: '默认', minWidth: 70, align: 'center', slot: 'itemDefault' },
  { prop: 'status', label: '状态', minWidth: 80, align: 'center', slot: 'itemStatus' },
  { prop: 'remark', label: '备注', minWidth: 140, showOverflowTooltip: true },
]

/* ===== 数据加载 ===== */
async function loadGroups(): Promise<void> {
  try {
    groups.value = await sysDictApi.getGroups()
  } catch {
    // 分组加载失败不阻断主流程
  }
}

async function loadDictTypes(): Promise<void> {
  typeLoading.value = true
  try {
    const params: DictQueryForm = { size: 200 }
    if (selectedGroup.value) params.dictGroup = selectedGroup.value
    if (typeSearchName.value.trim()) params.dictName = typeSearchName.value.trim()
    if (typeSearchCode.value.trim()) params.dictCode = typeSearchCode.value.trim()
    const res = await sysDictApi.getTypeList(params)
    dictTypes.value = res?.records || []
    // 保持选中状态，若已不存在则选第一个
    if (selectedDictId.value && !dictTypes.value.find(d => d.dictId === selectedDictId.value)) {
      selectedDictId.value = null
    }
    if (!selectedDictId.value && dictTypes.value.length > 0) {
      selectedDictId.value = dictTypes.value[0].dictId
      await loadDictItems()
    } else if (selectedDictId.value) {
      await loadDictItems()
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载字典类型失败')
    dictTypes.value = []
  } finally {
    typeLoading.value = false
  }
}

async function handleDictTypeClick(row: DictTypeItem): Promise<void> {
  selectedDictId.value = row.dictId
  itemSearchLabel.value = ''
  itemSearchStatus.value = ''
  await loadDictItems()
}

async function loadDictItems(): Promise<void> {
  if (!selectedDictId.value) {
    dictItems.value = []
    return
  }
  itemLoading.value = true
  try {
    const params: DictItemQueryForm = { dictId: selectedDictId.value, size: 200 }
    if (itemSearchLabel.value.trim()) params.itemLabel = itemSearchLabel.value.trim()
    if (itemSearchStatus.value !== '') params.status = itemSearchStatus.value
    const res = await sysDictApi.getItemList(params)
    dictItems.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载字典项失败')
    dictItems.value = []
  } finally {
    itemLoading.value = false
  }
}

async function handleGroupChange(): Promise<void> {
  await loadDictTypes()
}

async function handleTypeSearch(): Promise<void> {
  await loadDictTypes()
}

/* ===== 字典类型对话框 ===== */
const typeDialogVisible = ref(false)
const typeDialogMode = ref<'create' | 'edit'>('create')
const typeFormRef = ref<FormInstance>()
const typeSubmitting = ref(false)
const typeForm = ref<{
  dictId: number | null
  dictName: string
  dictCode: string
  dictGroup: string
  description: string
  sortOrder: number
}>({
  dictId: null,
  dictName: '',
  dictCode: '',
  dictGroup: 'default',
  description: '',
  sortOrder: 0,
})

const typeRules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictGroup: [{ required: true, message: '请输入字典分组', trigger: 'blur' }],
}

function openTypeCreate(): void {
  typeDialogMode.value = 'create'
  typeForm.value = {
    dictId: null,
    dictName: '',
    dictCode: '',
    dictGroup: selectedGroup.value || 'default',
    description: '',
    sortOrder: 0,
  }
  typeDialogVisible.value = true
}

function openTypeEdit(row: DictTypeItem): void {
  typeDialogMode.value = 'edit'
  typeForm.value = {
    dictId: row.dictId,
    dictName: row.dictName,
    dictCode: row.dictCode,
    dictGroup: row.dictGroup,
    description: row.description || '',
    sortOrder: row.sortOrder ?? 0,
  }
  typeDialogVisible.value = true
}

async function submitType(): Promise<void> {
  if (!typeFormRef.value) return
  try {
    await typeFormRef.value.validate()
  } catch {
    return
  }
  typeSubmitting.value = true
  try {
    if (typeDialogMode.value === 'create') {
      const payload: DictTypeCreateDTO = {
        dictName: typeForm.value.dictName,
        dictCode: typeForm.value.dictCode,
        dictGroup: typeForm.value.dictGroup,
        description: typeForm.value.description,
        sortOrder: typeForm.value.sortOrder,
      }
      await sysDictApi.createType(payload)
      ElMessage.success('字典类型已创建')
    } else {
      const payload: DictTypeUpdateDTO = {
        dictName: typeForm.value.dictName,
        dictGroup: typeForm.value.dictGroup,
        description: typeForm.value.description,
        sortOrder: typeForm.value.sortOrder,
      }
      await sysDictApi.updateType(typeForm.value.dictId as number, payload)
      ElMessage.success('字典类型已更新')
    }
    typeDialogVisible.value = false
    await loadGroups()
    await loadDictTypes()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  } finally {
    typeSubmitting.value = false
  }
}

async function handleDeleteType(row: DictTypeItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除字典类型「${row.dictName}」？其下所有字典项将一并删除。`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
    await sysDictApi.deleteType(row.dictId)
    ElMessage.success('已删除')
    if (selectedDictId.value === row.dictId) selectedDictId.value = null
    await loadDictTypes()
  } catch (error: unknown) {
    if (error === 'cancel' || error === 'close') return
    if (error instanceof Error) ElMessage.error(error.message || '删除失败')
  }
}

async function handleToggleType(row: DictTypeItem): Promise<void> {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    if (row.status === 1) {
      await sysDictApi.disableType(row.dictId)
    } else {
      await sysDictApi.enableType(row.dictId)
    }
    ElMessage.success(`已${action}`)
    await loadDictTypes()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || `${action}失败`)
  }
}

/* ===== 字典项对话框 ===== */
const itemDialogVisible = ref(false)
const itemDialogMode = ref<'create' | 'edit'>('create')
const itemFormRef = ref<FormInstance>()
const itemSubmitting = ref(false)
const itemForm = ref<{
  itemId: number | null
  itemLabel: string
  itemValue: string
  sortOrder: number
  colorType: string
  isDefault: number
  status: number
  remark: string
}>({
  itemId: null,
  itemLabel: '',
  itemValue: '',
  sortOrder: 0,
  colorType: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

const itemRules: FormRules = {
  itemLabel: [{ required: true, message: '请输入字典项标签', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入字典项值', trigger: 'blur' }],
}

/** 颜色类型选项 */
const colorTypeOptions = [
  { value: '', label: '默认' },
  { value: 'primary', label: '主要' },
  { value: 'success', label: '成功' },
  { value: 'warning', label: '警告' },
  { value: 'danger', label: '危险' },
  { value: 'info', label: '信息' },
]

/** 状态选项 */
const statusOptions = [
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
]

function openItemCreate(): void {
  if (!selectedDictId.value) {
    ElMessage.warning('请先选择字典类型')
    return
  }
  itemDialogMode.value = 'create'
  itemForm.value = {
    itemId: null,
    itemLabel: '',
    itemValue: '',
    sortOrder: dictItems.value.length + 1,
    colorType: '',
    isDefault: 0,
    status: 1,
    remark: '',
  }
  itemDialogVisible.value = true
}

function openItemEdit(row: DictItem): void {
  itemDialogMode.value = 'edit'
  itemForm.value = {
    itemId: row.itemId,
    itemLabel: row.itemLabel,
    itemValue: row.itemValue,
    sortOrder: row.sortOrder ?? 0,
    colorType: row.colorType || '',
    isDefault: row.isDefault ?? 0,
    status: row.status ?? 1,
    remark: row.remark || '',
  }
  itemDialogVisible.value = true
}

async function submitItem(): Promise<void> {
  if (!itemFormRef.value) return
  try {
    await itemFormRef.value.validate()
  } catch {
    return
  }
  itemSubmitting.value = true
  try {
    if (itemDialogMode.value === 'create') {
      const payload: DictItemCreateDTO = {
        dictId: selectedDictId.value as number,
        itemLabel: itemForm.value.itemLabel,
        itemValue: itemForm.value.itemValue,
        sortOrder: itemForm.value.sortOrder,
        colorType: itemForm.value.colorType,
        isDefault: itemForm.value.isDefault,
        remark: itemForm.value.remark,
      }
      await sysDictApi.createItem(payload)
      ElMessage.success('字典项已创建')
    } else {
      const payload: DictItemUpdateDTO = {
        itemLabel: itemForm.value.itemLabel,
        itemValue: itemForm.value.itemValue,
        sortOrder: itemForm.value.sortOrder,
        colorType: itemForm.value.colorType,
        isDefault: itemForm.value.isDefault,
        status: itemForm.value.status,
        remark: itemForm.value.remark,
      }
      await sysDictApi.updateItem(itemForm.value.itemId as number, payload)
      ElMessage.success('字典项已更新')
    }
    itemDialogVisible.value = false
    await loadDictItems()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  } finally {
    itemSubmitting.value = false
  }
}

async function handleDeleteItem(row: DictItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除字典项「${row.itemLabel}」？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
    await sysDictApi.deleteItem(row.itemId)
    ElMessage.success('已删除')
    await loadDictItems()
  } catch (error: unknown) {
    if (error === 'cancel' || error === 'close') return
    if (error instanceof Error) ElMessage.error(error.message || '删除失败')
  }
}

/* ===== 缓存管理 ===== */
async function handleRefreshCache(): Promise<void> {
  if (!selectedDictId.value) {
    ElMessage.warning('请先选择字典类型')
    return
  }
  try {
    await sysDictApi.refreshCache(selectedDictId.value)
    ElMessage.success('缓存已刷新')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '刷新缓存失败')
  }
}

async function handleClearAllCache(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定清除所有字典缓存？', '清除缓存', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    await sysDictApi.clearCache()
    ElMessage.success('所有字典缓存已清除')
  } catch (error: unknown) {
    if (error === 'cancel' || error === 'close') return
    if (error instanceof Error) ElMessage.error(error.message || '清除缓存失败')
  }
}

/* ===== 辅助函数 ===== */
/** 状态数字 → StatusTag 状态 */
function statusToTagStatus(status: number): 'success' | 'error' {
  return status === 1 ? 'success' : 'error'
}

/** 状态数字 → 文本 */
function statusToText(status: number): string {
  return status === 1 ? '启用' : '禁用'
}

/* ===== 初始化 ===== */
onMounted(async () => {
  await loadGroups()
  await loadDictTypes()
})
</script>

<template>
  <div class="dict-management">
    <!-- 顶部工具栏 -->
    <div class="dict-toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedGroup"
          placeholder="全部分组"
          clearable
          style="width: 160px"
          :teleported="false"
          @change="handleGroupChange"
        >
          <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
        </el-select>
        <el-input
          v-model="typeSearchName"
          placeholder="字典名称"
          clearable
          style="width: 160px"
          @keyup.enter="handleTypeSearch"
        />
        <el-input
          v-model="typeSearchCode"
          placeholder="字典编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleTypeSearch"
        />
        <el-button @click="handleTypeSearch">搜索</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="openTypeCreate">新增字典类型</el-button>
        <el-button :icon="Refresh" @click="loadDictTypes">刷新</el-button>
        <el-button @click="handleClearAllCache">清空缓存</el-button>
      </div>
    </div>

    <!-- 主体：左侧字典类型 + 右侧字典项 -->
    <div class="dict-layout">
      <!-- 左侧字典类型列表 -->
      <div class="dict-type-panel">
        <div class="panel-header">
          <span class="panel-title">字典类型</span>
          <span class="panel-count">{{ dictTypes.length }}</span>
        </div>
        <div v-loading="typeLoading" class="type-list">
          <div
            v-for="item in dictTypes"
            :key="item.dictId"
            :class="['type-item', { 'type-item--active': selectedDictId === item.dictId }]"
            @click="handleDictTypeClick(item)"
          >
            <div class="type-item__main">
              <span class="type-item__name">{{ item.dictName }}</span>
              <span class="type-item__code">{{ item.dictCode }}</span>
            </div>
            <div class="type-item__meta">
              <span class="type-item__group">{{ item.dictGroup }}</span>
              <StatusTag :status="statusToTagStatus(item.status)" :label="statusToText(item.status)" size="small" />
            </div>
          </div>
          <el-empty v-if="dictTypes.length === 0" description="暂无字典类型" :image-size="60" />
        </div>
      </div>

      <!-- 右侧字典项表格 -->
      <div class="dict-item-panel">
        <div class="item-header">
          <div class="item-header__left">
            <span class="item-header__title">
              {{ selectedDict ? selectedDict.dictName : '字典项' }}
            </span>
            <span v-if="selectedDict" class="item-header__code">{{ selectedDict.dictCode }}</span>
          </div>
          <div class="item-header__right">
            <el-input
              v-model="itemSearchLabel"
              placeholder="标签搜索"
              clearable
              style="width: 150px"
              :disabled="!selectedDictId"
              @keyup.enter="loadDictItems"
            />
            <el-select
              v-model="itemSearchStatus"
              placeholder="状态"
              clearable
              style="width: 110px"
              :teleported="false"
              :disabled="!selectedDictId"
              @change="loadDictItems"
            >
              <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-button :disabled="!selectedDictId" @click="loadDictItems">搜索</el-button>
            <el-button type="primary" :icon="Plus" :disabled="!selectedDictId" @click="openItemCreate">新增字典项</el-button>
            <el-button :disabled="!selectedDictId" @click="handleRefreshCache">刷新缓存</el-button>
          </div>
        </div>

        <DataTable
          :columns="itemColumns"
          :data="dictItems"
          :loading="itemLoading"
          :actions-width="160"
          stripe
        >
          <template #itemDefault="{ row }">
            <StatusTag :status="row.isDefault === 1 ? 'warning' : 'info'" :label="row.isDefault === 1 ? '是' : '否'" size="small" />
          </template>
          <template #itemStatus="{ row }">
            <StatusTag :status="statusToTagStatus(row.status)" :label="statusToText(row.status)" size="small" />
          </template>
          <template #actions="{ row }">
            <el-button link type="primary" size="small" @click="openItemEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteItem(row)">删除</el-button>
          </template>
        </DataTable>
      </div>
    </div>

    <!-- 字典类型弹窗 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogMode === 'create' ? '新增字典类型' : '编辑字典类型'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：用户状态" />
        </el-form-item>
        <el-form-item label="字典编码" prop="dictCode">
          <el-input
            v-model="typeForm.dictCode"
            placeholder="如：user_status"
            :disabled="typeDialogMode === 'edit'"
          />
        </el-form-item>
        <el-form-item label="字典分组" prop="dictGroup">
          <el-select
            v-model="typeForm.dictGroup"
            placeholder="选择或输入分组"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            :teleported="false"
          >
            <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
            <el-option label="default" value="default" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="typeForm.sortOrder" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典项弹窗 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="itemDialogMode === 'create' ? '新增字典项' : '编辑字典项'"
      width="540px"
      destroy-on-close
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="标签" prop="itemLabel">
          <el-input v-model="itemForm.itemLabel" placeholder="显示文本，如：启用" />
        </el-form-item>
        <el-form-item label="值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" placeholder="实际值，如：active" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sortOrder" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="颜色类型">
          <el-select v-model="itemForm.colorType" placeholder="选择颜色类型" clearable style="width: 100%" :teleported="false">
            <el-option v-for="c in colorTypeOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否默认">
          <el-switch v-model="itemForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="itemDialogMode === 'edit'" label="状态">
          <el-select v-model="itemForm.status" style="width: 100%" :teleported="false">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="submitItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.dict-management { display: flex; flex-direction: column; gap: var(--fts-space-3); }

/* 工具栏 */
.dict-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: var(--fts-space-3);
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: var(--fts-space-2); flex-wrap: wrap; }

/* 主体布局 */
.dict-layout { display: flex; gap: var(--fts-space-4); min-height: 420px; }

/* 左侧字典类型面板 */
.dict-type-panel {
  width: 280px; flex-shrink: 0; background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-primary); border-radius: var(--fts-radius-md);
  display: flex; flex-direction: column; overflow: hidden;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4); border-bottom: 1px solid var(--fts-border-primary);
}
.panel-title { font-size: var(--fts-font-size-md); font-weight: 600; color: var(--fts-text-primary); }
.panel-count { font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary); }
.type-list { flex: 1; overflow-y: auto; padding: var(--fts-space-2); }
.type-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--fts-space-2) var(--fts-space-3); border-radius: var(--fts-radius-sm);
  cursor: pointer; transition: all var(--fts-transition-fast); margin-bottom: var(--fts-space-1);
  border: 1px solid transparent;
}
.type-item:hover { background: var(--fts-bg-card); }
.type-item--active { background: var(--fts-bg-card); border-color: var(--fts-primary); }
.type-item__main { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.type-item__name { font-size: var(--fts-font-size-md); color: var(--fts-text-primary); font-weight: 500; }
.type-item__code { font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary); font-family: monospace; }
.type-item__meta { display: flex; flex-direction: column; align-items: flex-end; gap: var(--fts-space-1); }
.type-item__group { font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary); }

/* 右侧字典项面板 */
.dict-item-panel {
  flex: 1; min-width: 0; background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary); border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3); display: flex; flex-direction: column; gap: var(--fts-space-3);
}
.item-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: var(--fts-space-2); }
.item-header__left { display: flex; align-items: center; gap: var(--fts-space-2); }
.item-header__title { font-size: var(--fts-font-size-md); font-weight: 600; color: var(--fts-text-primary); }
.item-header__code { font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary); font-family: monospace; }
.item-header__right { display: flex; align-items: center; gap: var(--fts-space-2); flex-wrap: wrap; }
</style>
