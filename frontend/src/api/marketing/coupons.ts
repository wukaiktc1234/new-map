/**
 * 优惠券管理API
 * 对应后端: /v1/coupons
 *
 * 使用标准 get/post/put 请求，错误由 request 拦截器统一处理。
 */
import { get, post, put } from '../request'

export const couponsApi = {
  /** 获取优惠券模板列表 */
  getTemplates(params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/coupons/templates', params)
  },

  /** 创建优惠券模板 */
  createTemplate(data: Record<string, unknown>) {
    return post<unknown>('/v1/coupons/templates', data)
  },

  /** 更新优惠券模板 */
  updateTemplate(id: number, data: Record<string, unknown>) {
    return put('/v1/coupons/templates/' + id, data)
  },

  /** 发放优惠券 */
  issueCoupon(data: Record<string, unknown>) {
    return post<unknown>('/v1/coupons/issue', data)
  },

  /** 批量发放 */
  batchIssue(data: Record<string, unknown>) {
    return post<unknown>('/v1/coupons/batch-issue', data)
  },

  /** 查询会员持有的优惠券 */
  getMemberCoupons(memberId: number, params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/coupons/member/' + memberId, params)
  },

  /** 核销优惠券 */
  redeem(couponId: number, data: Record<string, unknown>) {
    return post<unknown>('/v1/coupons/' + couponId + '/redeem', data)
  },
}
