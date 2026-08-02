/**
 * 产品中心统一类型定义
 * 涵盖菜品、套餐、定价、成本分析、BOM检查等所有产品相关类型
 * 对应后端实体: FoodNew (foods), FoodCategoryNew (food_categories), DishComboNew (dish_combos)
 * 遵循规范：金额以分为单位在后端传输，前端以元展示
 */

// ============================================================
// 通用类型
// ============================================================

/** 产品状态（前端语义化） */
export type ProductStatus = 'active' | 'inactive' | 'soldout'

/** 分类选项（通用，供下拉选择使用） */
export interface CategoryOption {
  value: number
  label: string
}

/** 分页请求参数 */
export interface ProductPageParams {
  page: number
  size: number
  foodName?: string
  categoryId?: number
  status?: number
  keyword?: string
}

/** 分页响应 */
export interface ProductPageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 菜品原料明细项（对应后端 RecipeItemDTO，金额单位：分） */
export interface RecipeItemDTO {
  /** 原料ID */
  materialId: number
  /** 原料名称 */
  materialName?: string
  /** 所需数量 */
  quantity?: number
  /** 单位 */
  unit?: string
  /** 单价（分） */
  unitPrice?: number
  /** 损耗率（百分比） */
  lossRate?: number
}

// ============================================================
// 菜品（Food）相关类型
// ============================================================

/** 后端菜品数据类型（FoodVO API响应原始格式） */
export interface FoodBackend {
  foodId: number
  foodCode: string
  foodName: string
  categoryId: number
  categoryName: string
  specification: string
  unit: string
  salePrice: number
  costPrice: number
  profit: number
  profitRate: number
  stock: number
  minStock: number
  imageUrl: string
  description: string
  cookingTime: number
  status: number
  statusName: string
  isRecommend: boolean
  isSpicy: boolean
  sortOrder: number
  createTime: string
  updateTime: string
  /** 原料明细列表（金额单位：分） */
  recipes?: RecipeItemDTO[]
}

/** 前端菜品展示类型（经过Converter转换） */
export interface Food {
  foodId: number
  foodCode: string
  foodName: string
  categoryId: number
  categoryName: string
  specification: string
  unit: string
  salePrice: string
  costPrice: string
  profit: string
  profitRate: number
  stock: number
  minStock: number
  imageUrl: string
  description: string
  cookingTime: number
  foodStatus: ProductStatus
  statusName: string
  isRecommend: boolean
  isSpicy: boolean
  sortOrder: number
  createTime: string
  updateTime: string
  /** 原料明细列表（金额单位：分，与后端 RecipeItemDTO 一致） */
  recipes?: RecipeItemDTO[]
}

/** 菜品表单提交数据（对应后端FoodCreateDTO/FoodUpdateDTO） */
export interface FoodFormData {
  foodId?: number
  foodCode?: string
  foodName: string
  categoryId: number
  specification?: string
  unit?: string
  salePrice: string
  costPrice?: string
  stock?: number
  minStock?: number
  imageUrl?: string
  description?: string
  cookingTime?: number
  status?: number
  isRecommend?: boolean
  isSpicy?: boolean
  sortOrder?: number
  /** 原料明细列表（金额单位：分，与后端 RecipeItemDTO 一致） */
  recipes?: RecipeItemDTO[]
}

/** 菜品列表行数据（表格展示用） */
export interface FoodRow {
  foodId: number
  foodCode: string
  foodName: string
  categoryId: number
  categoryName: string
  specification: string
  unit: string
  salePrice: string
  costPrice: string
  profit: string
  profitRate: number
  stock: number
  minStock: number
  imageUrl: string
  description: string
  cookingTime: number
  foodStatus: ProductStatus
  statusName: string
  isRecommend: boolean
  isSpicy: boolean
  sortOrder: number
  createTime: string
  updateTime: string
}

// ============================================================
// 分类（Category）相关类型
// ============================================================

