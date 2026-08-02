/**
 * 食品溯源系统 - 主题系统核心逻辑
 * ============================================
 * 版本: 2.1 (FTS桥接版)
 *
 * 【架构说明】
 * 本文件负责管理应用的主题切换逻辑，采用"双写策略"实现过渡期兼容：
 * - 旧变量体系: --primary, --bg, --text, --card, --border (用于现有组件)
 * - 新变量体系: --fts-* (项目统一标准，定义于 _tokens.scss)
 *
 * 【数据流】
 * themes配置 → applyTheme() → 双写CSS变量 → syncFtsVariables() → updateElementPlusTheme()
 *                                    ↓                          ↓
  *                              旧变量(--primary)
新变量(--fts-primary)
 *
 * 【权威数据源】
 * - CSS变量值以 theme-system-v2.css 为准（视觉呈现的最终来源）
 * - JS中的 themeVariables 必须与 CSS 保持同步
 *
 * 【迁移计划】
 * Phase 0 (当前): 双写策略，同时维护新旧两套变量
 * Phase 1: 新组件统一使用 --fts-* 变量
 * Phase 2: 移除旧变量，完成迁移
 *
 * 【硬编码色值说明】
 * 本文件中的颜色值为主题色值定义，属于合法硬编码（与 styles 目录的变量声明等同），
 * 不需要替换为 CSS 变量——它们本身就是 CSS 变量的来源。
 */

import { ref, computed } from 'vue'

// 主题配置类型
export interface ThemeConfig {
  key: string
  name: string
  type: 'light' | 'dark'
  description?: string
}

/**
 * FTS 变量映射表 - 旧变量到新变量的桥接
 * 用于在主题切换时同步设置 --fts-* 变量
 *
 * 映射规则：
 * - --primary (主色调) → --fts-primary (品牌主色)
 * - --bg (页面背景) → --fts-bg-white (白色背景/页面底色)
 * - --card (卡片背景) → --fts-bg-secondary (次级背景)
 * - --text (主文字) → --fts-text-primary (主要文字)
 * - --text-secondary (次要文字) → --fts-text-secondary (次要文字)
 * - --border (边框) → --fts-border-primary (主边框)
 */
const ftsVariableMap: Record<string, string> = {
  '--primary': '--fts-primary',
  '--bg': '--fts-bg-white',
  '--card': '--fts-bg-secondary',
  '--text': '--fts-text-primary',
  '--fts-text-secondary': '--fts-text-secondary',
  '--border': '--fts-border-primary',
}

// 侧边栏变量映射 - 根据主题类型设置不同的侧边栏颜色
const sidebarVariableMap: Record<string, Record<string, string>> = {
  light: {
    '--fts-sidebar-bg': '#1e3a5f',
    '--fts-sidebar-text': '#ffffff',
    '--fts-sidebar-item-text': 'rgba(255, 255, 255, 0.7)',
    '--fts-sidebar-hover-bg': 'rgba(255, 255, 255, 0.08)',
    '--fts-sidebar-active-bg': 'rgba(255, 255, 255, 0.12)',
    '--fts-sidebar-active-indicator': '#3b82f6',
    '--fts-sidebar-submenu-bg': 'rgba(0, 0, 0, 0.15)',
    '--fts-sidebar-subitem-text': 'rgba(255, 255, 255, 0.7)',
    '--fts-sidebar-child-bg': '#152a45',
    '--fts-sidebar-border': 'rgba(255, 255, 255, 0.1)',
  },
  dark: {
    '--fts-sidebar-bg': '#0f172a',
    '--fts-sidebar-text': '#f1f5f9',
    '--fts-sidebar-item-text': 'rgba(241, 245, 249, 0.7)',
    '--fts-sidebar-hover-bg': 'rgba(255, 255, 255, 0.05)',
    '--fts-sidebar-active-bg': 'rgba(255, 255, 255, 0.1)',
    '--fts-sidebar-active-indicator': '#60a5fa',
    '--fts-sidebar-submenu-bg': 'rgba(0, 0, 0, 0.2)',
    '--fts-sidebar-subitem-text': 'rgba(241, 245, 249, 0.6)',
    '--fts-sidebar-child-bg': '#1e293b',
    '--fts-sidebar-border': 'rgba(255, 255, 255, 0.05)',
  }
}

