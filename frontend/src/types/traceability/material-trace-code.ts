/**
 * 原料追溯码类型定义
 * 迁移自 types/materialTraceCode.ts，并补充 Backend/Display 类型
 * 对应后端 MaterialTraceCodeController（/v1/material-trace-code）
 */

/** 原料追溯码状态 */
export type MaterialTraceCodeStatus = 'pending' | 'in_stock' | 'picked' | 'used' | 'expired' | 'returned'

/** 原料追溯码（基础类型） */
export interface MaterialTraceCode {
  id?: number
  traceCodeId?: string
  traceCode?: string
  qrCodeUrl?: string
  productId?: number
  productName?: string
  productCode?: string
  productCategory?: string
  supplierId?: number
  supplierName?: string
  purchaseStockinId?: number
  purchaseOrderNo?: string
  batchNumber?: string
  productionDate?: string
  shelfLifeDays?: number
  expiryDate?: string
  inboundTime?: string
  quantity?: number
  unit?: string
  unitPrice?: number
  totalPrice?: number
  warehouseId?: number
  warehouseName?: string
  storeId?: number
  storeName?: string
  storageLocation?: string
  status?: MaterialTraceCodeStatus
  usedTime?: string
  usedById?: number
  usedByName?: string
  usagePurpose?: string
  relatedFoodTraceCodes?: string
  remark?: string
  createTime?: string
  updateTime: string
}

/** 原料追溯码生成 DTO */
export interface MaterialTraceCodeGenerateDTO {
  productId?: number
  productName?: string
  productCode?: string
  productCategory?: string
  supplierId?: number
  supplierName?: string
  purchaseStockinId?: number
  purchaseOrderNo?: string
  batchNumber?: string
  productionDate?: string
  shelfLifeDays?: number
  inboundTime?: string
  quantity?: number
  unit?: string
  unitPrice?: number
  warehouseId?: number
  warehouseName?: string
  storeId?: number
  storeName?: string
  storageLocation?: string
  generateCount?: number
  remark: string
}

/** 原料追溯码查询参数 */
export interface MaterialTraceCodeQuery {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 物料ID */
  productId?: number
  /** 状态 */
  status?: MaterialTraceCodeStatus
  /** 门店ID */
  storeId?: number
  /** 批次号 */
  batchNumber?: string
  /** 库位 */
  storageLocation?: string
  /** 关键字（追溯码/物料名称） */
  keyword?: string
}

// ============================================================
// 补充类型（Backend/Display，用于 DataConverter）
// ============================================================

/** 后端原始类型（金额为分） */
export interface MaterialTraceCodeBackend {
  id?: number
  traceCodeId?: string
  traceCode?: string
  qrCodeUrl?: string
  productId?: number
  productName?: string
  productCode?: string
  productCategory?: string
  supplierId?: number
  supplierName?: string
  purchaseStockinId?: number
  purchaseOrderNo?: string
  batchNumber?: string
  productionDate?: string
  shelfLifeDays?: number
  expiryDate?: string
  inboundTime?: string
  quantity?: number
  unit?: string
  /** 单价（分） */
  unitPrice?: number
  /** 总价（分） */
  totalPrice?: number
  warehouseId?: number
  warehouseName?: string
  storeId?: number
  storeName?: string
  storageLocation?: string
  status?: MaterialTraceCodeStatus
  usedTime?: string
  usedById?: number
  usedByName?: string
  usagePurpose?: string
  relatedFoodTraceCodes?: string
  remark?: string
  createTime?: string
  updateTime: string
}

/** 前端展示类型（金额为元字符串，含计算字段） */
export interface MaterialTraceCodeDisplay extends Omit<MaterialTraceCode, 'unitPrice' | 'totalPrice'> {
  /** 单价（元） */
  unitPriceYuan?: string
  /** 总价（元） */
  totalPriceYuan?: string
  /** 状态标签 */
  statusLabel?: string
  /** 生产日期展示（YYYY-MM-DD） */
  productionDateDisplay?: string
  /** 到期日期展示（YYYY-MM-DD） */
  expiryDateDisplay?: string
  /** 剩余天数（前端计算，负数表示已过期） */
  remainingDays?: number
}

/** 原料追溯码状态映射 */
export const MaterialTraceCodeStatusMap: Record<MaterialTraceCodeStatus, { label: string; type: string }> = {
  pending: { label: '待入库', type: 'info' },
  in_stock: { label: '在库', type: 'success' },
  picked: { label: '已领用', type: 'warning' },
  used: { label: '已使用', type: 'info' },
  expired: { label: '已过期', type: 'danger' },
  returned: { label: '已退货', type: 'default' },
}
