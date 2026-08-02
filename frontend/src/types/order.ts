/**
 * 订单管理模块类型定义
 *
 * 对应后端：
 * - OrderNewController (/v1/orders)
 * - OrderTrendsController (/v1/orders/trends)
 * - TableReservationNewController (/v1/reservations)
 *
 * 金额字段：后端以分为单位（Long/number），前端以元为单位（string）
 * 状态字段：后端为数字编码，前端为语义化字符串
 */

// ============================================================
// 通用分页响应
// ============================================================

/** 分页响应结构 */
export interface PageResponse<T> {
  /** 数据列表 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
  /** 总页数 */
  pages: number
}

// ============================================================
// 订单状态、类型、支付状态
// ============================================================

/**
 * 订单状态（前端语义化字符串）
 * 后端编码：0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款 6待评价
 */
export type OrderStatusValue =
  | 'pending'          // 0 待确认
  | 'confirmed'        // 1 已确认
  | 'completed'        // 2 已完成
  | 'cancelled'        // 3 已取消
  | 'partial_refund'   // 4 部分退款
  | 'full_refund'      // 5 全额退款
  | 'pending_review'   // 6 待评价

/**
 * 订单类型
 * 1堂食 2外卖 3自提 4打包
 */
export type OrderTypeValue = 1 | 2 | 3 | 4

/**
 * 支付状态
 * 0未支付 1部分支付 2已支付 3已退款
 */
export type PaymentStatusValue = 0 | 1 | 2 | 3

// ============================================================
// 订单 - 后端原始类型（金额为 number/分）
// ============================================================

/** 订单明细项（后端） */
export interface OrderItemBackend {
  itemId: number
  productName: string
  /** 单价（分） */
  unitPrice: number
  quantity: number
  /** 小计金额（分） */
  amount: number
  kitchenStatusName?: string
}

/** 订单支付记录（后端） */
export interface OrderPaymentBackend {
  paymentMethodName: string
  /** 支付金额（分） */
  paymentAmount: number
  paymentTime: string
  operatorName: string
}

/** 订单退款记录（后端） */
export interface OrderRefundRecordBackend {
  refundId: number
  /** 退款金额（分） */
  refundAmount: number
  refundReason: string
  refundStatusName: string
  approveUserName: string
  createTime: string
  completeTime: string
}

/** 订单后端原始类型（对应 OrderVO） */
export interface OrderBackend {
  orderId: number
  orderCode: string
  orderType: number
  orderTypeName: string
  /** 门店ID（POS订单关联门店） */
  storeId?: string
  /** 门店名称 */
  storeName?: string
  customerId: number
  customerName: string
  customerPhone: string
  tableId: number
  tableName: string
  diningPeopleCount: number
  orderStatus: number
  orderStatusName: string
  paymentStatus: number
  paymentStatusName: string
  /** 订单总金额（分） */
  totalAmount: number
  /** 优惠金额（分） */
  discountAmount: number
  /** 应收金额（分） */
  finalAmount: number
  /** 已支付金额（分） */
  paidAmount: number
  /** 已退款金额（分） */
  refundAmount: number
  remark: string
  cancelReason: string
  cashierUserName: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createTime: string
  updateTime: string
  items?: OrderItemBackend[]
  payments?: OrderPaymentBackend[]
  refunds?: OrderRefundRecordBackend[]
}

// ============================================================
// 订单 - 前端展示类型（金额为 string/元，状态为语义化字符串）
// ============================================================

/** 订单明细项（前端） */
export interface OrderItem {
  itemId: number
  productName: string
  /** 单价（元） */
  unitPrice: string
  quantity: number
  /** 小计金额（元） */
  amount: string
  kitchenStatusName?: string
}

/** 订单支付记录（前端） */
export interface OrderPayment {
  paymentMethodName: string
  /** 支付金额（元） */
  paymentAmount: string
  paymentTime: string
  operatorName: string
}

/** 订单退款记录（前端） */
export interface OrderRefundRecord {
  refundId: number
  /** 退款金额（元） */
  refundAmount: string
  refundReason: string
  refundStatusName: string
  approveUserName: string
  createTime: string
  completeTime: string
}

/** 订单前端展示类型 */
export interface Order {
  orderId: number
  orderCode: string
  orderType: number
  orderTypeName: string
  /** 门店ID（POS订单关联门店） */
  storeId?: string
  /** 门店名称 */
  storeName?: string
  customerId: number
  customerName: string
  customerPhone: string
  tableId: number
  tableName: string
  diningPeopleCount: number
  orderStatus: OrderStatusValue
  orderStatusName: string
  paymentStatus: PaymentStatusValue
  paymentStatusName: string
  /** 订单总金额（元） */
  totalAmount: string
  /** 优惠金额（元） */
  discountAmount: string
  /** 应收金额（元） */
  finalAmount: string
  /** 已支付金额（元） */
  paidAmount: string
  /** 已退款金额（元） */
  refundAmount: string
  remark: string
  cancelReason: string
  cashierUserName: string
  createTime: string
  updateTime: string
  items?: OrderItem[]
  payments?: OrderPayment[]
  refunds?: OrderRefundRecord[]
}

