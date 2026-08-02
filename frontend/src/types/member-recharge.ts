/**
 * 储值管理相关类型定义
 *
 * 设计原则：
 * 1. 本金/赠送余额分离（核心设计，影响退款、过期、会计）
 * 2. 所有风控参数可配置（不硬编码任何业务规则）
 * 3. 消费扣减顺序：先扣赠送余额，再扣本金余额
 * 4. 退款仅退本金，赠送不退
 * 5. 预留财务系统对接接口
 *
 * 法规依据：《单用途商业预付卡管理办法（试行）》（商务部2012年第9号令）
 */

// ============================================================
// 充值方案
// ============================================================

/** 充值方案类型 */
export type RechargePlanType = 'standard' | 'activity' | 'tiered' | 'custom'

/** 充值方案信息 */
export interface RechargePlanInfo {
  /** 方案ID */
  planId: string
  /** 方案名称 */
  planName: string
  /** 方案类型 */
  planType: RechargePlanType
  /** 充值金额（元） */
  rechargeAmount: string
  /** 赠送金额（元） */
  bonusAmount: string
  /** 赠送比例（百分比，如10表示10%） */
  bonusRate: number
  /** 赠送积分 */
  bonusPoints: number
  /** 赠送类型 */
  bonusType: RechargeBonusType
  /** 赠送有效期天数（-1=永久有效，0=跟随系统默认） */
  validityDays: number
  /** 是否推荐 */
  isRecommended: boolean
  /** 排序序号 */
  sortOrder: number
  /** 状态：active/inactive */
  status: string
  /** 方案描述 */
  description: string

  // === 活动方案字段 ===
  /** 关联促销活动ID（活动方案时填写） */
  promotionId?: string
  /** 方案生效时间（活动方案时填写） */
  startTime?: string
  /** 方案失效时间（活动方案时填写） */
  endTime?: string
  /** 目标人群：all/new_member/specified_level */
  targetAudience?: string
  /** 额外赠送配置（券、菜品等） */
  extraBonus?: Record<string, unknown>

  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
}

/** 赠送类型 */
export type RechargeBonusType = 'balance' | 'coupon' | 'points' | 'mixed'

/** 充值方案创建/编辑表单 */
export interface RechargePlanForm {
  planName: string
  planType: RechargePlanType
  rechargeAmount: string
  bonusAmount: string
  bonusPoints: number
  bonusType: RechargeBonusType
  validityDays: number
  isRecommended: boolean
  sortOrder: number
  status: string
  description: string
  promotionId?: string
  startTime?: string
  endTime?: string
  targetAudience?: string
}

/** 充值方案查询参数 */
export interface RechargePlanQueryForm {
  planName?: string
  planType?: RechargePlanType | null
  status?: string | null
}

// ============================================================
// 充值记录（含本金/赠送分离）
// ============================================================

/** 充值记录信息 */
export interface RechargeRecordInfo {
  /** 记录ID */
  recordId: string
  /** 流水号 */
  recordNo: string
  /** 会员ID */
  memberId: string
  /** 会员姓名 */
  memberName: string
  /** 会员手机号 */
  memberPhone: string
  /** 充值方案ID */
  planId: string
  /** 充值方案名称 */
  planName: string

  // === 金额明细 ===
  /** 充值金额（元，用户实际支付） */
  rechargeAmount: string
  /** 本金入账金额（元，等于充值金额） */
  principalAmount: string
  /** 赠送金额（元） */
  bonusAmount: string
  /** 赠送积分 */
  bonusPoints: number

  // === 支付信息 ===
  /** 支付方式 */
  paymentMethod: RechargePaymentMethod
  /** 支付状态 */
  paymentStatus: RechargePaymentStatus
  /** 支付时间 */
  paymentTime: string
  /** 交易流水号（第三方支付） */
  transactionNo: string

  // === 赠送有效期 ===
  /** 赠送金额过期时间 */
  bonusExpireTime: string

  // === 退款信息 ===
  /** 退款状态 */
  refundStatus: RechargeRefundStatus
  /** 退款金额（元） */
  refundAmount: string
  /** 退款时间 */
  refundTime: string
  /** 退款原因 */
  refundReason: string
  /** 退款审批人 */
  refundApprover: string

  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
}

/** 支付方式 */
export type RechargePaymentMethod = 'wechat' | 'alipay' | 'cash' | 'bank_card' | 'balance'

/** 支付状态 */
export type RechargePaymentStatus = 'pending' | 'success' | 'failed' | 'refunded' | 'partial_refunded'

/** 退款状态 */
export type RechargeRefundStatus = 'none' | 'pending' | 'approved' | 'rejected' | 'refunded'

