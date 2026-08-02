<script setup lang="ts">
/**
 * 商品档案管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理采购物料/商品的基础信息
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Goods } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { materialArchiveApi, materialCategoryApi } from '@/api/purchase'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  MaterialArchiveInfo,
  MaterialArchiveFormData,
  MaterialArchiveQueryForm,
  MaterialArchiveStatus,
} from '@/types/purchase-archive'

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
const selectedRows = ref<MaterialArchiveInfo[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentDetail = ref<MaterialArchiveInfo | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  categoryId: '' as number | string,
  status: '' as '' | MaterialArchiveStatus,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
  handleSizeChange,
} = useCrudTable<MaterialArchiveInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: MaterialArchiveQueryForm & { page: number; size: number } = {
        page: params.page,
        size: params.size,
        keyword: params.keyword || undefined,
        status: params.status || undefined,
        categoryId: params.categoryId || undefined,
      }
      return materialArchiveApi.getList(convertedParams)
    },
  } as unknown as CrudApi<MaterialArchiveInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
type ArchiveFormState = Partial<MaterialArchiveFormData> & {
  materialCode: string
  status: MaterialArchiveStatus
  barcode: string
  origin: string
  shelfLife: string
  storageCondition: string
  departmentId: string
}

const formData = reactive<ArchiveFormState>({
  materialCode: '',
  materialName: '',
  categoryId: '',
  unit: '',
  spec: '',
  barcode: '',
  referencePrice: 0,
  supplierId: '',
  origin: '',
  shelfLife: '',
  storageCondition: '',
  departmentId: '',
  status: 'active',
  remark: '',
})

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  active: tableData.value.filter(item => item.status === 'active').length,
  inactive: tableData.value.filter(item => item.status === 'inactive').length,
  lowStock: 0,
}))

// 分类选项
const categoryOptions = ref<Array<{ value: string; label: string }>>([])
const importLoading = ref(false)
const exportLoading = ref(false)

/** 部门选项（使用部门，用于按部门过滤物料） */
const { departmentOptions, loadDepartments } = useDepartmentOptions(true)

// 加载分类选项
async function loadCategoryOptions(): Promise<void> {
  try {
    const list = await materialCategoryApi.getEnabledList()
    categoryOptions.value = list.map(c => ({
      value: c.categoryId,
      label: c.categoryName,
    }))
  } catch {
    categoryOptions.value = []
  }
}

