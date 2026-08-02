import request from './request';
import type { HeldOrder } from '@/stores/hanging-order';

export interface Category {
  categoryId: string;
  categoryName: string;
  sortOrder: number;
}

export interface Dish {
  dishId: string;
  dishCode: string;
  dishName: string;
  categoryId: string;
  categoryName: string;
  price: number;
  description: string;
  imageUrl: string;
  dishType: string;
  /** 销售数量，用于判断热销商品 */
  salesCount?: number;
  /** 库存数量（来自后端 FoodVO.stock），0 表示售罄 */
  stock?: number;
}

export interface ComboItem {
  foodId: string;
  foodName: string;
  quantity: number;
  price: number;
}

export interface Combo {
  comboId: string;
  comboCode: string;
  comboName: string;
  price: number;
  description: string;
  imageUrl: string;
  comboType: string;
  peopleCount: number;
  dishType: string;
  items: ComboItem[];
  /** 销售数量，用于判断热销商品 */
  salesCount?: number;
}

export interface Menu {
  categories: Category[];
  dishes: Dish[];
  combos: Combo[];
}

// ============================================================
// 产品中心后端数据类型（金额单位：分，与后端 FoodVO/ComboVO/CategoryVO 对应）
// ============================================================

/** 后端菜品 VO（对应 /v1/product-center/foods/on-sale 返回数据） */
interface FoodVO {
  foodId: number;
  foodCode: string;
  foodName: string;
  categoryId: number;
  categoryName: string;
  specification: string;
  unit: string;
  /** 售价（分） */
  salePrice: number;
  costPrice: number;
  profit: number;
  profitRate: number;
  stock: number;
  minStock: number;
  imageUrl: string;
  description: string;
  cookingTime: number;
  /** 状态：1=在售, 0=停售, 2=售罄 */
  status: number;
  statusName: string;
  isRecommend: boolean;
  isSpicy: boolean;
  sortOrder: number;
}

/** 后端套餐明细 VO */
interface ComboIngredientVO {
  ingredientId: number;
  comboId: number;
  foodId: number;
  foodName: string;
  quantity: number;
  unit: string;
  isRequired: boolean;
  maxSelect: number;
  sortOrder: number;
}

/** 后端套餐 VO（对应 /v1/product-center/combos/on-sale 返回数据） */
interface ComboVO {
  comboId: number;
  comboCode: string;
  comboName: string;
  /** 套餐价（分） */
  comboPrice: number;
  originalPrice: number;
  discountAmount: number;
  imageUrl: string;
  description: string;
  validStartDate: string;
  validEndDate: string;
  dailyLimit: number;
  soldToday: number;
  /** 状态：1=在售, 0=停售 */
  status: number;
  statusName: string;
  sortOrder: number;
  ingredients: ComboIngredientVO[];
}

/** 后端分类 VO（对应 /v1/product-center/categories/tree/enabled 返回数据） */
interface CategoryVO {
  categoryId: number;
  categoryName: string;
  parentId: number;
  iconUrl: string;
  sortOrder: number;
  status: number;
  foodCount: number;
  children: CategoryVO[];
}

// ============================================================
// 金额转换工具（后端分 → 前端元）
// ============================================================

/** 将分转换为元（number） */
function fenToYuanNumber(fen: number | null | undefined): number {
  if (fen == null) return 0;
  return Number((fen / 100).toFixed(2));
}

// ============================================================
// 产品中心数据转换（后端 VO → POS 端 Dish/Combo/Category）
// ============================================================

/** 将后端 FoodVO 转换为 POS 端 Dish */
function foodVOToDish(vo: FoodVO): Dish {
  return {
    dishId: String(vo.foodId),
    dishCode: vo.foodCode || '',
    dishName: vo.foodName || '',
    categoryId: String(vo.categoryId),
    categoryName: vo.categoryName || '',
    price: fenToYuanNumber(vo.salePrice),
    description: vo.description || '',
    imageUrl: vo.imageUrl || '',
    dishType: 'single',
    salesCount: vo.sortOrder || 0,
    // 使用后端真实库存，未返回时默认 999（兼容旧逻辑）
    stock: typeof vo.stock === 'number' ? vo.stock : 999,
  };
}

