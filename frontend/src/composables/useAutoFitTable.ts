/**
 * 智能表格自适应列宽 Composable（精确版）
 *
 * 核心功能：
 * 1. 自动测量每列最宽内容（表头 + 数据）
 * 2. 使用 Canvas API 精确计算文本宽度
 * 3. 动态获取 Element Plus 单元格的真实 padding/border
 * 4. 加入安全余量对抗测量误差
 * 5. 数据变化时自动重新计算
 *
 * 使用方式：
 * ```vue
 * <script setup>
 * const { tableRef, autoFitWidth } = useAutoFitTable(
 *   [
 *     { prop: 'name', label: '名称', minWidth: 120 },
 *     { prop: 'status', label: '状态', minWidth: 80, extra: 40 } // 标签额外宽度
 *   ],
 *   computed(() => tableData.value),
 *   { safetyMargin: 16 }
 * )
 *
 * onMounted(() => {
 *   autoFitWidth()
 * })
 * </script>
 * ```
 */

import { ref, watch, nextTick, type Ref } from 'vue'

export interface AutoFitColumn {
  prop: string
  label: string
  minWidth?: number       // 最小宽度（px），默认 80
  maxWidth?: number       // 最大宽度限制（可选）
  extra?: number          // 额外宽度（如标签、图标、按钮等），默认 0
}

export interface AutoFitOptions {
  fontSize?: number       // 字体大小，默认 14
  fontFamily?: string     // 字体族
  debounceMs?: number     // 防抖延迟（毫秒），默认 100
  safetyMargin?: number   // 安全余量（px），默认 16，用于对抗 Canvas 测量误差
}

export interface UseAutoFitTableReturn {
  tableRef: Ref<any>
  autoFitWidth: () => Promise<void>
}

