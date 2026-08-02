import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { safeJsonParse } from '@/utils/storage'

/**
 * 标签页数据
 */
export interface Tab {
  name: string
  title: string
  path: string
}

const TABS_STORAGE_KEY = 'tab-bar-tabs'

/** 不应出现在标签页中的路径 */
const STANDALONE_PATHS = ['/login', '/forgot-password', '/register', '/404', '/403']

function loadTabs(): Tab[] {
  const stored = localStorage.getItem(TABS_STORAGE_KEY)
  const parsed = safeJsonParse<Tab[] | null>(stored, null)
  if (!parsed || !Array.isArray(parsed) || parsed.length === 0) {
    return [{ name: 'Dashboard', title: '工作台', path: '/home' }]
  }
  const valid = parsed.filter(
    tab => tab?.path && !STANDALONE_PATHS.includes(tab.path) && tab.title
  )
  return valid.length > 0 ? valid : [{ name: 'Dashboard', title: '工作台', path: '/home' }]
}

function saveTabs(tabs: Tab[]): void {
  try {
    localStorage.setItem(TABS_STORAGE_KEY, JSON.stringify(tabs))
  } catch { /* ignore quota errors */ }
}

export const useTabStore = defineStore('tab', () => {
  const tabs = ref<Tab[]>(loadTabs())
  const activePath = ref('')

  /** 当前激活的标签 */
  const activeTab = computed<Tab | undefined>(() =>
    tabs.value.find(t => t.path === activePath.value)
  )

  watch(tabs, (val) => saveTabs(val), { deep: true })

  /**
   * 打开或激活标签
   * @param tab 标签信息
   */
  function openTab(tab: Tab): void {
    if (STANDALONE_PATHS.includes(tab.path)) return
    const exists = tabs.value.find(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push(tab)
    }
    activePath.value = tab.path
  }

  /**
   * 设置普通模式下的激活标签
   */
  function setActive(path: string): void {
    if (!tabs.value.find(t => t.path === path)) return
    activePath.value = path
  }

  /**
   * 关闭标签（全局移除）
   * @returns 关闭后建议跳转的路径
   */
  function closeTab(path: string): string | null {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index < 0) return null

    tabs.value.splice(index, 1)

    // 若移除的是当前激活标签，回退到前一个标签
    let fallback: string | null = null
    if (activePath.value === path) {
      fallback = tabs.value[Math.max(0, index - 1)]?.path || tabs.value[0]?.path || '/home'
      activePath.value = fallback
    }

    return fallback
  }

  /**
   * 关闭其他标签
   */
  function closeOthers(path: string): void {
    const keep = tabs.value.find(t => t.path === path)
    const home = tabs.value.find(t => t.path === '/home')
    tabs.value = home && home.path !== path ? [home, keep].filter(Boolean) as Tab[] : keep ? [keep] : []
  }

  /**
   * 关闭所有标签
   */
  function closeAll(): void {
    const home = tabs.value.find(t => t.path === '/home')
    tabs.value = home ? [home] : []
    activePath.value = home?.path || '/home'
  }

  return {
    tabs,
    activePath,
    activeTab,
    openTab,
    setActive,
    closeTab,
    closeOthers,
    closeAll
  }
})