// 10套主题配置 - 精简版
const themes: ThemeConfig[] = [
  // 浅色主题
{ key: 'kitchen',
name: '烟火厨房',
type: 'light',
description: '传统厨房风格' },
{ key: 'tea',
name: '茶室雅座',
type: 'light',
description: '中式茶室风格' },
{ key: 'jade',
name: '翡翠琉璃',
type: 'light',
description: '清新翡翠风格' },
{ key: 'slate',
name: '青石灰韵',
type: 'light',
description: '现代青灰风格' },
{ key: 'lightOffice',
name: '浅色办公',
type: 'light',
description: '经典办公风格' },
  // 深色主题
{ key: 'amberDark',
name: '琥珀厨房',
type: 'dark',
description: '深色琥珀风格' },
{ key: 'emeraldDark',
name: '翡翠餐厅',
type: 'dark',
description: '深色翡翠风格' },
{ key: 'bakery',
name: '琥珀烘焙坊',
type: 'dark',
description: '烘焙店风格' },
{ key: 'business',
name: '商务行政',
type: 'dark',
description: '专业商务风格' },
]

// 当前主题
const currentTheme = ref<string>('kitchen')

// 浅色主题列表
const lightThemes = computed(() => themes.filter(t => t.type === 'light'))

// 深色主题列表
const darkThemes = computed(() => themes.filter(t => t.type === 'dark'))

// 获取主题渐变颜色
const getThemeGradient = (themeKey: string): string => {
  const gradients: Record<string, string> = {
    kitchen: 'linear-gradient(135deg, #8B4513, #A0522D)',
    tea: 'linear-gradient(135deg, #5a7c5a, #7a9c7a)',
    jade: 'linear-gradient(135deg, #059669, #10b981)',
    slate: 'linear-gradient(135deg, #475569, #64748b)',
    lightOffice: 'linear-gradient(135deg, #eff6ff, #2563eb)',
    amberDark: 'linear-gradient(135deg, #fbbf24, #f59e0b)',
    emeraldDark: 'linear-gradient(135deg, #10b981, #34d399)',
    bakery: 'linear-gradient(135deg, #f97316, #fb923c)',
    terracotta: 'linear-gradient(135deg, #c2410c, #ea580c)',
    darkOffice: 'linear-gradient(135deg, #1e293b, #334155)',
    business: 'linear-gradient(135deg, #2c3e50, #34495e)',
  }
  return gradients[themeKey] || gradients.kitchen
}

