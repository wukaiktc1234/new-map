/**
 * 会员相关类型定义（无积分体系版本）
 * 对应后端实体: MarketingMember (members表), MemberLevel (member_level表)
 * 用户偏好支持多端输入：管理端、小程序、APP、POS收银台
 */

// ==================== 会员状态 ====================

/** 会员状态 */
export type MemberStatus = 'active' | 'frozen' | 'blacklisted' | 'cancelled'

/** 性别 */
export type MemberGender = 'unknown' | 'male' | 'female'

/** 注册渠道 */
export type RegisterChannel = 'app' | 'pos' | 'qr' | 'import' | 'mini_program' | 'manual'

/** 客户分层（RFM分析结果） */
export type CustomerSegment = 'champion' | 'loyal' | 'potential' | 'new' | 'at_risk' | 'lost'

// ==================== 用户偏好设定 ====================

/** 饮食偏好 */
export interface FoodPreference {
  /** 辣度偏好: 不吃辣/微辣/中辣/重辣 */
  spiceLevel: string
  /** 口味偏好列表 */
  tasteTags: string[]
  /** 禁忌食材（过敏等） */
  allergies: string[]
  /** 常点菜品分类 */
  favoriteCategories: string[]
}

/** 用餐偏好 */
export interface DiningPreference {
  /** 就餐时段偏好 */
  preferredTimes: string[]
  /** 常用餐位区域 */
  preferredAreas: string[]
  /** 是否需要无烟区 */
  smokeFree: boolean
  /** 是否带儿童 */
  oftenWithChildren: boolean
  /** 人均消费区间 */
  budgetRange: string
}

/** 沟通偏好 */
export interface CommunicationPreference {
  /** 接受短信通知 */
  smsEnabled: boolean
  /** 接受微信通知 */
  wechatEnabled: boolean
  /** 是否接受营销推送 */
  marketingEnabled: boolean
  /** 生日提醒提前天数 */
  birthdayReminderDays: number
}

/** 用户偏好完整数据 */
export interface UserPreferences {
  /** 饮食偏好 */
  food?: FoodPreference
  /** 用餐偏好 */
  dining?: DiningPreference
  /** 沟通偏好 */
  communication?: CommunicationPreference
  /** 数据来源: admin(管理端)/mini_program(小程序)/app(APP)/pos(POS端) */
  source: string
  /** 最后更新时间 */
  updatedAt: string
  /** 最后更新来源 */
  updatedBy: string
}

// ==================== 会员信息 ====================

/** 会员信息 */
export interface MemberInfo {
  /** 主键ID */
  id: string
  /** 会员编号（业务编码） */
  memberNo: string
  /** 手机号 */
  phone: string
  /** 昵称 */
  nickname: string
  /** 头像URL */
  avatarUrl: string
  /** 性别 */
  gender: MemberGender
  /** 生日 */
  birthday: string
  /** 邮箱 */
  email: string
  /** 会员等级ID */
  memberLevelId: string
  /** 等级名称（关联查询） */
  levelName: string
  /** 等级编码 */
  levelCode: string
  /** 余额（元，字符串避免精度丢失） */
  balance: string
  /** 累计充值金额（元） */
  totalRecharge: string
  /** 累计消费金额（元） */
  totalConsume: string
  /** 订单数量 */
  orderCount: number
  /** 最后消费时间 */
  lastOrderTime: string
  /** 最后到店时间 */
  lastVisitTime: string
  /** 注册渠道 */
  registerChannel: RegisterChannel
  /** 状态 */
  status: MemberStatus
  /** 标签数组 */
  tags: string[]
  /** 备注 */
  remark: string
  /** RFM-R最近消费得分 */
  rScore: number
  /** RFM-F消费频率得分 */
  fScore: number
  /** RFM-M消费金额得分 */
  mScore: number
  /** 客户分层 */
  customerSegment: CustomerSegment
  /** 用户偏好 */
  preferences: UserPreferences
  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
}

/** 会员创建表单 */
export interface MemberCreateForm {
  /** 手机号（必填） */
  phone: string
  /** 昵称 */
  nickname: string
  /** 性别 */
  gender: MemberGender
  /** 生日 */
  birthday: string
  /** 邮箱 */
  email: string
  /** 初始等级ID */
  memberLevelId: string
  /** 备注 */
  remark: string
}

/** 会员编辑表单 */
export interface MemberUpdateForm {
  /** 昵称 */
  nickname: string
  /** 性别 */
  gender: MemberGender
  /** 生日 */
  birthday: string
  /** 邮箱 */
  email: string
  /** 手动调整等级ID */
  memberLevelId: string
  /** 标签 */
  tags: string[]
  /** 备注 */
  remark: string
}

/** 会员查询参数 */
export interface MemberQueryForm {
  /** 会员编号 */
  memberNo?: string
  /** 手机号 */
  phone?: string
  /** 昵称（模糊搜索） */
  nickname?: string
  /** 会员等级ID */
  memberLevelId?: string | null
  /** 状态 */
  status?: string | null
  /** 客户分层 */
  customerSegment?: string | null
  /** 注册渠道 */
  registerChannel?: string | null
  /** 标签筛选 */
  tag?: string
  /** 注册开始时间 */
  startCreatedAt?: string
  /** 注册结束时间 */
  endCreatedAt?: string
  /** 余额最小值 */
  minBalance?: string
  /** 余额最大值 */
  maxBalance?: string
  /** 消费金额最小值 */
  minConsume?: string
  /** 消费金额最大值 */
  maxConsume?: string
}

// ==================== 会员统计 ====================

