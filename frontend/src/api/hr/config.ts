/**
 * HR配置中心API
 * 对应后端: /v1/hr/config
 *
 * 提供病假扣薪、扣薪规则、加班调休、个税计算四种配置的CRUD
 * 所有配置支持自定义，防止政策调整时无法适配
 */
import { get, put } from '../request'
import type {
  SickPayConfig,
  DeductionRuleConfig,
  OvertimeConfig,
  TaxConfig,
} from '../../types/hr/config'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// ============================================================
// 默认配置数据（基于2026年劳动法标准）
// ============================================================

/** 默认病假扣薪配置 */
const defaultSickPayConfig: SickPayConfig = {
  id: 'sick_pay_default',
  name: '病假扣薪配置（默认）',
  defaultRegion: {
    regionCode: 'default',
    regionName: '默认地区',
    calcMethod: 'by_service_years',
    serviceYearItems: [
      { fromYears: 0, toYears: 2, payRatio: 0.6 },
      { fromYears: 2, toYears: 4, payRatio: 0.7 },
      { fromYears: 4, toYears: 6, payRatio: 0.8 },
      { fromYears: 6, toYears: 8, payRatio: 0.9 },
      { fromYears: 8, toYears: null, payRatio: 1.0 },
    ],
    minSickPay: 2200,
    enabled: true,
    remark: '按工龄阶梯发放病假工资，最低不低于当地最低工资80%',
  },
  regions: [
    {
      regionCode: 'beijing',
      regionName: '北京',
      calcMethod: 'fixed_ratio',
      fixedRatio: 0.8,
      minSickPay: 2420,
      enabled: true,
      remark: '北京地区病假工资不低于最低工资的80%',
    },
    {
      regionCode: 'shanghai',
      regionName: '上海',
      calcMethod: 'progressive',
      progressiveItems: [
        { fromDays: 1, toDays: 6, payRatio: 1.0 },
        { fromDays: 7, toDays: 30, payRatio: 0.8 },
        { fromDays: 31, toDays: null, payRatio: 0.6 },
      ],
      minSickPay: 2690,
      enabled: true,
      remark: '上海地区连续工龄不满2年者，病假工资按本人工资60%计发',
    },
    {
      regionCode: 'guangdong',
      regionName: '广东',
      calcMethod: 'by_service_years',
      serviceYearItems: [
        { fromYears: 0, toYears: 4, payRatio: 0.6 },
        { fromYears: 4, toYears: 8, payRatio: 0.7 },
        { fromYears: 8, toYears: null, payRatio: 0.8 },
      ],
      minSickPay: 2300,
      enabled: true,
    },
  ],
  createTime: '2026-01-01 00:00:00',
  updateTime: '2026-06-01 10:00:00',
}

/** 默认扣薪规则配置 */
const defaultDeductionConfig: DeductionRuleConfig = {
  id: 'deduction_default',
  name: '扣薪规则配置（按实际出勤计酬）',
  standardWorkHours: 8,
  standardWorkDays: 21.75,
  fullAttendanceBonus: 200,
  fullAttendanceCancelRules: ['late', 'early_leave', 'absent'],
  rules: [
    {
      type: 'late',
      enabled: true,
      calcDescription: '应当出勤8小时，实际到岗6小时，则按6小时计酬。迟到15分钟内不扣，超过后每30分钟扣0.5小时',
      graceMinutes: 15,
      deductPerMinutes: 30,
      affectFullAttendance: true,
    },
    {
      type: 'early_leave',
      enabled: true,
      calcDescription: '按实际在岗时间计酬，早退15分钟内不扣，超过后每30分钟扣0.5小时',
      graceMinutes: 15,
      deductPerMinutes: 30,
      affectFullAttendance: true,
    },
    {
      type: 'absent',
      enabled: true,
      calcDescription: '旷工当日不计酬，按日工资全额扣除',
      absentDeductRatio: 1.0,
      affectFullAttendance: true,
    },
    {
      type: 'personal_leave',
      enabled: true,
      calcDescription: '事假按实际出勤计酬，请假期间不计发工资',
      affectFullAttendance: false,
    },
    {
      type: 'sick_leave',
      enabled: true,
      calcDescription: '病假按病假扣薪配置比例计发工资',
      affectFullAttendance: false,
    },
    {
      type: 'annual_leave',
      enabled: true,
      calcDescription: '法定年假不扣薪，视为正常出勤',
      affectFullAttendance: false,
    },
  ],
  createTime: '2026-01-01 00:00:00',
  updateTime: '2026-06-01 10:00:00',
}

