import { ref, computed } from 'vue'
import { posApi } from '@/api/posApi'
import type { Category, Dish, Combo } from '@/api/posApi'

export function useMenu() {
  const loading = ref(false)
  /** 加载错误信息（null 表示无错误；非空时显示重试按钮） */
  const loadError = ref<string | null>(null)
  const categories = ref<Category[]>([])
  const dishes = ref<Dish[]>([])
  const combos = ref<Combo[]>([])
  const activeCategory = ref('all')

  /**
   * 当前可展示的菜单项（菜品 + 套餐），按 activeCategory 过滤
   * 套餐归入 'combo' 分类，菜品按各自 categoryId 过滤
   */
  const filteredItems = computed(() => {
    let items: Array<{
      id: string;
      name: string;
      price: number;
      categoryId: string;
      dishType: string;
      isHot?: boolean;
      imageUrl?: string;
      stock: number;
    }> = []
    items = items.concat(dishes.value.map(d => ({
      // 使用 dishCode(food_code) 作为 item.id，与后端 food 表主键一致
      // 后端 PosOrderCreateServiceImpl.createOrder 通过 food_code 查询 food 表并扣减库存
      id: d.dishCode,
      name: d.dishName,
      price: d.price,
      categoryId: d.categoryId,
      dishType: 'single',
      isHot: !!(d.salesCount && d.salesCount > 50),
      imageUrl: d.imageUrl,
      // 使用后端真实库存，未返回时默认 999
      stock: typeof d.stock === 'number' ? d.stock : 999,
    })))
    items = items.concat(combos.value.map(c => ({
      id: c.comboId,
      name: c.comboName,
      price: c.price,
      categoryId: 'combo',
      dishType: 'combo',
      isHot: !!(c.salesCount && c.salesCount > 50),
      imageUrl: c.imageUrl,
      stock: 999,
    })))
    if (activeCategory.value !== 'all') {
      items = items.filter(item => item.categoryId === activeCategory.value)
    }
    return items
  })

  /** 库存低于阈值的菜品（套餐无库存概念，使用 999 占位不会进入低库存列表） */
  const lowStockItems = computed(() => {
    return filteredItems.value.filter(item => item.stock < 10).sort((a, b) => a.stock - b.stock)
  })

  /**
   * 从产品中心加载菜单数据（菜品、套餐、分类）
   * 仅展示在售（status=1）数据，由后端 on-sale 接口保证
   * @param silent - 静默模式，不显示 loading 与错误提示（用于 WebSocket 自动刷新）
   */
  async function loadMenu(silent: boolean = false) {
    if (!silent) {
      loading.value = true
      loadError.value = null
    }
    try {
      // 并行加载菜品、套餐、分类三个接口
      const [newDishes, newCombos, loadedCategories] = await Promise.all([
        posApi.listFoodsOnSale(),
        posApi.listCombosOnSale(),
        posApi.listEnabledCategories(),
      ])

      // 构造"全部"分类项 + 实际分类列表
      const allCategories: Category[] = [
        { categoryId: 'all', categoryName: '全部', sortOrder: 0 },
        ...loadedCategories,
      ]
      // 套餐作为独立分类
      if (newCombos.length > 0) {
        allCategories.push({ categoryId: 'combo', categoryName: '套餐', sortOrder: 999 })
      }

      const categoriesChanged = JSON.stringify(categories.value) !== JSON.stringify(allCategories)
      const dishesChanged = JSON.stringify(dishes.value) !== JSON.stringify(newDishes)
      const combosChanged = JSON.stringify(combos.value) !== JSON.stringify(newCombos)

      if (categoriesChanged || dishesChanged || combosChanged) {
        categories.value = allCategories
        dishes.value = newDishes
        combos.value = newCombos

        // 当前选中的分类不存在时回退到"全部"
        if (activeCategory.value && activeCategory.value !== 'all') {
          const exists = categories.value.some((c: Category) => c.categoryId === activeCategory.value)
          if (!exists) {
            activeCategory.value = 'all'
          }
        }
      }
      loadError.value = null
    } catch (error: unknown) {
      // 静默模式不覆盖已有错误状态，避免后台刷新时把已显示的数据"清掉"
      if (!silent) {
        const err = error as { message?: string } | null
        loadError.value = err?.message || '加载菜单失败，请检查后端服务是否正常'
      }
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    loadError,
    categories,
    dishes,
    combos,
    activeCategory,
    filteredItems,
    lowStockItems,
    loadMenu,
  }
}