/** 后端分类数据类型（CategoryVO API响应原始格式） */
export interface CategoryBackend {
  categoryId: number
  categoryName: string
  parentId: number
  iconUrl: string
  sortOrder: number
  status: number
  foodCount: number
  children: CategoryBackend[]
  createTime: string
  updateTime: string
}

/** 前端分类展示类型 */
export interface FoodCategory {
  categoryId: number
  categoryName: string
  parentId: number
  iconUrl: string
  sortOrder: number
  categoryStatus: ProductStatus
  foodCount: number
  children: FoodCategory[]
  description?: string
  createTime: string
  updateTime: string
}

/** 分类表单提交数据（对应后端CategoryCreateDTO/CategoryUpdateDTO） */
export interface CategoryFormData {
  categoryId?: number
  categoryName: string
  parentId?: number
  iconUrl?: string
  sortOrder?: number
  status?: number
  description?: string
}

/** 分类查询表单（用于 useCrudTable 的查询参数，分页由 useCrudTable 自动注入） */
export interface CategoryQueryForm {
  /** 关键字（分类名称模糊查询） */
  keyword?: string
  /** 状态筛选（前端语义化值） */
  status?: ProductStatus
  /** 分页页码（由 useCrudTable 自动注入，可选） */
  page?: number
  /** 分页大小（由 useCrudTable 自动注入，可选） */
  size?: number
}

// ============================================================
// 套餐（DishCombo）相关类型
// ============================================================

/** 后端套餐数据类型（ComboVO API响应原始格式） */
export interface ComboBackend {
  comboId: number
  comboCode: string
  comboName: string
  comboPrice: number
  originalPrice: number
  discountAmount: number
  imageUrl: string
  description: string
  validStartDate: string
  validEndDate: string
  dailyLimit: number
  soldToday: number
  status: number
  statusName: string
  sortOrder: number
  ingredients: ComboIngredientBackend[]
  totalCost: number
  profit: number
  profitRate: number
  createTime: string
  updateTime: string
}

/** 后端套餐明细类型 */
export interface ComboIngredientBackend {
  ingredientId: number
  comboId: number
  foodId: number
  foodName: string
  quantity: number
  unit: string
  isRequired: boolean
  maxSelect: number
  sortOrder: number
  /** 单价（分） */
  unitPrice: number
  /** 小计（分） */
  subtotal: number
}

/** 前端套餐展示类型 */
export interface DishCombo {
  comboId: number
  comboCode: string
  comboName: string
  comboPrice: string
  originalPrice: string
  discountAmount: string
  imageUrl: string
  description: string
  validStartDate: string
  validEndDate: string
  dailyLimit: number
  soldToday: number
  comboStatus: ProductStatus
  statusName: string
  sortOrder: number
  ingredients: ComboIngredient[]
  totalCost: string
  profit: string
  profitRate: number
  createTime: string
  updateTime: string
}

/** 前端套餐明细类型 */
export interface ComboIngredient {
  ingredientId: number
  comboId: number
  foodId: number
  foodName: string
  quantity: number
  unit: string
  isRequired: boolean
  maxSelect: number
  sortOrder: number
  /** 单价（元） */
  unitPrice: number
  /** 小计（元） */
  subtotal: number
}

/** 套餐列表行数据 */
export interface DishComboRow {
  comboId: number
  comboCode: string
  comboName: string
  comboPrice: string
  originalPrice: string
  discountAmount: string
  description: string
  comboStatus: ProductStatus
  statusName: string
  dailyLimit: number
  soldToday: number
  validStartDate: string
  validEndDate: string
  /** 套餐总成本（元，由后端 totalCost 分除以 100 转换） */
  totalCost?: number
  /** 利润率（百分比，价格基准：(comboPrice - totalCost) / comboPrice * 100） */
  profitRate?: number
  createTime: string
  updateTime: string
}

