<script setup lang="ts">
/**
 * 菜品管理页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成菜品管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 重构要点：
 * ✅ 使用 .modern-page + PageHeader 替代 StandardPage
 * ✅ 使用 stats-section 响应式4列网格 + variant="bordered"
 * ✅ 使用 table-section + pagination-wrapper 分页分离模式
 * ✅ 使用 CSS 变量（无硬编码颜色）
 * ✅ 符合 project_rules.md 规范
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Food as FoodIcon, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import StockForecastDialog from './components/StockForecastDialog.vue'
import { useLayoutStore } from '@/stores/layout'
import { getToken } from '@/utils/auth'
import { foodApi } from '@/api/product/food'
import { categoryApi } from '@/api/product/category'
import { bomCheckApi, type StockForecastResult } from '@/api/product/bom-check'
import { storeInventoryApi } from '@/api/store-ops/store-inventory'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { Food, FoodFormData, ProductPageParams, CategoryOption, ProductStatus, RecipeItemDTO } from '@/types/product'
import { productDataConverter } from '@/api/product/converters'
import { yuanToFen, fenToYuanNumber } from '@/utils/money'

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
const selectedRows = ref<Food[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)

/** 当前正在检查库存的菜品ID（用于按钮 loading 状态） */
const checkingStockId = ref<number | string | null>(null)