// 主题变量映射 - 直接使用颜色值
const themeVariables: Record<string, Record<string, string>> = {
  kitchen: {
    '--primary': '#8B4513',
    '--secondary': '#A0522D',
    '--accent': '#CD853F',
    '--bg': '#faf8f5',
    '--card': '#FFFFFF',
    '--text': '#3E2723',
    '--fts-text-secondary': '#6D4C41',
    '--border': '#D7CCC8',
    '--shadow': '0 2px 8px rgba(0, 0, 0, 0.1)',
    '--radius': '8px',
    '--font': '"Noto Serif SC", serif',
  },
  tea: {
    // 【修复说明】以下值已与 theme-system-v2.css 保持同步（2026-04-06）
    // 修复前: bg=#f5f5f0, border=#c0c0c0, text=#3d3d3d
    // 修复后: 以CSS文件为准，确保视觉一致性 ? '--primary': '#5a7c5a',
    '--secondary': '#7a9c7a',
        '--accent': '#c9b896',           // 修复: 原值 #4a6c4a → CSS值 #c9b896 (茶金色)
    '--bg': '#f8f6f0',               // 修复: 原值 #f5f5f0 → CSS值 #f8f6f0 (暖白底)
'--card': '#FFFFFF',
        '--text': '#4a4a4a',             // 修复: 原值 #3d3d3d → CSS值 #4a4a4a (柔和灰)
    '--fts-text-secondary': '#7a7a7a',   // 修复: 原值 #666666 → CSS值 #7a7a7a (茶灰)
    '--border': '#e8e4d8',           // 修复: 原值 #c0c0c0 → CSS值 #e8e4d8 (米黄边框)
'--shadow': '0 2px 8px rgba(0, 0, 0, 0.1)',
    '--radius': '8px',
    '--font': '"Noto Serif SC", serif',  // 修复: 使用衬线字体与CSS一致
  },
  jade: {
    '--primary': '#059669',
    '--secondary': '#10b981',
    '--accent': '#34d399',
    '--bg': '#f0fdf4',
    '--card': '#FFFFFF',
    '--text': '#064e3b',
    '--fts-text-secondary': '#6b7284',
    '--border': '#d1fae5',
    '--shadow': '0 2px 8px rgba(0, 0, 0, 0.1)',
    '--radius': '8px',
    '--font': '"Noto Sans SC", sans-serif',
  },
  slate: {
    '--primary': '#475569',
    '--secondary': '#64748b',
    '--accent': '#94a3b8',
    '--bg': '#f8fafc',
    '--card': '#FFFFFF',
    '--text': '#0f172a',
    '--fts-text-secondary': '#475569',
    '--border': '#e2e8f0',
    '--shadow': '0 2px 8px rgba(0, 0, 0, 0.1)',
    '--radius': '8px',
    '--font': '"Noto Sans SC", sans-serif',
  },
  lightOffice: {
    '--primary': '#2563eb',
    '--secondary': '#3b82f6',
    '--accent': '#60a5fa',
    '--bg': '#f8fafc',
    '--card': '#FFFFFF',
    '--text': '#1e293b',
    '--fts-text-secondary': '#64748b',
    '--border': '#e2e8f0',
    '--shadow': '0 2px 8px rgba(0, 0, 0, 0.1)',
    '--radius': '8px',
    '--font': '"Noto Sans SC", sans-serif',
  },
  amberDark: {
    '--primary': '#d97706',
    '--secondary': '#b45309',
    '--accent': '#92400e',
    '--bg': '#0c0a09',
    '--card': '#1c1917',
    '--text': '#fafaf9',
    '--fts-text-secondary': '#a8a29e',
    '--border': '#292524',
    '--shadow': '0 4px 8px rgba(217, 119, 6, 0.2)',
    '--radius': '8px',
    '--font': '"Noto Sans SC", sans-serif',
  },
  emeraldDark: {
    '--primary': '#059669',
    '--secondary': '#047857',
    '--accent': '#065f46',
    '--bg': '#020617',
    '--card': '#0f172a',
    '--text': '#f8fafc',
    '--fts-text-secondary': '#94a3b8',
    '--border': '#1e293b',
    '--shadow': '0 4px 8px rgba(5, 150, 105, 0.2)',
    '--radius': '8px',
    '--font': '"Noto Sans SC", sans-serif',
  },
  bakery: {
    '--primary': '#d97706',
    '--secondary': '#b45309',
    '--accent': '#92400e',
    '--bg': '#1c1917',
    '--card': '#292524',
    '--text': '#fafaf9',
    '--fts-text-secondary': '#a8a29e',
    '--border': '#44403c',
    '--shadow': '0 4px 8px rgba(217, 119, 6, 0.2)',
    '--radius': '8px',
    '--font': '"Noto Sans SC", sans-serif',
  },
  business: {
    '--primary': '#95a5a6',
    '--secondary': '#2c3e50',
    '--accent': '#34495e',
    '--bg': '#1a1f2e',
    '--card': 'rgba(35, 40, 55, 0.95)',
    '--text': '#ecf0f1',
    '--fts-text-secondary': '#bdc3c7',
    '--border': 'rgba(44, 62, 80, 0.3)',
    '--shadow': '0 2px 8px rgba(0,0,0,0.3)',
    '--radius': '6px',
    '--font': '"Noto Sans SC", sans-serif',
  },
}

// 应用主题
export const applyTheme = (themeKey: string) => {
  if (!themeKey) return
  currentTheme.value = themeKey
  const root = document.documentElement
  const variables = themeVariables[themeKey]
  if (variables) {
    // 【双写策略】设置旧变量（兼容现有组件）
    Object.entries(variables).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })

    // 【双写策略】同步设置 FTS 新变量（桥接到 _tokens.scss 体系）
    syncFtsVariables(variables, root)

    // 设置侧边栏变量（根据主题类型）
    const themeConfig = themes.find(t => t.key === themeKey)
    const sidebarVars = sidebarVariableMap[themeConfig?.type || 'light']
    if (sidebarVars) {
      Object.entries(sidebarVars).forEach(([key, value]) => {
        root.style.setProperty(key, value)
      })
    }
  }

  // 移除旧的主题类
  const body = document.body
  themes.forEach(t => {
    body.classList.remove(`theme-${t.key}`)
  })

  // 添加新的主题类
  body.classList.add(`theme-${themeKey}`)

  // 判断是否为深色主题，给 html 元素添加 dark 类和 data-theme 属性
  const themeConfig = themes.find(t => t.key === themeKey)
  const html = document.documentElement
  if (themeConfig?.type === 'dark') {
    html.classList.add('dark')
    html.setAttribute('data-theme', 'dark')
  } else {
    html.classList.remove('dark')
    html.removeAttribute('data-theme')
  }

  // 保存到 localStorage
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('app-theme', themeKey)
  }

  // 更新 Element Plus 主题色（完整同步）
  updateElementPlusTheme(variables, root)
}