/** 默认加班调休配置 */
const defaultOvertimeConfig: OvertimeConfig = {
  id: 'overtime_default',
  name: '加班调休配置（法定标准）',
  dailyOvertimeLimit: 3,
  monthlyOvertimeLimit: 36,
  approvalRequired: true,
  aboveLegalStandard: false,
  rules: [
    {
      type: 'workday',
      compensation: 'pay',
      payMultiplier: 1.5,
      compTimeMultiplier: 1,
      compTimeExpiryDays: 0,
      allowCompTime: false,
      enabled: true,
    },
    {
      type: 'weekend',
      compensation: 'both',
      payMultiplier: 2.0,
      compTimeMultiplier: 1,
      compTimeExpiryDays: 90,
      allowCompTime: true,
      enabled: true,
    },
    {
      type: 'holiday',
      compensation: 'pay',
      payMultiplier: 3.0,
      compTimeMultiplier: 3,
      compTimeExpiryDays: 0,
      allowCompTime: false,
      enabled: true,
    },
  ],
  createTime: '2026-01-01 00:00:00',
  updateTime: '2026-06-01 10:00:00',
}

/** 默认个税计算配置 */
const defaultTaxConfig: TaxConfig = {
  id: 'tax_default',
  name: '个税计算配置（2026版）',
  taxThreshold: 5000,
  taxBrackets: [
    { level: 1, fromAmount: 0, toAmount: 3000, rate: 3, quickDeduction: 0 },
    { level: 2, fromAmount: 3000, toAmount: 12000, rate: 10, quickDeduction: 210 },
    { level: 3, fromAmount: 12000, toAmount: 25000, rate: 20, quickDeduction: 1410 },
    { level: 4, fromAmount: 25000, toAmount: 35000, rate: 25, quickDeduction: 2660 },
    { level: 5, fromAmount: 35000, toAmount: 55000, rate: 30, quickDeduction: 4410 },
    { level: 6, fromAmount: 55000, toAmount: 80000, rate: 35, quickDeduction: 7160 },
    { level: 7, fromAmount: 80000, toAmount: null, rate: 45, quickDeduction: 15160 },
  ],
  specialDeductions: [
    { type: 'children_education', standardAmount: 2000, enabled: true },
    { type: 'continuing_education', standardAmount: 400, enabled: true },
    { type: 'critical_illness', standardAmount: 0, enabled: true },
    { type: 'housing_loan', standardAmount: 1000, enabled: true },
    { type: 'housing_rent', standardAmount: 1500, enabled: true },
    { type: 'elderly_care', standardAmount: 3000, enabled: true },
    { type: 'infant_care', standardAmount: 2000, enabled: true },
  ],
  insurancePreTax: true,
  calcDescription: '应纳税所得额 = 税前工资 - 五险一金个人部分 - 起征点 - 专项附加扣除；个税 = 应纳税所得额 × 税率 - 速算扣除数',
  customTaxBrackets: false,
  createTime: '2026-01-01 00:00:00',
  updateTime: '2026-06-01 10:00:00',
}

// 内存中的配置副本（管理端可修改）
let sickPayConfig = { ...defaultSickPayConfig }
let deductionConfig = { ...defaultDeductionConfig }
let overtimeConfig = { ...defaultOvertimeConfig }
let taxConfig = { ...defaultTaxConfig }