/** 库存决策看板弹窗显隐 */
const forecastVisible = ref(false)
/** 库存决策看板数据 */
const forecastData = ref<StockForecastResult | null>(null)
/** 库存决策看板加载状态 */
const forecastLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
// 使用 ref 而非 reactive：useCrudTable 内部通过 queryForm.value 读取筛选字段，
// reactive 对象没有 .value 属性会导致筛选参数被丢弃。
const queryForm = ref({
  keyword: '',
  categoryId: '' as number | string,
  status: '' as '' | ProductStatus,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<Food, typeof queryForm.value>({
  // foodApi 的方法签名（id: number、status: ProductStatus）与 CrudApi 泛型接口（id: string、status: string）不完全一致；
  // 这里仅用于表格列表加载，差异在调用 delete/updateStatus 时不经过 useCrudTable（页面自行调用 foodApi），故安全断言。
  // 同时在此处对 queryForm 中的字符串类型字段做后端类型转换：
  //   - categoryId（'' | string）→ 后端 Long：空字符串移除，否则转 Number
  //   - status（'' | 'active' | 'inactive' | 'soldout'）→ 后端 Integer：空字符串移除，语义字符串按映射转数字
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: Record<string, unknown> = { ...(params as Record<string, unknown>) }
      // categoryId 转换
      if (convertedParams.categoryId === '' || convertedParams.categoryId === undefined || convertedParams.categoryId === null) {
        delete convertedParams.categoryId
      } else {
        convertedParams.categoryId = Number(convertedParams.categoryId)
      }
      // status 转换（active=1, inactive=0, soldout=2）
      if (convertedParams.status === '' || convertedParams.status === undefined || convertedParams.status === null) {
        delete convertedParams.status
      } else if (typeof convertedParams.status === 'string') {
        const statusMap: Record<string, number> = { active: 1, inactive: 0, soldout: 2 }
        convertedParams.status = statusMap[convertedParams.status]
      }
      return foodApi.getList(convertedParams as unknown as ProductPageParams)
    },
  } as unknown as CrudApi<Food, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

/** 原料明细项 */
interface IngredientItem {
  materialId: string
  materialName: string
  unit: string
  unitPrice: number
  quantity: number
  subtotal: number
}

/**
 * 表单数据状态类型
 * 在 FoodFormData 基础上扩展：
 * - foodStatus：表单使用语义化字符串状态（active/inactive/soldout），提交时由 productDataConverter 转换为后端数字 status
 * - ingredients：原料明细行（不属于后端 DTO，仅前端表单使用）
 */
type FoodFormState = Partial<FoodFormData> & {
  ingredients: IngredientItem[]
  foodStatus: ProductStatus
}

const formData = reactive<FoodFormState>({
  foodName: '',
  foodCode: '',
  categoryId: 0,
  salePrice: '0',
  costPrice: '0',
  minStock: 10,
  foodStatus: 'active',
  imageUrl: '',
  description: '',
  ingredients: [],
})

// 统计数据改为计算属性，基于实际数据动态计算
const statistics = computed(() => ({
  total: pagination?.total || 0,
  active: tableData.value.filter(item => item.foodStatus === 'active').length,
  inactive: tableData.value.filter(item => item.foodStatus === 'inactive').length,
  lowStock: tableData.value.filter(item => item.stock <= item.minStock).length,
}))

// 分类选项：优先从后端异步加载，失败时回退到 mockData
const categoryOptions = ref<CategoryOption[]>([])
const importLoading = ref(false)
const exportLoading = ref(false)

/** 原料选项（用于对话框中的原料选择） */
const ingredientOptions = ref<Array<{ materialId: string; materialName: string; unit: string; unitPrice: number }>>([])
/** 原料选项加载中状态 */
const ingredientOptionsLoading = ref(false)
/** 原料选项加载失败标记（用于在弹窗中显示错误提示和重试按钮） */
const ingredientOptionsError = ref(false)

/** 计算原料总成本 */
const ingredientTotalCost = computed(() => {
  return formData.ingredients.reduce((sum, item) => sum + Number(item.subtotal || 0), 0)
})

/** 原料选项不可用（加载失败或为空）时禁用保存按钮 */
const ingredientOptionsUnavailable = computed(() => {
  return !ingredientOptionsLoading.value && (ingredientOptionsError.value || ingredientOptions.value.length === 0)
})

/** 添加原料行 */
function addIngredient(): void {
  formData.ingredients.push({
    materialId: '',
    materialName: '',
    unit: 'g',
    unitPrice: 0,
    quantity: 100,
    subtotal: 0,
  })
}

/** 删除原料行 */
function removeIngredient(index: number): void {
  formData.ingredients.splice(index, 1)
  recalcTotalCost()
}

/** 选择原料后更新名称/单价 */
function onIngredientSelect(index: number): void {
  const item = formData.ingredients[index]
  const mat = ingredientOptions.value.find(m => m.materialId === item.materialId)
  if (mat) {
    item.materialName = mat.materialName
    item.unit = mat.unit
    item.unitPrice = mat.unitPrice
    recalcIngredientSubtotal(index)
  }
}

/** 重新计算单项小计 */
function recalcIngredientSubtotal(index: number): void {
  const item = formData.ingredients[index]
  item.subtotal = Math.round(Number(item.unitPrice) * Number(item.quantity) * 100) / 100
  recalcTotalCost()
}

/** 重新计算总成本并更新售价建议 */
function recalcTotalCost(): void {
  const total = ingredientTotalCost.value
  // FoodFormData 中 salePrice/costPrice 为 string（元为单位的字符串），需将计算结果（number）转字符串
  formData.costPrice = String(total)
  formData.salePrice = String(Math.round(total * 1.5 * 100) / 100)
}

// ==================== 菜品图片上传 ====================

/** 图片上传地址（vite 代理 /api → 后端） */
const uploadAction = '/api/v1/upload/image'
/** 上传请求头（el-upload 不走 axios 实例，需手动带 token） */
const uploadHeaders = { Authorization: `Bearer ${getToken() || ''}` }

/**
 * 解析图片 URL：后端 context-path 为 /api，上传返回的 /uploads/xxx.png 需加 /api 前缀才能访问
 */
function resolveImageUrl(url?: string): string {
  if (url && url.startsWith('/uploads')) return `/api${url}`
  return url || ''
}

function handleImageUploadSuccess(response: { code?: number; message?: string; data?: { url?: string } }): void {
  if (response?.code === 0 && response?.data?.url) {
    formData.imageUrl = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response?.message || '图片上传失败')
  }
}

function handleImageUploadError(): void {
  ElMessage.error('图片上传失败，请重试')
}

/** 异步加载分类选项（API 失败时使用空数组） */
async function loadCategoryOptions(): Promise<void> {
  try {
    categoryOptions.value = await categoryApi.getCategoryOptions()
  } catch {
    // API 失败时使用空数组（原 mockData 已清理）
    categoryOptions.value = []
  }
}

/** 异步加载原料选项（从门店库存 API 获取当前门店已有库存的原料）
 *
 * 设计说明：
 * - 原料来源为「门店库存」（store_inventory 表），后端自动按当前用户门店过滤
 * - 不传 materialName，加载当前门店全部库存原料
 * - unitCost 已由 converter 从分转为元
 */
async function loadIngredientOptions(): Promise<void> {
  ingredientOptionsLoading.value = true
  ingredientOptionsError.value = false
  try {
    const res = await storeInventoryApi.getList({ page: 1, size: 1000 })
    const records = res?.records || []
    ingredientOptions.value = records.map(m => ({
      materialId: m.materialId,
      materialName: m.materialName,
      unit: m.unit,
      unitPrice: Number(m.unitCost) || 0,
    }))
  } catch {
    ingredientOptions.value = []
    ingredientOptionsError.value = true
  } finally {
    ingredientOptionsLoading.value = false
  }
}

// 页面挂载时异步加载分类和原料选项
onMounted(() => {
  loadCategoryOptions()
  loadIngredientOptions()
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'foodName', label: '菜品信息', minWidth: 180, slot: 'foodName' },
  { prop: 'foodCode', label: '菜品编码', minWidth: 120, slot: 'foodCode' },
  { prop: 'categoryName', label: '分类', minWidth: 90, slot: 'categoryName', ellipsis: false },
  { prop: 'salePrice', label: '售价(元)', minWidth: 100, slot: 'salePrice' },
  { prop: 'costPrice', label: '成本(元)', minWidth: 100, slot: 'costPrice' },
  { prop: 'profitRate', label: '利润率', minWidth: 90, slot: 'profitRate' },
  { prop: 'stock', label: '库存', minWidth: 80, slot: 'stock' },
  { prop: 'foodStatus', label: '状态', minWidth: 85, slot: 'foodStatus', ellipsis: false },
  { prop: 'createTime', label: '创建时间', minWidth: 160, slot: 'createTime' },
  { prop: '_operation', label: '操作', width: 280, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

/**
 * 售价校验器 - 包含售价必须大于成本价的交叉校验
 * @param _rule - 校验规则（未使用）
 * @param value - 售价值
 * @param callback - 回调函数
 */
function validateSalePrice(
  _rule: unknown,
  value: number,
  callback: (error?: Error) => void
): void {
  if (!value) {
    callback(new Error('请输入售价'))
    return
  }
  if (value <= 0) {
    callback(new Error('售价必须大于0'))
    return
  }
  // 售价必须大于成本价
  const costPrice = Number(formData.costPrice || 0)
  if (costPrice > 0 && value <= costPrice) {
    callback(new Error('售价必须大于成本价'))
    return
  }
  // 价格上限检查
  if (value > 99999999.99) {
    callback(new Error('价格不能超过99,999,999.99元'))
    return
  }
  callback()
}

const formRules: FormRules = {
  foodName: [
    { required: true, message: '请输入菜品名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  salePrice: [
    { required: true, message: '请输入售价', trigger: 'blur' },
    { validator: validateSalePrice, trigger: 'blur' }
  ],
  costPrice: [{ required: true, message: '请输入成本', trigger: 'blur' }],
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

function handleSelectionChange(rows: Food[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    foodName: '',
    foodCode: '',
    categoryId: '',
    salePrice: 0,
    costPrice: 0,
    minStock: 10,
    foodStatus: 'active',
    imageUrl: '',
    description: '',
    ingredients: [],
  })
  // 默认添加一行空原料
  addIngredient()
  dialogVisible.value = true
}

async function handleEdit(row: Food) {
  isEdit.value = true
  try {
    // 从后端获取完整详情（包含 recipes 原料明细）
    const detail = await foodApi.getById(Number(row.foodId))
    const recipes: RecipeItemDTO[] = detail.recipes || []
    Object.assign(formData, {
      ...row,
      salePrice: row.salePrice,
      costPrice: row.costPrice,
      // 后端 recipes 金额单位为分，需转换为前端元（数值，参与成本计算）
      ingredients: recipes.map(r => ({
        materialId: String(r.materialId),
        materialName: r.materialName || '',
        unit: r.unit || '',
        unitPrice: r.unitPrice ? fenToYuanNumber(Number(r.unitPrice)) : 0,
        quantity: Number(r.quantity) || 0,
        subtotal: r.unitPrice && r.quantity
          ? fenToYuanNumber(Number(r.unitPrice) * Number(r.quantity))
          : 0,
      })),
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载菜品详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 校验：必须至少添加一个有效原料（成本和库存依赖原料明细）
  if (!formData.ingredients.some(i => i.materialId)) {
    ElMessage.warning('请至少添加一个原料，菜品成本依赖原料明细')
    return
  }

  submitLoading.value = true
  try {
    // 将前端 ingredients（元）映射为后端 recipes 格式（元转分）
    const recipes: RecipeItemDTO[] = formData.ingredients
      .filter(i => i.materialId)
      .map(i => ({
        materialId: Number(i.materialId),
        materialName: i.materialName,
        quantity: i.quantity,
        unit: i.unit,
        unitPrice: yuanToFen(i.unitPrice),
        lossRate: 0,
      }))

    // 修复：将语义化的 foodStatus（字符串）转换为后端需要的数字 status
    // toCreateDTO/toUpdateDTO 读取 formData.status（数字），而表单使用 foodStatus（字符串）
    const submitData = {
      ...formData,
      status: productDataConverter.statusToBackend(formData.foodStatus as ProductStatus),
      recipes,
    }
    delete (submitData as Record<string, unknown>).foodStatus
    delete (submitData as Record<string, unknown>).ingredients

    if (isEdit.value && formData.foodId) {
      await foodApi.update(Number(formData.foodId), submitData as unknown as FoodFormData)
      ElMessage.success('更新成功')
    } else {
      await foodApi.create(submitData as unknown as FoodFormData)
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

/**
 * 处理文件上传前的验证
 * 仅允许 Excel 文件（.xlsx / .xls），用于菜品批量导入
 * @param rawFile - 原始文件对象
 * @returns 是否通过验证
 */
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

/**
 * 导入菜品数据（从Excel文件）
 * 调用后端 /v1/product-center/foods/import 端点，后端解析并批量创建
 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true

    // 调用真实API导入Excel数据
    const result = await foodApi.importFoods(uploadFile.raw)

    // 显示导入结果（成功/失败统计 + 错误详情）
    let message = `导入完成！成功 ${result.successCount} 条`
    if (result.failCount > 0) {
      message += `，失败 ${result.failCount} 条`
    }

    await ElMessageBox.alert(
      `<div style="text-align: left;">
        <p><strong>${message}</strong></p>
        ${result.errorMessages && result.errorMessages.length > 0 ? `
        <div style="margin-top: 10px; max-height: 200px; overflow-y: auto;">
          <strong>错误详情：</strong>
          <ul style="margin: 5px 0; padding-left: 20px;">
            ${result.errorMessages.slice(0, 10).map(err => `<li class="import-error-item">${err}</li>`).join('')}
            ${result.errorMessages.length > 10 ? `<li class="import-error-more">...还有 ${result.errorMessages.length - 10} 条错误</li>` : ''}
          </ul>
        </div>
        ` : ''}
      </div>`,
      '导入结果',
      { dangerouslyUseHTMLString: true }
    )

    // 刷新列表数据
    refresh()
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

/**
 * 导出菜品数据为Excel文件
 * 调用后端 /v1/product-center/foods/export 端点，按当前查询条件导出
 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    // 构建当前查询参数用于导出
    const params: ProductPageParams = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: queryForm.value.keyword || undefined,
      categoryId: queryForm.value.categoryId ? Number(queryForm.value.categoryId) : undefined,
      status: queryForm.value.status ? (queryForm.value.status === 'active' ? 1 : queryForm.value.status === 'inactive' ? 0 : 2) : undefined,
    }

    // 调用真实API导出Excel数据
    const blob = await foodApi.exportFoods(params)

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    // 生成文件名（带时间戳）
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `菜品数据_${timestamp}.xlsx`

    // 触发下载
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    // 释放URL对象
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/**
 * 删除菜品确认（使用 ElMessageBox.confirm 替代 el-popconfirm）
 * @param row - 菜品数据
 */
async function handleFoodDeleteClick(row: Food): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除菜品「${row.foodName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await foodApi.delete(Number(row.foodId))
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 切换菜品在售/停售状态
 * @param row - 菜品数据
 */
async function handleFoodStatusToggle(row: Food): Promise<void> {
  const isActive = row.foodStatus === 'active'
  const text = isActive ? '停售' : '在售'
  try {
    await ElMessageBox.confirm(`确定要${text}菜品「${row.foodName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newStatus: ProductStatus = isActive ? 'inactive' : 'active'
    // 直接调用 API，由后端处理状态变更；不本地修改 tableData，避免与后端数据不一致
    await foodApi.updateStatus(Number(row.foodId), newStatus)
    ElMessage.success(`${text}成功`)
    // 调用 refresh() 重新拉取数据，保证 UI 与后端数据一致
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/**
 * 批量在售/停售选中菜品
 * 调用后端 /v1/product-center/foods/batch-status 批量接口
 * @param targetStatus - 目标状态：active 在售 / inactive 停售
 */
async function handleBatchStatus(targetStatus: ProductStatus): Promise<void> {
  if (selectedRows.value.length === 0) return
  const text = targetStatus === 'active' ? '在售' : '停售'
  try {
    await ElMessageBox.confirm(
      `确定要批量${text}选中的 ${selectedRows.value.length} 个菜品吗？`,
      '批量操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const foodIds = selectedRows.value.map(row => Number(row.foodId))
    await foodApi.batchUpdateStatus(foodIds, targetStatus)
    ElMessage.success(`批量${text}成功`)
    selectedRows.value = []
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : `批量${text}失败`)
    }
  }
}

/**
 * 检查菜品库存决策（点击"检查库存"按钮触发，方案D+）
 * 基于近 N 天订单销量 + 库存流水自校准"实际每份用量"（含损耗），
 * 预测 可做份数 / 理论份数 / 售罄时间 / 建议补货，并展示损耗差异分析（方案E）。
 */
async function handleCheckStock(row: Food): Promise<void> {
  if (!row.foodId) {
    ElMessage.warning('菜品ID缺失，无法检查库存')
    return
  }
  checkingStockId.value = row.foodId
  forecastLoading.value = true
  // 先打开弹窗并清空旧数据，让用户看到 loading 状态
  forecastData.value = null
  forecastVisible.value = true
  try {
    const result = await bomCheckApi.forecast(Number(row.foodId), 7)
    forecastData.value = result
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '库存预测失败')
    // 出错时关闭弹窗
    forecastVisible.value = false
  } finally {
    forecastLoading.value = false
    checkingStockId.value = null
  }
}

/**
 * 下载导入模板（Excel）
 * 调用后端 /v1/product-center/foods/import-template 端点
 */
async function handleDownloadTemplate(): Promise<void> {
  try {
    exportLoading.value = true

    const blob = await foodApi.downloadImportTemplate()

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '菜品导入模板.xlsx'

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('模板下载成功')
  } catch (error) {
    ElMessage.error('模板下载失败')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

function getFoodStatusColor(status: string): string {
  const map: Record<string, string> = {
    active: 'active',
    inactive: 'inactive',
    soldout: 'error',
  }
  return map[status] || 'info'
}

function getFoodStatusLabel(status: string): string {
  const map: Record<string, string> = {
    active: '在售',
    inactive: '停售',
    soldout: '售罄',
  }
  return map[status] || status
}

function getProfitRateClass(rate: number): string {
  if (rate >= 60) return 'profit-high'
  if (rate >= 30) return 'profit-medium'
  return 'profit-low'
}

function getStockClass(stock: number, minStock: number): string {
  if (stock <= 0) return 'stock-out'
  if (stock <= minStock) return 'stock-warning'
  return 'stock-normal'
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
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="菜品管理" description="管理所有菜品信息、价格和库存状态">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增菜品
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Food" label="菜品总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="在售中" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="已停售" :value="String(statistics.inactive)" color-type="warning" variant="bordered" />
      <StatCard icon="Warning" label="库存预警" :value="String(statistics.lowStock)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导入导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.categoryId"
            placeholder="分类"
            clearable
            style="width: 130px"
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
            <el-option label="在售" value="active" />
            <el-option label="停售" value="inactive" />
            <el-option label="售罄" value="soldout" />
          </el-select>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索菜品名称或编码..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="success"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchStatus('active')"
          >
            批量在售
          </el-button>
          <el-button
            type="warning"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchStatus('inactive')"
          >
            批量停售
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImport"
            accept=".xlsx,.xls"
            :disabled="importLoading"
          >
            <el-button size="default" class="action-btn--import" :loading="importLoading">
              <el-icon :size="14"><Upload /></el-icon>导入
            </el-button>
          </el-upload>
          <el-dropdown split-button type="default" size="default" class="action-btn--export" @click="handleExport" :loading="exportLoading">
            <el-icon :size="14"><Download /></el-icon>导出
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleExport">导出当前筛选数据</el-dropdown-item>
                <el-dropdown-item @click="handleDownloadTemplate">下载导入模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
        <!-- 菜品名称列 -->
        <template #foodName="{ row }">
          <div class="food-cell">
            <el-avatar :size="36" :src="resolveImageUrl(row.imageUrl)" :icon="FoodIcon" shape="square" />
            <div class="food-info">
              <div class="food-name">{{ row.foodName }}</div>
            </div>
          </div>
        </template>

        <!-- 菜品编码列 -->
        <template #foodCode="{ row }">
          <span class="code-text">{{ row.foodCode || '-' }}</span>
        </template>

        <!-- 分类列 -->
        <template #categoryName="{ row }">
          <span class="category-text">{{ row.categoryName }}</span>
        </template>

        <!-- 售价列 -->
        <template #salePrice="{ row }">
          <span class="price-text">¥{{ row.salePrice }}</span>
        </template>

        <!-- 成本列 -->
        <template #costPrice="{ row }">
          <span class="cost-text">¥{{ row.costPrice }}</span>
        </template>

        <!-- 利润率列 -->
        <template #profitRate="{ row }">
          <span :class="getProfitRateClass(row.profitRate)">{{ row.profitRate }}%</span>
        </template>

        <!-- 库存列 -->
        <template #stock="{ row }">
          <span :class="getStockClass(row.stock, row.minStock)">{{ row.stock }}</span>
        </template>

        <!-- 状态列 -->
        <template #foodStatus="{ row }">
          <StatusTag :status="getFoodStatusColor(row.foodStatus)" :label="getFoodStatusLabel(row.foodStatus)" size="small" variant="light" />
        </template>

        <!-- 创建时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              link
              type="info"
              size="default"
              :loading="checkingStockId === row.foodId"
              @click.stop="handleCheckStock(row)"
            >
              检查库存
            </el-button>
            <el-button
              link
              :type="row.foodStatus === 'active' ? 'warning' : 'primary'"
              size="default"
              @click.stop="handleFoodStatusToggle(row)"
            >
              {{ row.foodStatus === 'active' ? '停售' : '在售' }}
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleFoodDeleteClick(row)">
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

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜品' : '新增菜品'"
      width="1000px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="菜品名称" prop="foodName">
              <el-input v-model="formData.foodName" placeholder="请输入菜品名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜品编码" prop="foodCode">
              <el-input v-model="formData.foodCode" disabled placeholder="保存后自动生成" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="菜品图片" prop="imageUrl">
              <div class="image-field">
                <el-upload
                  class="image-field__uploader"
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="handleImageUploadSuccess"
                  :on-error="handleImageUploadError"
                  accept="image/*"
                >
                  <el-avatar
                    v-if="formData.imageUrl"
                    :size="72"
                    :src="resolveImageUrl(formData.imageUrl)"
                    shape="square"
                    class="image-field__preview"
                  />
                  <div v-else class="image-field__placeholder">
                    <el-icon><Plus /></el-icon>
                    <span>上传图片</span>
                  </div>
                </el-upload>
                <span class="image-field__hint">支持 jpg/png，点击图片可替换</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 原料明细 -->
        <div class="form-section" v-loading="ingredientOptionsLoading" element-loading-text="正在加载原料列表...">
          <div class="section-title">
            <span>原料明细</span>
            <el-button
              type="primary"
              size="small"
              :icon="Plus"
              :disabled="ingredientOptionsUnavailable"
              @click="addIngredient"
            >
              添加原料
            </el-button>
          </div>

          <!-- 原料列表加载失败提示 -->
          <el-alert
            v-if="ingredientOptionsError"
            type="error"
            :closable="false"
            show-icon
            title="原料列表加载失败"
            description="无法加载原料数据，请检查网络连接后重试。保存按钮已禁用。"
            class="ingredient-alert"
          >
            <el-button size="small" type="primary" @click="loadIngredientOptions">重新加载</el-button>
          </el-alert>
          <!-- 原料列表为空提示 -->
          <el-alert
            v-else-if="!ingredientOptionsLoading && ingredientOptions.length === 0"
            type="warning"
            :closable="false"
            show-icon
            title="暂无可用原料"
            description="系统中尚未录入启用的原料档案，无法添加菜品原料。请先在采购管理-商品档案中创建原料。保存按钮已禁用。"
            class="ingredient-alert"
          />

          <el-table :data="formData.ingredients" size="small" border style="width: 100%" class="ingredient-table" :resizable="false">
            <el-table-column label="原料名称" min-width="160">
              <template #default="{ row, $index }">
                <el-select
                  v-model="row.materialId"
                  filterable
                  placeholder="选择原料"
                  style="width: 100%"
                  :popper-options="{ strategy: 'fixed' }"
                  @change="() => onIngredientSelect($index)"
                >
                  <el-option
                    v-for="mat in ingredientOptions"
                    :key="mat.materialId"
                    :label="mat.materialName"
                    :value="mat.materialId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单价(元/单位)" width="120" align="center">
              <template #default="{ row }">
                <span>¥{{ row.unitPrice }}/{{ row.unit }}</span>
              </template>
            </el-table-column>
            <el-table-column label="份量" width="100" align="center">
              <template #default="{ row, $index }">
                <el-input-number
                  v-model="row.quantity"
                  :min="0.001"
                  :max="99999"
                  :precision="3"
                  :step="0.05"
                  controls-position="right"
                  size="small"
                  style="width: 90px"
                  @change="() => recalcIngredientSubtotal($index)"
                />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="60" align="center">
              <template #default="{ row }">{{ row.unit || 'g' }}</template>
            </el-table-column>
            <el-table-column label="小计(元)" width="100" align="center">
              <template #default="{ row }">
                <span class="cost-text">¥{{ (row.subtotal || 0).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="50" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeIngredient($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!formData.ingredients.length" class="empty-tip">
            <el-text type="info">暂未添加原料，点击上方"添加原料"按钮开始配置</el-text>
          </div>

          <!-- 成本汇总 -->
          <div class="cost-summary">
            <div class="summary-row">
              <span>原料成本合计：</span>
              <strong class="cost-value">¥{{ ingredientTotalCost.toFixed(2) }}</strong>
            </div>
          </div>
        </div>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="formData.categoryId" placeholder="请选择分类" :teleported="false" style="width: 100%">
                <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="foodStatus">
              <el-radio-group v-model="formData.foodStatus">
                <el-radio value="active">在售</el-radio>
                <el-radio value="inactive">停售</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="售价(元)" prop="salePrice">
              <el-input-number v-model="formData.salePrice" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本(元)" prop="costPrice">
              <el-input-number v-model="formData.costPrice" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低库存" prop="minStock">
              <el-input-number v-model="formData.minStock" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="菜品描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入菜品描述" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          :disabled="ingredientOptionsUnavailable"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 库存决策看板（方案D+/E） -->
    <StockForecastDialog v-model="forecastVisible" :forecast="forecastData" />
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

// 导入结果弹窗中的错误项颜色（深色模式适配）
:global(.import-error-item) {
  color: var(--fts-error);
}
:global(.import-error-more) {
  color: var(--fts-text-tertiary);
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

// 分类文字
.category-text {
  color: var(--fts-text-primary);
}

// 原料明细表格和成本汇总
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

// 原料列表加载失败/为空的提示框
.ingredient-alert {
  margin-bottom: var(--fts-space-3);

  :deep(.el-alert__description) {
    margin-bottom: var(--fts-space-2);
  }
}

// 原料明细表格内下拉（teleported=true 挂 body，锚点正确；strategy:fixed 避免滚动追锚抖动）

.cost-summary {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fts-space-3);
  gap: var(--fts-space-4);

  .summary-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }

  .cost-value {
    color: var(--fts-primary);
    font-size: var(--fts-font-size-lg);
  }
}

// 菜品信息单元格
.food-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

// 菜品图片字段（上传 + 预览）
.image-field {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  width: 100%;

  &__uploader {
    flex-shrink: 0;

    :deep(.el-upload) {
      cursor: pointer;
    }
  }

  &__preview {
    border: 1px solid var(--fts-border-primary);
    border-radius: var(--fts-radius-sm);
  }

  &__placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--fts-space-1);
    width: 72px;
    height: 72px;
    border: 1px dashed var(--fts-border-primary);
    border-radius: var(--fts-radius-sm);
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
    background: var(--fts-bg-fill);
  }

  &__hint {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
  }
}

.code-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.food-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .food-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .food-code {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
    white-space: nowrap;
  }
}

// 价格
.price-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.cost-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 利润率
.profit-high {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
}

.profit-medium {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.profit-low {
  color: var(--fts-error);
  font-variant-numeric: tabular-nums;
}

// 库存
.stock-normal {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.stock-warning {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.stock-out {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
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
  .stats-grid {
    grid-template-columns: 1fr;
  }

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