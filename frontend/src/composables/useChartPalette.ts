/**
 * ECharts 统一配置构建器（与项目 --fts-* 变量深度整合）
 *
 * 设计目标：
 * 1. 所有页面从这里取 baseOption/chartColors，不再散点 getCssVar/getChartTextColor。
 * 2. 内置性能友好选项：animation 关闭增量更新、tooltip 延迟为 0、dataZoom 懒渲染。
 * 3. 辅助方法：toYuan / toWan / withGradient / withBarRadius，减少页面重复代码。
 */

/** 颜色插值器（根据 CSS 变量生成完整调色板，含深浅与透明度） */
export interface ChartPalette {
  /** 主色系（6色循环，与 --fts-chart-color-1..6 对齐） */
  primary: string[]
  /** 成功/警告/错误/信息/主色 语义色 */
  semantic: {
    primary: string
    success: string
    warning: string
    error: string
    info: string
  }
  /** 文本色（与 --fts-text-primary 对齐） */
  textColor: string
  /** 次级文本色 */
  textSecondary: string
  /** 分隔/轴颜色 */
  splitColor: string
  /** 卡片背景 */
  cardBg: string
}

function readVar(name: string, fallback: string): string {
  if (typeof document === 'undefined') return fallback
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

/** 读取 --fts-chart-color-1..6 的完整调色板 */
export function getChartPalette(): ChartPalette {
  const primary = [
    readVar('--fts-chart-color-1', '#5B8FF9'),
    readVar('--fts-chart-color-2', '#61DDAA'),
    readVar('--fts-chart-color-3', '#65789B'),
    readVar('--fts-chart-color-4', '#F6BD16'),
    readVar('--fts-chart-color-5', '#7262FD'),
    readVar('--fts-chart-color-6', '#78D3F8'),
  ]
  return {
    primary,
    semantic: {
      primary: readVar('--fts-primary', '#5B8FF9'),
      success: readVar('--fts-success', '#61DDAA'),
      warning: readVar('--fts-warning', '#F6BD16'),
      error: readVar('--fts-error', '#F56C6C'),
      info: readVar('--fts-info', '#65789B'),
    },
    textColor: readVar('--fts-text-primary', '#1F2937'),
    textSecondary: readVar('--fts-text-secondary', '#6B7280'),
    splitColor: readVar('--fts-border-secondary', '#E5E7EB'),
    cardBg: readVar('--fts-bg-card', '#FFFFFF'),
  }
}

/** 生成线性渐变（用于折线填充/柱状渐变） */
export function linearGradient(color: string, fromOpacity = 0.35, toOpacity = 0.02): {
  type: 'linear'
  x: number
  y: number
  x2: number
  y2: number
  colorStops: Array<{ offset: number; color: string }>
} {
  function hexWithOpacity(hex: string, opacity: number): string {
    const clean = hex.replace('#', '')
    if (clean.length !== 6) return hex
    const r = parseInt(clean.slice(0, 2), 16)
    const g = parseInt(clean.slice(2, 4), 16)
    const b = parseInt(clean.slice(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${opacity})`
  }
  return {
    type: 'linear',
    x: 0, y: 0, x2: 0, y2: 1,
    colorStops: [
      { offset: 0, color: hexWithOpacity(color, fromOpacity) },
      { offset: 1, color: hexWithOpacity(color, toOpacity) },
    ],
  }
}

/** 分 → 元（保留 2 位） */
export function fenToYuan(v: number | null | undefined): number {
  if (v == null) return 0
  return Math.round((v / 100) * 100) / 100
}

/** 数字压缩显示：< 1w 原数 / >= 1w x.xx万 */
export function formatWan(v: number, unit = ''): string {
  if (!Number.isFinite(v)) return `0${unit}`
  const abs = Math.abs(v)
  if (abs >= 100000000) return `${(v / 100000000).toFixed(2)}亿${unit}`
  if (abs >= 10000) return `${(v / 10000).toFixed(2)}万${unit}`
  return `${Math.round(v * 100) / 100}${unit}`
}

/** 为柱状数组设置圆角（只对顶部两个角） */
export function withBarRadius<T extends { itemStyle?: { borderRadius?: [number, number, number, number] } }>(
  data: T[],
  radius = [6, 6, 0, 0] as [number, number, number, number]
): T[] {
  return data.map(d => ({
    ...d,
    itemStyle: {
      ...(d.itemStyle ?? {}),
      borderRadius: radius,
    },
  }))
}

/** 构建通用图表 baseOption（含 tooltip/grid/axis/legend 性能友好默认值） */
export function buildBaseOption(palette = getChartPalette()): Record<string, unknown> {
  const textColor = palette.textColor
  const split = palette.splitColor
  return {
    color: palette.primary,
    textStyle: {
      color: textColor,
      fontSize: 12,
    },
    // 性能友好：关闭动画过渡抖动，增量更新交给 dataZoom/lazyUpdate
    animation: false,
    animationThreshold: 2000,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: palette.semantic.primary, opacity: 0.2 } },
      transitionDuration: 0,
      showDelay: 0,
      hideDelay: 100,
      confine: true,
      extraCssText: `
        background: ${palette.cardBg};
        border: 1px solid ${split};
        box-shadow: 0 6px 24px rgba(15, 23, 42, 0.08);
        border-radius: 8px;
      `,
    },
    legend: {
      type: 'scroll',
      textStyle: { color: palette.textSecondary, fontSize: 12 },
      itemWidth: 10,
      itemHeight: 10,
      pageIconColor: palette.textSecondary,
      pageTextStyle: { color: palette.textSecondary },
    },
    grid: {
      left: 48,
      right: 24,
      top: 48,
      bottom: 40,
    },
    xAxis: {
      type: 'category' as const,
      axisLine: { lineStyle: { color: split } },
      axisTick: { show: false },
      axisLabel: { color: palette.textSecondary, fontSize: 12 },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value' as const,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: palette.textSecondary, fontSize: 12 },
      splitLine: { lineStyle: { color: split, type: 'dashed' as const } },
    },
  }
}
