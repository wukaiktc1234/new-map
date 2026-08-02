/**
 * 采购订单相关类型定义
 * 对应后端实体: PurchaseOrder (purchase_order, PurchaseOrderItem
 */

/**
 * 采购订单状态
 */
export type PurchaseOrderStatus =
  | 'draft'           // 草稿
  | 'pending'         // 待审批
  | 'approved'        // 已审批
  | 'ordered'         // 已下单
  | 'shipped'         // 已发货
  | 'received'        // 已收货
  | 'rejected'        // 已拒绝
  | 'partial_received'// 部分到货
  | 'completed'       // 已完成
  | 'terminated'      // 已终止
  | 'cancelled';      // 已取消

/**
 * 付款状态
 */
export type PaymentStatus =
  | 'unpaid'   // 未付款
  | 'partial'  // 部分付款
  | 'paid';    // 已付款

/**
 * 采购订单信息
 */
export interface PurchaseOrderInfo {
  /** 采购订单ID（主键，UUID格式
 */
  purchaseOrderId: string;
  /** 采购订单编号（业务编号） */
  orderNo: string;
  /** 供应商ID
 */
  supplierId: string;
  /** 供应商名
 */
  supplierName: string;
  /** 请购单ID
 */
  requestId: string;
  /** 请购单编
 */
  requestNo: string;
  /** 合同ID
 */
  contractId: string;
  /** 合同编号
 */
  contractNo: string;
  /** 来源类型（如：手动创建、请购转采购等） */
  sourceType: string;
  /** 优先级（如：normal-普通，urgent-紧急） */
  priority: string;
  /** 采购类型（如：常规采购、紧急采购） */
  purchaseType: string;
  /** 采购总金额（元） */
  totalAmount: number;
  /** 订单状
 */
  status: PurchaseOrderStatus;
  /** 下单日期
 */
  orderDate: string;
  /** 预计到货日期
 */
  expectedDate: string;
  /** 联系
 */
  contactPerson: string;
  /** 联系电话
 */
  contactPhone: string;
  /** 收货仓库ID（用于采购入库单自动带入仓库）
 */
  warehouseId?: string;
  /** 备注
 */
  remark: string;
  /** 拒绝原因
 */
  rejectReason: string;
  /** 付款状
 */
  paymentStatus: PaymentStatus;
  /** 已付款金额（元） */
  paidAmount: number;
  /** 预算ID
 */
  budgetId: string;
  /** 预算状态
  */
  budgetStatus: string;
  /** 关联采购计划ID（可选，从计划创建订单时填入） */
  planId?: number;
  /** 采购明细列表
  */
  items: PurchaseOrderItemInfo[];
  /** 创建时间
*/
  createTime: string;
  /** 创建人名称（统一命名）[待后端配合：当前后端仅返回 createUserId]
*/
  createByName: string;
  /** 更新时间
*/
  updateTime: string;
  /** 更新人（后端实体未持久化，可选） */
  updateBy?: string;
  /** 删除时间（后端实体未持久化，可选） */
  deletedTime?: string;
  /** 删除人（后端实体未持久化，可选） */
  deletedBy?: string;
}

/**
 * 采购订单明细
 */
export interface PurchaseOrderItemInfo {
  /** 明细ID
 */
  id: number;
  /** 采购订单ID
 */
  purchaseOrderId: string;
  /** 物料ID（统一命名）
   */
  materialId: string;
  /** 物料名称（统一命名）
   */
  materialName: string;
  /** 规格型号
 */
  specification: string;
  /** 单位
 */
  unit: string;
  /** 采购数量
 */
  quantity: number;
  /** 单价（元
 */
  unitPrice: number;
  /** 金额（元
 */
  amount: number;
  /** 已到货数
 */
  receivedQuantity: number;
  /** 备注
 */
  remark: string;
  /** 计划收货方类型：STORE / WAREHOUSE */
  plannedReceiverType?: 'STORE' | 'WAREHOUSE';
  /** 计划收货门店ID */
  plannedStoreId?: string;
  /** 计划收货仓库ID */
  plannedWarehouseId?: string;
}

/**
 * 采购订单查询参数
 */
export interface PurchaseOrderQueryForm {
  /** �ɹ��������
 */
  orderNo?: string;
  /** 来源申请/计划编号（requestNo） */
  requestNo?: string;
  /** 供应商名称（模糊搜索
 */
  supplierName?: string;
  /** 订单状
 */
  status?: PurchaseOrderStatus | null;
  /** 付款状
 */
  paymentStatus?: PaymentStatus | null;
  /** 开始日期（下单日期范围
 */
  startDate?: string;
  /** 结束日期
   */
  endDate?: string;
  /** 创建人ID（我的单据筛选） */
  createUserId?: string;
}

/** 采购订单状态选项
 */
export const PurchaseOrderStatusOptions = [
{ label: '草稿',
value: 'draft' },
{ label: '待审核',
value: 'pending' },
{ label: '已审核',
value: 'approved' },
{ label: '已下单',
value: 'ordered' },
{ label: '已发货',
value: 'shipped' },
{ label: '已收货',
value: 'received' },
{ label: '已拒绝',
value: 'rejected' },
{ label: '部分到货',
value: 'partial_received' },
{ label: '已完成',
value: 'completed' },
{ label: '已终止',
value: 'terminated' },
{ label: '已取消',
value: 'cancelled' },
] as const;

/** 采购订单付款状态选项
 */
export const PaymentStatusOptions = [
{ label: '未付',
value: 'unpaid' },
{ label: '部分付款',
value: 'partial' },
{ label: '已付款',
value: 'paid' },
] as const;
