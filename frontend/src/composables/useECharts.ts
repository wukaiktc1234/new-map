/**
 * ECharts 按需加载 Composable（稳定版）
 *
 * 功能：
 * 1. 动态导入 ECharts（仅在需要时加载）
 * 2. 自动管理图表实例生命周期
 * 3. 响应式自适应（window resize 监听）
 * 4. 内存泄漏防护
 *
 * 使用方式：
 * const { init, setOption, resize } = useECharts(containerRef)
 */

import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import type { Ref } from 'vue'

// 使用 any 类型避免 ECharts 类型导入问题
type EChartsInstance = any
type EChartsOption = Record<string, any>

/** 缓存的 echarts 模块 */
let cachedEcharts: any = null

/** 等待 echarts 加载完成的 Promise 池（避免 setInterval 轮询） */
let loadingPromise: Promise<any> | null = null

/**
 * 安全地加载 ECharts 模块（带 Promise 池去重）
 */
async function loadEcharts(): Promise<any> {
  if (cachedEcharts) return cachedEcharts
  if (loadingPromise) return loadingPromise

  loadingPromise = (async () => {
    try {
      const [
        echartsCore,
        chartsModule,
        componentsModule,
        renderersModule,
      ] = await Promise.all([
        import('echarts/core'),
        import('echarts/charts'),
        import('echarts/components'),
        import('echarts/renderers'),
      ])

      // echarts 子模块使用命名导出（无 default 导出），直接使用模块命名空间
      const echarts = echartsCore

      const componentsToRegister = [
        chartsModule?.PieChart,
        chartsModule?.BarChart,
        chartsModule?.LineChart,
        chartsModule?.ScatterChart,
        chartsModule?.RadarChart,
        componentsModule?.TitleComponent,
        componentsModule?.TooltipComponent,
        componentsModule?.LegendComponent,
        componentsModule?.GridComponent,
        componentsModule?.MarkPointComponent,
        componentsModule?.MarkLineComponent,
        componentsModule?.DataZoomComponent,
        renderersModule?.CanvasRenderer,
      ].filter((c): c is NonNullable<typeof c> => c != null)

      if (componentsToRegister.length > 0) {
        echarts.use(componentsToRegister)
      }

      cachedEcharts = echarts
      return echarts
    } catch (error) {
      console.error('[useECharts] 加载模块失败:', error)
      loadingPromise = null // 失败时清空，允许重试
      throw error
    }
  })()

  return loadingPromise
}

interface UseEChartsOptions {
  autoInit?: boolean
  theme?: string | object
}

export function useECharts(
  containerRef: Ref<HTMLElement | undefined>,
  options: UseEChartsOptions = {}
) {
  const { autoInit = true, theme } = options

  const chartInstance = ref<EChartsInstance | null>(null)
  const isLoading = ref(false)
  let isDisposed = false
  /** 实例尚未就绪时缓存的最后一次配置 */
  let pendingOption: EChartsOption | null = null

  async function init(): Promise<void> {
    if (isDisposed) return

    // 已存在实例时先清理（重置场景）
    if (chartInstance.value) {
      try { chartInstance.value.dispose() } catch (e) { /* ignore */ }
      chartInstance.value = null
    }

    if (!containerRef.value) return

    try {
      isLoading.value = true
      const echarts = await loadEcharts()

      // 异步过程中可能已卸载
      if (isDisposed || !containerRef.value) return

      chartInstance.value = echarts.init(containerRef.value, theme, {
        renderer: 'canvas',
      })

      // 应用缓存的配置（解决异步初始化与同步 setOption 的竞争）
      if (pendingOption) {
        setOption(pendingOption, true)
        pendingOption = null
      }
    } catch (error) {
      console.error('[useECharts] 初始化失败:', error)
    } finally {
      isLoading.value = false
    }
  }

  function setOption(option: EChartsOption, notMerge = false): void {
    if (isDisposed) return
    if (!chartInstance.value) {
      pendingOption = option
      return
    }
    try {
      chartInstance.value.setOption(option, notMerge)
    } catch (error) {
      console.error('[useECharts] 设置配置失败:', error)
    }
  }

  function resize(): void {
    if (!chartInstance.value || isDisposed) return
    try {
      chartInstance.value.resize()
    } catch (error) {
      /* ignore */
    }
  }

  function showLoading(text?: string): void {
    if (!chartInstance.value || isDisposed) return
    chartInstance.value.showLoading('default', { text: text || '数据加载中...' })
    isLoading.value = true
  }

  function hideLoading(): void {
    if (!chartInstance.value || isDisposed) return
    chartInstance.value.hideLoading()
    isLoading.value = false
  }

  function dispose(): void {
    isDisposed = true
    if (chartInstance.value) {
      try { chartInstance.value.dispose() } catch (e) { /* ignore */ }
      chartInstance.value = null
    }
  }

  // 自动初始化（onMounted 时一次性触发，配合 nextTick 确保 DOM 就绪）
  onMounted(() => {
    if (autoInit) nextTick(() => init())
  })

  // 监听容器引用：当图表容器受 v-if 控制在挂载后才渲染时，自动补初始化
  watch(
    containerRef,
    (el) => {
      if (el && !chartInstance.value && !isDisposed && autoInit) {
        init()
      }
    },
    { flush: 'post' }
  )

  // 卸载时清理
  onUnmounted(() => {
    dispose()
    // 移除 resize 监听
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', handleResize)
    }
  })

  // 响应式调整（防抖 100ms）
  let resizeTimer: ReturnType<typeof setTimeout> | null = null
  function handleResize(): void {
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => resize(), 100)
  }

  // 挂载 window resize 监听器（修复之前定义但未挂载的 bug）
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', handleResize, { passive: true })
  }

  return {
    chartInstance,
    isLoading,
    init,
    setOption,
    resize,
    showLoading,
    hideLoading,
    dispose,
  }
}
