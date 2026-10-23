import { ref } from 'vue'
import { formatDateTime } from '@/utils/format'

const THEME_KEY = 'pos-theme-dark'

function applyTheme(dark: boolean) {
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
}

export function useTheme() {
  const isDark = ref(false)
  const currentTime = ref('')
  let timer: number | null = null

  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem(THEME_KEY, String(isDark.value))
    applyTheme(isDark.value)
  }

  function updateTime() {
    // 统一走 utils/format，保证 POS 端时钟与订单时间同格式（YYYY-MM-DD HH:mm:ss）
    currentTime.value = formatDateTime(new Date())
  }

  function initTime() {
    const saved = localStorage.getItem(THEME_KEY)
    isDark.value = saved === 'true'
    applyTheme(isDark.value)
    updateTime()
    timer = window.setInterval(updateTime, 1000)
  }

  function cleanupTime() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  return {
    isDark,
    currentTime,
    toggleTheme,
    initTime,
    cleanupTime
  }
}
