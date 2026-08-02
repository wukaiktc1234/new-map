/**
 * 采购收货相关类型定义
 * 对应后端实体: PurchaseStockin (purchase_stockin, PurchaseStockinItem)
 */

/**
 * 采购收货状态
 */
export type PurchaseStockinStatus =
  | 'pending'     // 待检验
  | 'inspecting'  // 检验中
  | 'qualified'   // 检验合格
  | 'unqualified' // 检验不合格
  | 'partial'     // 部分合格
  | 'completed';  // 已完成

/**
 * 收货方式
 */
export type DeliveryMethod =
  | 'warehouse_direct'  // 直发仓库
  | 'store_direct'      // 直发门店
  | 'batch';            // 分批收货

/** 收货方式选项 */
export const DeliveryMethodOptions = [
  { label: '直发仓库', value: 'warehouse_direct' },
  { label: '直发门店', value: 'store_direct' },
  { label: '分批收货', value: 'batch' },
] as const;

/**
 * 采购收货明细
 */
export interface PurchaseStockinItem {
  /** 入库明细ID（采购退货关联原入库明细时使用） */
  stockinItemId?: string;
  /** 关联的订单明细ID（来自 PurchaseOrderItemInfo.id，创建入库单时传给后端 orderItemId） */
  orderItemId?: string;
  /** 物料ID */
  materialId: string;
  /** 物料名称 */
  materialName: string;
  /** 规格 */
  specification?: string;
  /** 单位（从采购订单明细继承，不可在前端随意修改） */
  unit?: string;
  /** 单价（元，从采购订单明细继承；由后端 unitPrice 分转换而来） */
  unitPrice?: number;
  /** 单价（分，后端原始值，用于需要精确计算的退货场景） */
  unitPriceFen?: number;
  /** 收货数量 */
  quantity: number;
  /** 合格数量 */
  qualifiedQuantity: number;
  /** 不合格数量 */
  unqualifiedQuantity: number;
  /** 关联订单明细的采购数量（用于前端校验最大可收货数量） */
  sourceOrderQuantity?: number;
  /** 关联订单明细的已收货数量（用于前端校验最大可收货数量） */
  sourceReceivedQuantity?: number;
  /** 入库批次号 */
  batchNo?: string;
  /** 食材温度(℃) */
  temperature?: number | null;
  /** 保质期(天) */
  shelfLife?: number | null;
  /** 生产日期 */
  productionDate?: string;
  /** 备注 */
  remark: string;
}

/**
 * 采购收货信息
 */
export interface PurchaseStockinInfo {
  /** 收货ID */
  stockinId: string;
  /** 收货编号 */
  stockinNo: string;
  /** 采购订单ID */
  orderId: string;
  /** 采购订单编号 */
  orderNo: string;
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName: string;
  /** 收货方式 */
  deliveryMethod: DeliveryMethod;
  /** 收货仓库（直发仓库时） */
  deliveryWarehouse?: string;
  /** 收货门店（直发门店时） */
  deliveryStore?: string;
  /** 收货人 */
  receiverName?: string;
  /** 总金额（元） */
  totalAmount: number;
  /** 收货日期 */
  stockinDate: string;
  /** 总数量 */
  totalQuantity: number;
  /** 检验员ID */
  inspectorId: string;
  /** 检验员名称 */
  inspectorName: string;
  /** 状态 */
  status: PurchaseStockinStatus;
  /** 合格数量 */
  qualifiedCount: number;
  /** 不合格数量 */
  unqualifiedCount: number;
  /** 入库明细列表 */
  items?: PurchaseStockinItem[];
  /** 备注 */
  remark: string;
  /** 外观检查结果 */
  appearanceResult?: string;
  /** 气味检查结果 */
  odorResult?: string;
  /** 实测温度(℃) */
  temperature?: number | null;
  /** 实测湿度(%) */
  humidity?: number | null;
  /** 抽检数量 */
  sampleQuantity?: number | null;
  /** 抽检比例(%) */
  sampleRate?: number | null;
  /** 不合格类型 */
  unqualifiedType?: string;
  /** 处理意见 */
  disposalOpinion?: string;
  /** 随货单据核查结果 */
  documentCheck?: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 采购收货查询参数
 */
export interface PurchaseStockinQueryForm {
  /** 收货编号 */
  stockinNo?: string;
  /** 状态 */
  status?: PurchaseStockinStatus | null;
  /** 供应商ID */
  supplierId?: string;
  /** 收货方式 */
  deliveryMethod?: DeliveryMethod | null;
  /** 开始日期 */
  startDate?: string;
  /** 结束日期 */
  endDate?: string;
  /** 搜索关键词 */
  keyword?: string;
}

/**
 * 采购收货表单数据（新建/编辑）
 */
export interface PurchaseStockinFormData {
  /** 采购订单ID */
  orderId: string;
  /** 供应商ID */
  supplierId?: string;
  /** 供应商名称 */
  supplierName?: string;
  /** 收货方式 */
  deliveryMethod: DeliveryMethod;
  /** 收货仓库 */
  deliveryWarehouse?: string;
  /** 收货门店 */
  deliveryStore?: string;
  /** 收货人 */
  receiverName?: string;
  /** 收货日期 */
  stockinDate: string;
  /** 检验员ID */
  inspectorId: string;
  /** 收货明细列表 */
  items: PurchaseStockinItem[];
  /** 创建备注（与质检备注 qualityRemark 职责分离） */
  remark?: string;
}

/** 采购收货状态选项 */
export const PurchaseStockinStatusOptions = [
  { label: '待检验', value: 'pending' },
  { label: '检验中', value: 'inspecting' },
  { label: '检验合格', value: 'qualified' },
  { label: '检验不合格', value: 'unqualified' },
  { label: '部分合格', value: 'partial' },
  { label: '已完成', value: 'completed' },
] as const;
