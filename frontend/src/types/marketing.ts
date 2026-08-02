/**
 * 营销模块相关类型定义
 */

/** 促销活动列表项 */
export interface PromotionItem {
  /** 活动ID */
  id: number | string
  /** 活动名称 */
  name: string
  /** 活动类型 (discount=满减, percentage=折扣, gift=赠品, combo=套餐) */
  type: string
  /** 状态 (active=进行中, pending=未开始, ended=已结束) */
  status: string
  /** 开始时间 */
  startTime: string
  /** 结束时间 */
  endTime: string
}

/** 积分记录项 */
export interface PointsRecord {
  /** 记录ID */
  id: number
  /** 会员姓名 */
  memberName: string
  /** 类型 (earn=获得, consume=消耗) */
  type: 'earn' | 'consume'
  /** 积分数值 */
  points: number
  /** 来源/用途描述 */
  sourceDesc: string
  /** 操作后余额 */
  balanceAfter: number
  /** 时间 */
  createTime: string
  /** 操作人 */
  operatorName: string
}

/** 积分统计数据 */
export interface PointsStats {
  totalPoints: number
  issuedToday: number
  consumedToday: number
  activeMembers: number
}

/** 会员列表项 */
export interface MemberItem {
  /** 会员ID */
  id: number | string
  /** 姓名 */
  name: string
  /** 手机号 */
  phone: string
  /** 等级名称 */
  levelName: string
  /** 积分 */
  totalPoints: number
  /** 累计消费(分) */
  totalConsumption: number
  /** 订单数 */
  orderCount: number
}

/** 会员统计数据 */
export interface MemberStats {
  totalMembers: number
  newToday: number
  avgConsumption: number
}
