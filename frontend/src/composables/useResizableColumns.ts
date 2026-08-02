import { ref, watch, onMounted, type Ref } from 'vue'
import type { TableInstance } from 'element-plus'

/**
 * 列宽配置接口
 */
export interface ColumnWidthConfig {
  /** 列的prop属性 */
  prop: string
  /** 列宽度（px） */
  width: number
}

/**
 * useResizableColumns 配置选项
 */
interface UseResizableColumnsOptions {
  /** 页面唯一标识（用于localStorage key） */
  storageKey: string
  /** 默认列宽配置（首次使用或重置时的默认值） */
  defaultColumns: ColumnWidthConfig[]
  /** 最小列宽限制（px） */
  minWidth?: number
  /** 最大列宽限制（px） */
  maxWidth?: number
  /** 是否启用自动保存（默认true） */
  autoSave?: boolean
  /** 防抖延迟时间（ms），避免频繁保存 */
  debounceDelay?: number
}

/**
 * useResizableColumns 返回值
 */
interface UseResizableColumnsReturn {
  /** 当前列宽配置 */
  columnWidths: Ref<ColumnWidthConfig[]>
  /** 是否已加载本地配置 */
  isLoaded: Ref<boolean>
  /** Element Plus表格ref绑定 */
  tableRef: Ref<TableInstance | null>
  /**
   * 列宽变化回调 - 绑定到el-table的@header-dragend事件
   * @param newWidth - 新宽度
   * @param oldWidth - 旧宽度
   * @param column - 列对象
   * @param event - 原生事件
   */
  onHeaderDragEnd: (newWidth: number, oldWidth: number, column: { property: string }, event: MouseEvent) => void
  /** 重置为默认列宽 */
  resetToDefault: () => void
  /** 手动保存当前配置 */
  save: () => void
  /** 获取某个列的当前宽度 */
  getColumnWidth: (prop: string) => number | undefined
}

/**
 * 从localStorage获取用户ID（用于区分不同账号）
 */
function getCurrentUserId(): string {
  try {
    const token = localStorage.getItem('token')
    if (!token) return 'anonymous'

    const payload = JSON.parse(atob(token.split('.')[1]))
    return String(payload.userId || payload.sub || 'anonymous')
  } catch {
    return 'anonymous'
  }
}

/**
 * 生成完整的localStorage key
 * @param pageKey - 页面标识
 * @param userId - 用户ID
 */
function generateStorageKey(pageKey: string, userId: string): string {
  return `fts_table_columns_${pageKey}_${userId}`
}

/**
 * 从localStorage加载列宽配置
 * @param storageKey - 完整的存储key
 */
function loadFromStorage(storageKey: string): ColumnWidthConfig[] | null {
  try {
    const data = localStorage.getItem(storageKey)
    if (!data) return null

    const parsed = JSON.parse(data)
    
    // 验证数据格式
    if (!Array.isArray(parsed)) {
      console.warn(`[useResizableColumns] 存储的数据格式无效: ${storageKey}`)
      return null
    }

    return parsed as ColumnWidthConfig[]
  } catch (error) {
    console.warn(`[useResizableColumns] 加载列宽配置失败:`, error)
    return null
  }
}

/**
 * 保存列宽配置到localStorage
 * @param storageKey - 完整的存储key
 * @param columns - 列宽配置
 */
function saveToStorage(storageKey: string, columns: ColumnWidthConfig[]): void {
  try {
    localStorage.setItem(storageKey, JSON.stringify(columns))
  } catch (error) {
    console.error('[useResizableColumns] 保存列宽配置失败:', error)
  }
}

/**
 * 可拖拽调整列宽 + 本地持久化 Composable
 *
 * 功能特性：
 * 1. 支持Element Plus表格的列宽拖拽调整
 * 2. 自动保存用户的列宽偏好设置到localStorage
 * 3. 按用户ID + 页面标识隔离存储（不同用户/页面互不影响）
 * 4. 支持防抖保存，避免频繁写入
 * 5. 提供重置默认值功能
 *
 * 使用示例：
 * ```typescript
 * const { columnWidths, onHeaderDragEnd, resetToDefault } = useResizableColumns({
 *   storageKey: 'order-refund',
 *   defaultColumns: [
 *     { prop: 'createTime', width: 180 },
 *     { prop: 'refundNo', width: 160 },
 *     { prop: 'orderNo', width: 180 },
 *   ]
 * })
 *
 * // 在模板中使用
 * <el-table ref="tableRef" border @header-dragend="onHeaderDragEnd">
 *   <el-table-column :width="getColumnWidth('createTime')" ... />
 * </el-table>
 * ```
 *
 * @param options - 配置选项
 * @returns 可拖拽列宽相关的响应式状态和方法
 */
