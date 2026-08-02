import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark' | 'auto'
export type ContentWidth = 'full' | 'fixed'
export type TabStyle = 'default' | 'chrome' | 'card'
export type MenuLayout = 'left' | 'icon' | 'mixed'
export type AnimationLevel = 'full' | 'reduced' | 'none'
export type FontFamily = 'system' | 'serif' | 'mono'
export type NavigationMode = 'tab' | 'breadcrumb'
export type TableDensity = 'default' | 'compact' | 'comfortable'
export type TableBorderStyle = 'none' | 'horizontal' | 'full'
export type PageScale = 'small' | 'default' | 'large'

function loadSettings<T>(key: string, defaultValue: T): T {
  try {
    const stored = localStorage.getItem(`layout_${key}`)
    if (stored) {
      return JSON.parse(stored) as T
    }
  } catch {
  }
  return defaultValue
}

function saveSettings<T>(key: string, value: T) {
  localStorage.setItem(`layout_${key}`, JSON.stringify(value))
}

export const useLayoutStore = defineStore('layout', () => {
  const themeMode = ref<ThemeMode>(loadSettings<ThemeMode>('themeMode', 'light'))
  const menuLayout = ref<MenuLayout>(loadSettings<MenuLayout>('menuLayout', 'left'))
  const sidebarCollapsed = ref<boolean>(loadSettings<boolean>('sidebarCollapsed', false))
  const contentWidth = ref<ContentWidth>(loadSettings<ContentWidth>('contentWidth', 'full'))
  const tabStyle = ref<TabStyle>(loadSettings<TabStyle>('tabStyle', 'default'))
  const borderRadius = ref<number>(loadSettings<number>('borderRadius', 8))
  const colorWeakMode = ref<boolean>(loadSettings<boolean>('colorWeakMode', false))
  const animationLevel = ref<AnimationLevel>(loadSettings<AnimationLevel>('animationLevel', 'full'))
  const fontFamily = ref<FontFamily>(loadSettings<FontFamily>('fontFamily', 'system'))
  const navigationMode = ref<NavigationMode>(loadSettings<NavigationMode>('navigationMode', 'tab'))
  const showWatermark = ref<boolean>(loadSettings<boolean>('showWatermark', false))
  const showFooter = ref<boolean>(loadSettings<boolean>('showFooter', true))
  const compactMode = ref<boolean>(loadSettings<boolean>('compactMode', false))
  const tableDensity = ref<TableDensity>(loadSettings<TableDensity>('tableDensity', 'default'))
  const tableBorderStyle = ref<TableBorderStyle>(loadSettings<TableBorderStyle>('tableBorderStyle', 'horizontal'))
  const tableStriped = ref<boolean>(loadSettings<boolean>('tableStriped', true))
  const tableHover = ref<boolean>(loadSettings<boolean>('tableHover', true))
  const pageFontSize = ref<PageScale>(loadSettings<PageScale>('pageFontSize', 'default'))
  const pageIconSize = ref<PageScale>(loadSettings<PageScale>('pageIconSize', 'default'))

  const sidebarWidth = computed(() => {
    switch (menuLayout.value) {
      case 'icon':
        return 128
      case 'mixed':
        return 264
      case 'left':
      default:
        return sidebarCollapsed.value ? 64 : 264
    }
  })

  const sidebarWidthValue = computed(() => `${sidebarWidth.value}px`)

  watch(borderRadius, (val) => {
    document.documentElement.style.setProperty('--fts-page-radius', `${val}px`)
  }, { immediate: true })

  watch(fontFamily, (val) => {
    applyFontFamily()
  }, { immediate: true })

  watch(animationLevel, (val) => {
    applyAnimation()
  }, { immediate: true })

  function setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
    saveSettings('themeMode', mode)
    applyTheme()
  }

  function setMenuLayout(layout: MenuLayout) {
    menuLayout.value = layout
    saveSettings('menuLayout', layout)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    saveSettings('sidebarCollapsed', sidebarCollapsed.value)
  }

  function setContentWidth(width: ContentWidth) {
    contentWidth.value = width
    saveSettings('contentWidth', width)
  }

  function setTabStyle(style: TabStyle) {
    tabStyle.value = style
    saveSettings('tabStyle', style)
  }

  function setBorderRadius(radius: number) {
    borderRadius.value = radius
    saveSettings('borderRadius', radius)
  }

  function setColorWeakMode(enabled: boolean) {
    colorWeakMode.value = enabled
    saveSettings('colorWeakMode', enabled)
    applyColorWeak()
  }

  function setAnimationLevel(level: AnimationLevel) {
    animationLevel.value = level
    saveSettings('animationLevel', level)
    applyAnimation()
  }

  function setFontFamily(family: FontFamily) {
    fontFamily.value = family
    saveSettings('fontFamily', family)
    applyFontFamily()
  }

  function setNavigationMode(mode: NavigationMode) {
    navigationMode.value = mode
    saveSettings('navigationMode', mode)
  }

  function setShowWatermark(show: boolean) {
    showWatermark.value = show
    saveSettings('showWatermark', show)
  }

  function setShowFooter(show: boolean) {
    showFooter.value = show
    saveSettings('showFooter', show)
  }

  function setCompactMode(compact: boolean) {
    compactMode.value = compact
    saveSettings('compactMode', compact)
    applyCompactMode()
  }

  function setTableDensity(density: TableDensity) {
    tableDensity.value = density
    saveSettings('tableDensity', density)
    applyTableDensity()
  }

  function setTableBorderStyle(style: TableBorderStyle) {
    tableBorderStyle.value = style
    saveSettings('tableBorderStyle', style)
    applyTableBorderStyle()
  }

  function setTableStriped(striped: boolean) {
    tableStriped.value = striped
    saveSettings('tableStriped', striped)
  }

  function setTableHover(hover: boolean) {
    tableHover.value = hover
    saveSettings('tableHover', hover)
    applyTableHover()
  }

  function setPageFontSize(size: PageScale) {
    pageFontSize.value = size
    saveSettings('pageFontSize', size)
    applyPageScale()
  }

  function setPageIconSize(size: PageScale) {
    pageIconSize.value = size
    saveSettings('pageIconSize', size)
  }

  let isInitializing = true

  function applyTheme() {
    const html = document.documentElement
    const mode = themeMode.value
    const isDark = mode === 'dark' || (mode === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches)

    if (isInitializing) {
      html.classList.add('no-transition')
      if (isDark) {
        html.classList.add('dark')
      } else {
        html.classList.remove('dark')
      }
      requestAnimationFrame(() => {
        html.classList.remove('no-transition')
      })
      isInitializing = false
      return
    }

    // 根据动画级别选择不同的过渡策略
    if (animationLevel.value === 'none') {
      // 无动画：瞬间切换
      if (isDark) html.classList.add('dark')
      else html.classList.remove('dark')
      return
    }

    if (animationLevel.value === 'reduced') {
      // 简化动画：抑制元素级过渡后用极短的交叉淡入淡出
      html.classList.add('theme-switching')
      if (isDark) html.classList.add('dark')
      else html.classList.remove('dark')
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          html.classList.remove('theme-switching')
        })
      })
      return
    }

    // anim-full: 使用 View Transitions API 页面级交叉淡入淡出
    html.classList.add('theme-switching')

    if (!document.startViewTransition) {
      if (isDark) html.classList.add('dark')
      else html.classList.remove('dark')
      setTimeout(() => {
        html.classList.remove('theme-switching')
      }, 100)
      return
    }

    const transition = document.startViewTransition(() => {
      if (isDark) {
        html.classList.add('dark')
      } else {
        html.classList.remove('dark')
      }
    })

    transition.finished.then(() => {
      html.classList.remove('theme-switching')
    })
  }

  function applyColorWeak() {
    const html = document.documentElement
    if (colorWeakMode.value) {
      html.classList.add('color-weak')
    } else {
      html.classList.remove('color-weak')
    }
  }

  function applyAnimation() {
    const html = document.documentElement
    html.classList.remove('anim-full', 'anim-reduced', 'anim-none')
    html.classList.add(`anim-${animationLevel.value}`)
  }

  function applyFontFamily() {
    const html = document.documentElement
    html.classList.remove('font-system', 'font-serif', 'font-mono')
    html.classList.add(`font-${fontFamily.value}`)
  }

  function applyCompactMode() {
    const html = document.documentElement
    if (compactMode.value) {
      html.classList.add('compact-mode')
    } else {
      html.classList.remove('compact-mode')
    }
  }

  function applyTableDensity() {
    const html = document.documentElement
    html.classList.remove('table-density-default', 'table-density-compact', 'table-density-comfortable')
    html.classList.add(`table-density-${tableDensity.value}`)
  }

  function applyTableBorderStyle() {
    const html = document.documentElement
    html.classList.remove('table-border-none', 'table-border-horizontal', 'table-border-full')
    html.classList.add(`table-border-${tableBorderStyle.value}`)
  }

  function applyTableHover() {
    const html = document.documentElement
    if (tableHover.value) {
      html.classList.remove('table-no-hover')
    } else {
      html.classList.add('table-no-hover')
    }
  }

  const fontScaleMap: Record<PageScale, number> = { small: 0.875, default: 1, large: 1.125 }
  const iconScaleMap: Record<PageScale, number> = { small: 0.875, default: 1, large: 1.25 }

  function applyPageScale() {
    const html = document.documentElement
    html.style.setProperty('--fts-page-font-scale', String(fontScaleMap[pageFontSize.value]))
    html.style.setProperty('--fts-page-icon-scale', String(iconScaleMap[pageIconSize.value]))
  }

  function init() {
    applyTheme()
    applyColorWeak()
    applyAnimation()
    applyFontFamily()
    applyCompactMode()
    applyTableDensity()
    applyTableBorderStyle()
    applyTableHover()
    applyPageScale()

    document.documentElement.style.setProperty('--fts-page-radius', `${borderRadius.value}px`)

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (themeMode.value === 'auto') {
        if (e.matches) {
          document.documentElement.classList.add('dark')
        } else {
          document.documentElement.classList.remove('dark')
        }
      }
    })
  }

  return {
    themeMode,
    menuLayout,
    sidebarCollapsed,
    contentWidth,
    tabStyle,
    borderRadius,
    colorWeakMode,
    animationLevel,
    fontFamily,
    navigationMode,
    showWatermark,
    showFooter,
    compactMode,
    tableDensity,
    tableBorderStyle,
    tableStriped,
    tableHover,
    pageFontSize,
    pageIconSize,
    sidebarWidth,
    sidebarWidthValue,
    setThemeMode,
    setMenuLayout,
    toggleSidebar,
    setContentWidth,
    setTabStyle,
    setBorderRadius,
    setColorWeakMode,
    setAnimationLevel,
    setFontFamily,
    setNavigationMode,
    setShowWatermark,
    setShowFooter,
    setCompactMode,
    setTableDensity,
    setTableBorderStyle,
    setTableStriped,
    setTableHover,
    setPageFontSize,
    setPageIconSize,
    init
  }
})