onMounted(() => {
  loadCategoryOptions()
  loadDepartments()
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: '_index', label: '#', width: 60, align: 'right', slot: 'index' },
  { prop: 'materialCode', label: '商品编码', minWidth: 130 },
  { prop: 'materialName', label: '商品名称', minWidth: 160, slot: 'materialName' },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'unit', label: '单位', minWidth: 80, slot: 'unit' },
  { prop: 'categoryName', label: '分类', minWidth: 100, slot: 'categoryName' },
  { prop: 'referencePrice', label: '单价(元)', minWidth: 110, slot: 'referencePrice' },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  materialName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  referencePrice: [
    { required: true, message: '请输入参考价', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.categoryId = ''
  queryForm.value.status = ''
  refresh()
}

function handleSelectionChange(rows: MaterialArchiveInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    materialCode: `MA${Date.now().toString(36).toUpperCase()}`,
    materialName: '',
    categoryId: '',
    unit: '',
    spec: '',
    barcode: '',
    referencePrice: 0,
    supplierId: '',
    origin: '',
    shelfLife: '',
    storageCondition: '',
    departmentId: '',
    status: 'active',
    remark: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: MaterialArchiveInfo) {
  isEdit.value = true
  try {
    const detail = await materialArchiveApi.getById(row.materialId)
    if (detail) {
      Object.assign(formData, {
        ...detail,
        barcode: detail.barcode ?? '',
        origin: detail.origin ?? '',
        shelfLife: detail.shelfLife ?? '',
        storageCondition: detail.storageCondition ?? '',
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载商品详情失败')
  }
}

async function handleView(row: MaterialArchiveInfo) {
  try {
    const detail = await materialArchiveApi.getById(row.materialId)
    if (detail) {
      currentDetail.value = detail
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('加载商品详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: MaterialArchiveFormData = {
      materialName: formData.materialName!,
      categoryId: formData.categoryId!,
      unit: formData.unit!,
      spec: formData.spec!,
      referencePrice: formData.referencePrice!,
      supplierId: formData.supplierId!,
      remark: formData.remark!,
    }

    if (isEdit.value && formData.materialId) {
      await materialArchiveApi.update(formData.materialId, submitData)
      ElMessage.success('更新成功')
    } else {
      await materialArchiveApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch {
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
    ElMessage.info('导入功能开发中...')
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('导出功能开发中...')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

async function handleDelete(row: MaterialArchiveInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除商品「${row.materialName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await materialArchiveApi.delete(row.materialId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function handleStatusToggle(row: MaterialArchiveInfo): Promise<void> {
  const isActive = row.status === 'active'
  const text = isActive ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${text}商品「${row.materialName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newStatus: MaterialArchiveStatus = isActive ? 'inactive' : 'active'
    await materialArchiveApi.updateStatus(row.materialId, newStatus)
    ElMessage.success(`${text}成功`)
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    active: 'active',
    inactive: 'inactive',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    active: '启用',
    inactive: '停用',
  }
  return map[status] || status
}

function formatPrice(val: number): string {
  return val != null ? val.toFixed(2) : '0.00'
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="商品档案" description="管理采购物料/商品的基础信息">
      <el-button v-permission="'purchase:archive:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增商品
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Goods" label="商品总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="在售商品" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="停用商品" :value="String(statistics.inactive)" color-type="warning" variant="bordered" />
      <StatCard icon="Warning" label="低库存" :value="String(statistics.lowStock)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="商品名称"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.keyword"
            placeholder="商品编码"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="queryForm.categoryId"
            placeholder="分类"
            clearable
            style="width: 140px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
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
              <el-icon :size="14"><Upload /></el-icon>批量导入
            </el-button>
          </el-upload>
          <el-button size="default" class="action-btn--export" @click="handleExport" :loading="exportLoading">
            <el-icon :size="14"><Download /></el-icon>导出
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
        <!-- 序号列 -->
        <template #index="{ $index }">
          <span class="index-text">{{ (pagination.current - 1) * pagination.pageSize + $index + 1 }}</span>
        </template>

        <!-- 商品名称列 -->
        <template #materialName="{ row }">
          <div class="material-cell">
            <el-avatar :size="36" :icon="Goods" shape="square" />
            <div class="material-info">
              <div class="material-name">{{ row.materialName }}</div>
            </div>
          </div>
        </template>

        <!-- 单位列 -->
        <template #unit="{ row }">
          <span class="unit-text">{{ row.unit }}</span>
        </template>

        <!-- 分类列 -->
        <template #categoryName="{ row }">
          <span class="category-text">{{ row.categoryName }}</span>
        </template>

        <!-- 单价列 -->
        <template #referencePrice="{ row }">
          <span class="price-text">¥{{ formatPrice(row.referencePrice) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusColor(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button v-permission="'purchase:archive:view'" link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button v-permission="'purchase:archive:edit'" link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-permission="'purchase:archive:edit'"
              link
              :type="row.status === 'active' ? 'warning' : 'primary'"
              size="default"
              @click.stop="handleStatusToggle(row)"
            >
              {{ row.status === 'active' ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'purchase:archive:delete'" link type="danger" size="default" @click.stop="handleDelete(row)">
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
          @current-change="refresh"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="1100px"
      class="fts-dialog--wide"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="商品编码">
                <el-input v-model="formData.materialCode" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商品名称" prop="materialName">
                <el-input v-model="formData.materialName" placeholder="请输入商品名称" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="分类" prop="categoryId">
                <el-select v-model="formData.categoryId" placeholder="请选择分类" :teleported="false" style="width: 100%">
                  <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="规格型号" prop="spec">
                <el-input v-model="formData.spec" placeholder="请输入规格型号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="单位" prop="unit">
                <el-input v-model="formData.unit" placeholder="如：个、箱、kg" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="条码">
                <el-input v-model="formData.barcode" placeholder="请输入条码" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 价格信息 -->
        <div class="form-section">
          <div class="section-title">价格信息</div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="参考价" prop="referencePrice">
                <el-input-number v-model="formData.referencePrice" :precision="2" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item label="备注">
                <el-input v-model="formData.remark" placeholder="请输入备注" maxlength="200" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 其他信息 -->
        <div class="form-section">
          <div class="section-title">其他信息</div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="产地">
                <el-input v-model="formData.origin" placeholder="请输入产地" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="保质期">
                <el-input v-model="formData.shelfLife" placeholder="如：12个月" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="存储条件">
                <el-input v-model="formData.storageCondition" placeholder="请输入存储条件" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="使用部门">
                <el-select
                  v-model="formData.departmentId"
                  placeholder="通用（所有部门可见）"
                  clearable
                  style="width: 100%"
                >
                  <el-option
                    v-for="dept in departmentOptions"
                    :key="dept.id"
                    :label="dept.label"
                    :value="dept.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item label="状态">
                <el-radio-group v-model="formData.status">
                  <el-radio value="active">启用</el-radio>
                  <el-radio value="inactive">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="商品详情"
      width="1100px"
      class="fts-dialog--wide"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商品编码">{{ currentDetail.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ currentDetail.materialName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentDetail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="单位">{{ currentDetail.unit }}</el-descriptions-item>
          <el-descriptions-item label="规格型号">{{ currentDetail.spec || '-' }}</el-descriptions-item>
          <el-descriptions-item label="条码">{{ currentDetail.barcode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="参考价(元)">¥{{ formatPrice(currentDetail.referencePrice) }}</el-descriptions-item>
          <el-descriptions-item label="产地">{{ currentDetail.origin || '-' }}</el-descriptions-item>
          <el-descriptions-item label="保质期">{{ currentDetail.shelfLife || '-' }}</el-descriptions-item>
          <el-descriptions-item label="存储条件">{{ currentDetail.storageCondition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主供应商">{{ currentDetail.supplierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="getStatusColor(currentDetail.status)" :label="getStatusLabel(currentDetail.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">{{ formatTime(currentDetail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="detailVisible = false">关闭</el-button>
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    padding-bottom: var(--fts-space-2);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

// 商品信息单元格
.material-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

// 序号列（等宽数字，防跳动）
.index-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-tertiary);
}

.material-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .material-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .material-code {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
    white-space: nowrap;
  }
}

// 单位文字
.unit-text {
  color: var(--fts-text-secondary);
}

// 分类文字
.category-text {
  color: var(--fts-text-primary);
}

// 价格
.price-text {
  font-weight: var(--fts-font-weight-semibold);
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