/**
 * 同步 FTS 变量 - 双写策略的核心实现
 *
 * 功能说明：
 * 在设置旧变量的同时，同步设置对应的 --fts-* 变量，
 * 确保新旧两套变量体系保持一致。
 *
 * @param variables - 当前主题的旧变量对象
 * @param root - document.documentElement 引用
 */
const syncFtsVariables = (
  variables: Record<string, string>,
  root: HTMLElement
): void => {
  Object.entries(ftsVariableMap).forEach(([oldVar, newVar]) => {
    const value = variables[oldVar]
    if (value) {
      root.style.setProperty(newVar, value)
    }
  })

  // 额外同步：根据主题类型设置 FTS 语义化变量
  const themeConfig = themes.find(t =>
    Object.values(themeVariables).includes(variables)
  )
  if (themeConfig?.type === 'dark') {
    // 深色模式：反转文字颜色
    root.style.setProperty('--fts-text-white', variables['--text'] || '#ffffff')
  }
}

// 更新 Element Plus 主题 - 完整版
/**
 * 更新 Element Plus 组件库的主题变量
 *
 * 增强内容：
 * - 同步主色调（--el-color-primary）
 * - 同步背景色（--el-bg-color, --el-bg-color-overlay）
 * - 同步文字色（--el-text-color-primary）
 * - 同步边框色（--el-border-color）
 * - 同步填充色（--el-fill-color-blank）
 *
 * @param variables - 当前主题变量对象
 * @param root - document.documentElement 引用
 */
const updateElementPlusTheme = (
  variables: Record<string, string>,
  root: HTMLElement
): void => {
  // 主色调
  if (variables['--primary']) {
    root.style.setProperty('--el-color-primary', variables['--primary'])
  }

  // 背景色
  if (variables['--card']) {
    root.style.setProperty('--el-bg-color', variables['--card'])
  }
  if (variables['--bg']) {
    root.style.setProperty('--el-bg-color-overlay', variables['--bg'])
    root.style.setProperty('--el-bg-color-page', variables['--bg'])
  }

  // 文字色
  if (variables['--text']) {
    root.style.setProperty('--el-text-color-primary', variables['--text'])
  }
  if (variables['--fts-text-secondary']) {
    root.style.setProperty('--el-text-color-regular', variables['--fts-text-secondary'])
  }

  // 边框色
  if (variables['--border']) {
    root.style.setProperty('--el-border-color', variables['--border'])
  }

  // 填充色
  if (variables['--card']) {
    root.style.setProperty('--el-fill-color-blank', variables['--card'])
  }
}

// 初始化主题
export const initTheme = () => {
  // 从 localStorage 读取保存的主题
  if (typeof localStorage !== 'undefined') {
    const savedTheme = localStorage.getItem('app-theme')
    if (savedTheme && themes.some(t => t.key === savedTheme)) {
      applyTheme(savedTheme)
      return
    }
  }

  // 默认使用烟火厨房主题
  applyTheme('kitchen')
}

// 获取当前主题配置
const getCurrentThemeConfig = (): ThemeConfig | undefined => {
  return themes.find(t => t.key === currentTheme.value)
}

// 判断当前是否为深色主题
const isDarkTheme = computed(() => {
  const theme = getCurrentThemeConfig()
  return theme?.type === 'dark'
})

// 导出 composable
export function useThemes() {
  return {
    currentTheme,
    lightThemes,
    darkThemes,
    themes,
    isDarkTheme,
    applyTheme,
    initTheme,
    getThemeGradient,
    getCurrentThemeConfig,
  }
}

export default useThemes