/** 将后端 ComboVO 转换为 POS 端 Combo */
function comboVOToCombo(vo: ComboVO): Combo {
  return {
    comboId: String(vo.comboId),
    comboCode: vo.comboCode || '',
    comboName: vo.comboName || '',
    price: fenToYuanNumber(vo.comboPrice),
    description: vo.description || '',
    imageUrl: vo.imageUrl || '',
    comboType: 'value',
    peopleCount: 1,
    dishType: 'combo',
    items: (vo.ingredients || []).map(ing => ({
      foodId: String(ing.foodId),
      foodName: ing.foodName || '',
      quantity: ing.quantity || 1,
      price: fenToYuanNumber(vo.comboPrice),
    })),
    salesCount: vo.soldToday || 0,
  };
}

/** 递归将后端 CategoryVO 树扁平化为 POS 端 Category 列表 */
function categoryVOTreeToList(tree: CategoryVO[]): Category[] {
  const result: Category[] = [];
  const walk = (nodes: CategoryVO[]) => {
    for (const node of nodes) {
      result.push({
        categoryId: String(node.categoryId),
        categoryName: node.categoryName || '',
        sortOrder: node.sortOrder || 0,
      });
      if (node.children && node.children.length > 0) {
        walk(node.children);
      }
    }
  };
  walk(tree);
  return result;
}

export interface OrderItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  dishType: string;
  items?: { foodId: string; foodName: string; quantity: number }[];
}

export interface OrderRequest {
  tableNumber?: number;
  items: OrderItem[];
  totalAmount: number;
  paymentMethod?: string;
  orderType?: string;
  remark?: string;
}

export interface PayRequest {
  orderId: string;
  /** 支付方式：微信支付/支付宝/现金/余额支付/银行卡 */
  paymentMethod: string;
  openid?: string;
  /** 支付金额（元），后端必填且大于0 */
  amount: number;
}

/** 退款请求参数 */
export interface RefundRequest {
  /** 订单号或订单ID */
  orderNumber: string;
  /** 退款原因 */
  refundReason?: string;
}

/** 退款响应结果 */
export interface RefundResult {
  /** 是否成功 */
  success: boolean;
  /** 退款单号 */
  refundId?: string;
  /** 退款金额 */
  refundAmount?: number;
  /** 消息 */
  message?: string;
}

/** 券码验证请求 */
export interface CouponVerifyRequest {
  /** 券码 */
  code: string;
}

/** 券码验证响应（与后端 PosCouponVerifyResponse 对应） */
export interface CouponVerifyResult {
  /** 是否有效 */
  valid: boolean;
  /** 优惠券编码 */
  code?: string;
  /** 优惠券名称 */
  name?: string;
  /** 折扣描述 */
  discount?: string;
  /** 折扣金额 */
  discountAmount?: number;
  /** 最低订单金额 */
  minAmount?: number;
  /** 有效期描述 */
  validPeriod?: string;
  /** 提示消息 */
  message?: string;
}

/** 券码应用/核销请求 */
export interface CouponApplyRequest {
  /** 券码 */
  code: string;
  /** 订单金额（元） */
  orderAmount: number;
}

/** 券码应用/核销响应（与后端 PosCouponApplyResponse 对应） */
export interface CouponApplyResult {
  /** 是否成功 */
  success: boolean;
  /** 折扣金额 */
  discountAmount?: number;
  /** 最终金额 */
  finalAmount?: number;
  /** 提示消息 */
  message?: string;
}

export interface OrderResultItem {
  name: string;
  price: number;
  quantity: number;
}

export interface OrderResult {
  orderId: string;
  orderNumber: string;
  pickupNumber: string;
  pickupCode: string;
  status: string;
  orderType: string;
  tableNumber?: string;
  totalAmount?: number;
  createTime?: string;
  orderItems?: OrderResultItem[];
  /** 扫码支付二维码URL（仅微信/支付宝返回） */
  qrCodeUrl?: string;
  /** 扫码支付交易号（用于确认支付） */
  qrTransactionId?: string;
  /** 二维码过期时间（毫秒时间戳） */
  qrExpiresAt?: number;
  /** 消息（如扫码支付提示） */
  message?: string;
}

export interface OrderQuery {
  orderId: string;
  orderNumber: string;
  createTime: string;
  itemCount: number;
  totalAmount?: number;
  paymentMethod?: string;
  status: string;
}

/** 挂单请求参数 */
export interface HangOrderRequest {
  items: OrderItem[];
  totalAmount: number;
  remark?: string;
}

/** 挂单响应 */
export interface HangOrderResponse {
  orderId: string;
  holdTime: string;
}

/** 会员余额信息 */
export interface MemberBalance {
  memberId: string;
  balance: number;
  availableBalance: number;
  frozenAmount: number;
}

