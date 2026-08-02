import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import logger from '@/utils/logger'
import type { ChannelType, CompareType } from '@/views/operations/reports/types/report'

/**
 * 报表筛选状态管理Store
 *
 * 功能：
 * - 管理报表全局筛选状态（日期范围、门店、渠道、对比维度）
 * - 提供筛选参数构建方法
 * - 支持筛选条件持久化到localStorage
 */
export const useReportFilterStore = defineStore('reportFilter', () => {
  // ========== State ==========

  /** 日期范围 [开始日期, 结束日期] */
  const dateRange = ref<[string, string]>(['', ''])

  /** 选中的门店ID列表 */
  const selectedStores = ref<string[]>([])

  /** 渠道类型 */
  const channel = ref<ChannelType>('all')

  /** 对比维度 */
  const compareType = ref<CompareType>('none')

  /** 是否已初始化 */
  const initialized = ref(false)

  // ========== Getters ==========

  /** 筛选参数是否有效 */
  const isValid = computed(() => {
    return dateRange.value[0] && dateRange.value[1]
  })

  /** 构建查询参数 */
  const queryParams = computed(() => {
    return {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      storeIds: selectedStores.value,
      channel: channel.value,
      compareType: compareType.value
    }
  })

  // ========== Actions ==========

  /**
   * 初始化筛选状态
   * 从localStorage恢复或设置默认值
   */
  function init(): void {
    if (initialized.value) return

    try {
      const saved = localStorage.getItem('report_filter_state')
      if (saved) {
        const state = JSON.parse(saved)
        dateRange.value = state.dateRange || getDefaultDateRange()
        selectedStores.value = state.selectedStores || []
        channel.value = state.channel || 'all'
        compareType.value = state.compareType || 'none'
      } else {
        dateRange.value = getDefaultDateRange()
      }
      initialized.value = true
    } catch (error: unknown) {
      logger.error('REPORT_FILTER', `初始化报表筛选状态失败: ${error instanceof Error ? error.message : String(error)}`)
      dateRange.value = getDefaultDateRange()
      initialized.value = true
    }
  }

  /**
   * 设置日期范围
   * @param range 日期范围
   */
  function setDateRange(range: [string, string]): void {
    dateRange.value = range
    persistState()
  }

  /**
   * 设置选中门店
   * @param stores 门店ID列表
   */
  function setSelectedStores(stores: string[]): void {
    selectedStores.value = stores
    persistState()
  }

  /**
   * 设置渠道
   * @param ch 渠道类型
   */
  function setChannel(ch: ChannelType): void {
    channel.value = ch
    persistState()
  }

  /**
   * 设置对比维度
   * @param type 对比类型
   */
  function setCompareType(type: CompareType): void {
    compareType.value = type
    persistState()
  }

  /**
   * 重置筛选条件
   */
  function reset(): void {
    dateRange.value = getDefaultDateRange()
    selectedStores.value = []
    channel.value = 'all'
    compareType.value = 'none'
    persistState()
  }

  /**
   * 持久化状态到localStorage
   */
  function persistState(): void {
    try {
      const state = {
        dateRange: dateRange.value,
        selectedStores: selectedStores.value,
        channel: channel.value,
        compareType: compareType.value
      }
      localStorage.setItem('report_filter_state', JSON.stringify(state))
    } catch (error: unknown) {
      logger.error('REPORT_FILTER', `持久化报表筛选状态失败: ${error instanceof Error ? error.message : String(error)}`)
    }
  }

  /**
   * 获取默认日期范围（本月）
   * @returns 默认日期范围
   */
  function getDefaultDateRange(): [string, string] {
    const now = new Date()
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1)
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    return [
      firstDay.toISOString().split('T')[0],
      lastDay.toISOString().split('T')[0]
    ]
  }

  return {
    dateRange,
    selectedStores,
    channel,
    compareType,
    initialized,
    isValid,
    queryParams,
    init,
    setDateRange,
    setSelectedStores,
    setChannel,
    setCompareType,
    reset,
    persistState
  }
})
