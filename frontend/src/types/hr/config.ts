/**
 * HR配置中心类型定义
 *
 * 基于劳动法合规要求，提供可自定义的薪资计算配置：
 * 1. 病假扣薪：按地区配置，不同地区政策不同
 * 2. 扣薪规则：按实际出勤计酬（非"克扣工资"），应当出勤8小时实际6小时则按6小时计
 * 3. 加班调休：可自定义，部分地方标准高于基本法
 * 4. 个税计算：可自定义，防止政策调整时无法适配
 */

/* ============================================================
 * 1. 病假扣薪配置（按地区）
 * ============================================================ */

/** 病假扣薪计算方式 */
export type SickPayCalcMethod = 'fixed_ratio' | 'progressive' | 'by_service_years'

/** 病假扣薪配置选项 */
export const SickPayCalcMethodOptions: { value: SickPayCalcMethod; label: string; description: string }[] = [
  { value: 'fixed_ratio', label: '固定比例', description: '按日工资的固定比例发放病假工资' },
  { value: 'progressive', label: '阶梯比例', description: '根据病假天数分段适用不同比例' },
  { value: 'by_service_years', label: '按工龄', description: '根据员工工龄适用不同比例' },
]

/** 阶梯比例配置项 */
export interface SickPayProgressiveItem {
  /** 起始天数（含） */
  fromDays: number
  /** 结束天数（含），null表示无上限 */
  toDays: number | null
  /** 日工资发放比例（0-1，如0.6表示60%） */
  payRatio: number
}

/** 按工龄配置项 */
export interface SickPayServiceYearItem {
  /** 最低工龄（含） */
  fromYears: number
  /** 最高工龄（含），null表示无上限 */
  toYears: number | null
  /** 日工资发放比例（0-1） */
  payRatio: number
}

/** 病假扣薪地区配置 */
export interface SickPayRegionConfig {
  /** 地区编码（如 'beijing', 'shanghai', 'default'） */
  regionCode: string
  /** 地区名称 */
  regionName: string
  /** 计算方式 */
  calcMethod: SickPayCalcMethod
  /** 固定比例（calcMethod='fixed_ratio'时使用，0-1） */
  fixedRatio?: number
  /** 阶梯比例配置（calcMethod='progressive'时使用） */
  progressiveItems?: SickPayProgressiveItem[]
  /** 按工龄配置（calcMethod='by_service_years'时使用） */
  serviceYearItems?: SickPayServiceYearItem[]
  /** 病假工资最低标准（元/月，不低于当地最低工资的80%） */
  minSickPay?: number
  /** 是否启用 */
  enabled: boolean
  /** 备注 */
  remark?: string
}

/** 病假扣薪配置 */
export interface SickPayConfig {
  /** 配置ID */
  id: string
  /** 配置名称 */
  name: string
  /** 默认地区配置（未匹配到特定地区时使用） */
  defaultRegion: SickPayRegionConfig
  /** 各地区配置 */
  regions: SickPayRegionConfig[]
  /** 更新时间 */
  updateTime?: string
  /** 创建时间 */
  createTime?: string
}

/* ============================================================
 * 2. 扣薪规则配置（按实际出勤计酬）
 * ============================================================ */

/** 扣薪类型 */
export type DeductionType = 'late' | 'early_leave' | 'absent' | 'personal_leave' | 'sick_leave' | 'annual_leave'

/** 扣薪类型选项 */
export const DeductionTypeOptions: { value: DeductionType; label: string; description: string }[] = [
  { value: 'late', label: '迟到', description: '未按时到岗，按实际到岗时间计酬' },
  { value: 'early_leave', label: '早退', description: '提前离岗，按实际在岗时间计酬' },
  { value: 'absent', label: '旷工', description: '无故缺勤，当日不计酬' },
  { value: 'personal_leave', label: '事假', description: '个人请假，按实际出勤计酬' },
  { value: 'sick_leave', label: '病假', description: '因病请假，按病假扣薪比例计酬' },
  { value: 'annual_leave', label: '年假', description: '法定年假，不扣薪' },
]

/** 扣薪规则配置项 */
export interface DeductionRuleItem {
  /** 扣薪类型 */
  type: DeductionType
  /** 是否启用 */
  enabled: boolean
  /** 计算方式说明 */
  calcDescription: string
  /** 迟到/早退的宽限时间（分钟，0表示无宽限） */
  graceMinutes?: number
  /** 迟到/早退超过宽限时间后，每N分钟扣多少（单位：分钟） */
  deductPerMinutes?: number
  /** 旷工当日工资扣除比例（0-1，默认1表示全扣） */
  absentDeductRatio?: number
  /** 是否影响满勤奖 */
  affectFullAttendance: boolean
}

/** 扣薪规则配置 */
export interface DeductionRuleConfig {
  /** 配置ID */
  id: string
  /** 配置名称 */
  name: string
  /** 标准日工作时长（小时） */
  standardWorkHours: number
  /** 标准月工作天数 */
  standardWorkDays: number
  /** 满勤奖金额（元） */
  fullAttendanceBonus: number
  /** 满勤奖扣减规则：当月出现以下情况时取消满勤奖 */
  fullAttendanceCancelRules: DeductionType[]
  /** 各扣薪类型规则 */
  rules: DeductionRuleItem[]
  /** 更新时间 */
  updateTime?: string
  /** 创建时间 */
  createTime?: string
}

/* ============================================================
 * 3. 加班调休配置
 * ============================================================ */

/** 加班类型 */
export type OvertimeType = 'workday' | 'weekend' | 'holiday'

