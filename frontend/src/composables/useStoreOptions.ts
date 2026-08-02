/**
 * 门店下拉选项 Composable
 *
 * 提供统一的门店下拉数据加载能力，避免各页面硬编码门店列表。
 * 后端真实 API：/v1/stores/active、/v1/stores（分页查询）
 *
 * 使用方式：
 * ```ts
 * const { storeOptions, loading, loadStores, getStoreName } = useStoreOptions()
 * await loadStores()
 * ```
 */
import { ref, type Ref } from 'vue'
import { storeArchiveApi } from '@/api/store-ops/store-archive'
import type { StoreOption } from '@/types/store-operation/store-archive'

/**
 * 门店下拉选项 Composable
 *
 * @param autoLoad 是否在创建时自动加载，默认为 false
 */
export function useStoreOptions(autoLoad = false): {
  /** 门店选项列表 */
  storeOptions: Ref<StoreOption[]>
  /** 加载状态 */
  loading: Ref<boolean>
  /** 错误信息 */
  error: Ref<string | null>
  /** 加载营业中门店列表（用于下拉选项） */
  loadStores: () => Promise<void>
  /** 根据 ID 获取门店名称 */
  getStoreName: (id: number | string | null | undefined) => string
  /** 根据 ID 获取门店选项 */
  getStoreById: (id: number | string | null | undefined) => StoreOption | undefined
} {
  const storeOptions = ref<StoreOption[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  /** 加载营业中门店列表 */
  async function loadStores(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const list = await storeArchiveApi.getActiveStores()
      storeOptions.value = list || []
    } catch (err) {
      storeOptions.value = []
      error.value = err instanceof Error ? err.message : '加载门店列表失败'
      // eslint-disable-next-line no-console
      console.error('[useStoreOptions] 加载门店数据失败:', err)
    } finally {
      loading.value = false
    }
  }

  /** 根据 ID 获取门店名称 */
  function getStoreName(id: number | string | null | undefined): string {
    if (id === null || id === undefined || id === '') return '-'
    const numId = typeof id === 'string' ? Number(id) : id
    if (Number.isNaN(numId)) return '-'
    const found = storeOptions.value.find((s) => s.storeId === numId)
    return found?.storeName ?? '-'
  }

  /** 根据 ID 获取门店选项 */
  function getStoreById(id: number | string | null | undefined): StoreOption | undefined {
    if (id === null || id === undefined || id === '') return undefined
    const numId = typeof id === 'string' ? Number(id) : id
    if (Number.isNaN(numId)) return undefined
    return storeOptions.value.find((s) => s.storeId === numId)
  }

  if (autoLoad) {
    loadStores()
  }

  return {
    storeOptions,
    loading,
    error,
    loadStores,
    getStoreName,
    getStoreById,
  }
}
