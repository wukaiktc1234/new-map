/**
 * 会员等级相关类型定义
 * 对应后端实体: MemberLevel (member_level表)
 *
 * 设计原则：
 * 1. 等级编码完全自由化，不限制为预设枚举
 * 2. 只升不降，等级永久保留（不降级）
 * 3. 积分年度重置，但不影响等级
 * 4. 升级条件与权益配置分离（支持简单模式和高级模式）
 */

// ============================================================
// 等级主表
// ============================================================

/** 会员等级信息 */
export interface MemberLevelInfo {
  /** 等级ID */
  levelId: string
  /** 等级名称（完全自定义，如"茶友"、"尊享会员"） */
  levelName: string
  /** 等级编码（完全自定义，如VIP1、BRONZE等） */
  levelCode: string
  /** 等级颜色（CSS变量值或色值，用于前端展示） */
  levelColor: string
  /** 等级图标标识（图标库名称，如'star'、'crown'） */
  levelIcon: string

  // === 快捷配置字段（简单模式） ===
  /** 升级所需最低累计消费金额（元，可自定义） */
  minConsumption: string
  /** 折扣率（1.0=无折扣，0.95=95折） */
  discountRate: number
  /** 积分倍率（1.0=标准，1.5=1.5倍积分） */
  pointsRate: number
  /** 生日折扣率（1.0=无折扣，0.90=生日9折） */
  birthdayDiscountRate: number
  /** 生日额外赠送积分 */
  birthdayBonusPoints: number

  // === 等级规则 ===
  /** 等级权益说明（文本摘要，自动生成或手动填写） */
  benefitsDescription: string
  /** 排序序号（从小到大排列） */
  sortOrder: number
  /** 状态：active/inactive */
  status: string

  // === 统计字段 ===
  /** 当前等级会员数 */
  memberCount: number

  // === 高级配置（可选） ===
  /** 升级条件列表（高级模式，为空时使用快捷字段minConsumption） */
  criteria?: LevelCriteria[]
  /** 权益列表（高级模式，为空时使用快捷字段discountRate等） */
  benefits?: LevelBenefit[]

  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
}

// ============================================================
// 升级条件（高级模式）
// ============================================================

/** 等级升级条件 */
export interface LevelCriteria {
  /** 条件ID */
  criteriaId: string
  /** 等级ID */
  levelId: string
  /** 条件类型 */
  criteriaType: CriteriaType
  /** 条件值 */
  criteriaValue: string
  /** 比较运算符 */
  operator: CriteriaOperator
  /** 统计周期（天），0=累计（不按周期） */
  periodDays: number
  /** 逻辑分组（同组AND，跨组OR） */
  logicGroup: number
  /** 排序序号 */
  sortOrder: number
  /** 状态 */
  status: string
}

/** 条件类型（仅作UI展示参考，不限制后端值） */
export type CriteriaType =
  | 'total_consumption'    // 累计消费金额
  | 'period_consumption'   // 周期内消费金额
  | 'total_orders'         // 累计消费次数
  | 'period_orders'        // 周期内消费次数
  | 'total_recharge'       // 累计充值金额
  | 'register_days'        // 注册天数
  | 'referral_count'       // 推荐人数

/** 条件类型选项 */
export const CriteriaTypeOptions: { value: CriteriaType; label: string; unit: string }[] = [
  { value: 'total_consumption', label: '累计消费金额', unit: '元' },
  { value: 'period_consumption', label: '周期内消费金额', unit: '元' },
  { value: 'total_orders', label: '累计消费次数', unit: '次' },
  { value: 'period_orders', label: '周期内消费次数', unit: '次' },
  { value: 'total_recharge', label: '累计充值金额', unit: '元' },
  { value: 'register_days', label: '注册天数', unit: '天' },
  { value: 'referral_count', label: '推荐人数', unit: '人' },
]

/** 比较运算符 */
export type CriteriaOperator = 'GTE' | 'GT' | 'EQ' | 'BETWEEN'

/** 运算符选项 */
export const CriteriaOperatorOptions: { value: CriteriaOperator; label: string }[] = [
  { value: 'GTE', label: '大于等于' },
  { value: 'GT', label: '大于' },
  { value: 'EQ', label: '等于' },
  { value: 'BETWEEN', label: '区间' },
]

// ============================================================
// 等级权益（高级模式）
// ============================================================

/** 等级权益配置 */
export interface LevelBenefit {
  /** 权益ID */
  benefitId: string
  /** 等级ID */
  levelId: string
  /** 权益类型 */
  benefitType: BenefitType
  /** 权益名称（显示用） */
  benefitName: string
  /** 权益值 */
  benefitValue: string
  /** 扩展配置（JSON） */
  benefitConfig: Record<string, unknown>
  /** 排序序号 */
  sortOrder: number
  /** 状态 */
  status: string
}

/** 权益类型 */
export type BenefitType =
  | 'discount'              // 折扣率
  | 'points_rate'           // 积分倍率
  | 'birthday_discount'    // 生日折扣
  | 'birthday_points'      // 生日积分
  | 'birthday_gift'        // 生日礼品
  | 'priority_queue'       // 优先排队
  | 'exclusive_dish'       // 专属菜品
  | 'skip_queue'           // 免排队
  | 'dedicated_service'    // 专属客服
  | 'free_packaging'       // 免费打包
  | 'vip_room'             // VIP包间
  | 'annual_cashback'      // 年度返现
  | 'new_product_first'    // 新品优先
  | 'exclusive_coupon'     // 专属优惠券
  | 'free_parking'         // 免费停车
  | 'free_delivery'        // 免运费
  | 'custom'               // 自定义权益

