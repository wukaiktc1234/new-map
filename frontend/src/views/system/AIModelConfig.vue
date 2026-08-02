<script setup lang="ts">
/**
 * AI模型配置页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理AI大模型的配置和参数设置
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Refresh,
  Download,
  Connection,
  CopyDocument,
  View,
  Edit,
  Delete,
  SwitchButton,
  Cpu,
  Loading,
  CircleCheckFilled,
  CircleCloseFilled,
  TrendCharts,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { aiModelApi } from '@/api/system/ai-model'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import {
  AI_MODEL_TYPE_OPTIONS,
  AI_MODEL_STATUS_OPTIONS,
  aiModelTypeLabel,
  type AIModelVO,
  type AIModelType,
  type AIModelStatus,
  type AIModelCreateDTO,
  type AIModelUpdateDTO,
  type AIModelQueryParams,
} from '@/types/ai-model'

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
const selectedRows = ref<AIModelVO[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const detailData = ref<AIModelVO | null>(null)
const testDialogVisible = ref(false)
const testLoading = ref(false)
const testResult = ref<{ success: boolean; message: string; responseTime?: number } | null>(null)
const testModelId = ref<number | null>(null)
const editingId = ref<number>(0)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  modelType: '' as AIModelType | '',
  provider: '',
  status: '' as AIModelStatus | '',
})

// 模拟服务商选项
const providerOptions = [
  { value: 'openai', label: 'OpenAI' },
  { value: 'azure', label: 'Azure OpenAI' },
  { value: 'anthropic', label: 'Anthropic' },
  { value: 'qwen', label: '通义千问' },
  { value: 'deepseek', label: '深度求索' },
  { value: 'zhipu', label: '智谱AI' },
  { value: 'baidu', label: '文心一言' },
  { value: 'moonshot', label: '月之暗面' },
  { value: 'local', label: '本地部署' },
]

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<AIModelVO, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: AIModelQueryParams = {
        page: params.page,
        size: params.size,
        keyword: params.keyword || undefined,
        modelType: params.modelType || undefined,
        provider: params.provider || undefined,
        status: params.status || undefined,
      }
      return aiModelApi.getList(convertedParams)
    },
  } as unknown as CrudApi<AIModelVO, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = ref({
  total: 0,
  active: 0,
  todayCalls: 0,
  successRate: 0,
})

// 表单数据类型
interface ModelFormData {
  modelName: string
  modelType: AIModelType
  provider: string
  modelVersion: string
  endpoint: string
  apiKey: string
  defaultModel: string
  temperature: number
  maxTokens: number
  contextLength: number
  timeout: number
  maxRetries: number
  status: AIModelStatus
  description: string
}

const defaultFormData = (): ModelFormData => ({
  modelName: '',
  modelType: 'api',
  provider: 'openai',
  modelVersion: '',
  endpoint: '',
  apiKey: '',
  defaultModel: '',
  temperature: 0.7,
  maxTokens: 2048,
  contextLength: 4096,
  timeout: 30,
  maxRetries: 3,
  status: 'active',
  description: '',
})

const formData = reactive<ModelFormData>(defaultFormData())

// apiKey是否被修改
const apiKeyModified = ref(false)

// ==================== 计算属性 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'modelName', label: '模型名称', minWidth: 160, slot: 'modelName' },
  { prop: 'modelType', label: '模型类型', minWidth: 100, slot: 'modelType' },
  { prop: 'provider', label: '服务商', minWidth: 120, slot: 'provider' },
  { prop: 'modelVersion', label: '模型版本', minWidth: 120 },
  { prop: 'endpoint', label: 'API端点', minWidth: 200, slot: 'endpoint', ellipsis: true },
  { prop: 'todayCalls', label: '今日调用次数', minWidth: 120, slot: 'todayCalls' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: '_operation', label: '操作', width: 320, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
  loadStatistics()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.modelType = ''
  queryForm.value.provider = ''
  queryForm.value.status = ''
  refresh()
  loadStatistics()
}

function handleSelectionChange(rows: AIModelVO[]) {
  selectedRows.value = rows
}

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await aiModelApi.getStatistics()
    statistics.value.total = res.total
    statistics.value.active = res.active
    // 今日调用次数与成功率后端暂未提供，默认显示 0
    statistics.value.todayCalls = 0
    statistics.value.successRate = 0
  } catch {
    statistics.value.todayCalls = 0
    statistics.value.successRate = 0
  }
}

// 获取服务商标签
function getProviderLabel(provider: string): string {
  const item = providerOptions.find(p => p.value === provider)
  return item ? item.label : provider || '-'
}

// 新增模型
function handleCreate() {
  isEdit.value = false
  Object.assign(formData, defaultFormData())
  apiKeyModified.value = false
  dialogVisible.value = true
}

// 编辑模型
async function handleEdit(row: AIModelVO) {
  isEdit.value = true
  editingId.value = row.id
  try {
    const detail = await aiModelApi.getById(row.id)
    if (detail) {
      Object.assign(formData, {
        modelName: detail.modelName,
        modelType: detail.modelType,
        provider: detail.provider || 'openai',
        modelVersion: detail.modelVersion || '',
        endpoint: detail.endpoint || '',
        apiKey: '',
        defaultModel: detail.defaultModel || '',
        temperature: detail.temperature ?? 0.7,
        maxTokens: detail.maxTokens ?? 2048,
        contextLength: detail.contextLength ?? 4096,
        timeout: detail.timeout || 30,
        maxRetries: detail.maxRetries ?? 3,
        status: detail.status,
        description: detail.description || '',
      })
      apiKeyModified.value = false
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载模型详情失败')
  }
}

// 查看详情
async function handleDetail(row: AIModelVO) {
  try {
    const detail = await aiModelApi.getById(row.id)
    detailData.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载模型详情失败')
  }
}

// 测试连接
function handleTestConnection(row: AIModelVO) {
  testModelId.value = row.id
  testResult.value = null
  testDialogVisible.value = true
}

/**
 * 执行测试连接
 * 调用后端 POST /v1/ai-models/{id}/test-connection 接口
 * - local 类型：检查 modelPath 是否存在（内置规则引擎视为可用）
 * - api 类型：发送 HTTP GET 到 endpoint，根据响应状态码判断
 */
