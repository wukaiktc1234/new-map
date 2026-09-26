import { ref } from 'vue'
import { posApi } from '@/api/posApi'
import type { Category, Dish, Combo } from '@/api/posApi'

/**
 * P1-POS-MENU-UNIFICATION-001（3a）：Order / CustomerOrder 页面菜单数据源
 *
 * 与 Home.vue 的 useMenu 采用同一数据源（产品中心三接口），
 * 但为独立实现（不复用 useMenu），保证 Home.vue 零 diff（A8 回归约束）：
 * - GET /v1/product-center/foods/on-sale      → 菜品（新 foods 表）
 * - GET /v1/product-center/combos/on-sale     → 套餐（新 dish_combos 表）
 * - GET /v1/product-center/categories/tree/enabled → 分类（新 food_categories 表）
 *
 * 三接口并行加载（Home.vue 生产已验证模式）。
 * 数据形状（Dish/Combo/Category）由 posApi.ts 的既有转换器
 * foodVOToDish / comboVOToCombo / categoryVOTreeToList 统一产出。
 */
export function usePosMenu() {
  const loading = ref(false)
  /** 加载错误信息（null 表示无错误；非空时页面可显示重试入口） */
  const loadError = ref<string | null>(null)
  const categories = ref<Category[]>([])
  const dishes = ref<Dish[]>([])
  const combos = ref<Combo[]>([])
  const activeCategory = ref('all')

  /**
   * 从产品中心加载菜单数据（菜品、套餐、分类）
   * 仅展示在售（status=1）数据，由后端 on-sale 接口保证
   * @param silent - 静默模式，不显示 loading 与错误提示（预留自动刷新扩展）
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

      // 分类列表 + 套餐虚拟分类（P1-COMBO-ORDER-001，与 useMenu 保持一致）
      const allCategories: Category[] = [...loadedCategories]
      if (newCombos.length > 0) {
        allCategories.push({ categoryId: 'combo', categoryName: '套餐', sortOrder: 999 })
      }

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
      loadError.value = null
    } catch (error: unknown) {
      // 静默模式不覆盖已有错误状态
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
    loadMenu,
  }
}
