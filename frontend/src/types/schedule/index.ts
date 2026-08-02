/**
 * 排班管理模块 - TypeScript 类型定义
 *
 * 【层级】L2 - 基础设施层
 * 【职责】定义排班相关的所有 TypeScript 接口和类型
 *
 * 状态字段使用语义化字符串（符合项目规范 §24 前端语义化状态）
 */

// ============================================================
// 一、状态枚举（全部使用 string 联合类型，非数字枚举）
// ============================================================

/** 排班计划状态 */
export type SchedulePlanStatus = 'draft' | 'published' | 'executing' | 'archived'

/** 换班请求状态 */
export type SwapRequestStatus = 'pending' | 'approved' | 'rejected' | 'cancelled'

/** 班次类型代码 */
export type ShiftTypeCode = 'morning' | 'noon' | 'evening' | 'night_off'

/** 排班条目来源 */
export type EntrySource = 'auto' | 'manual' | 'swap'

/** 冲突级别 */
export type ConflictLevel = 'error' | 'warning' | 'info'

/** 规则分类 */
export type RuleCategory = 'hard_constraint' | 'soft_constraint'

/** 通知渠道 */
export type NotificationChannel = 'in_app' | 'sms' | 'email' | 'wechat_work'

/** 发送状态 */
export type SendStatus = 'pending' | 'sending' | 'success' | 'failed'

/** 同步状态 */
export type SyncStatus = 'not_synced' | 'synced' | 'failed'

/** 模板状态 */
export type TemplateStatus = 'active' | 'inactive'

// ============================================================
// 二、核心实体接口
// ============================================================

/**
 * 排班计划
 * 排班管理的核心实体，表示一个时间范围内的完整排班方案
 */