async function executeTestConnection() {
  if (!testModelId.value) return
  testLoading.value = true
  testResult.value = null
  try {
    const result = await aiModelApi.testConnection(testModelId.value)
    testResult.value = {
      success: result.success,
      message: result.message,
      responseTime: result.responseTimeMs,
    }
    if (result.success) {
      ElMessage.success(result.message || '连接成功')
    } else {
      ElMessage.warning(result.message || '连接失败')
    }
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '测试异常，请检查网络连接'
    testResult.value = {
      success: false,
      message: msg,
    }
    ElMessage.error(msg)
  } finally {
    testLoading.value = false
  }
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const baseData = {
      modelName: formData.modelName,
      modelType: formData.modelType,
      description: formData.description,
      timeout: formData.timeout,
      provider: formData.provider,
      modelVersion: formData.modelVersion,
      defaultModel: formData.defaultModel,
      temperature: formData.temperature,
      maxTokens: formData.maxTokens,
      contextLength: formData.contextLength,
      maxRetries: formData.maxRetries,
    }

    if (isEdit.value && editingId.value) {
      const updateData: AIModelUpdateDTO = {
        ...baseData,
        status: formData.status,
      }
      if (formData.modelType === 'api') {
        updateData.endpoint = formData.endpoint
        if (apiKeyModified.value && formData.apiKey) {
          updateData.apiKey = formData.apiKey
        }
      }
      await aiModelApi.update(editingId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      const createData: AIModelCreateDTO = {
        ...baseData,
      }
      if (formData.modelType === 'api') {
        createData.endpoint = formData.endpoint
        createData.apiKey = formData.apiKey
      }
      await aiModelApi.create(createData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadStatistics()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 启用/禁用模型
async function handleToggleStatus(row: AIModelVO) {
  const isActive = row.status === 'active'
  const text = isActive ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${text}模型「${row.modelName}」吗？`,
      '操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const newStatus: AIModelStatus = isActive ? 'inactive' : 'active'
    await aiModelApi.updateStatus(row.id, newStatus)
    ElMessage.success(`${text}成功`)
    await refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// 删除模型
async function handleDelete(row: AIModelVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型「${row.modelName}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await aiModelApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

// 导出配置
async function handleExport() {
  try {
    ElMessage.info('正在导出配置，请稍候...')
    const data = JSON.stringify(tableData.value, null, 2)
    const blob = new Blob([data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `ai-model-config_${timestamp}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

// 复制API密钥
function copyApiKey(key: string) {
  if (!key) return
  navigator.clipboard.writeText(key).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 状态颜色映射
function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    active: 'success',
    inactive: 'info',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    active: '启用',
    inactive: '禁用',
  }
  return map[status] || status
}

function formatTime(iso: string | null): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 表单校验规则
const formRules: FormRules = {
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  provider: [{ required: true, message: '请选择服务商', trigger: 'change' }],
  endpoint: [
    {
      validator: (_rule, value, callback) => {
        if (formData.modelType !== 'api') return callback()
        if (!value) return callback(new Error('请输入API端点'))
        try {
          new URL(value as string)
          callback()
        } catch {
          callback(new Error('请输入有效的URL地址'))
        }
      },
      trigger: 'blur',
    },
  ],
  apiKey: [
    {
      validator: (_rule, value, callback) => {
        if (!isEdit.value && formData.modelType === 'api' && !value) {
          return callback(new Error('请输入API密钥'))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  temperature: [
    {
      validator: (_rule, value, callback) => {
        const num = Number(value)
        if (isNaN(num)) return callback(new Error('请输入有效的温度值'))
        if (num < 0 || num > 2) return callback(new Error('温度值必须在0-2之间'))
        callback()
      },
      trigger: 'blur',
    },
  ],
  maxTokens: [
    {
      validator: (_rule, value, callback) => {
        const num = Number(value)
        if (!num) return callback(new Error('请输入最大token数'))
        if (num < 1) return callback(new Error('最大token数必须大于0'))
        callback()
      },
      trigger: 'blur',
    },
  ],
  timeout: [
    {
      validator: (_rule, value, callback) => {
        if (formData.modelType !== 'api') return callback()
        const num = Number(value)
        if (!num) return callback(new Error('请输入超时时间'))
        if (num < 1 || num > 300) return callback(new Error('超时时间必须在1-300秒之间'))
        callback()
      },
      trigger: 'blur',
    },
  ],
  maxRetries: [
    {
      validator: (_rule, value, callback) => {
        const num = Number(value)
        if (isNaN(num)) return callback(new Error('请输入重试次数'))
        if (num < 0 || num > 10) return callback(new Error('重试次数必须在0-10之间'))
        callback()
      },
      trigger: 'blur',
    },
  ],
}

// 模型类型切换
function handleModelTypeChange() {
  formRef.value?.clearValidate(['endpoint', 'apiKey', 'timeout'])
}

onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="AI模型配置" description="管理AI大模型的配置和参数设置">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增模型
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Cpu" label="已配置模型" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="启用中的模型" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="Connection" label="今日调用次数" :value="String(statistics.todayCalls)" color-type="warning" variant="bordered" />
      <StatCard icon="TrendCharts" label="调用成功率" :value="`${statistics.successRate}%`" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="模型名称"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.modelType"
            placeholder="模型类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in AI_MODEL_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.provider"
            placeholder="服务商"
            clearable
            filterable
            style="width: 150px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in providerOptions"
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
            <el-option
              v-for="opt in AI_MODEL_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="success"
            size="default"
            :disabled="selectedRows.length !== 1"
            @click="() => selectedRows[0] && handleTestConnection(selectedRows[0])"
          >
            测试连接
          </el-button>
          <el-button size="default" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出配置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格区域 -->
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
        <!-- 模型名称列 -->
        <template #modelName="{ row }">
          <div class="model-cell">
            <el-icon class="model-icon"><Cpu /></el-icon>
            <div class="model-info">
              <div class="model-name">{{ row.modelName }}</div>
              <div class="model-code">{{ row.modelCode }}</div>
            </div>
          </div>
        </template>

        <!-- 模型类型列 -->
        <template #modelType="{ row }">
          <StatusTag
            :status="row.modelType === 'local' ? 'info' : 'warning'"
            :label="aiModelTypeLabel(row.modelType)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 服务商列 -->
        <template #provider="{ row }">
          <span class="provider-text">{{ getProviderLabel(row.provider) }}</span>
        </template>

        <!-- API端点列 -->
        <template #endpoint="{ row }">
          <span class="endpoint-text" :title="row.endpoint">{{ row.endpoint || '-' }}</span>
        </template>

        <!-- 今日调用次数列 -->
        <template #todayCalls="{ row }">
          <span class="calls-text">{{ row.todayCalls ?? '-' }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusColor(row.status)"
            :label="getStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="success" size="default" @click.stop="handleTestConnection(row)">
              测试
            </el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'warning' : 'primary'"
              size="default"
              @click.stop="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>
      </DataTable>

      <!-- 分页组件 -->
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模型' : '新增模型'"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="模型名称" prop="modelName">
                <el-input v-model="formData.modelName" placeholder="请输入模型名称" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型类型" prop="modelType">
              <el-select
                v-model="formData.modelType"
                placeholder="请选择模型类型"
                style="width: 100%"
                :teleported="false"
                @change="handleModelTypeChange"
              >
                <el-option
                  v-for="opt in AI_MODEL_TYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="服务商" prop="provider">
                <el-select
                  v-model="formData.provider"
                  placeholder="请选择服务商"
                  filterable
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option
                    v-for="opt in providerOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型版本" prop="modelVersion">
                <el-input v-model="formData.modelVersion" placeholder="请输入模型版本，如 gpt-4" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 连接配置 -->
        <div class="form-section" v-if="formData.modelType === 'api'">
          <div class="section-title">连接配置</div>
          <el-form-item label="API端点" prop="endpoint">
            <el-input
              v-model="formData.endpoint"
              placeholder="请输入API端点地址，如 https://api.openai.com/v1"
            />
          </el-form-item>
          <el-form-item label="API密钥" prop="apiKey">
            <el-input
              v-model="formData.apiKey"
              type="password"
              :placeholder="isEdit ? '留空表示不修改API密钥' : '请输入API密钥'"
              show-password
              @input="apiKeyModified = true"
            >
              <template #append>
                <el-button :icon="CopyDocument" @click="copyApiKey(formData.apiKey)" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="默认模型" prop="defaultModel">
            <el-input v-model="formData.defaultModel" placeholder="请输入默认模型名称" />
          </el-form-item>
        </div>

        <!-- 参数配置 -->
        <div class="form-section">
          <div class="section-title">参数配置</div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="温度" prop="temperature">
                <el-input-number
                  v-model="formData.temperature"
                  :min="0"
                  :max="2"
                  :step="0.1"
                  :precision="1"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最大token数" prop="maxTokens">
                <el-input-number
                  v-model="formData.maxTokens"
                  :min="1"
                  :max="128000"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="上下文长度" prop="contextLength">
                <el-input-number
                  v-model="formData.contextLength"
                  :min="1"
                  :max="128000"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 高级配置 -->
        <div class="form-section">
          <div class="section-title">高级配置</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="超时时间(秒)" prop="timeout">
                <el-input-number
                  v-model="formData.timeout"
                  :min="1"
                  :max="300"
                  :step="5"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="重试次数" prop="maxRetries">
                <el-input-number
                  v-model="formData.maxRetries"
                  :min="0"
                  :max="10"
                  :step="1"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 状态 -->
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模型描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="模型详情"
      width="680px"
      destroy-on-close
    >
      <div v-if="detailData" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模型名称">
            {{ detailData.modelName }}
          </el-descriptions-item>
          <el-descriptions-item label="模型编码">
            {{ detailData.modelCode }}
          </el-descriptions-item>
          <el-descriptions-item label="模型类型">
            <StatusTag
              :status="detailData.modelType === 'local' ? 'info' : 'warning'"
              :label="aiModelTypeLabel(detailData.modelType)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="getStatusColor(detailData.status)"
              :label="getStatusLabel(detailData.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="服务商">
            {{ getProviderLabel(detailData.provider || '-') }}
          </el-descriptions-item>
          <el-descriptions-item label="模型版本">
            {{ detailData.modelVersion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="默认模型">
            {{ detailData.defaultModel || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="温度">
            {{ detailData.temperature ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="最大token数">
            {{ detailData.maxTokens ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="上下文长度">
            {{ detailData.contextLength ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="重试次数">
            {{ detailData.maxRetries ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="API端点" :span="2">
            {{ detailData.endpoint || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="API密钥" :span="2">
            <span v-if="detailData.apiKey">
              {{ detailData.apiKey }}
              <el-button link type="primary" size="small" :icon="CopyDocument" @click="copyApiKey(detailData.apiKey!)">
                复制
              </el-button>
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="超时时间">
            {{ detailData.timeout }} 秒
          </el-descriptions-item>
          <el-descriptions-item label="最后同步时间">
            {{ formatTime(detailData.lastSyncTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatTime(detailData.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatTime(detailData.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ detailData.description || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 测试连接对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="测试连接"
      width="500px"
      destroy-on-close
    >
      <div class="test-connection-content">
        <div v-if="testLoading" class="test-loading">
          <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
          <p>正在测试连接，请稍候...</p>
        </div>
        <div v-else-if="testResult" class="test-result">
          <div v-if="testResult.success" class="result-success">
            <el-icon class="result-icon success" :size="48"><CircleCheckFilled /></el-icon>
            <p class="result-title">连接成功</p>
            <p class="result-message">{{ testResult.message }}</p>
            <p v-if="testResult.responseTime" class="result-time">
              响应时间：{{ testResult.responseTime }}ms
            </p>
          </div>
          <div v-else class="result-error">
            <el-icon class="result-icon error" :size="48"><CircleCloseFilled /></el-icon>
            <p class="result-title">连接失败</p>
            <p class="result-message">{{ testResult.message }}</p>
          </div>
        </div>
        <div v-else class="test-tip">
          <el-icon class="tip-icon" :size="48"><Connection /></el-icon>
          <p>点击下方按钮开始测试连接</p>
        </div>
      </div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="executeTestConnection">
          开始测试
        </el-button>
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

// 统计卡片区域
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

// 工具栏面板
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
}

// 表格区域
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

// 模型信息单元格
.model-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.model-icon {
  font-size: 28px;
  color: var(--fts-primary);
  background: var(--fts-primary-light-9);
  padding: 4px;
  border-radius: var(--fts-card-radius);
}

.model-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .model-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .model-code {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
    white-space: nowrap;
  }
}

// 服务商文字
.provider-text {
  color: var(--fts-text-primary);
}

// API端点文字
.endpoint-text {
  color: var(--fts-text-secondary);
  font-family: var(--fts-font-family-mono);
  font-size: var(--fts-font-size-sm);
}

// 调用次数文字
.calls-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-bottom: var(--fts-space-2);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

// 详情内容
.detail-content {
  padding: var(--fts-space-2) 0;
}

// 测试连接内容
.test-connection-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  text-align: center;

  .test-loading,
  .test-result,
  .test-tip {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--fts-space-3);
  }

  .loading-icon {
    color: var(--fts-primary);
    animation: rotate 1s linear infinite;
  }

  .result-icon {
    &.success {
      color: var(--fts-success);
    }
    &.error {
      color: var(--fts-error);
    }
  }

  .tip-icon {
    color: var(--fts-text-tertiary);
  }

  .result-title {
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin: 0;
  }

  .result-message {
    color: var(--fts-text-secondary);
    margin: 0;
  }

  .result-time {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
    margin: 0;
  }

  p {
    color: var(--fts-text-secondary);
    margin: 0;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
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