export const hrConfigApi = {
  /* ===== 病假扣薪配置 ===== */

  /** 获取病假扣薪配置 */
  async getSickPayConfig(): Promise<SickPayConfig> {
    return await get<SickPayConfig>('/v1/hr/config/sick-pay')
  },

  /** 保存病假扣薪配置 */
  async saveSickPayConfig(data: SickPayConfig): Promise<SickPayConfig> {
    return await put<SickPayConfig>('/v1/hr/config/sick-pay', data)
  },

  /* ===== 扣薪规则配置 ===== */

  /** 获取扣薪规则配置 */
  async getDeductionConfig(): Promise<DeductionRuleConfig> {
    return await get<DeductionRuleConfig>('/v1/hr/config/deduction')
  },

  /** 保存扣薪规则配置 */
  async saveDeductionConfig(data: DeductionRuleConfig): Promise<DeductionRuleConfig> {
    return await put<DeductionRuleConfig>('/v1/hr/config/deduction', data)
  },

  /* ===== 加班调休配置 ===== */

  /** 获取加班调休配置 */
  async getOvertimeConfig(): Promise<OvertimeConfig> {
    return await get<OvertimeConfig>('/v1/hr/config/overtime')
  },

  /** 保存加班调休配置 */
  async saveOvertimeConfig(data: OvertimeConfig): Promise<OvertimeConfig> {
    return await put<OvertimeConfig>('/v1/hr/config/overtime', data)
  },

  /* ===== 个税计算配置 ===== */

  /** 获取个税计算配置 */
  async getTaxConfig(): Promise<TaxConfig> {
    return await get<TaxConfig>('/v1/hr/config/tax')
  },

  /** 保存个税计算配置 */
  async saveTaxConfig(data: TaxConfig): Promise<TaxConfig> {
    return await put<TaxConfig>('/v1/hr/config/tax', data)
  },

  /* ===== 配置预览（计算测试） ===== */

  /** 预览病假工资计算 */
  async previewSickPay(params: {
    dailyWage: number
    sickDays: number
    serviceYears: number
    regionCode?: string
  }): Promise<{ sickPay: number; calcDetail: string }> {
    await delay(100)
    const config = sickPayConfig
    const region = params.regionCode
      ? config.regions.find(r => r.regionCode === params.regionCode && r.enabled) || config.defaultRegion
      : config.defaultRegion

    let ratio = 0.6
    let detail = ''

    if (region.calcMethod === 'fixed_ratio') {
      ratio = region.fixedRatio ?? 0.6
      detail = `固定比例${(ratio * 100).toFixed(0)}%`
    } else if (region.calcMethod === 'progressive' && region.progressiveItems) {
      const item = region.progressiveItems.find(i =>
        params.sickDays >= i.fromDays && (i.toDays === null || params.sickDays <= i.toDays)
      )
      ratio = item?.payRatio ?? 0.6
      detail = `病假${params.sickDays}天，适用比例${(ratio * 100).toFixed(0)}%`
    } else if (region.calcMethod === 'by_service_years' && region.serviceYearItems) {
      const item = region.serviceYearItems.find(i =>
        params.serviceYears >= i.fromYears && (i.toYears === null || params.serviceYears <= i.toYears)
      )
      ratio = item?.payRatio ?? 0.6
      detail = `工龄${params.serviceYears}年，适用比例${(ratio * 100).toFixed(0)}%`
    }

    const sickPay = Math.round(params.dailyWage * ratio * params.sickDays)
    const minPay = region.minSickPay ?? 0
    const finalPay = Math.max(sickPay, minPay)

    return {
      sickPay: finalPay,
      calcDetail: `${detail}，日工资${params.dailyWage}元 × ${params.sickDays}天 = ${finalPay}元`,
    }
  },

  /** 预览个税计算 */
  async previewTax(params: {
    grossSalary: number
    insuranceDeduction: number
    specialDeduction: number
  }): Promise<{ taxAmount: number; calcDetail: string }> {
    await delay(100)
    const config = taxConfig
    const taxableIncome = Math.max(0,
      params.grossSalary - params.insuranceDeduction - config.taxThreshold - params.specialDeduction
    )

    const bracket = config.taxBrackets.find(b =>
      taxableIncome > b.fromAmount && (b.toAmount === null || taxableIncome <= b.toAmount)
    ) || config.taxBrackets[0]

    const taxAmount = Math.round(taxableIncome * bracket.rate / 100 - bracket.quickDeduction)
    const finalTax = Math.max(0, taxAmount)

    return {
      taxAmount: finalTax,
      calcDetail: `应纳税所得额 = ${params.grossSalary} - ${params.insuranceDeduction}(社保) - ${config.taxThreshold}(起征点) - ${params.specialDeduction}(专项扣除) = ${taxableIncome}元；个税 = ${taxableIncome} × ${bracket.rate}% - ${bracket.quickDeduction} = ${finalTax}元`,
    }
  },
}