/** 订单详情（含明细、支付、退款列表） */
export interface OrderDetail extends Order {}

/** 订单查询表单 */
export interface OrderQueryForm {
  page?: number
  size?: number
  orderCode?: string
  orderType?: number
  orderStatus?: OrderStatusValue | null
  paymentStatus?: number
  customerName?: string
  customerPhone?: string
  startTime?: string
  endTime?: string
  /** 最小金额（元） */
  minAmount?: string
  /** 最大金额（元） */
  maxAmount?: string
  /** 日期范围（前端搜索表单使用） */
  dateRange?: string[]
  /** 门店ID（用于按门店筛选订单） */
  storeId?: string
  /** 支付方式（数字编码） */
  paymentMethod?: number
}

// ============================================================
// 今日销售统计
// ============================================================

/** 今日销售统计后端类型（TodayStatisticsVO） */
export interface OrderStatsBackend {
  totalOrders: number
  completedOrders: number
  cancelledOrders: number
  /** 总销售额（分） */
  totalSalesAmount: number
  /** 实收金额（分） */
  actualReceivedAmount: number
  /** 退款金额（分） */
  refundAmount: number
  orderTypeDistribution?: Record<number, number>
  paymentMethodDistribution?: Record<number, number>
  hourlyStats?: Array<{ hour: number; orderCount: number; amount: number }>
  topProducts?: Array<{ productName: string; totalQuantity: number; totalAmount: number }>
}

/** 今日销售统计前端类型（金额为元/字符串） */
export interface OrderStats {
  /** 今日订单数 */
  todayOrders: number
  /** 今日营业额（元） */
  todayRevenue: string
  /** 待处理订单数 */
  pendingOrders: number
  /** 平均每单金额（元） */
  avgOrderAmount: string
  /** 已完成订单数 */
  completedOrders: number
}

// ============================================================
// 订单趋势
// ============================================================

/**
 * 订单趋势数据（后端返回 Map<String, Object>）
 *
 * 后端 OrderNewServiceImpl.getOrderTrends 返回字段：
 * - timeSeries: 按天聚合的时间序列（date/orderCount/sales）
 * - summary: 总览统计（totalOrders/totalSales/averageOrderValue/totalCustomers）
 * - comparison: 同比环比（orderCountChange/salesChange/averageOrderValueChange）
 * - trendAnalysis: 趋势分析（trend/growthRate/peakDay/peakHour）
 * - orderTypeDistribution: 订单类型占比（堂食/外卖/自提/打包 → 百分比数字）
 */
export interface OrderTrendData {
  timeSeries?: unknown
  summary?: unknown
  comparison?: unknown
  trendAnalysis?: unknown
  /** 订单类型分布（key 为中文类型名，value 为百分比数字） */
  orderTypeDistribution?: Record<string, number>
  [key: string]: unknown
}

// ============================================================
// 销售分析（对应 SalesAnalysisController /v1/analytics/sales）
// ============================================================

/**
 * 品类销售占比项（后端 /category-analysis 返回的 categoryData 数组项）
 *
 * 注意：后端 SalesAnalysisServiceImpl.getCategorySalesAnalysis 当前为 TODO，
 *      字段名基于业务约定，后端实现后可能需要调整映射。
 */
export interface CategorySalesItem {
  /** 品类名称 */
  categoryName?: string
  /** 品类ID */
  categoryId?: string | number
  /** 销售额（分） */
  salesAmount?: number
  /** 订单数 */
  orderCount?: number
  /** 占比（百分比数字） */
  ratio?: number
  [key: string]: unknown
}

/**
 * 品类销售占比响应（后端 /category-analysis 返回 Map<String, Object>）
 *
 * 后端当前返回 { categoryData: [] }（TODO 未实现）
 */
export interface CategorySalesAnalysis {
  /** 品类数据列表 */
  categoryData?: CategorySalesItem[]
  [key: string]: unknown
}

/**
 * 热销菜品项（后端 /top-foods 返回的 List<Map<String, Object>> 数组项）
 *
 * 注意：后端 SalesAnalysisServiceImpl.getTopSellingFoods 当前为 TODO，
 *      字段名基于业务约定（foodName/totalQuantity/totalAmount），后端实现后可能需要调整。
 */
