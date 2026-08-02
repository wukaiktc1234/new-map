<script setup lang="ts">
/**
 * 合同模板管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理劳动合同模板和标准条款，支持8种合同类型
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Document, View, Edit, SwitchButton, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { contractTemplateApi } from '@/api/hr/contract-template'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  ContractTemplate,
  ContractTemplateFormData,
  ContractTemplateQueryParams,
  ContractType,
  TemplateStatus,
  TemplateVariable,
  ContractTemplatePreviewData,
} from '@/types/hr/contract'
import {
  ContractTypeOptions,
  ContractTypeTagMap,
  TemplateStatusOptions,
  TemplateStatusTagMap,
} from '@/types/hr/contract'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<ContractTemplate[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewContent = ref('')
const previewTarget = ref<ContractTemplate | null>(null)
const previewVariables = ref<Record<string, string>>({})

const versionDialogVisible = ref(false)
const versionList = ref<ContractTemplate[]>([])
const versionLoading = ref(false)

const formRef = ref<FormInstance>()

const queryForm = ref({
  keyword: '',
  contractType: '' as ContractType | '',
  status: '' as TemplateStatus | '',
})

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<ContractTemplate, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: Record<string, unknown> = { ...(params as Record<string, unknown>) }
      if (convertedParams.contractType === '' || convertedParams.contractType === undefined || convertedParams.contractType === null) {
        delete convertedParams.contractType
      }
      if (convertedParams.status === '' || convertedParams.status === undefined || convertedParams.status === null) {
        delete convertedParams.status
      }
      return contractTemplateApi.getList(convertedParams as unknown as ContractTemplateQueryParams)
    },
  } as unknown as CrudApi<ContractTemplate, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

const formData = reactive<Partial<ContractTemplateFormData> & { status: TemplateStatus }>({
  templateCode: '',
  templateName: '',
  contractType: 'labor',
  version: '1.0',
  templateContent: '',
  templateVariables: [],
  description: '',
  status: 'draft',
})

const statistics = computed(() => ({
  total: pagination?.total || 0,
  active: tableData.value.filter(item => item.status === 'active').length,
  inactive: tableData.value.filter(item => item.status === 'inactive').length,
  monthlyUse: 0,
}))

const importLoading = ref(false)
const exportLoading = ref(false)

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'templateName', label: '模板名称', minWidth: 200, slot: 'templateName' },
  { prop: 'contractType', label: '合同类型', minWidth: 130, slot: 'contractType' },
  { prop: 'version', label: '版本号', width: 100, slot: 'version' },
  { prop: 'useCount', label: '使用次数', width: 100, slot: 'useCount' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'createTime', label: '创建时间', minWidth: 160, slot: 'createTime' },
  { prop: 'updateTime', label: '更新时间', minWidth: 160, slot: 'updateTime' },
  { prop: '_operation', label: '操作', width: 280, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' },
  ],
  contractType: [
    { required: true, message: '请选择合同类型', trigger: 'change' },
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
  ],
  templateContent: [
    { required: true, message: '请输入模板内容', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.contractType = ''
  queryForm.value.status = ''
  refresh()
}

function handleSelectionChange(rows: ContractTemplate[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    templateCode: `TPL${Date.now().toString(36).toUpperCase()}`,
    templateName: '',
    contractType: 'labor',
    version: '1.0',
    templateContent: '',
    templateVariables: [],
    description: '',
    status: 'draft',
  })
  dialogVisible.value = true
}

async function handleEdit(row: ContractTemplate) {
  isEdit.value = true
  try {
    const detail = await contractTemplateApi.getById(row.id)
    Object.assign(formData, {
      ...detail,
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载模板详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = {
      ...formData,
    } as ContractTemplateFormData

    if (isEdit.value && formData.id) {
      await contractTemplateApi.update(formData.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await contractTemplateApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

function beforeUpload(rawFile: UploadRawFile): boolean {
  const allowedTypes = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ]

  if (!allowedTypes.includes(rawFile.type) && !rawFile.name.endsWith('.xlsx') && !rawFile.name.endsWith('.xls')) {
    ElMessage.error('只支持 Excel 格式的文件（.xlsx 或 .xls）')
    return false
  }

  const isLt5M = rawFile.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }

  return true
}

async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('导入功能开发中')
  } catch (error) {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('导出功能开发中')
  } catch (error) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

async function handleDelete(row: ContractTemplate): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除模板「${row.templateName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await contractTemplateApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function handleToggleStatus(row: ContractTemplate): Promise<void> {
  const isActive = row.status === 'active'
  const text = isActive ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${text}模板「${row.templateName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newStatus: TemplateStatus = isActive ? 'inactive' : 'active'
    await contractTemplateApi.updateStatus(row.id, newStatus)
    ElMessage.success(`${text}成功`)
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handlePreview(row: ContractTemplate): Promise<void> {
  previewTarget.value = row
  previewDialogVisible.value = true
  previewLoading.value = true
  previewContent.value = ''

  const varMap: Record<string, string> = {}
  row.templateVariables.forEach(v => {
    varMap[v.name] = v.example || v.defaultValue || ''
  })
  previewVariables.value = varMap
  await refreshPreview()
}

async function refreshPreview(): Promise<void> {
  if (!previewTarget.value) return
  previewLoading.value = true
  try {
    const content = await contractTemplateApi.preview({
      templateId: previewTarget.value.id,
      variables: previewVariables.value,
    } as ContractTemplatePreviewData)
    previewContent.value = content
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '预览失败')
  } finally {
    previewLoading.value = false
  }
}

async function handleVersionManage(row: ContractTemplate): Promise<void> {
  versionDialogVisible.value = true
  versionLoading.value = true
  try {
    versionList.value = [row]
  } catch {
    ElMessage.error('加载版本列表失败')
  } finally {
    versionLoading.value = false
  }
}

function handleAddVariable(): void {
  formData.templateVariables?.push({
    name: '',
    description: '',
    type: 'string',
    required: false,
  })
}

function handleRemoveVariable(index: number): void {
  formData.templateVariables?.splice(index, 1)
}

// ==================== 辅助方法 ====================

function getContractTypeLabel(type: ContractType): string {
  return ContractTypeOptions.find(o => o.value === type)?.label || type
}

function getTemplateStatusLabel(status: TemplateStatus): string {
  return TemplateStatusOptions.find(o => o.value === status)?.label || status
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="合同模板" description="管理劳动合同模板和标准条款">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增模板
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard icon="Document" label="模板总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="启用中" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="已停用" :value="String(statistics.inactive)" color-type="warning" variant="bordered" />
      <StatCard icon="Calendar" label="本月使用" :value="String(statistics.monthlyUse)" color-type="info" variant="bordered" />
    </section>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索模板名称..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.contractType"
            placeholder="合同类型"
            clearable
            style="width: 150px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in ContractTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="草稿" value="draft" />
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImport"
            accept=".xlsx,.xls"
            :disabled="importLoading"
          >
            <el-button size="default" class="action-btn--import" :loading="importLoading">
              <el-icon :size="14"><Upload /></el-icon>导入模板
            </el-button>
          </el-upload>
          <el-button
            size="default"
            class="action-btn--export"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
        </div>
      </div>
    </div>

    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="true"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        @selection-change="handleSelectionChange"
      >
        <template #templateName="{ row }">
          <div class="template-cell">
            <el-icon :size="18" color="var(--fts-primary)"><Document /></el-icon>
            <div class="template-info">
              <div class="template-name">{{ row.templateName }}</div>
              <div class="template-code">{{ row.templateCode }}</div>
            </div>
          </div>
        </template>

        <template #contractType="{ row }">
          <StatusTag :status="ContractTypeTagMap[row.contractType as ContractType]" :label="getContractTypeLabel(row.contractType)" size="small" variant="light" />
        </template>

        <template #version="{ row }">
          <span class="version-text">v{{ row.version }}</span>
        </template>

        <template #useCount="{ row }">
          <span class="count-text">{{ (row as any).useCount || 0 }}</span>
        </template>

        <template #status="{ row }">
          <StatusTag :status="TemplateStatusTagMap[row.status as TemplateStatus]" :label="getTemplateStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime || '') }}</span>
        </template>

        <template #updateTime="{ row }">
          <span class="time-text">{{ formatTime(row.updateTime || '') }}</span>
        </template>

        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handlePreview(row)">
              预览
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="info" size="default" @click.stop="handleVersionManage(row)">
              版本
            </el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'warning' : 'success'"
              size="default"
              @click.stop="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>
      </DataTable>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模板' : '新增模板'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="formData.templateName" placeholder="请输入模板名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="formData.templateCode" :disabled="isEdit" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="formData.contractType" placeholder="请选择合同类型" :teleported="false" style="width: 100%">
                <el-option v-for="opt in ContractTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version">
              <el-input v-model="formData.version" placeholder="请输入版本号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模板内容" prop="templateContent">
          <el-input
            v-model="formData.templateContent"
            type="textarea"
            :rows="10"
            placeholder="请输入模板内容，支持 {{变量名}} 格式的变量占位符"
            style="font-family: monospace;"
          />
        </el-form-item>

        <div class="form-section">
          <div class="section-title">
            <span>变量说明</span>
            <el-button
              type="primary"
              size="small"
              :icon="Plus"
              @click="handleAddVariable"
            >
              添加变量
            </el-button>
          </div>

          <el-table v-if="formData.templateVariables && formData.templateVariables.length > 0" :data="formData.templateVariables" size="small" border style="width: 100%">
            <el-table-column label="变量名" width="160">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="如 employeeName" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.description" placeholder="变量说明" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small" :teleported="false" style="width: 100%">
                  <el-option label="字符串" value="string" />
                  <el-option label="数字" value="number" />
                  <el-option label="日期" value="date" />
                  <el-option label="金额" value="currency" />
                  <el-option label="长文本" value="text" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.required" />
              </template>
            </el-table-column>
            <el-table-column label="示例值" width="140">
              <template #default="{ row }">
                <el-input v-model="row.example" placeholder="示例值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="handleRemoveVariable($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else class="empty-tip">
            <el-text type="info">暂未添加变量，点击上方"添加变量"按钮开始配置</el-text>
          </div>
        </div>

        <el-form-item label="附件上传">
          <el-upload
            action="#"
            :auto-upload="false"
            multiple
            :show-file-list="true"
          >
            <el-button size="default">
              <el-icon :size="14"><Upload /></el-icon>点击上传
            </el-button>
            <template #tip>
              <div class="el-upload__tip">支持上传附件，单个文件不超过10MB</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="模板描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入模板描述" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewDialogVisible"
      title="模板预览"
      width="960px"
      class="fts-dialog--lg"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="previewTarget">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: var(--fts-space-4)">
          修改左侧变量值后，点击"刷新预览"查看替换后的合同正文
        </el-alert>
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="preview-vars-panel">
              <div class="preview-vars-header">
                <span>变量赋值</span>
                <el-button link type="primary" size="small" @click="refreshPreview">刷新预览</el-button>
              </div>
              <el-form label-width="100px" size="small" style="margin-top: var(--fts-space-3)">
                <el-form-item v-for="v in previewTarget.templateVariables" :key="v.name" :label="v.description || v.name">
                  <el-input v-model="previewVariables[v.name]" :placeholder="v.example || ''" />
                </el-form-item>
              </el-form>
            </div>
          </el-col>
          <el-col :span="16">
            <div class="preview-content-panel" v-loading="previewLoading">
              <div class="preview-content-header">合同正文预览</div>
              <div class="preview-content-body" v-html="previewContent"></div>
            </div>
          </el-col>
        </el-row>
      </template>
    </el-dialog>

    <el-dialog
      v-model="versionDialogVisible"
      title="版本管理"
      width="960px"
      class="fts-dialog--lg"
      destroy-on-close
      lock-scroll="false"
    >
      <el-table :data="versionList" v-loading="versionLoading" border>
        <el-table-column prop="version" label="版本号" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="TemplateStatusTagMap[row.status as TemplateStatus]" :label="getTemplateStatusLabel(row.status)" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column prop="updateTime" label="更新时间" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small">使用此版本</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="versionDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 48px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;

  .action-btn--import,
  .action-btn--export {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
}

.table-section {
  padding: 0 var(--fts-space-6) var(--fts-space-6);
  overflow-x: auto;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);

  :deep(.el-table) {
    width: 100%;
  }

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

.template-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.template-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .template-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .template-code {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
    white-space: nowrap;
  }
}

.version-text {
  font-family: monospace;
  color: var(--fts-text-primary);
}

.count-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--fts-space-6) 0;
}

.preview-vars-panel {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
  background: var(--fts-bg-card);
  max-height: 500px;
  overflow-y: auto;
}

.preview-vars-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-primary);
}

.preview-content-panel {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-card);
  height: 500px;
  display: flex;
  flex-direction: column;
}

.preview-content-header {
  padding: var(--fts-space-2) var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-primary);
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.preview-content-body {
  flex: 1;
  padding: var(--fts-space-4);
  overflow-y: auto;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}

@media (max-width: 576px) {
  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .table-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
