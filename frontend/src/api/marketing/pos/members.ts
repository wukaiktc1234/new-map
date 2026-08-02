/**
 * POS会员管理API
 * 对应后端: /v1/pos/members
 */
import { get, post } from '../../request'

export const posMembersApi = {
  /** POS端查询会员（按手机号快速检索） */
  search(phone: string) {
    return get<unknown[]>('/v1/pos/members/search', { phone })
  },

  /** POS端获取会员详情 */
  getById(id: number) {
    return get<unknown>('/v1/pos/members/' + id)
  },

  /** POS端快速注册会员 */
  quickRegister(data: { phone: string; name?: string }) {
    return post<unknown>('/v1/pos/members/quick-register', data)
  },

  /** POS端获取会员积分 */
  getPoints(memberId: number) {
    return get<number>('/v1/pos/members/' + memberId + '/points')
  },

  /** POS端获取会员可用优惠券 */
  getCoupons(memberId: number) {
    return get<unknown[]>('/v1/pos/members/' + memberId + '/coupons')
  },

  /** POS端会员消费积分结账 */
  checkout(data: { memberId: number; orderId: number; usePoints?: number; couponId?: number }) {
    return post<unknown>('/v1/pos/members/checkout', data)
  },
}