/** 加班类型选项 */
export const OvertimeTypeOptions: { value: OvertimeType; label: string; description: string }[] = [
  { value: 'workday', label: '工作日加班', description: '工作日延长工作时间' },
  { value: 'weekend', label: '休息日加班', description: '周末加班，优先安排调休' },
  { value: 'holiday', label: '法定节假日加班', description: '法定节假日加班，不可调休' },
]

/** 加班补偿方式 */
export type OvertimeCompensation = 'pay' | 'comp_time' | 'both'

/** 加班补偿方式选项 */
export const OvertimeCompensationOptions: { value: OvertimeCompensation; label: string; description: string }[] = [
  { value: 'pay', label: '加班费', description: '按法定标准支付加班费' },
  { value: 'comp_time', label: '调休', description: '安排等量或倍量调休' },
  { value: 'both', label: '可选', description: '员工可选择加班费或调休' },
]

/** 加班规则配置项 */
export interface OvertimeRuleItem {
  /** 加班类型 */
  type: OvertimeType
  /** 补偿方式 */
  compensation: OvertimeCompensation
  /** 加班费倍率（相对日工资，如1.5表示1.5倍） */
  payMultiplier: number
  /** 调休倍率（1表示1:1，1.5表示1小时加班换1.5小时调休） */
  compTimeMultiplier: number
  /** 调休有效期（天，0表示无限制） */
  compTimeExpiryDays: number
  /** 是否允许调休 */
  allowCompTime: boolean
  /** 是否启用 */
  enabled: boolean
}

/** 加班调休配置 */
export interface OvertimeConfig {
  /** 配置ID */
  id: string
  /** 配置名称 */
  name: string
  /** 日加班上限（小时） */
  dailyOvertimeLimit: number
  /** 月加班上限（小时） */
  monthlyOvertimeLimit: number
  /** 加班审批是否必需 */
  approvalRequired: boolean
  /** 各加班类型规则 */
  rules: OvertimeRuleItem[]
  /** 是否高于法定标准（自定义说明） */
  aboveLegalStandard: boolean
  /** 自定义说明 */
  customRemark?: string
  /** 更新时间 */
  updateTime?: string
  /** 创建时间 */
  createTime?: string
}

/* ============================================================
 * 4. 个税计算配置
 * ============================================================ */

/** 个税税率表项 */
export interface TaxBracketItem {
  /** 级数 */
  level: number
  /** 应纳税所得额下限（元/月，含） */
  fromAmount: number
  /** 应纳税所得额上限（元/月，含），null表示无上限 */
  toAmount: number | null
  /** 税率（%，如3表示3%） */
  rate: number
  /** 速算扣除数（元） */
  quickDeduction: number
}

/** 专项附加扣除类型 */
export type SpecialDeductionType =
  | 'children_education'
  | 'continuing_education'
  | 'critical_illness'
  | 'housing_loan'
  | 'housing_rent'
  | 'elderly_care'
  | 'infant_care'

/** 专项附加扣除类型选项 */
export const SpecialDeductionTypeOptions: { value: SpecialDeductionType; label: string; defaultAmount: number }[] = [
  { value: 'children_education', label: '子女教育', defaultAmount: 2000 },
  { value: 'continuing_education', label: '继续教育', defaultAmount: 400 },
  { value: 'critical_illness', label: '大病医疗', defaultAmount: 0 },
  { value: 'housing_loan', label: '住房贷款利息', defaultAmount: 1000 },
  { value: 'housing_rent', label: '住房租金', defaultAmount: 1500 },
  { value: 'elderly_care', label: '赡养老人', defaultAmount: 3000 },
  { value: 'infant_care', label: '3岁以下婴幼儿照护', defaultAmount: 2000 },
]

/** 专项附加扣除配置项 */
export interface SpecialDeductionItem {
  /** 扣除类型 */
  type: SpecialDeductionType
  /** 标准扣除金额（元/月） */
  standardAmount: number
  /** 是否启用 */
  enabled: boolean
}

/** 个税计算配置 */
export interface TaxConfig {
  /** 配置ID */
  id: string
  /** 配置名称 */
  name: string
  /** 起征点（元/月） */
  taxThreshold: number
  /** 税率表 */
  taxBrackets: TaxBracketItem[]
  /** 专项附加扣除配置 */
  specialDeductions: SpecialDeductionItem[]
  /** 社保公积金是否税前扣除 */
  insurancePreTax: boolean
  /** 计算方式说明 */
  calcDescription: string
  /** 是否使用自定义税率表（false表示使用法定标准） */
  customTaxBrackets: boolean
  /** 自定义说明（政策调整时使用） */
  customRemark?: string
  /** 更新时间 */
  updateTime?: string
  /** 创建时间 */
  createTime?: string
}

/* ============================================================
 * 配置中心统一接口
 * ============================================================ */

/** 配置类型标签 */
export type ConfigType = 'sick_pay' | 'deduction' | 'overtime' | 'tax'

/** 配置类型选项 */
export const ConfigTypeOptions: { value: ConfigType; label: string; icon: string; description: string }[] = [
  { value: 'sick_pay', label: '病假扣薪', icon: 'FirstAidKit', description: '按地区配置病假工资发放比例' },
  { value: 'deduction', label: '扣薪规则', icon: 'Calendar', description: '按实际出勤计酬，符合劳动法规定' },
  { value: 'overtime', label: '加班调休', icon: 'Clock', description: '加班费与调休补偿规则配置' },
  { value: 'tax', label: '个税计算', icon: 'Money', description: '个税税率表与专项附加扣除配置' },
]