/** 银行卡支付请求 */
export interface BankCardPaymentRequest {
  orderId: string;
  cardNumber?: string;
  cardHolder?: string;
  amount: number;
}

/** YOLO 服务状态 */
export interface YoloStatus {
  enabled: boolean;
}

/** YOLO 识别到的单个商品 */
export interface YoloRecognizedProduct {
  foodId: string;
  foodName: string;
  price: number;
  quantity: number;
  confidence: number;
  bbox?: number[];
}

/** YOLO 未匹配的检测框 */
export interface YoloUnmatchedDetection {
  className: string;
  confidence: number;
  bbox?: number[];
}

/** YOLO 识别结果 */
export interface YoloRecognitionResult {
  enabled: boolean;
  success: boolean;
  processingTimeMs: number;
  products: YoloRecognizedProduct[];
  unmatchedDetections?: YoloUnmatchedDetection[];
  errorMessage?: string;
}

export const posApi = {
  // ========== 菜单/分类/套餐 API（后端 PosApiController: /v1/pos/api/*） ==========

  getCategories: () => {
    return request.get<Category[]>('/v1/pos/api/categories');
  },

  getDishes: () => {
    return request.get<Dish[]>('/v1/pos/api/dishes');
  },

  getDishesByCategory: (categoryId: string) => {
    return request.get<Dish[]>(`/v1/pos/api/dishes/category/${categoryId}`);
  },

  getCombos: () => {
    return request.get<Combo[]>('/v1/pos/api/combos');
  },

  getFullMenu: () => {
    return request.get<Menu>('/v1/pos/api/menu');
  },

  // ========== 订单 API（后端 PosOrderController: /v1/pos/orders/*） ==========

  /** 创建订单（路径：POST /v1/pos/orders/order） */
  createOrder: (data: OrderRequest) => {
    return request.post<OrderResult>('/v1/pos/orders/order', data);
  },

  /** 订单支付（路径：POST /v1/pos/orders/order/pay） */
  payOrder: (data: PayRequest) => {
    return request.post<OrderResult>('/v1/pos/orders/order/pay', data);
  },

  /**
   * 确认扫码支付完成（微信/支付宝）
   * 路径：POST /v1/pos/orders/order/pay/confirm
   * 开发模式：前端模拟"用户已扫码支付"按钮触发
   * 生产模式：由微信/支付宝服务端异步回调触发，前端无需调用
   */
  confirmQrPayment: (qrTransactionId: string) => {
    return request.post<OrderResult>(
      '/v1/pos/orders/order/pay/confirm',
      null,
      { params: { qrTransactionId } }
    );
  },

  /** 订单退款（路径：POST /v1/pos/orders/order/{orderNumber}/refund） */
  refundOrder: (orderNumber: string, refundReason?: string) => {
    return request.post<boolean>(
      `/v1/pos/orders/order/${encodeURIComponent(orderNumber)}/refund`,
      null,
      { params: { refundReason: refundReason || '用户申请退款' } }
    );
  },

  /** 获取订单详情（路径：GET /v1/pos/orders/order/{orderNumber}） */
  getOrderDetail: (orderNumber: string) => {
    return request.get<OrderResult>(`/v1/pos/orders/order/${encodeURIComponent(orderNumber)}`);
  },

  getOrders: (params?: { startDate?: string; endDate?: string; keyword?: string }) => {
    return request.get<OrderQuery[]>('/v1/pos/orders', { params });
  },

  // ========== 券码核销相关API（后端 PosCouponController: /v1/pos/coupons/*） ==========

  /** 验证券码有效性（POST /v1/pos/coupons/verify） */
  verifyCoupon: (code: string) => {
    return request.post<CouponVerifyResult>('/v1/pos/coupons/verify', { code });
  },

  /** 应用/核销优惠券（POST /v1/pos/coupons/apply） */
  applyCoupon: (code: string, orderAmount: number) => {
    return request.post<CouponApplyResult>('/v1/pos/coupons/apply', { code, orderAmount });
  },

  // ========== 挂单相关API（后端无此端点，全部本地存储） ==========

  /**
   * 挂单：将当前订单挂起
   * 后端未提供挂单端点，挂单数据仅存于本地 localStorage
   * 接口保留以便未来后端实现后切换为远程
   */
  hangOrder: (_data: HangOrderRequest) => {
    // 后端未实现，挂单走本地存储（stores/hanging-order.ts 已处理）
    return Promise.resolve({ orderId: '', holdTime: '' } as HangOrderResponse);
  },

  /** 获取所有挂起的订单（本地存储） */
  getHeldOrders: () => {
    return Promise.resolve([] as HeldOrder[]);
  },

  /** 恢复挂单（本地操作） */
  resumeOrder: (_orderId: string) => {
    return Promise.resolve();
  },

  /** 删除挂单（本地操作） */
  deleteHeldOrder: (_orderId: string) => {
    return Promise.resolve();
  },

  // ========== 会员余额相关API ==========
  // 后端无 /v1/pos/member/balance 专用端点
  // 余额支付走 /v1/pos/orders/order/pay，paymentMethod="余额支付"
  // 会员余额查询走 /v1/pos/members/{id} 或 /v1/pos/members/phone/{phone}
  // 此处 getMemberBalance 为本地降级实现，返回 null 让前端走"无会员"流程

  /** 获取会员余额（后端无专用端点，返回 null 触发前端降级处理） */
  getMemberBalance: (_memberId?: string) => {
    return Promise.resolve(null as unknown as MemberBalance);
  },

  /**
   * 余额支付（统一走 /v1/pos/orders/order/pay，paymentMethod=余额支付）
   * 后端 PayRequestDTO 的 paymentMethod 正则允许 "余额支付"
   */
  payWithBalance: (data: { orderId: string; amount: number; memberId?: string }) => {
    return request.post<OrderResult>('/v1/pos/orders/order/pay', {
      orderId: data.orderId,
      paymentMethod: '余额支付',
      amount: data.amount,
      openid: data.memberId
    });
  },

  // ========== 银行卡支付API ==========
  // 后端无专用端点，统一走 /v1/pos/orders/order/pay，paymentMethod=银行卡

  /**
   * 银行卡支付（统一走 /v1/pos/orders/order/pay）
   * 后端 PayRequestDTO 的 paymentMethod 正则允许 "银行卡"
   */
  payWithBankCard: (data: BankCardPaymentRequest) => {
    return request.post<OrderResult>('/v1/pos/orders/order/pay', {
      orderId: data.orderId,
      paymentMethod: '银行卡',
      amount: data.amount
    });
  },

  // ========== 小票打印相关API ==========

  /** 重新打印小票（路径：POST /v1/pos/orders/order/{orderId}/receipt） */
  reprintReceipt: (orderId: string) => {
    return request.post<boolean>(`/v1/pos/orders/order/${orderId}/receipt`);
  },

  // ========== YOLO 辅助收银 API（后端 YoloProductRecognitionController: /v1/pos/yolo/*） ==========

  /** 查询 YOLO 服务状态 */
  getYoloStatus: () => {
    return request.get<YoloStatus>('/v1/pos/yolo/status');
  },

  /**
   * 识别商品（base64 图片）
   * 后端会根据配置返回真实推理结果或 Mock 数据
   */
  recognizeProducts: (image: string, storeId?: string) => {
    return request.post<YoloRecognitionResult>('/v1/pos/yolo/recognize', {
      image,
      storeId
    });
  },

  // ============================================================
  // 产品中心 API（与后台管理端共用同一后端，路径 /v1/product-center/*）
  // POS 端菜品/套餐/分类数据从产品中心同步，仅展示在售（status=1）数据
  // 注意：request.ts 响应拦截器已提取 data.data，运行时返回值即为 VO 数组；
  // 但 TypeScript 仍按 axios 默认签名推断为 AxiosResponse<T>，因此这里做类型断言。
  // ============================================================

  /** 获取在售菜品列表（同步产品中心） */
  async listFoodsOnSale(): Promise<Dish[]> {
    const res = (await request.get<FoodVO[]>('/v1/product-center/foods/on-sale')) as unknown as FoodVO[];
    return (res || []).map(foodVOToDish);
  },

  /** 获取在售套餐列表（同步产品中心） */
  async listCombosOnSale(): Promise<Combo[]> {
    const res = (await request.get<ComboVO[]>('/v1/product-center/combos/on-sale')) as unknown as ComboVO[];
    return (res || []).map(comboVOToCombo);
  },

  /** 获取启用的分类树（同步产品中心），返回扁平化列表 */
  async listEnabledCategories(): Promise<Category[]> {
    const res = (await request.get<CategoryVO[]>('/v1/product-center/categories/tree/enabled')) as unknown as CategoryVO[];
    return categoryVOTreeToList(res || []);
  },
};
