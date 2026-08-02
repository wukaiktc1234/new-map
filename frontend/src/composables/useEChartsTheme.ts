/**
 * ECharts 暗色模式主题适配
 *
 * 统一处理所有 ECharts 图表在深色/浅色主题下的配色方案，
 * 确保图例、坐标轴、提示框等文字在任何主题下都清晰可读。
 *
 * 使用方式：
 * ```ts
 * import { useEChartsTheme } from '@/composables/useEChartsTheme'
 * const { chartColors, isDark } = useEChartsTheme()
 *
 * // 在 setOption 中使用
 * chartInstance.setOption({
 *   ...chartColors,
 *   xAxis: {
 *     axisLabel: {
 *       color: chartColors.textColor
 *     }
 *   },
 *   ...
 * })
 * ```
 *
 * 【硬编码色值说明】
 * 本文件中的颜色值为 ECharts 主题配置定义，属于合法硬编码（与 styles 目录的变量声明等同），
 * ECharts 配置项不支持 CSS 变量，必须使用具体颜色值。
 */

import { computed } from 'vue'
import { useThemes } from './useThemes'

export function useEChartsTheme() {
  const { isDarkTheme } = useThemes()
  const isDark = computed(() => isDarkTheme.value)

  /** 主文字颜色 */
  const textColor = computed(() =>
    isDark.value ? '#e2e8f0' : '#1f2937'
  )

  /** 次要文字颜色（坐标轴标签、图例等） */
  const textSecondaryColor = computed(() =>
    isDark.value ? '#94a3b8' : '#6b7280'
  )

  /** 辅助文字颜色（更淡的说明性文字） */
  const textMutedColor = computed(() =>
    isDark.value ? '#64748b' : '#9ca3af'
  )

  /** 边框/分割线颜色 */
  const borderColor = computed(() =>
    isDark.value ? '#334155' : '#e5e7eb'
  )

  /** 背景色 */
  const bgColor = computed(() =>
    isDark.value ? '#1e2937' : '#ffffff'
  )

  /** tooltip 背景颜色 */
  const tooltipBg = computed(() =>
    isDark.value ? 'rgba(30,41,59,0.95)' : 'rgba(255,255,255,0.95)'
  )

  /** tooltip 文字颜色 */
  const tooltipTextColor = computed(() =>
    isDark.value ? '#f1f5f9' : '#1f2937'
  )

  /** tooltip 边框颜色 */
  const tooltipBorderColor = computed(() =>
    isDark.value ? '#475569' : '#e5e7eb'
  )

  /** 分割线样式 */
  const splitLineStyle = computed(() => ({
    lineStyle: {
      color: borderColor.value,
      type: 'dashed' as const,
      opacity: isDark.value ? 0.3 : 1
    }
  }))

  /** 坐标轴线样式 */
  const axisLineStyle = computed(() => ({
    lineStyle: {
      color: borderColor.value
    }
  }))

  /** 坐标轴标签样式 */
  const axisLabelStyle = computed(() => ({
    color: textSecondaryColor.value
  }))

  /** 图例文字样式 */
  const legendTextStyle = computed(() => ({
    color: textSecondaryColor.value
  }))

  /** 饼图标签样式 */
  const pieLabelStyle = computed(() => ({
    color: textColor.value
  }))

  /** 完整的 tooltip 配置 */
  const tooltipOption = computed(() => ({
    backgroundColor: tooltipBg.value,
    borderColor: tooltipBorderColor.value,
    textStyle: {
      color: tooltipTextColor.value
    },
    extraCssText: 'border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);'
  }))

  /** 完整的 legend 配置（基础版） */
  const legendOption = computed(() => ({
    textStyle: legendTextStyle.value,
    icon: 'circle',
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 16
  }))

  /** 完整的 xAxis 基础配置 */
  const xAxisBaseOption = computed(() => ({
    axisLine: axisLineStyle.value,
    axisLabel: axisLabelStyle.value,
    splitLine: splitLineStyle.value
  }))

  /** 完整的 yAxis 基础配置 */
  const yAxisBaseOption = computed(() => ({
    axisLine: axisLineStyle.value,
    axisLabel: axisLabelStyle.value,
    splitLine: splitLineStyle.value
  }))

  /**
   * 将暗色模式配置合并到 ECharts option 中
   * @param option 原始 ECharts option
   * @returns 合并后的 option（不修改原对象）
   */
  function withTheme<T extends Record<string, unknown>>(option: T) {
    return {
      ...option,
      backgroundColor: bgColor.value,
      tooltip: {
        ...tooltipOption.value,
        ...(option.tooltip as Record<string, unknown> | undefined)
      },
      legend: {
        ...legendOption.value,
        ...(option.legend as Record<string, unknown> | undefined)
      }
    }
  }

  /** 所有图表颜色的聚合对象，方便解构使用 */
  const chartColors = computed(() => ({
    backgroundColor: bgColor.value,
    textColor: textColor.value,
    textSecondaryColor: textSecondaryColor.value,
    textMutedColor: textMutedColor.value,
    borderColor: borderColor.value,
    tooltipBg: tooltipBg.value,
    tooltipTextColor: tooltipTextColor.value,
    tooltipBorderColor: tooltipBorderColor.value
  }))

  return {
    isDark,
    textColor,
    textSecondaryColor,
    textMutedColor,
    borderColor,
    bgColor,
    tooltipBg,
    tooltipTextColor,
    tooltipBorderColor,
    splitLineStyle,
    axisLineStyle,
    axisLabelStyle,
    legendTextStyle,
    pieLabelStyle,
    tooltipOption,
    legendOption,
    xAxisBaseOption,
    yAxisBaseOption,
    withTheme,
    chartColors
  }
}