export interface TopSellingFood {
  /** 菜品ID */
  foodId?: string | number
  /** 菜品名称 */
  foodName?: string
  /** 菜品名称（备选字段） */
  productName?: string
  /** 销售数量 */
  totalQuantity?: number
  /** 销售数量（备选字段） */
  quantity?: number
  /** 销售额（分） */
  totalAmount?: number
  /** 销售额（备选字段，分） */
  amount?: number
  [key: string]: unknown
}

/**
 * 时段分布项（后端 /hourly-distribution 返回的 hourlyData 数组项）
 *
 * 后端 SalesAnalysisServiceImpl.getHourlyDistribution 已实现基础结构，
 * 返回 24 小时的 hour/orderCount/amount（当前全为 0，TODO 待接入真实统计）。
 */
export interface HourlyDistributionItem {
  /** 小时（0-23） */
  hour: number
  /** 订单数 */
  orderCount: number
  /** 金额（分） */
  amount: number
}

/**
 * 时段分布响应（后端 /hourly-distribution 返回 Map<String, Object>）
 */
export interface HourlyDistribution {
  /** 24 小时数据列表 */
  hourlyData?: HourlyDistributionItem[]
  /** 日期（yyyy-MM-dd） */
  date?: string
  [key: string]: unknown
}

// ============================================================
// 按日期分组统计
// ============================================================

/** 按日期分组统计后端类型（DailyStatsVO，金额为分） */
export interface DailyStatsBackend {
  /** 日期（yyyy-MM-dd） */
  date: string
  storeName: string
  orderCount: number
  /** 营业额（分） */
  revenue: number
  /** 成本（分） */
  cost: number
  /** 利润（分） */
  profit: number
  /** 利润率 */
  profitRate: number
}

/** 按日期分组统计前端类型（金额为元/字符串） */
export interface DailyStats {
  /** 日期（yyyy-MM-dd） */
  date: string
  storeName: string
  orderCount: number
  /** 营业额（元） */
  revenue: string
  /** 成本（元） */
  cost: string
  /** 利润（元） */
  profit: string
  /** 利润率 */
  profitRate: number
}

// ============================================================
// 退款管理
// ============================================================

/**
 * 退款状态（前端语义化字符串）
 * 后端编码：1待审核 2已通过 3已拒绝 4已执行
 */
export type RefundStatusValue = 'pending' | 'approved' | 'rejected' | 'executed'

/** 退款列表后端类型（OrderRefundListVO，金额为分） */
export interface OrderRefundBackend {
  refundId: number
  refundNo: string
  orderId: number
  orderCode: string
  /** 退款金额（分） */
  refundAmount: number
  refundReason: string
  refundStatus: number
  refundStatusName: string
  applyUserName: string
  approveUserName: string
  rejectReason: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createTime: string
  completeTime: string
}

/** 退款列表前端类型（金额为元/字符串） */
export interface OrderRefund {
  refundId: number
  refundNo: string
  orderId: number
  orderCode: string
  /** 退款金额（元） */
  refundAmount: string
  refundReason: string
  refundStatus: RefundStatusValue
  refundStatusName: string
  applyUserName: string
  approveUserName: string
  rejectReason: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createTime: string
  completeTime: string
}

/** 退款查询表单 */
export interface OrderRefundQueryForm {
  page?: number
  size?: number
  refundNo?: string
  orderCode?: string
  refundStatus?: RefundStatusValue | null
  startTime?: string
  endTime?: string
  /** 日期范围（前端搜索表单使用） */
  dateRange?: string[]
}

/** 退款统计后端类型（OrderRefundStatsVO，金额为分） */
export interface OrderRefundStatsBackend {
  pendingCount: number
  /** 已退款金额（分） */
  refundedAmount: number
  refundRate: number
  avgProcessHours: number
}

/** 退款统计前端类型（金额为元/字符串） */
export interface OrderRefundStats {
  /** 待处理退款数 */
  pendingCount: number
  /** 已退款金额（元） */
  refundedAmount: string
  /** 退款率 */
  refundRate: number
  /** 平均处理时长（小时） */
  avgProcessHours: number
}

// ============================================================
// POS 订单支付
// 对应后端：
// - PosOrderController (/v1/pos/orders)
// - PayRequestDTO（请求）
// - OrderResultDTO（响应）
//
// 注意：本组接口走 PosOrderController，与 OrderNewController (/v1/orders)
// 是不同的后端控制器，返回结构（OrderResultDTO）也与 OrderVO 不同。
// ============================================================

/**
 * 支付方式（前端语义化字符串）
 *
 * 后端 PayRequestDTO.paymentMethod 通过正则同时接受中英文：
 * 微信支付/wechat、支付宝/alipay、现金/cash、银行卡/card、余额支付/balance
 *
 * 前端统一使用英文语义化字符串提交，无需 DataConverter 转换。
 */
