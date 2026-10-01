/**
 * 位置下拉选项 Composable（P1-USER-LOCATION-001）
 *
 * 与 useStoreOptions 同构：统一从 `/v1/locations` 加载活跃位置
 * （locations 表：STORE / CENTRAL / DEPOT / TRANSIT），供"用户归属位置"等下拉使用。
 *
 * 归属字段语义见 design-002 §5 修订一：`locationId` = `locations.location_id`
 * （不再用 stores_new.store_id；跨 ID 空间换算由后端经 location_id_map 处理）。
 *
 * 使用方式：
 * ```ts
 * const { locationOptions, loadLocations } = useLocationOptions(true)
 * ```
 */
import { ref, type Ref } from 'vue'
import { locationApi, type LocationOption } from '@/api/location'

export function useLocationOptions(autoLoad = false): {
  /** 位置选项列表 */
  locationOptions: Ref<LocationOption[]>
  /** 加载状态 */
  loading: Ref<boolean>
  /** 错误信息 */
  error: Ref<string | null>
  /** 加载活跃位置列表（可按类型过滤） */
  loadLocations: (locationType?: string) => Promise<void>
  /** 根据 locationId 获取位置名称 */
  getLocationName: (id: number | string | null | undefined) => string
} {
  const locationOptions = ref<LocationOption[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadLocations(locationType?: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const list = await locationApi.listActive(locationType)
      locationOptions.value = list || []
    } catch (err) {
      locationOptions.value = []
      error.value = err instanceof Error ? err.message : '加载位置列表失败'
      // eslint-disable-next-line no-console
      console.error('[useLocationOptions] 加载位置数据失败:', err)
    } finally {
      loading.value = false
    }
  }

  function getLocationName(id: number | string | null | undefined): string {
    if (id === null || id === undefined || id === '') return '-'
    const numId = typeof id === 'string' ? Number(id) : id
    if (Number.isNaN(numId)) return '-'
    return locationOptions.value.find((l) => l.locationId === numId)?.locationName ?? '-'
  }

  if (autoLoad) {
    loadLocations()
  }

  return {
    locationOptions,
    loading,
    error,
    loadLocations,
    getLocationName,
  }
}