/** 充值记录查询参数 */
export interface RechargeRecordQueryForm {
  memberPhone?: string
  planId?: string | null
  paymentMethod?: RechargePaymentMethod | null
  paymentStatus?: RechargePaymentStatus | null
  refundStatus?: RechargeRefundStatus | null
  startTime?: string
  endTime?: string
}

// ============================================================
// 赠送余额明细（支持过期管理）
// ============================================================

/** 赠送余额明细 */
export interface BonusBalanceDetail {
  /** 明细ID */
  bonusId: string
  /** 会员ID */
  memberId: string
  /** 关联充值记录ID */
  rechargeRecordId: string
  /** 赠送金额（元） */
  bonusAmount: string
  /** 剩余金额（元） */
  remainingAmount: string
  /** 过期时间（空=永不过期） */
  expireTime: string
  /** 状态：active/expired/refunded */
  status: string
  /** 创建时间 */
  createdAt: string
}

// ============================================================
// 退款管理
// ============================================================

/** 退款申请信息 */
export interface RefundRequestInfo {
  /** 退款ID */
  refundId: string
  /** 关联充值记录ID */
  rechargeRecordId: string
  /** 充值流水号 */
  rechargeRecordNo: string
  /** 会员ID */
  memberId: string
  /** 会员姓名 */
  memberName: string
  /** 会员手机号 */
  memberPhone: string

  // === 退款金额 ===
  /** 申请退款金额（元） */
  requestedAmount: string
  /** 可退本金（元，系统计算） */
  refundablePrincipal: string
  /** 实际退款金额（元） */
  actualRefundAmount: string
  /** 赠送金额处理（清零/按比例扣减） */
  bonusHandling: 'clear' | 'proportional'

  // === 审批流程 ===
  /** 退款状态 */
  status: RefundRequestStatus
  /** 申请人 */
  applicant: string
  /** 申请时间 */
  applyTime: string
  /** 审批人 */
  approver: string
  /** 审批时间 */
  approveTime: string
  /** 审批意见 */
  approveComment: string
  /** 退款执行人 */
  executor: string
  /** 退款执行时间 */
  executeTime: string

  /** 退款原因 */
  refundReason: string
  /** 退款方式（原路返回） */
  refundMethod: RechargePaymentMethod
  /** 备注 */
  remark: string
}

/** 退款申请状态 */
export type RefundRequestStatus = 'pending' | 'approved' | 'rejected' | 'executed' | 'cancelled'

// ============================================================
// 财务流水（预留财务系统对接）
// ============================================================

/** 财务流水类型 */
export type FinanceLogType = 'recharge' | 'consume' | 'refund' | 'bonus_expire' | 'bonus_grant'

/** 储值财务流水 */
export interface RechargeFinanceLog {
  /** 日志ID */
  logId: string
  /** 关联流水号 */
  recordNo: string
  /** 会员ID */
  memberId: string
  /** 会员姓名 */
  memberName: string
  /** 流水类型 */
  financeType: FinanceLogType
  /** 本金变动（元，正=增加，负=减少） */
  principalChange: string
  /** 赠送变动（元） */
  bonusChange: string
  /** 收入变动（元） */
  revenueChange: string
  /** 变动后余额（元） */
  balanceAfter: string
  /** 变动后本金余额（元） */
  principalAfter: string
  /** 变动后赠送余额（元） */
  bonusAfter: string
  /** 备注 */
  remark: string
  /** 创建时间 */
  createdAt: string
}

// ============================================================
// 储值系统设置（全局可配置）
// ============================================================

/** 储值系统设置 */
export interface RechargeSystemSettings {
  // === 充值限额 ===
  /** 单次充值上限（元） */
  singleRechargeLimit: number
  /** 单日累计充值上限（元） */
  dailyRechargeLimit: number
  /** 单月累计充值上限（元） */
  monthlyRechargeLimit: number
  /** 单卡余额上限（元，法规要求默认5000） */
  maxBalanceLimit: number

  // === 赠送规则 ===
  /** 赠送比例硬上限（百分比，默认20） */
  maxBonusRate: number
  /** 赠送有效期默认天数（默认180） */
  defaultBonusValidityDays: number
  /** 赠送形式限制 */
  allowedBonusTypes: RechargeBonusType[]

  // === 退款规则 ===
  /** 免审批阈值（元，默认500） */
  noApprovalThreshold: number
  /** 店长审批阈值（元，默认2000） */
  managerApprovalThreshold: number
  /** 退款手续费比例（百分比，默认0） */
  refundFeeRate: number
  /** 充值后冷却期（小时，默认24） */
  refundCooldownHours: number
  /** 赠送处理方式 */
  bonusHandlingOnRefund: 'clear' | 'proportional'