export function useResizableColumns(options: UseResizableColumnsOptions): UseResizableColumnsReturn {
  const {
    storageKey: pageKey,
    defaultColumns,
    minWidth = 80,
    maxWidth = 800,
    autoSave = true,
    debounceDelay = 500,
  } = options

  const userId = getCurrentUserId()
  const fullStorageKey = generateStorageKey(pageKey, userId)

  const columnWidths = ref<ColumnWidthConfig[]>([])
  const isLoaded = ref(false)
  const tableRef = ref<TableInstance | null>(null)

  let saveTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 加载列宽配置（优先从localStorage，否则使用默认值）
   */
  const loadConfig = (): void => {
    const savedConfig = loadFromStorage(fullStorageKey)

    if (savedConfig && savedConfig.length > 0) {
      // 合并保存的配置与默认配置（防止新增列丢失）
      const mergedConfig = defaultColumns.map(defaultCol => {
        const savedCol = savedConfig.find(s => s.prop === defaultCol.prop)
        return {
          prop: defaultCol.prop,
          width: savedCol?.width || defaultCol.width,
        }
      })

      // 添加默认配置中不存在但保存中有的列（兼容旧版本）
      savedConfig.forEach(savedCol => {
        if (!mergedConfig.find(m => m.prop === savedCol.prop)) {
          mergedConfig.push(savedCol)
        }
      })

      columnWidths.value = mergedConfig
    } else {
      // 首次使用，采用默认配置
      columnWidths.value = [...defaultColumns]
    }

    isLoaded.value = true
  }

  /**
   * 防抖保存
   */
  const debouncedSave = (): void => {
    if (!autoSave) return

    if (saveTimer) {
      clearTimeout(saveTimer)
    }

    saveTimer = setTimeout(() => {
      saveToStorage(fullStorageKey, columnWidths.value)
    }, debounceDelay)
  }

  /**
   * 列宽拖拽结束回调
   */
  const onHeaderDragEnd = (
    newWidth: number,
    _oldWidth: number,
    column: { property: string },
    _event: MouseEvent
  ): void => {
    if (!column.property) return

    // 限制在最小/最大范围内
    const clampedWidth = Math.min(Math.max(newWidth, minWidth), maxWidth)

    // 更新配置
    const index = columnWidths.value.findIndex(c => c.prop === column.property)
    if (index !== -1) {
      columnWidths.value[index].width = clampedWidth
    } else {
      // 新增未记录的列
      columnWidths.value.push({ prop: column.property, width: clampedWidth })
    }

    // 防抖保存
    debouncedSave()
  }

  /**
   * 重置为默认列宽
   */
  const resetToDefault = (): void => {
    columnWidths.value = [...defaultColumns]
    
    // 清除本地存储
    try {
      localStorage.removeItem(fullStorageKey)
    } catch (error) {
      console.warn('[useResizableColumns] 清除存储失败:', error)
    }
  }

  /**
   * 手动保存当前配置
   */
  const save = (): void => {
    if (saveTimer) {
      clearTimeout(saveTimer)
      saveTimer = null
    }
    saveToStorage(fullStorageKey, columnWidths.value)
  }

  /**
   * 获取指定列的当前宽度
   */
  const getColumnWidth = (prop: string): number | undefined => {
    const col = columnWidths.value.find(c => c.prop === prop)
    return col?.width
  }

  // 组件挂载时加载配置
  onMounted(() => {
    loadConfig()
  })

  // 监听配置变化，用于调试（生产环境可移除）
  if (import.meta.env.DEV) {
    watch(columnWidths, () => {
      // 调试监听器：列宽变化时不输出调试日志（规范要求禁止console.log）
    }, { deep: true })
  }

  return {
    columnWidths,
    isLoaded,
    tableRef,
    onHeaderDragEnd,
    resetToDefault,
    save,
    getColumnWidth,
  }
}
