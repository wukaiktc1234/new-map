/**
 * 产品中心数据转换器
 * 负责后端数据与前端展示数据之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 *
 * 金额转换说明：
 * - 后端以分为单位（BIGINT），前端以元为单位（string）
 * - 转换逻辑统一委托给 utils/money
 */

import { fenToYuan, yuanToFen } from '@/utils/money'
import type {
  Food,
  FoodBackend,
  FoodFormData,
  FoodRow,
  ProductStatus,
  CategoryOption,
  CategoryBackend,
  FoodCategory,
  CategoryFormData,
  ComboBackend,
  DishCombo,
  DishComboRow,
  DishComboFormData,
  ComboIngredientBackend,
  ComboIngredient,
  ComboIngredientFormData,
  RecipeBackend,
  DishRecipe,
  RecipeFormData,
  PricingBackend,
  PricingRecord,
  PricingFormData,
  CostAnalysisBackend,
  CostAnalysisItem,
} from '@/types/product'

/** 后端状态值 → 前端语义化状态 */
const STATUS_MAP: Record<string, ProductStatus> = {
  '1': 'active',
  '0': 'inactive',
  '2': 'soldout',
}

/** 前端语义化状态 → 后端状态值 */
const STATUS_REVERSE_MAP: Record<ProductStatus, number> = {
  active: 1,
  inactive: 0,
  soldout: 2,
}

/** 前端状态 → 中文标签 */
const STATUS_LABEL_MAP: Record<ProductStatus, string> = {
  active: '在售',
  inactive: '停售',
  soldout: '售罄',
}

// 金额转换（fenToYuan / yuanToFen）统一使用 utils/money 实现，详见 import

