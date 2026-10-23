import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局组织/门店上下文 Store（P1-DATA-010）
 *
 * 背景：多数页面下拉在 onMounted 一次性加载，组织/门店变更后不刷新，导致可选到陈旧数据。
 * 本 store 提供统一的「当前组织/门店」上下文：切换时递增 version，
 * useStoreOptions / useDepartmentOptions 等公共 composable 监听 version 变化自动重载下拉，
 * 实现「切换门店/组织后下拉立即更新；旧数据不再可选」。
 *
 * 使用方式（门店/组织切换入口调用）：
 * ```ts
 * const orgContext = useOrgContextStore()
 * orgContext.setStore(storeId)  // 切换门店（null/undefined 表示全部）
 * orgContext.setOrg(orgId)      // 切换组织（null/undefined 表示全部）
 * ```
 */
export const useOrgContextStore = defineStore('orgContext', () => {
  /** 当前门店 ID（null 表示不限定门店） */
  const currentStoreId = ref<string | null>(null)

  /** 当前组织 ID（null 表示不限定组织） */
  const currentOrgId = ref<string | null>(null)

  /** 上下文版本号：每次切换组织/门店或主动刷新时递增，下拉 composable 监听此值触发重载 */
  const version = ref(0)

  /** 切换当前门店（storeId 传 null/undefined 表示切回「全部门店」） */
  function setStore(storeId: string | number | null | undefined): void {
    const next = storeId === null || storeId === undefined || storeId === '' ? null : String(storeId)
    if (next !== currentStoreId.value) {
      currentStoreId.value = next
      version.value += 1
    }
  }

  /** 切换当前组织（orgId 传 null/undefined 表示切回「全部组织」） */
  function setOrg(orgId: string | number | null | undefined): void {
    const next = orgId === null || orgId === undefined || orgId === '' ? null : String(orgId)
    if (next !== currentOrgId.value) {
      currentOrgId.value = next
      version.value += 1
    }
  }

  /** 主动刷新：强制所有监听上下文的下拉重载 */
  function refresh(): void {
    version.value += 1
  }

  return {
    currentStoreId,
    currentOrgId,
    version,
    setStore,
    setOrg,
    refresh,
  }
})