export interface SchedulePlan {
  /** 计划ID */
  planId: string
  /** 计划名称 */
  planName: string
  /** 门店ID */
  storeId: string
  /** 门店名称 */
  storeName: string
  /** 开始日期 (YYYY-MM-DD) */
  startDate: string
  /** 结束日期 (YYYY-MM-DD) */
  endDate: string
  /** 计划状态 */
  status: SchedulePlanStatus
  /** 版本号 */
  version: number
  /** 关联模板ID（可选） */
  templateId?: string
  /** 关联模板名称（可选） */
  templateName?: string
  /** 员工数量 */
  employeeCount: number
  /** 总工时（分钟） */
  totalWorkHours: number
  /** 发布人ID（可选） */
  publisherId?: string
  /** 发布人名称（可选） */
  publisherName?: string
  /** 发布时间（可选） */
  publishTime?: string
  /** 考勤同步状态 */
  attendanceSyncStatus: SyncStatus
  /** 考勤同步时间（可选） */
  attendanceSyncTime?: string
  /** 冲突检查状态 */
  conflictCheckStatus: SyncStatus
  /** 冲突检查时间（可选） */
  conflictCheckTime?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/**
 * 排班条目
 * 表示某个员工在特定日期的班次安排
 */
export interface ScheduleEntry {
  /** 条目ID */
  entryId: string
  /** 所属计划ID */
  planId: string
  /** 员工ID */
  employeeId: string
  /** 员工姓名 */
  employeeName: string
  /** 职位名称（可选） */
  positionName?: string
  /** 工作日期 (YYYY-MM-DD) */
  workDate: string
  /** 班次类型代码 */
  shiftType: ShiftTypeCode
  /** 班次名称 */
  shiftName: string
  /** 开始时间 (HH:mm) */
  startTime: string
  /** 结束时间 (HH:mm) */
  endTime: string
  /** 条目来源 */
  source: EntrySource
  /** 冲突级别（可选） */
  conflictLevel?: ConflictLevel
  /** 冲突消息（可选） */
  conflictMessage?: string
}

// ============================================================
// 三、日历视图相关接口
// ============================================================

/**
 * 日历视图响应
 * 用于周视图/月视图展示
 */
export interface TimelineResponse {
  /** 周起始日期 */
  weekStart: string
  /** 周结束日期 */
  weekEnd: string
  /** 日期列表 */
  dates: TimelineDate[]
  /** 员工列表 */
  employees: TimelineEmployee[]
  /** 每日汇总 */
  dailySummary: DailySummary[]
}

/** 日历视图中的日期信息 */
export interface TimelineDate {
  /** 日期 (YYYY-MM-DD) */
  date: string
  /** 星期几 (1-7, 1=周一) */
  dayOfWeek: number
  /** 是否周末 */
  isWeekend: boolean
  /** 是否节假日 */
  isHoliday: boolean
  /** 节假日名称（可选） */
  holidayName?: string
}

/**
 * 日历视图员工行
 * 包含该员工一周的排班信息
 */
export interface TimelineEmployee {
  /** 员工ID */
  employeeId: string
  /** 员工姓名 */
  employeeName: string
  /** 头像URL（可选） */
  avatarUrl?: string
  /** 职位名称 */
  positionName: string
  /** 该周的排班条目 */
  entries: TimelineEntry[]
  /** 本周总工时（小时） */
  weeklyHours: number
  /** 连续工作天数 */
  consecutiveDays: number
  /** 是否存在冲突 */
  hasConflict: boolean
}

/**
 * 日历视图中的排班条目
 * 单个单元格的数据
 */
export interface TimelineEntry {
  /** 条目ID */
  entryId: string
  /** 日期 (YYYY-MM-DD) */
  date: string
  /** 班次类型代码 */
  shiftType: ShiftTypeCode
  /** 班次名称 */
  shiftName: string
  /** 班次颜色（CSS变量值） */
  shiftColor: string
  /** 开始时间 (HH:mm) */
  startTime: string
  /** 结束时间 (HH:mm) */
  endTime: string
  /** 条目来源 */
  source: EntrySource
  /** 冲突级别（可选） */
  conflictLevel?: ConflictLevel
  /** 冲突消息（可选） */
  conflictMessage?: string
}

/**
 * 每日汇总统计
 * 用于显示每日的人员需求与实际排班对比
 */
export interface DailySummary {
  /** 日期 (YYYY-MM-DD) */
  date: string
  /** 星期几 (1-7) */
  dayOfWeek: number
  /** 是否周末 */
  isWeekend: boolean
  /** 是否节假日 */
  isHoliday: boolean
  /** 各班次需求人数 */
  demand: Record<string, number>
  /** 各班次实际人数 */
  actual: Record<string, number>
  /** 各班次缺编人数 */
  shortage: Record<string, number>
}

// ============================================================
// 四、换班管理接口
// ============================================================

/**
 * 换班请求
 * 员工之间互换班次的申请记录
 */
export interface SwapRequest {
  /** 请求ID */
  requestId: string
  /** 所属计划ID */
  planId: string
  /** 计划名称 */
  planName: string
  /** 请求状态 */
  status: SwapRequestStatus
  /** 发起人ID */
  initiatorId: string
  /** 发起人姓名 */
  initiatorName: string
  /** 发起人的原班次 */
  initiatorEntry: SwapEntryInfo
  /** 目标员工ID */
  targetEmployeeId: string
  /** 目标员工姓名 */
  targetEmployeeName: string
  /** 目标员工的班次 */
  targetEntry: SwapEntryInfo
  /** 换班原因（可选） */
  reason?: string
  /** 申请时间 */
  createTime: string
  /** 审批人ID（可选） */
  approverId?: string
  /** 审批人姓名（可选） */
  approverName?: string
  /** 审批时间（可选） */
  approveTime?: string
  /** 审批意见（可选） */
  approveComment?: string
}

/**
 * 换班条目信息
 * 换班涉及的班次详情
 */
export interface SwapEntryInfo {
  /** 条目ID */
  entryId: string
  /** 日期 (YYYY-MM-DD) */
  date: string
  /** 班次类型代码 */
  shiftType: ShiftTypeCode
  /** 班次名称 */
  shiftName: string
  /** 开始时间 (HH:mm) */
  startTime: string
  /** 结束时间 (HH:mm) */
  endTime: string
}

// ============================================================
// 五、模板管理接口
// ============================================================

/**
 * 排班模板
 * 可复用的排班规则配置
 */
export interface ScheduleTemplate {
  /** 模板ID */
  templateId: string
  /** 模板名称 */
  templateName: string
  /** 模板描述（可选） */
  description?: string
  /** 门店ID */
  storeId: string
  /** 需求矩阵 */
  demandMatrix: DemandMatrix
  /** 启用的规则ID列表 */
  enabledRuleIds: string[]
  /** 是否默认模板 */
  isDefault: boolean
  /** 模板状态 */
  status: TemplateStatus
  /** 使用次数 */
  useCount: number
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/**
 * 需求矩阵
 * 定义不同日期类型的各班次人员需求
 */
export interface DemandMatrix {
  /** 工作日需求 */
  weekday: ShiftDemand
  /** 周末需求 */
  weekend: ShiftDemand
  /** 节假日需求 */
  holiday: ShiftDemand
}

/**
 * 班次需求
 * 特定日期类型下各班次的需求数量
 */
export interface ShiftDemand {
  /** 早班需求人数 */
  morning: number
  /** 中午班需求人数 */
  noon: number
  /** 晚班需求人数 */
  evening: number
}

// ============================================================
// 六、班次类型接口
// ============================================================

/**
 * 排班班次类型
 * 定义可用的班次选项及其属性
 */
export interface ScheduleShiftType {
  /** 班次类型ID */
  shiftTypeId: string
  /** 班次类型代码 */
  shiftCode: ShiftTypeCode
  /** 班次名称 */
  shiftName: string
  /** 门店ID */
  storeId: string
  /** 开始时间 (HH:mm) */
  startTime: string
  /** 结束时间 (HH:mm) */
  endTime: string
  /** 显示颜色（CSS变量或色值） */
  color: string
  /** 持续时长（分钟） */
  durationMinutes: number
  /** 是否休息班次 */
  isRest: boolean
  /** 排序顺序 */
  sortOrder: number
  /** 状态 */
  status: 'active' | 'inactive'
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

// ============================================================
// 七、排班规则接口
// ============================================================

/**
 * 排班规则
 * 用于约束和优化排班结果的业务规则
 */
export interface ScheduleRule {
  /** 规则ID */
  ruleId: string
  /** 规则代码 */
  ruleCode: string
  /** 规则名称 */
  ruleName: string
  /** 规则描述（可选） */
  description?: string
  /** 门店ID */
  storeId: string
  /** 规则分类 */
  category: RuleCategory
  /** 规则参数 */
  parameters: Record<string, unknown>
  /** 优先级（数值越大优先级越高） */
  priority: number
  /** 状态 */
  status: 'active' | 'inactive'
  /** 是否系统内置 */
  isSystem: boolean
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

// ============================================================
// 八、通知日志接口
// ============================================================

/**
 * 通知日志
 * 记录排班相关的通知发送情况
 */
export interface NotificationLog {
  /** 日志ID */
  logId: string
  /** 业务类型 */
  businessType: string
  /** 业务ID */
  businessId: string
  /** 通知标题 */
  title: string
  /** 通知内容 */
  content: string
  /** 接收人ID列表 */
  receiverIds: number[]
  /** 接收人姓名列表 */
  receiverNames: string[]
  /** 通知渠道 */
  channel: NotificationChannel
  /** 发送状态 */
  sendStatus: SendStatus
  /** 发送时间（可选） */
  sendTime?: string
  /** 重试次数 */
  retryCount: number
  /** 错误信息（可选） */
  errorMessage?: string
  /** 创建时间 */
  createTime: string
}

// ============================================================
// 九、冲突检测接口
// ============================================================

/**
 * 冲突检测结果
 * 排班冲突检查的汇总结果
 */
export interface ConflictCheckResult {
  /** 计划ID */
  planId: string
  /** 检查时间 */
  checkTime: string
  /** 冲突汇总 */
  summary: {
    /** 错误级冲突数 */
    error: number
    /** 警告级冲突数 */
    warning: number
    /** 信息级提示数 */
    info: number
  }
  /** 是否可以发布 */
  canPublish: boolean
  /** 冲突详情列表 */
  conflicts: ConflictDetail[]
}

/**
 * 冲突详情
 * 单个冲突的具体信息
 */
export interface ConflictDetail {
  /** 冲突ID */
  conflictId: string
  /** 冲突级别 */
  level: ConflictLevel
  /** 冲突类型 */
  type: string
  /** 相关员工ID（可选） */
  employeeId?: string
  /** 相关员工姓名（可选） */
  employeeName?: string
  /** 冲突日期（可选） */
  date?: string
  /** 冲突班次（可选） */
  shiftType?: string
  /** 相关规则ID（可选） */
  ruleId?: string
  /** 冲突描述 */
  message: string
  /** 修复建议 */
  suggestion: string
  /** 是否支持自动修复 */
  autoFixAvailable: boolean
}

// ============================================================
// 十、考勤同步接口
// ============================================================

/**
 * 考勤差异报告
 * 排班数据与考勤数据的对比结果
 */
export interface AttendanceDiffReport {
  /** 报告ID */
  reportId: string
  /** 计划ID */
  planId: string
  /** 统计周期开始 */
  periodStart: string
  /** 统计周期结束 */
  periodEnd: string
  /** 生成时间 */
  generateTime: string
  /** 差异汇总 */
  summary: {
    /** 总排班条目数 */
    totalEntries: number
    /** 已打卡条目数 */
    checkedIn: number
    /** 未打卡条目数 */
    notCheckedIn: number
    /** 异常条目数 */
    abnormal: number
    /** 迟到次数 */
    lateCount: number
    /** 早退次数 */
    earlyLeaveCount: number
    /** 缺卡次数 */
    missingCount: number
  }
  /** 差异明细列表 */
  details: AttendanceDiffDetail[]
}

/**
 * 考勤差异明细
 * 单个员工的考勤差异详情
 */
export interface AttendanceDiffDetail {
  /** 明细ID */
  detailId: string
  /** 员工ID */
  employeeId: string
  /** 员工姓名 */
  employeeName: string
  /** 日期 (YYYY-MM-DD) */
  date: string
  /** 排班班次 */
  scheduleShift: string
  /** 排班开始时间 */
  scheduleStart: string
  /** 排班结束时间 */
  scheduleEnd: string
  /** 实际签到时间（可选） */
  actualCheckIn?: string
  /** 实际签退时间（可选） */
  actualCheckOut?: string
  /** 差异类型：late/early_leave/missing/absent/overtime */
  diffType: string
  /** 差异时长（分钟） */
  diffMinutes: number
  /** 备注（可选） */
  remark?: string
}

// ============================================================
// 十一、表单数据类型
// ============================================================

/**
 * 排班计划表单数据
 * 用于创建/编辑排班计划
 */
export interface SchedulePlanFormData {
  /** 计划名称 */
  planName: string
  /** 门店ID */
  storeId: string
  /** 开始日期 */
  startDate: string
  /** 结束日期 */
  endDate: string
  /** 模板ID（可选） */
  templateId?: string
  /** 描述（可选） */
  description?: string
}

/**
 * 排班计划查询表单
 * 用于筛选排班计划列表
 */
export interface SchedulePlanQueryForm extends Record<string, unknown> {
  /** 关键词搜索（计划名称） */
  keyword?: string
  /** 门店筛选 */
  storeId?: string
  /** 状态筛选 */
  status?: SchedulePlanStatus | ''
  /** 开始日期范围 - 起 */
  startDateStart?: string
  /** 开始日期范围 - 止 */
  startDateEnd?: string
}

/**
 * 换班请求表单数据
 * 用于提交换班申请
 */
export interface SwapRequestFormData {
  /** 目标计划ID */
  planId: string
  /** 自己要换出的条目ID */
  initiatorEntryId: string
  /** 目标员工ID */
  targetEmployeeId: string
  /** 目标员工的条目ID */
  targetEntryId: string
  /** 换班原因（可选） */
  reason?: string
}

/**
 * 生成排班表单数据
 * 用于触发自动排班生成
 */
export interface GenerateScheduleFormData {
  /** 计划ID */
  planId: string
  /** 使用的模板ID（可选，不传则使用默认模板） */
  templateId?: string
  /** 是否覆盖已有排班 */
  overwriteExisting: boolean
  /** 生效日期（可选，不传则从计划开始日期生效） */
  effectiveDate?: string
}