/** 套餐表单提交数据（对应后端ComboCreateDTO/ComboUpdateDTO） */
export interface DishComboFormData {
  comboId?: number
  /** 编辑时使用的套餐ID（与 comboId 同义，dialog 表单字段） */
  id?: number
  comboCode?: string
  comboName: string
  /** 套餐类型（fixed 固定套餐 / optional 自选套餐 / discount 优惠套餐） */
  comboType?: string
  comboPrice: string
  /** 表单中的售价（元，数字），提交时转换为 comboPrice 字符串 */
  price?: number
  /** 适用人数 */
  peopleCount?: number
  originalPrice?: string
  discountAmount?: string
  imageUrl?: string
  description?: string
  validStartDate?: string
  validEndDate?: string
  dailyLimit?: number
  status?: number
  sortOrder?: number
  ingredients?: ComboIngredientFormData[]
}

/** 套餐明细表单数据（对应后端ComboIngredientDTO） */
export interface ComboIngredientFormData {
  foodId: number
  quantity: number
  unit: string
  isRequired: boolean
  maxSelect?: number
  sortOrder?: number
}

// ============================================================
// 配方/BOM相关类型
// ============================================================

/** 后端配方数据类型（RecipeVO API响应原始格式） */
export interface RecipeBackend {
  recipeId: number
  foodId: number
  foodName: string
  materialId: number
  materialName: string
  specification: string
  requiredQuantity: number
  unit: string
  lossRate: number
  unitCost: number
  subtotalCost: number
  costRatio: number
  createTime: string
  updateTime: string
}

/** 前端配方展示类型 */
export interface DishRecipe {
  recipeId: number
  foodId: number
  foodName: string
  materialId: number
  materialName: string
  specification: string
  requiredQuantity: number
  unit: string
  lossRate: number
  unitCost: string
  subtotalCost: string
  costRatio: number
  createTime: string
  updateTime: string
}

/** 配方表单提交数据（对应后端RecipeCreateDTO） */
export interface RecipeFormData {
  foodId: number
  materialId?: number
  materialName: string
  specification?: string
  requiredQuantity: number
  unit: string
  lossRate?: number
  unitCost?: string
}

// ============================================================
// 定价相关类型
// ============================================================

/** 后端定价记录类型（PricingVO API响应原始格式） */
export interface PricingBackend {
  pricingId: number
  productType: string
  productId: number
  productName: string
  oldSalePrice: number
  newSalePrice: number
  costPrice: number
  priceChange: string
  changePercent: number
  profit: number
  profitRate: number
  pricingStrategy: string
  pricingStrategyName: string
  remark: string
  operatorId: number
  operatorName: string
  effectiveDate: string
  createTime: string
  updateTime: string
}

/**
 * 前端定价记录展示类型
 *
 * 说明：本类型同时承载两种用途：
 * 1. 定价历史记录（来自 /pricing/history 接口）— 使用 pricingId/productType/productId 等字段
 * 2. 菜品定价表格行（来自 /foods 接口后映射）— 使用 foodId/foodName/salePrice/profitRate 等字段
 *
 * 因此将菜品相关字段标注为可选，便于在 DishPricing 页面复用此类型作为表格行。
 */
export interface PricingRecord {
  // 定价历史记录字段
  pricingId?: number
  productType?: string
  productId?: number
  productName?: string
  oldPrice?: string
  newPrice?: string
  adjustType?: string
  adjustValue?: string
  remark?: string
  operatorName?: string
  createTime?: string
  // 菜品定价表格行字段（DishPricing 页面复用）
  foodId?: number
  foodCode?: string
  foodName?: string
  categoryName?: string
  salePrice?: string
  costPrice?: string
  profit?: string
  profitRate?: number
  foodStatus?: ProductStatus
  statusName?: string
  lastAdjustTime?: string
}

/** 定价表单提交数据（对应后端PricingCreateDTO） */
export interface PricingFormData {
  productType: string
  productId: number
  newPrice: string
  adjustType?: string
  adjustValue?: string
  remark?: string
  /** 产品名称（批量调价时透传，便于后端记录） */
  productName?: string
  /** 成本价（元，string，批量调价时透传，便于后端记录） */
  costPrice?: string
  /** 调价策略：MANUAL手动 / MARKUP加价 / DISCOUNT折扣 / ROUND取整，默认 MANUAL */
  pricingStrategy?: string
  /** 生效日期（YYYY-MM-DD，批量调价时使用） */
  effectiveDate?: string
}