  // === 风控配置 ===
  /** 风控总开关 */
  riskControlEnabled: boolean
  /** 异常交易告警开关 */
  anomalyAlertEnabled: boolean
  /** 告警通知方式 */
  alertChannels: ('sms' | 'wechat' | 'email')[]

  // === 协议管理 ===
  /** 当前充值协议版本号 */
  currentAgreementVersion: string
  /** 协议变更需重新确认（开关） */
  agreementReconfirmOnChange: boolean

  // === 财务对接 ===
  /** 财务系统对接开关 */
  financeIntegrationEnabled: boolean
  /** 财务系统类型 */
  financeSystemType: 'none' | 'kingdee' | 'yonyou' | 'custom'
  /** 自动生成凭证 */
  autoVoucherGeneration: boolean

  // === 资金存管（预留，默认关闭） ===
  /** 资金存管开关 */
  fundCustodyEnabled: boolean
  /** 存管银行 */
  custodyBank: string
  /** 存管比例（百分比） */
  custodyRate: number
  /** 存管账户 */
  custodyAccount: string
}

/** 默认储值系统设置 */
export const DefaultRechargeSettings: RechargeSystemSettings = {
  singleRechargeLimit: 1000,
  dailyRechargeLimit: 3000,
  monthlyRechargeLimit: 10000,
  maxBalanceLimit: 1000,

  maxBonusRate: 20,
  defaultBonusValidityDays: 180,
  allowedBonusTypes: ['balance', 'coupon', 'points', 'mixed'],

  noApprovalThreshold: 500,
  managerApprovalThreshold: 2000,
  refundFeeRate: 0,
  refundCooldownHours: 24,
  bonusHandlingOnRefund: 'clear',

  riskControlEnabled: true,
  anomalyAlertEnabled: true,
  alertChannels: ['sms', 'wechat'],

  currentAgreementVersion: 'v1.0.0',
  agreementReconfirmOnChange: true,

  financeIntegrationEnabled: false,
  financeSystemType: 'none',
  autoVoucherGeneration: false,

  fundCustodyEnabled: false,
  custodyBank: '',
  custodyRate: 0,
  custodyAccount: '',
}

// ============================================================
// 储值统计概览
// ============================================================

/** 储值统计概览 */
export interface RechargeStatsOverview {
  // === 余额统计 ===
  /** 总储值余额（元） */
  totalBalance: string
  /** 本金余额（元） */
  totalPrincipalBalance: string
  /** 赠送余额（元） */
  totalBonusBalance: string

  // === 本月统计 ===
  /** 本月充值总额（元） */
  rechargeThisMonth: string
  /** 本月赠送总额（元） */
  bonusThisMonth: string
  /** 本月退款总额（元） */
  refundThisMonth: string
  /** 本月充值笔数 */
  rechargeCountThisMonth: number

  // === 今日统计 ===
  /** 今日充值笔数 */
  rechargeCountToday: number
  /** 今日充值总额（元） */
  rechargeAmountToday: string

  // === 分析数据 ===
  /** 平均充值金额（元） */
  avgRechargeAmount: string
  /** 充值方案使用分布 */
  planUsageDistribution: { planName: string; count: number; totalAmount: string }[]
  /** 近7天充值趋势 */
  weeklyTrend: { date: string; amount: string; count: number }[]
  /** 余额预警（余额<100元的会员数） */
  lowBalanceCount: number
  /** 即将过期赠送金额（元，30天内） */
  expiringBonusAmount: string
  /** 即将过期赠送笔数 */
  expiringBonusCount: number
}

// ============================================================
// 异常交易告警
// ============================================================

/** 异常交易告警 */
export interface AnomalyAlert {
  /** 告警ID */
  alertId: string
  /** 会员ID */
  memberId: string
  /** 会员姓名 */
  memberName: string
  /** 告警类型 */
  alertType: AnomalyAlertType
  /** 告警描述 */
  description: string
  /** 触发金额（元） */
  triggerAmount: string
  /** 告警时间 */
  alertTime: string
  /** 处理状态 */
  status: 'pending' | 'handled' | 'ignored'
  /** 处理人 */
  handler: string
  /** 处理时间 */
  handleTime: string
  /** 处理备注 */
  handleRemark: string
}

/** 异常告警类型 */
export type AnomalyAlertType =
  | 'frequent_recharge'      // 频繁充值
  | 'fast_consume'           // 充值后快速消费
  | 'high_refund_rate'       // 高退款率
  | 'new_member_high_recharge' // 新会员高额充值
  | 'multi_device'           // 多设备充值
  | 'over_limit'             // 超限额
