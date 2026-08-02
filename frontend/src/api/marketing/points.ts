/**
 * 积分管理API
 * 对应后端: /v1/points
 */
import { get, post, put } from '../request'

/** 积分变动 DTO（管理员调整积分） */
export interface PointsChangeDTO {
  memberId: number
  points: number
  type: string
  reason?: string
  operatorId?: string
}

export const pointsApi = {
  /** 消费获得积分 */
  earnFromConsume(data: { memberId: number; amount: number; scene?: string; orderNo?: string }) {
    return post<number>('/v1/points/consume/earn', data)
  },

  /** 签到获取积分 */
  checkin(memberId: number) {
    return post<number>('/v1/points/checkin', { memberId })
  },

  /** 使用积分抵扣 */
  deduct(data: { memberId: number; points: number; orderAmount: number; orderNo?: string }) {
    return post<Record<string, unknown>>('/v1/points/deduct', data)
  },

  /** 管理员调整积分 */
  adjust(data: PointsChangeDTO) {
    return post<number>('/v1/points/adjust', data)
  },

  /** 查询积分变动明细 */
  getLog(memberId: number, current = 1, size = 20) {
    return get<unknown[]>(`/v1/points/${memberId}/log`, { current, size })
  },

  /** 计算最大可抵扣积分 */
  maxDeductible(memberId: number, orderAmount: number) {
    return get<number>(`/v1/points/${memberId}/max-deductible`, { orderAmount })
  },

  /** 获取积分规则配置 */
  getConfig() {
    return get<Record<string, string>>('/v1/points/config')
  },

  /** 更新积分规则配置 */
  updateConfig(ruleKey: string, ruleValue: string) {
    return put('/v1/points/config/' + ruleKey, { ruleValue })
  },

  /** 手动触发过期积分清理 */
  expirePoints() {
    return post<number>('/v1/points/expire', {})
  },
}