/** 调价策略枚举（与后端 pricing_records.pricing_strategy 字段对应） */
export type PricingStrategy = 'MANUAL' | 'MARKUP' | 'DISCOUNT' | 'ROUND'

/** 产品类型枚举（与后端 pricing_records.product_type 字段对应） */
export type PricingProductType = 'FOOD' | 'COMBO'

/** 批量调价对话框中单个待调价项（前端展示用） */
export interface BatchPricingItem {
  productId: number
  productName: string
  /** 当前售价（元，string） */
  currentPrice: string
  /** 成本价（元，string） */
  costPrice: string
}

/** 定价查询表单（用于 DishPricing 页面的查询参数，分页由 useCrudTable 自动注入） */
export interface PricingQueryForm {
  /** 关键字（菜品名称模糊查询） */
  keyword?: string
  /** 分类筛选 */
  categoryId?: number
  /** 状态筛选（前端语义化值） */
  status?: ProductStatus
  /** 分页页码（由 useCrudTable 自动注入，可选） */
  page?: number
  /** 分页大小（由 useCrudTable 自动注入，可选） */
  size?: number
}

/** 菜品定价列表项 */
export interface DishPricingItem {
  productId: number
  productName: string
  categoryName: string
  costPrice: string
  salePrice: string
  profitMargin: number
  pricingStatus: ProductStatus
}

/** 批量调价表单数据 */
export interface BatchPriceForm {
  adjustType: 'percentage' | 'fixed'
  adjustValue: number
  adjustDirection: 'increase' | 'decrease'
  category: string
}

/** 单个菜品调价表单数据 */
export interface SinglePriceForm {
  id: number
  name: string
  currentPrice: string
  newPrice: string
  remark: string
}

// ============================================================
// 成本分析相关类型
// ============================================================

/** 后端成本分析数据类型（CostAnalysisVO API响应原始格式） */
export interface CostAnalysisBackend {
  productId: number
  productCode: string
  productName: string
  productType: string
  categoryName: string
  specification: string
  unit: string
  salePrice: number
  costPrice: number
  profit: number
  profitRate: number
  salesCount: number
  salesAmount: number
  totalCost: number
  totalProfit: number
  status: number
  /** 周销售量（份）- 近7天累计销量 */
  weeklySales?: number
  /** 周转率(%) = 周销售量 / 当前库存 * 100（套餐无库存，返回 null） */
  turnoverRate?: number | null
}

/** 前端成本分析展示类型 */
export interface CostAnalysisItem {
  productId: number
  productCode: string
  productName: string
  productType: string
  categoryName: string
  salePrice: string
  costPrice: string
  profit: string
  profitRate: number
  totalCost: string
  totalProfit: string
  salesCount: number
  salesAmount: string
  costStatus: 'normal' | 'warning' | 'danger'
  productStatus: ProductStatus
  /** 周销售量（份）- 近7天累计销量 */
  weeklySales: number
  /** 周转率(%) = 周销售量 / 当前库存 * 100（套餐无库存，返回 null） */
  turnoverRate: number | null
}

/** 成本概览统计 */
export interface CostSummary {
  totalDishes: number
  normalCount: number
  warningCount: number
  dangerCount: number
  avgMarginRate: number
  totalCostChange: number
  topCostIncrease: Array<{ dishName: string; changePercent: number }>
  topNegativeProfit: Array<{ dishName: string; marginRate: number }>
}

/** 成本趋势数据 */
export interface CostTrendData {
  date: string
  avgCost: number
  maxCost: number
  minCost: number
  dishCount: number
}