export type PaymentMethod = 'wechat' | 'alipay' | 'cash' | 'card' | 'balance'

/**
 * 支付请求（对应后端 PayRequestDTO）
 *
 * 字段说明：
 * - orderId：后端为 String 类型（雪花算法生成的字符串 ID，非 Long）
 * - amount：后端为 BigDecimal（元，非分），JSON 序列化为 number；
 *           取值范围 0.01 ~ 999999.00；为 0 或不传时后端按订单全额支付
 * - openid：仅微信/支付宝等需要用户身份的支付方式使用，可选
 */
export interface PayRequest {
  /** 订单ID（字符串） */
  orderId: string
  /** 支付方式（前端语义化字符串） */
  paymentMethod: PaymentMethod
  /** 支付金额（元，与后端 BigDecimal 对齐；不传或为 0 表示按订单全额支付） */
  amount: number
  /** 用户 openid（微信/支付宝等场景可选） */
  openid?: string
}

/**
 * POS 订单项信息（对应后端 OrderResultDTO.OrderItemInfo）
 */
export interface PayOrderItem {
  /** 商品ID */
  id: string
  /** 商品名称 */
  name: string
  /** 单价（元，后端 BigDecimal 序列化为 number） */
  price: number
  /** 数量 */
  quantity: number
}

/**
 * 支付响应（对应后端 OrderResultDTO）
 *
 * 注意：后端 PosOrderController.payOrder 返回 Result<OrderResultDTO>，
 * 响应拦截器已自动提取 data 字段，前端直接拿到 OrderResultDTO。
 *
 * 该结构与 OrderVO 不同：
 * - 金额字段为 number（BigDecimal 序列化），不是 string
 * - status/message 为字符串描述，不是数字编码
 */
export interface PayResponse {
  /** 订单ID */
  orderId: string
  /** 订单编号 */
  orderNumber: string
  /** 取餐号 */
  pickupNumber: string
  /** 取餐码（外卖使用） */
  pickupCode: string
  /** 状态描述 */
  status: string
  /** 消息 */
  message: string
  /** 订单类型 */
  orderType: string
  /** 桌号 */
  tableNumber: string
  /** 订单金额（元） */
  totalAmount: number
  /** 创建时间 */
  createTime: string
  /** 订单来源（0-APP/1-支付宝/2-现金/3-银行卡/4-POS终端） */
  orderSource?: number
  /** 订单项列表 */
  orderItems?: PayOrderItem[]
}

// ============================================================
// 预约管理
// ============================================================

/**
 * 预约状态（前端语义化字符串）
 * 后端编码：1待确认 2已确认 3已到店 4已取消 5未到
 */
export type ReservationStatusValue = 'pending' | 'confirmed' | 'arrived' | 'cancelled' | 'no_show'

/** 预约后端类型（TableReservationNew 实体，金额为分） */
export interface ReservationBackend {
  reservationId: number
  reservationCode: string
  customerName: string
  customerPhone: string
  tableId: number
  /** 门店ID */
  storeId?: number
  /** 预约日期（yyyy-MM-dd） */
  reservationDate: string
  /** 预约时间（HH:mm:ss） */
  reservationTime: string
  peopleCount: number
  /** 定金金额（分） */
  depositAmount: number
  /** 备注/特殊要求 */
  remark?: string
  status: number
  createTime: string
  confirmTime: string
  arriveTime: string
  cancelTime: string
}

/** 预约前端类型（金额为元/字符串，状态为语义化字符串） */
export interface Reservation {
  reservationId: number
  reservationCode: string
  customerName: string
  customerPhone: string
  tableId: number
  /** 门店ID */
  storeId?: number
  /** 预约日期（yyyy-MM-dd） */
  reservationDate: string
  /** 预约时间（HH:mm:ss） */
  reservationTime: string
  peopleCount: number
  /** 定金金额（元） */
  depositAmount: string
  /** 备注/特殊要求 */
  remark?: string
  status: ReservationStatusValue
  createTime: string
  confirmTime: string
  arriveTime: string
  cancelTime: string
}

/** 预约查询表单 */
export interface ReservationQueryForm {
  page?: number
  size?: number
  status?: ReservationStatusValue | null
  /** 关键词搜索（预约人/手机号） */
  keyword?: string
  /** 预约日期（yyyy-MM-dd） */
  date?: string
  /** 结束日期（yyyy-MM-dd） */
  dateEnd?: string
}

/** 预约统计（ReservationStatsVO） */
export interface ReservationStats {
  /** 今日预约数 */
  todayReservations: number
  /** 待确认数 */
  pendingConfirm: number
  /** 已到店数 */
  arrived: number
  /** 取消率 */
  cancelRate: number
  /** 今日已确认数 */
  confirmed: number
  /** 今日已取消数 */
  cancelled: number
}