export const productDataConverter = {
  // ============================================================
  // 菜品（Food）转换
  // ============================================================

  toFrontend(backend: FoodBackend): Food {
    const rawStatus = String(backend.status ?? '0')
    return {
      foodId: backend.foodId,
      foodCode: backend.foodCode || '',
      foodName: backend.foodName || '',
      categoryId: backend.categoryId,
      categoryName: backend.categoryName || '',
      specification: backend.specification || '',
      unit: backend.unit || '',
      salePrice: fenToYuan(backend.salePrice),
      costPrice: fenToYuan(backend.costPrice),
      profit: fenToYuan(backend.profit),
      profitRate: backend.profitRate ?? 0,
      stock: backend.stock ?? 0,
      minStock: backend.minStock ?? 0,
      imageUrl: backend.imageUrl || '',
      description: backend.description || '',
      cookingTime: backend.cookingTime ?? 0,
      foodStatus: STATUS_MAP[rawStatus] || 'inactive',
      statusName: backend.statusName || STATUS_LABEL_MAP[STATUS_MAP[rawStatus] || 'inactive'],
      isRecommend: backend.isRecommend ?? false,
      isSpicy: backend.isSpicy ?? false,
      sortOrder: backend.sortOrder ?? 0,
      createTime: backend.createTime || '',
      updateTime: backend.updateTime || '',
      // 原料明细直接透传（金额单位：分，与后端 RecipeItemDTO 一致）
      recipes: backend.recipes,
    }
  },

  toCreateDTO(frontend: FoodFormData): Record<string, unknown> {
    // 防御性状态转换：form.status 可能为数字（1/0）或字符串（'active'/'inactive'）
    let statusNum = 1
    if (typeof frontend.status === 'number') {
      statusNum = frontend.status
    } else if (typeof frontend.status === 'string') {
      statusNum = STATUS_REVERSE_MAP[frontend.status as ProductStatus] ?? 1
    }
    return {
      foodName: frontend.foodName,
      foodCode: frontend.foodCode || undefined,
      categoryId: frontend.categoryId,
      specification: frontend.specification || undefined,
      unit: frontend.unit || undefined,
      salePrice: yuanToFen(frontend.salePrice),
      costPrice: frontend.costPrice ? yuanToFen(frontend.costPrice) : undefined,
      stock: frontend.stock ?? undefined,
      minStock: frontend.minStock ?? undefined,
      imageUrl: frontend.imageUrl || undefined,
      description: frontend.description || undefined,
      cookingTime: frontend.cookingTime ?? undefined,
      status: statusNum,
      isRecommend: frontend.isRecommend ?? false,
      isSpicy: frontend.isSpicy ?? false,
      sortOrder: frontend.sortOrder ?? 0,
      // 原料明细直接透传（金额单位：分，由组件在 submit 时完成 元→分 转换）
      recipes: frontend.recipes,
    }
  },

  toUpdateDTO(frontend: FoodFormData): Record<string, unknown> {
    return this.toCreateDTO(frontend)
  },

  toFrontendList(backendList: FoodBackend[]): Food[] {
    return backendList.map(item => this.toFrontend(item))
  },

  toTableRow(data: Food): FoodRow {
    return { ...data }
  },

  toForm(data: Food): FoodFormData {
    return {
      foodId: data.foodId,
      foodCode: data.foodCode,
      foodName: data.foodName,
      categoryId: data.categoryId,
      specification: data.specification,
      unit: data.unit,
      salePrice: data.salePrice,
      costPrice: data.costPrice,
      stock: data.stock,
      minStock: data.minStock,
      imageUrl: data.imageUrl,
      description: data.description,
      cookingTime: data.cookingTime,
      status: STATUS_REVERSE_MAP[data.foodStatus] ?? 1,
      isRecommend: data.isRecommend,
      isSpicy: data.isSpicy,
      sortOrder: data.sortOrder,
    }
  },

  fromForm(form: FoodFormData): Food {
    return {
      foodId: form.foodId ?? 0,
      foodCode: form.foodCode || '',
      foodName: form.foodName,
      categoryId: form.categoryId,
      categoryName: '',
      specification: form.specification || '',
      unit: form.unit || '',
      salePrice: form.salePrice,
      costPrice: form.costPrice || '0.00',
      profit: '0.00',
      profitRate: 0,
      stock: form.stock ?? 0,
      minStock: form.minStock ?? 0,
      imageUrl: form.imageUrl || '',
      description: form.description || '',
      cookingTime: form.cookingTime ?? 0,
      foodStatus: STATUS_MAP[String(form.status ?? 1)] || 'active',
      statusName: STATUS_LABEL_MAP[STATUS_MAP[String(form.status ?? 1)] || 'active'],
      isRecommend: form.isRecommend ?? false,
      isSpicy: form.isSpicy ?? false,
      sortOrder: form.sortOrder ?? 0,
      createTime: '',
      updateTime: '',
    }
  },

  // ============================================================
  // 分类（Category）转换
  // ============================================================

  categoryToFrontend(backend: CategoryBackend): FoodCategory {
    const rawStatus = String(backend.status ?? '1')
    return {
      categoryId: backend.categoryId,
      categoryName: backend.categoryName || '',
      parentId: backend.parentId ?? 0,
      iconUrl: backend.iconUrl || '',
      sortOrder: backend.sortOrder ?? 0,
      categoryStatus: STATUS_MAP[rawStatus] || 'active',
      foodCount: backend.foodCount ?? 0,
      children: backend.children ? backend.children.map(c => this.categoryToFrontend(c)) : [],
      createTime: backend.createTime || '',
      updateTime: backend.updateTime || '',
    }
  },

  categoryToCreateDTO(form: CategoryFormData): Record<string, unknown> {
    // 防御性状态转换：form.status 可能为数字（1/0）或字符串（'active'/'inactive'）
    let statusNum = 1
    if (typeof form.status === 'number') {
      statusNum = form.status
    } else if (typeof form.status === 'string') {
      statusNum = STATUS_REVERSE_MAP[form.status as ProductStatus] ?? 1
    }
    return {
      categoryName: form.categoryName,
      parentId: form.parentId ?? 0,
      iconUrl: form.iconUrl || undefined,
      sortOrder: form.sortOrder ?? 0,
      status: statusNum,
    }
  },

  categoryListToOptions(categories: FoodCategory[]): CategoryOption[] {
    const options: CategoryOption[] = []
    for (const cat of categories) {
      options.push({ value: cat.categoryId, label: cat.categoryName })
      if (cat.children && cat.children.length > 0) {
        // 子分类按 categoryId ASC 排序，确保按添加顺序显示
        // 后端按 sortOrder ASC, categoryId ASC 排序，但 sortOrder 可能存在异常值（如 0）导致顺序错乱
        // 下拉菜单场景下，按 categoryId ASC（添加顺序）展示更符合用户预期
        const sortedChildren = [...cat.children].sort((a, b) => a.categoryId - b.categoryId)
        for (const child of sortedChildren) {
          options.push({ value: child.categoryId, label: `  └ ${child.categoryName}` })
        }
      }
    }
    return options
  },

  // ============================================================
  // 套餐（Combo）转换
  // ============================================================

  comboToFrontend(backend: ComboBackend): DishCombo {
    const rawStatus = String(backend.status ?? '1')
    return {
      comboId: backend.comboId,
      comboCode: backend.comboCode || '',
      comboName: backend.comboName || '',
      comboPrice: fenToYuan(backend.comboPrice),
      originalPrice: fenToYuan(backend.originalPrice),
      discountAmount: fenToYuan(backend.discountAmount),
      imageUrl: backend.imageUrl || '',
      description: backend.description || '',
      validStartDate: backend.validStartDate || '',
      validEndDate: backend.validEndDate || '',
      dailyLimit: backend.dailyLimit ?? 0,
      soldToday: backend.soldToday ?? 0,
      comboStatus: STATUS_MAP[rawStatus] || 'active',
      statusName: backend.statusName || STATUS_LABEL_MAP[STATUS_MAP[rawStatus] || 'active'],
      sortOrder: backend.sortOrder ?? 0,
      ingredients: backend.ingredients ? backend.ingredients.map(i => this.ingredientToFrontend(i)) : [],
      totalCost: fenToYuan(backend.totalCost),
      profit: fenToYuan(backend.profit),
      profitRate: backend.profitRate ?? 0,
      createTime: backend.createTime || '',
      updateTime: backend.updateTime || '',
    }
  },

  comboToRow(combo: DishCombo): DishComboRow {
    return {
      comboId: combo.comboId,
      comboCode: combo.comboCode,
      comboName: combo.comboName,
      comboPrice: combo.comboPrice,
      originalPrice: combo.originalPrice,
      discountAmount: combo.discountAmount,
      description: combo.description,
      comboStatus: combo.comboStatus,
      statusName: combo.statusName,
      dailyLimit: combo.dailyLimit,
      soldToday: combo.soldToday,
      validStartDate: combo.validStartDate,
      validEndDate: combo.validEndDate,
      // totalCost：DishCombo.totalCost 是元字符串，转换为数字便于表格展示
      totalCost: Number(combo.totalCost) || 0,
      profitRate: combo.profitRate ?? 0,
      createTime: combo.createTime,
      updateTime: combo.updateTime,
    }
  },

  comboToCreateDTO(form: DishComboFormData): Record<string, unknown> {
    // 防御性状态转换：form.status 可能为数字（1/0）或字符串（'active'/'inactive'）
    // 后端 ComboUpdateDTO.status 是 Integer，必须传数字
    let statusNum = 1
    if (typeof form.status === 'number') {
      statusNum = form.status
    } else if (typeof form.status === 'string') {
      statusNum = STATUS_REVERSE_MAP[form.status as ProductStatus] ?? 1
    }
    return {
      comboName: form.comboName,
      comboCode: form.comboCode || undefined,
      comboPrice: yuanToFen(form.comboPrice),
      originalPrice: form.originalPrice ? yuanToFen(form.originalPrice) : undefined,
      discountAmount: form.discountAmount ? yuanToFen(form.discountAmount) : undefined,
      imageUrl: form.imageUrl || undefined,
      description: form.description || undefined,
      validStartDate: form.validStartDate || undefined,
      validEndDate: form.validEndDate || undefined,
      dailyLimit: form.dailyLimit ?? undefined,
      status: statusNum,
      sortOrder: form.sortOrder ?? 0,
      ingredients: form.ingredients ? form.ingredients.map(i => this.ingredientToCreateDTO(i)) : undefined,
    }
  },

  ingredientToFrontend(backend: ComboIngredientBackend): ComboIngredient {
    return {
      ingredientId: backend.ingredientId,
      comboId: backend.comboId,
      foodId: backend.foodId,
      foodName: backend.foodName || '',
      quantity: backend.quantity,
      unit: backend.unit || '',
      isRequired: backend.isRequired ?? true,
      maxSelect: backend.maxSelect ?? 0,
      sortOrder: backend.sortOrder ?? 0,
      unitPrice: fenToYuan(backend.unitPrice),
      subtotal: fenToYuan(backend.subtotal),
    }
  },

  ingredientToCreateDTO(form: ComboIngredientFormData): Record<string, unknown> {
    return {
      foodId: form.foodId,
      quantity: form.quantity,
      unit: form.unit,
      isRequired: form.isRequired ?? true,
      maxSelect: form.maxSelect ?? undefined,
      sortOrder: form.sortOrder ?? 0,
    }
  },

  // ============================================================
  // 配方（Recipe）转换
  // ============================================================

  recipeToFrontend(backend: RecipeBackend): DishRecipe {
    return {
      recipeId: backend.recipeId,
      foodId: backend.foodId,
      foodName: backend.foodName || '',
      materialId: backend.materialId,
      materialName: backend.materialName || '',
      specification: backend.specification || '',
      requiredQuantity: backend.requiredQuantity,
      unit: backend.unit || '',
      lossRate: backend.lossRate ?? 0,
      unitCost: fenToYuan(backend.unitCost),
      subtotalCost: fenToYuan(backend.subtotalCost),
      costRatio: backend.costRatio ?? 0,
      createTime: backend.createTime || '',
      updateTime: backend.updateTime || '',
    }
  },

  recipeToCreateDTO(form: RecipeFormData): Record<string, unknown> {
    return {
      foodId: form.foodId,
      materialId: form.materialId ?? undefined,
      materialName: form.materialName,
      specification: form.specification || undefined,
      requiredQuantity: form.requiredQuantity,
      unit: form.unit,
      lossRate: form.lossRate ?? 0,
      unitCost: form.unitCost ? yuanToFen(form.unitCost) : undefined,
    }
  },

  // ============================================================
  // 定价（Pricing）转换
  // ============================================================

  pricingToFrontend(backend: PricingBackend): PricingRecord {
    return {
      pricingId: backend.pricingId,
      productType: backend.productType,
      productId: backend.productId,
      productName: backend.productName || '',
      oldPrice: fenToYuan(backend.oldSalePrice),
      newPrice: fenToYuan(backend.newSalePrice),
      adjustType: backend.pricingStrategy || '',
      adjustValue: fenToYuan(backend.costPrice),
      remark: backend.remark || '',
      operatorName: backend.operatorName || '',
      createTime: backend.createTime || '',
    }
  },

  pricingToCreateDTO(form: PricingFormData): Record<string, unknown> {
    return {
      productType: form.productType,
      productId: form.productId,
      // 产品名称、成本价、生效日期为批量调价新增字段，存在时透传给后端
      productName: form.productName || undefined,
      costPrice: form.costPrice != null ? yuanToFen(form.costPrice) : undefined,
      salePrice: yuanToFen(form.newPrice),
      remark: form.remark || undefined,
      // 调价策略：未指定时默认 MANUAL（兼容旧调用方）
      pricingStrategy: form.pricingStrategy || 'MANUAL',
      effectiveDate: form.effectiveDate || undefined,
    }
  },

  // ============================================================
  // 成本分析（CostAnalysis）转换
  // ============================================================

  costAnalysisToFrontend(backend: CostAnalysisBackend): CostAnalysisItem {
    let costStatus: 'normal' | 'warning' | 'danger' = 'normal'
    if (backend.profitRate < 20) costStatus = 'danger'
    else if (backend.profitRate < 35) costStatus = 'warning'
    return {
      productId: backend.productId,
      productCode: backend.productCode || '',
      productName: backend.productName || '',
      productType: backend.productType || 'FOOD',
      categoryName: backend.categoryName || '',
      salePrice: fenToYuan(backend.salePrice),
      costPrice: fenToYuan(backend.costPrice),
      profit: fenToYuan(backend.profit),
      profitRate: backend.profitRate ?? 0,
      totalCost: fenToYuan(backend.totalCost),
      totalProfit: fenToYuan(backend.totalProfit),
      salesCount: backend.salesCount ?? 0,
      salesAmount: fenToYuan(backend.salesAmount),
      costStatus,
      productStatus: STATUS_MAP[String(backend.status)] || 'inactive',
      // 周销售量：后端未返回时默认 0
      weeklySales: backend.weeklySales ?? 0,
      // 周转率：套餐无库存，后端返回 null；前端透传 null 表示"不适用"
      turnoverRate: backend.turnoverRate ?? null,
    }
  },

  // ============================================================
  // 通用工具方法
  // ============================================================

  statusToFrontend(backendStatus: string | number): ProductStatus {
    return STATUS_MAP[String(backendStatus)] || 'inactive'
  },

  statusToBackend(frontendStatus: ProductStatus): number {
    return STATUS_REVERSE_MAP[frontendStatus]
  },

  getStatusLabel(status: ProductStatus): string {
    return STATUS_LABEL_MAP[status] || status
  },

  getStatusTagType(status: ProductStatus): 'success' | 'info' | 'warning' | 'danger' {
    const map: Record<ProductStatus, 'success' | 'info' | 'warning' | 'danger'> = {
      active: 'success',
      inactive: 'info',
      soldout: 'warning',
    }
    return map[status] || 'info'
  },

  formatPrice(price: string | number | null | undefined): string {
    if (price == null) return '0.00'
    return Number(price).toFixed(2)
  },

  calculateProfitMargin(costPrice: number | string, salePrice: number | string): number {
    const cost = Number(costPrice)
    const sale = Number(salePrice)
    if (!sale || sale <= 0) return 0
    return ((sale - cost) / sale) * 100
  },
}