/** 会员概览统计数据 */
export interface MemberStatsOverview {
  /** 会员总数 */
  totalMembers: number
  /** 本月新增 */
  newThisMonth: number
  /** 今日新增 */
  newToday: number
  /** 活跃会员数（30天内有消费） */
  activeMembers: number
  /** 休眠会员数（90天未消费） */
  dormantMembers: number
  /** 流失风险会员数 */
  atRiskCount: number
  /** 总储值余额（元） */
  totalBalance: string
  /** 本月充值总额（元） */
  rechargeThisMonth: string
  /** 本月消费总额（元） */
  consumeThisMonth: string
  /** 会员消费占比 */
  consumeRatio: string
  /** 平均客单价 */
  avgOrderAmount: string
  /** 复购率 */
  repurchaseRate: string
  /** 各等级分布 */
  levelDistribution: { levelName: string; count: number; percentage: number }[]
  /** 各渠道注册占比 */
  channelDistribution: { channelName: string; count: number; percentage: number }[]
  /** 近7天新增趋势 */
  weeklyGrowthTrend: { date: string; count: number }[]
  /** 近6个月消费趋势 */
  monthlyConsumeTrend: { month: string; amount: string; orderCount: number }[]
  /** 客户分层分布 */
  segmentDistribution: { segment: string; segmentName: string; count: number; percentage: number }[]
}

// ==================== 会员消费记录 ====================

/** 会员消费记录 */
export interface MemberConsumeRecord {
  /** 记录ID */
  id: string
  /** 会员ID */
  memberId: string
  /** 会员昵称 */
  memberNickname: string
  /** 订单号 */
  orderNo: string
  /** 消费金额（元） */
  amount: string
  /** 支付方式 */
  paymentMethod: string
  /** 使用余额抵扣（元） */
  balanceDeducted: string
  /** 消费门店 */
  storeName: string
  /** 消费时间 */
  consumeTime: string
  /** 操作人 */
  operatorName: string
}

// ==================== 会员储值记录 ====================

/** 会员储值记录 */
export interface MemberRechargeRecord {
  /** 记录ID */
  recordId: string
  /** 充值流水号 */
  recordNo: string
  /** 会员ID */
  memberId: string
  /** 会员手机号 */
  memberPhone: string
  /** 充值方案名称 */
  planName: string
  /** 实充金额（元） */
  rechargeAmount: string
  /** 赠送金额（元） */
  bonusAmount: string
  /** 支付方式 */
  paymentMethod: string
  /** 支付状态 */
  paymentStatus: RechargePaymentStatus
  /** 操作人 */
  operateUserName: string
  /** 充值时间 */
  createTime: string
  /** 备注 */
  remark: string
}

/** 储值支付状态 */
export type RechargePaymentStatus = 'pending' | 'paid' | 'refunded' | 'failed'

// ==================== 常量选项 ====================

/** 会员性别选项 */
export const MemberGenderOptions = [
  { label: '未知', value: 'unknown' },
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
] as const

/** 会员性别文本映射 */
export const MemberGenderText: Record<string, string> = {
  unknown: '未知',
  male: '男',
  female: '女',
}

/** 会员状态选项 */
export const MemberStatusOptions = [
  { label: '正常', value: 'active' },
  { label: '冻结', value: 'frozen' },
  { label: '黑名单', value: 'blacklisted' },
  { label: '注销', value: 'cancelled' },
] as const

/** 会员状态文本映射 */
export const MemberStatusText: Record<string, string> = {
  active: '正常',
  frozen: '冻结',
  blacklisted: '黑名单',
  cancelled: '注销',
}

/** 注册渠道选项 */
export const RegisterChannelOptions = [
  { label: 'APP注册', value: 'app' },
  { label: '收银台注册', value: 'pos' },
  { label: '扫码注册', value: 'qr' },
  { label: '批量导入', value: 'import' },
  { label: '小程序注册', value: 'mini_program' },
  { label: '手动录入', value: 'manual' },
] as const

/** 注册渠道文本映射 */
export const RegisterChannelText: Record<string, string> = {
  app: 'APP注册',
  pos: '收银台注册',
  qr: '扫码注册',
  import: '批量导入',
  mini_program: '小程序注册',
  manual: '手动录入',
}

/** 客户分层选项 */
export const CustomerSegmentOptions = [
  { label: '高价值活跃客户', value: 'champion' },
  { label: '忠诚客户', value: 'loyal' },
  { label: '潜力客户', value: 'potential' },
  { label: '新客户', value: 'new' },
  { label: '流失风险', value: 'at_risk' },
  { label: '已流失', value: 'lost' },
] as const

/** 客户分层文本映射 */
export const CustomerSegmentText: Record<string, string> = {
  champion: '高价值活跃',
  loyal: '忠诚客户',
  potential: '潜力客户',
  new: '新客户',
  at_risk: '流失风险',
  lost: '已流失',
}

/** 客户分层颜色映射（用于StatusTag） */
export const CustomerSegmentStatusMap: Record<string, string> = {
  champion: 'primary',
  loyal: 'success',
  potential: 'warning',
  new: 'info',
  at_risk: 'error',
  lost: 'inactive',
}

/** 支付方式选项 */
export const PaymentMethodOptions = [
  { label: '微信支付', value: 'wechat' },
  { label: '支付宝', value: 'alipay' },
  { label: '现金', value: 'cash' },
  { label: '银行卡', value: 'bank_card' },
  { label: '余额支付', value: 'balance' },
] as const

/** 支付方式文本映射 */
export const PaymentMethodText: Record<string, string> = {
  wechat: '微信支付',
  alipay: '支付宝',
  cash: '现金',
  bank_card: '银行卡',
  balance: '余额支付',
}
