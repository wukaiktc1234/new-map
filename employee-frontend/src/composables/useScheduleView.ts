/**
 * 排班视图状态机 composable
 * 管理排班页面的视图层级切换、周偏移、选中状态
 * 支持三层视图: personal(个人) -> team(团队) -> detail(详情)
 */

import { ref, computed } from 'vue'

/** 视图层级类型 */
export type ScheduleLayer = 'personal' | 'team' | 'detail'

/** 周范围信息 */
export interface WeekRange {
  /** 周一日期 YYYY-MM-DD */
  startDate: string
  /** 周日日期 YYYY-MM-DD */
  endDate: string
  /** 周描述文本（如"本周"、"上周"） */
  label: string
}

/** 获取指定日期所在周的周一日期 */
function getMonday(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  return new Date(d.setDate(diff))
}

/** 格式化日期为 YYYY-MM-DD */
function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 排班视图状态机
 * @returns 排班视图相关的状态、计算属性和方法
 */
export function useScheduleView() {
  // ============================================================
  // 状态
  // ============================================================

  /** 当前视图层级 */
  const currentLayer = ref<ScheduleLayer>('personal')

  /** 选中的排班记录ID（进入detail层时使用） */
  const selectedShiftId = ref<string | null>(null)

  /** 选中的同事ID（team视图中高亮用） */
  const selectedColleagueId = ref<string | null>(null)

  /** 团队搜索关键词 */
  const searchKeyword = ref('')

  /** 周偏移量：0=本周, 正数=未来周, 负数=过去周 */
  const weekOffset = ref(0)

  // ============================================================
  // 计算属性
  // ============================================================

  /**
   * 根据周偏移量计算当前周的起止日期范围
   */
  const weekRange = computed<WeekRange>(() => {
    const today = new Date()
    const thisMonday = getMonday(today)

    // 应用偏移
    const targetMonday = new Date(thisMonday)
    targetMonday.setDate(targetMonday.getDate() + weekOffset.value * 7)

    const sunday = new Date(targetMonday)
    sunday.setDate(sunday.getDate() + 6)

    let label = ''
    if (weekOffset.value === 0) {
      label = '本周'
    } else if (weekOffset.value === -1) {
      label = '上周'
    } else if (weekOffset.value === 1) {
      label = '下周'
    } else {
      const m = targetMonday.getMonth() + 1
      const d = targetMonday.getDate()
      label = `${m}/${d} 周`
    }

    return {
      startDate: formatDate(targetMonday),
      endDate: formatDate(sunday),
      label,
    }
  })

  /** 是否在个人视图 */
  const isPersonalView = computed(() => currentLayer.value === 'personal')

  /** 是否在团队视图 */
  const isTeamView = computed(() => currentLayer.value === 'team')

  /** 是否在详情视图 */
  const isDetailView = computed(() => currentLayer.value === 'detail')

  // ============================================================
  // 方法 - 视图导航
  // ============================================================

  /**
   * 切换到团队视图
   */
  function goToTeamView(): void {
    currentLayer.value = 'team'
    selectedShiftId.value = null
  }

  /**
   * 打开排班详情视图
   * @param shiftId - 排班记录ID
   */
  function openShiftDetail(shiftId: string): void {
    selectedShiftId.value = shiftId
    currentLayer.value = 'detail'
  }

  /**
   * 返回个人视图，清除所有选中状态
   */
  function backToPersonal(): void {
    currentLayer.value = 'personal'
    selectedShiftId.value = null
    selectedColleagueId.value = null
  }

  /**
   * 返回上一级视图
   * detail -> personal/team, team -> personal
   */
  function goBack(): void {
    if (currentLayer.value === 'detail') {
      // 详情返回时回到之前的上下文（团队或个人）
      currentLayer.value = 'team'
      selectedShiftId.value = null
    } else if (currentLayer.value === 'team') {
      backToPersonal()
    }
  }

  // ============================================================
  // 方法 - 周导航
  // ============================================================

  /**
   * 切换到上一周
   */
  function prevWeek(): void {
    weekOffset.value--
  }

  /**
   * 切换到下一周
   */
  function nextWeek(): void {
    weekOffset.value++
  }

  /**
   * 回到本周
   */
  function goToday(): void {
    weekOffset.value = 0
  }

  // ============================================================
  // 方法 - 选中管理
  // ============================================================

  /**
   * 选中/取消选中同事
   * @param colleagueId - 同事ID
   */
  function toggleColleagueSelection(colleagueId: string): void {
    if (selectedColleagueId.value === colleagueId) {
      selectedColleagueId.value = null
    } else {
      selectedColleagueId.value = colleagueId
    }
  }

  /**
   * 清除搜索关键词
   */
  function clearSearch(): void {
    searchKeyword.value = ''
  }

  /**
   * 重置所有状态到初始值
   */
  function reset(): void {
    currentLayer.value = 'personal'
    selectedShiftId.value = null
    selectedColleagueId.value = null
    searchKeyword.value = ''
    weekOffset.value = 0
  }

  return {
    // 状态
    currentLayer,
    selectedShiftId,
    selectedColleagueId,
    searchKeyword,
    weekOffset,

    // 计算属性
    weekRange,
    isPersonalView,
    isTeamView,
    isDetailView,

    // 视图导航
    goToTeamView,
    openShiftDetail,
    backToPersonal,
    goBack,

    // 周导航
    prevWeek,
    nextWeek,
    goToday,

    // 选中管理
    toggleColleagueSelection,
    clearSearch,
    reset,
  }
}
