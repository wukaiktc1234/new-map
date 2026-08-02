import { ref } from 'vue'

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
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hours = String(now.getHours()).padStart(2, '0')
    const minutes = String(now.getMinutes()).padStart(2, '0')
    const seconds = String(now.getSeconds()).padStart(2, '0')
    currentTime.value = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
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
