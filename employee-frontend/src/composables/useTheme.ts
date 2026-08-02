import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'emp-theme'

const THEME_MODE_LABELS: Record<ThemeMode, string> = {
  light: '浅色',
  dark: '深色',
  system: '跟随系统',
}

function getSystemPrefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function applyTheme(mode: ThemeMode) {
  const root = document.documentElement
  const isDark = mode === 'dark' || (mode === 'system' && getSystemPrefersDark())

  root.classList.toggle('dark', isDark)
  root.setAttribute('data-theme', isDark ? 'dark' : 'light')

  const metaThemeColor = document.querySelector('meta[name="theme-color"]')
  if (metaThemeColor) {
    metaThemeColor.setAttribute('content', isDark ? '#1a1a2e' : '#ffffff')
  }
}

const currentMode = ref<ThemeMode>('system')

let mediaQuery: MediaQueryList | null = null
let systemListener: ((e: MediaQueryListEvent) => void) | null = null

export function useTheme() {
  onMounted(() => {
    const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    if (saved && ['light', 'dark', 'system'].includes(saved)) {
      currentMode.value = saved
    }

    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemListener = () => {
      if (currentMode.value === 'system') {
        applyTheme(currentMode.value)
      }
    }
    mediaQuery.addEventListener('change', systemListener)
  })

  onBeforeUnmount(() => {
    if (mediaQuery && systemListener) {
      mediaQuery.removeEventListener('change', systemListener)
    }
  })

  function setTheme(mode: ThemeMode) {
    currentMode.value = mode
    localStorage.setItem(STORAGE_KEY, mode)
    applyTheme(mode)
  }

  function isDarkMode(): boolean {
    if (currentMode.value === 'dark') return true
    if (currentMode.value === 'light') return false
    return getSystemPrefersDark()
  }

  const themeLabel = (): string => THEME_MODE_LABELS[currentMode.value]

  return {
    currentMode,
    setTheme,
    isDark: isDarkMode,
    themeLabel,
    THEME_MODE_LABELS,
  }
}