/** 权益类型选项 */
export const BenefitTypeOptions: { value: BenefitType; label: string; unit: string }[] = [
  { value: 'discount', label: '折扣率', unit: '折' },
  { value: 'points_rate', label: '积分倍率', unit: '倍' },
  { value: 'birthday_discount', label: '生日折扣', unit: '折' },
  { value: 'birthday_points', label: '生日积分', unit: '分' },
  { value: 'birthday_gift', label: '生日礼品', unit: '' },
  { value: 'priority_queue', label: '优先排队', unit: '' },
  { value: 'exclusive_dish', label: '专属菜品', unit: '' },
  { value: 'skip_queue', label: '免排队', unit: '' },
  { value: 'dedicated_service', label: '专属客服', unit: '' },
  { value: 'free_packaging', label: '免费打包', unit: '' },
  { value: 'vip_room', label: 'VIP包间', unit: '' },
  { value: 'annual_cashback', label: '年度返现', unit: '%' },
  { value: 'new_product_first', label: '新品优先', unit: '' },
  { value: 'exclusive_coupon', label: '专属优惠券', unit: '' },
  { value: 'free_parking', label: '免费停车', unit: '小时' },
  { value: 'free_delivery', label: '免运费', unit: '' },
  { value: 'custom', label: '自定义权益', unit: '' },
]

// ============================================================
// 表单与查询
// ============================================================

/** 会员等级创建/编辑表单 */
export interface MemberLevelForm {
  /** 等级名称 */
  levelName: string
  /** 等级编码 */
  levelCode: string
  /** 等级颜色 */
  levelColor: string
  /** 等级图标 */
  levelIcon: string
  /** 升级所需最低消费金额（元） */
  minConsumption: string
  /** 折扣率 */
  discountRate: number
  /** 积分倍率 */
  pointsRate: number
  /** 生日折扣率 */
  birthdayDiscountRate: number
  /** 生日额外积分 */
  birthdayBonusPoints: number
  /** 等级权益说明 */
  benefitsDescription: string
  /** 排序序号 */
  sortOrder: number
  /** 状态 */
  status: string
  /** 升级条件列表（高级模式） */
  criteria: LevelCriteria[]
  /** 权益列表（高级模式） */
  benefits: LevelBenefit[]
}

/** 会员等级查询参数 */
export interface MemberLevelQueryForm {
  /** 等级名称 */
  levelName?: string
  /** 状态 */
  status?: string | null
}

// ============================================================
// 积分重置规则
// ============================================================

/** 积分重置周期 */
export type PointsResetCycle = 'annual' | 'registration_anniversary' | 'rolling_365' | 'never'

/** 积分重置配置 */
export interface PointsResetConfig {
  /** 重置周期 */
  cycle: PointsResetCycle
  /** 重置前提醒天数 */
  reminderDays: number
  /** 提醒方式 */
  reminderChannels: ('sms' | 'wechat')[]
  /** 重置规则 */
  resetRule: 'all' | 'keep_half' | 'keep_rate_based'
  /** 等级是否重置（默认false，只升不降） */
  resetLevel: boolean
}

/** 积分重置周期选项 */
export const PointsResetCycleOptions: { value: PointsResetCycle; label: string }[] = [
  { value: 'annual', label: '每年1月1日' },
  { value: 'registration_anniversary', label: '注册周年日' },
  { value: 'rolling_365', label: '滚动365天' },
  { value: 'never', label: '不重置' },
]

/** 默认积分重置配置 */
export const DefaultPointsResetConfig: PointsResetConfig = {
  cycle: 'annual',
  reminderDays: 30,
  reminderChannels: ['sms', 'wechat'],
  resetRule: 'all',
  resetLevel: false, // 只升不降
}

// ============================================================
// 等级编码建议（仅作参考，不限制自定义）
// ============================================================

/** 等级编码建议（仅作参考，可自定义任意编码） */
export const LevelCodeSuggestions = [
  { label: 'NORMAL', description: '普通会员' },
  { label: 'SILVER', description: '银卡会员' },
  { label: 'GOLD', description: '金卡会员' },
  { label: 'DIAMOND', description: '钻石会员' },
  { label: 'BRONZE', description: '铜卡会员' },
  { label: 'PLATINUM', description: '白金会员' },
  { label: 'VIP1', description: 'VIP一级' },
  { label: 'VIP2', description: 'VIP二级' },
] as const

/** 等级颜色建议（用于前端展示，可自定义） */
export const LevelColorSuggestions = [
  { label: '普通灰', value: 'var(--fts-info)' },
  { label: '银色', value: 'var(--fts-text-secondary)' },
  { label: '金色', value: 'var(--fts-warning)' },
  { label: '钻石蓝', value: 'var(--fts-primary)' },
  { label: '铜色', value: '#CD7F32' },
  { label: '白金', value: 'var(--fts-success)' },
  { label: '紫色', value: '#9C27B0' },
  { label: '自定义', value: '' },
] as const