/**
 * 销售数据汇总项（来源于 order_items 表聚合）
 * 用于菜品成本分析页面的"销售数据图表"
 * 金额单位：分（与后端一致），前端展示时通过 converter 转为元
 */
export interface SalesSummaryItem {
  /** 商品名称（菜品/套餐名称） */
  productName: string
  /** 产品类型：FOOD-菜品 / COMBO-套餐 */
  productType: string
  /** 累计销量（份） */
  salesCount: number
  /** 累计销售额（分） */
  salesAmount: number
}

/** 批量操作结果 */
export interface BatchCostResult {
  success: number
  failed: number
  skipped: number
  details: Array<{ dishId: string; dishName: string; reason: string }>
}

// ============================================================
// API辅助类型
// ============================================================

/** 价格历史记录 */
export interface PriceHistoryRecord {
  pricingId: number
  productType: string
  productId: number
  productName: string
  oldPrice: string
  newPrice: string
  adjustType: string
  createTime: string
  operatorName: string
  remark: string
}

/** 套餐详情 */
export interface ComboDetail {
  comboId: number
  comboCode: string
  comboName: string
  comboPrice: string
  originalPrice: string
  description: string
  imageUrl: string
  comboStatus: ProductStatus
  ingredients: ComboIngredient[]
}

/** 产品库存记录 */
export interface ProductInventoryRecord {
  productId: string
  productName: string
  category: string
  currentStock: number
  safeStock: number
  unit: string
  status: string
}

/** 导入结果 */
export interface ImportResult {
  success: number
  fail: number
  errors?: string[]
}

/** 批量上传结果（对应后端 FoodImportResultDTO） */
export interface BatchUploadResult {
  totalCount: number
  successCount: number
  failCount: number
  errorMessages: string[]
}

/** BOM检查结果 */
export interface BomCheckResult {
  dishId: string
  dishName: string
  canMake: boolean
  totalRequired: number
  insufficientItems: InsufficientItem[]
  alternativeDishes: AlternativeDish[]
  checkedAt: string
}

/** 缺料项 */
export interface InsufficientItem {
  materialId: string
  materialName: string
  requiredQty: number
  availableQty: number
  shortageQty: number
  shortagePercent: number
  unit: string
  urgency: 'low' | 'medium' | 'high'
}

/** 替代菜品 */
export interface AlternativeDish {
  dishId: string
  dishName: string
  category: string
  price: number
  canMake: boolean
  similarityScore: number
  reason: string
}

/** BOM库存预警配置 */
export interface BomStockWarningConfig {
  id: string
  enabled: boolean
  checkPoint: 'order' | 'kitchen_pick' | 'both'
  autoSuggestAlternative: boolean
  lowStockThreshold: number
  warnBeforeOrder: boolean
}

/** BOM 批量检查请求项 */
export interface BatchCheckItem {
  dishId: string
  quantity: number
}

/** BOM 批量检查请求 */
export interface BatchCheckRequest {
  items: BatchCheckItem[]
}

/** BOM 批量检查结果 */
export interface BatchCheckResult {
  totalChecked: number
  canMakeCount: number
  insufficientCount: number
  healthScore: number
  results: BomCheckResult[]
}

/** 菜品成本 BOM 项明细 */
export interface BomItemCost {
  materialId: string
  materialName: string
  quantity: number
  unit: string
  unitPrice: number
  subtotal: number
  costPercent: number
}

/** 菜品成本快照 */
export interface DishCostSnapshot {
  dishId: string
  dishName: string
  categoryId?: string
  categoryName?: string
  salePrice: number
  totalCost: number
  profit: number
  marginRate: number
  costChangePercent: number
  status: 'normal' | 'warning' | 'danger'
  lastUpdated: string
  bomItems: BomItemCost[]
}

/** 成本预警规则 */
export interface CostAlertRule {
  id: string
  name: string
  ruleType: 'margin_below' | 'cost_increase' | 'absolute_cost'
  threshold: number
  enabled: boolean
  categoryId?: string
  categoryName?: string
  createdAt: string
}
