/**
 * 订单管理 API 模块 - 统一导出
 *
 * 导出所有订单管理相关的 API 和数据转换器：
 * - orderApi：订单查询/管理
 * - orderStatisticsApi：订单统计/趋势
 * - orderRefundApi：退款管理
 * - reservationApi：预约管理
 * - orderDataConverter：数据转换器
 */

// 订单查询/管理
export { orderApi } from './order'

// 订单统计/趋势
export { orderStatisticsApi } from './statistics'
export type { TrendQueryForm, DailyStatsQueryForm, TopFoodsQueryForm } from './statistics'

// 退款管理
export { orderRefundApi } from './refund'

// 预约管理
export { reservationApi } from './reservation'

// 数据转换器
export { orderDataConverter } from './converters'
