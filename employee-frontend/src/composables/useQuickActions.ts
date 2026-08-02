import { ref, watch } from 'vue'

const STORAGE_KEY = 'emp-quick-actions'
const MAX_COUNT = 8
const DEFAULT_IDS = ['approval', 'tasks', 'review', 'schedule', 'leave', 'salary', 'training', 'knowledge'] as const

const selectedIds = ref<string[]>(loadFromStorage())

function loadFromStorage(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.slice(0, MAX_COUNT)
    }
  } catch {
    // ignore
  }
  return [...DEFAULT_IDS]
}

function saveToStorage() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(selectedIds.value))
}

watch(selectedIds, saveToStorage, { deep: true })

export function useQuickActions() {
  function isSelected(id: string): boolean {
    return selectedIds.value.includes(id)
  }

  function toggle(id: string): boolean {
    const idx = selectedIds.value.indexOf(id)
    if (idx >= 0) {
      if (selectedIds.value.length > 1) {
        selectedIds.value.splice(idx, 1)
        return false
      }
      return true
    } else {
      if (selectedIds.value.length < MAX_COUNT) {
        selectedIds.value.push(id)
        return true
      }
      return false
    }
  }

  function reset() {
    selectedIds.value = [...DEFAULT_IDS]
  }

  function setIds(ids: string[]) {
    selectedIds.value = ids.slice(0, MAX_COUNT)
  }

  return {
    selectedIds,
    maxCount: MAX_COUNT,
    defaultIds: DEFAULT_IDS,
    isSelected,
    toggle,
    reset,
    setIds,
  }
}
