/**
 * 排班管理 API 模块 - 统一导出
 *
 * 导出所有排班相关的API和数据转换器
 */

// 排班计划管理
export { schedulePlanApi } from './plans'

// 日历视图
export { calendarApi } from './calendar'

// 排班引擎
export { engineApi } from './engine'

// 冲突检测
export { conflictApi } from './conflict'

// 换班管理
export { swapApi } from './swap'

// 模板管理
export { templateApi } from './templates'

// 班次类型管理
export { shiftTypeApi } from './shift-types'

// 考勤同步
export { attendanceApi } from './attendance'

// 通知日志
export { notificationApi } from './notifications'

// 数据导出
export { exportApi } from './export'

// 数据转换器
export {
  schedulePlanConverter,
  swapRequestConverter,
  notificationLogConverter,
  getShiftTypeLabel,
} from './converters'