export function useAutoFitTable(
  columns: AutoFitColumn[],
  data: Ref<any[]>,
  options: AutoFitOptions = {}
): UseAutoFitTableReturn {
  const tableRef = ref<any>(null)

  const {
    fontSize = 14,
    fontFamily = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Microsoft YaHei", sans-serif',
    debounceMs = 100,
    safetyMargin = 16  // 安全余量：12-16px 可有效对抗测量误差
  } = options

  let canvas: HTMLCanvasElement | null = null
  let ctx: CanvasRenderingContext2D | null = null
  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  let cachedCellPadding: number | null = null // 缓存实际单元格内边距

  /**
   * 初始化 Canvas 测量环境（只创建一次）
   */
  const initCanvas = () => {
    if (!canvas) {
      canvas = document.createElement('canvas')
      ctx = canvas.getContext('2d')
      if (ctx) {
        ctx.font = `${fontSize}px ${fontFamily}`
      }
    }
  }

  /**
   * 获取单元格的实际内边距（padding + border）
   * 使用 getComputedStyle 从真实 DOM 获取精确值
   */
  const getActualCellPadding = (): number => {
    // 如果已缓存，直接返回
    if (cachedCellPadding !== null) {
      return cachedCellPadding
    }

    // 尝试从表格 DOM 获取实际样式
    if (tableRef.value?.$el) {
      const td = tableRef.value.$el.querySelector('.el-table__body td') as HTMLElement
      if (td) {
        const style = getComputedStyle(td)
        const paddingLeft = parseFloat(style.paddingLeft) || 0
        const paddingRight = parseFloat(style.paddingRight) || 0
        const borderLeft = parseFloat(style.borderLeftWidth) || 0
        const borderRight = parseFloat(style.borderRightWidth) || 0
        
        cachedCellPadding = paddingLeft + paddingRight + borderLeft + borderRight
        return cachedCellPadding
      }
    }

    // 降级：使用经验值（Element Plus 默认 padding=14*2 + border=1*2 = 30）
    cachedCellPadding = 30
    return cachedCellPadding
  }

  /**
   * 测量文本宽度
   */
  const measureText = (text: string): number => {
    initCanvas()
    if (!ctx) return text.length * fontSize * 0.7 // 降级估算
    return ctx.measureText(text).width
  }

  /**
   * 获取单元格的实际显示内容
   * 处理特殊格式（金额前缀、状态映射等）
   */
  const getCellDisplayText = (
    row: Record<string, any>,
    column: AutoFitColumn
  ): string => {
    const value = row[column.prop]

    if (value === null || value === undefined || value === '') {
      return ''
    }

    return String(value)
  }

  /**
   * 计算单列的最优宽度（核心算法 - 精确版）
   * 
   * 公式：width = max(表头宽度, 最宽数据宽度) + 实际padding + 安全余量 + 额外宽度
   */
  const calculateColumnWidth = (column: AutoFitColumn): number => {
    // 1. 测量表头文字宽度
    let maxWidth = measureText(column.label)

    // 2. 遍历所有数据行，找到最宽的内容
    for (const row of data.value) {
      const cellText = getCellDisplayText(row, column)
      const textWidth = measureText(cellText)
      if (textWidth > maxWidth) {
        maxWidth = textWidth
      }
    }

    // 3. 获取实际的单元格内边距（动态获取或使用缓存值）
    const actualPadding = getActualCellPadding()

    // 4. 计算总宽度 = 最宽文本 + 实际padding + 安全余量 + 额外宽度
    const totalWidth = maxWidth + actualPadding + safetyMargin + (column.extra || 0)

    // 5. 应用最小/最大宽度限制
    const finalWidth = Math.max(totalWidth, column.minWidth ?? 80)

    if (column.maxWidth) {
      return Math.min(finalWidth, column.maxWidth)
    }

    return Math.ceil(finalWidth) // 向上取整确保不截断
  }

  /**
   * 执行自适应宽度计算（根治版 - DeepSeek 方案）
   * 
   * 核心原理：
   * 1. 通过 Canvas 测量每列最宽内容
   * 2. 直接设置 <colgroup> 中 <col> 的 width（绕过 EP 再分配）
   * 3. 强制设置 <td> 和 <div class="cell"> 的样式（防止内部容器限制）
   * 4. 设置表格总宽度为 auto/max-content（不被压缩到 100%）
   */
  const autoFitWidth = async (): Promise<void> => {
    if (!tableRef.value) return

    // 防抖处理
    if (debounceMs > 0) {
      if (debounceTimer) clearTimeout(debounceTimer)
      await new Promise<void>((resolve) => {
        debounceTimer = setTimeout(resolve, debounceMs)
      })
    }

    await nextTick()

    const tableEl = tableRef.value.$el
    if (!tableEl) return

    // 计算每列宽度
    const columnWidths = columns.map(col => calculateColumnWidth(col))

    // 计算总宽度
    const totalWidth = columnWidths.reduce((sum, w) => sum + w, 0)

    // ========== 第一步：设置 colgroup 列宽 ==========
    await nextTick()
    const colgroup = tableEl.querySelector('colgroup')
    if (colgroup) {
      const cols = colgroup.querySelectorAll('col')

      columns.forEach((column, index) => {
        if (cols[index]) {
          // 关键：使用 !important 确保覆盖内联样式
          cols[index].style.cssText = `width: ${columnWidths[index]}px !important; min-width: ${columnWidths[index]}px !important;`
        }
      })
    }

    // ========== 第二步：强制设置 table 宽度 ==========
    const headerTable = tableEl.querySelector('.el-table__header table') as HTMLElement
    const bodyTable = tableEl.querySelector('.el-table__body table') as HTMLElement

    if (headerTable && bodyTable) {
      // 不使用固定像素值，让浏览器根据 colgroup 自动分配
      headerTable.style.cssText = 'width: max-content !important; min-width: 100% !important; table-layout: fixed !important;'
      bodyTable.style.cssText = 'width: max-content !important; min-width: 100% !important; table-layout: fixed !important;'
    }

    // ========== 第三步：强制内部容器不截断 ==========
    await nextTick()

    // 不再设置 overflow: visible（会导致重叠）
    // 改为依靠足够大的列宽来避免截断

    // ========== 第四步：触发布局更新 ==========
    tableRef.value?.doLayout?.()

    // ========== 第五步：doLayout 后再次强制设置（防止被重置）==========
    await nextTick()
    
    // 重新应用 colgroup 宽度（doLayout 可能会重置）
    const colgroupAfter = tableEl.querySelector('colgroup')
    if (colgroupAfter) {
      const colsAfter = colgroupAfter.querySelectorAll('col')
      columns.forEach((column, index) => {
        if (colsAfter[index]) {
          colsAfter[index].style.cssText = `width: ${columnWidths[index]}px !important; min-width: ${columnWidths[index]}px !important;`
        }
      })
    }

    // 再次确保 table 宽度
    const headerTable2 = tableEl.querySelector('.el-table__header table') as HTMLElement
    const bodyTable2 = tableEl.querySelector('.el-table__body table') as HTMLElement

    if (headerTable2 && bodyTable2) {
      // 使用固定像素值而非 max-content（更可靠）
      const finalTotalWidth = columnWidths.reduce((sum, w) => sum + w, 0)
      headerTable2.style.cssText = `width: ${finalTotalWidth}px !important; min-width: ${finalTotalWidth}px !important; table-layout: fixed !important;`
      bodyTable2.style.cssText = `width: ${finalTotalWidth}px !important; min-width: ${finalTotalWidth}px !important; table-layout: fixed !important;`
    }

    // 最后一次触发布局
    tableRef.value?.doLayout?.()
  }

  /**
   * 清理 Canvas 资源
   */
  const cleanupCanvas = () => {
    if (canvas) {
      canvas.remove()
      canvas = null
      ctx = null
    }
  }

  // 监听数据变化，自动重新计算
  watch(
    () => data.value,
    () => {
      autoFitWidth()
    },
    { deep: true, flush: 'post' }
  )

  return {
    tableRef,
    autoFitWidth
  }
}
