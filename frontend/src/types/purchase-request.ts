/**
 * 采购申请模块类型定义
 * 从 api/purchaseRequest.ts 迁移而来
 */

export interface PurchaseRequestItem {
  itemId: string;
  requestId: string;
  /** 物料ID（统一命名） */
  materialId: string;
  /** 物料名称（统一命名） */
  materialName: string;
  /** 物料编码（统一命名） */
  materialCode: string;
  specification: string;
  quantity: number;
  unit: string;
  estimatedPrice: number;
  subtotalAmount: number;
  remark: string;
  /** 计划收货方类型：STORE / WAREHOUSE */
  plannedReceiverType?: 'STORE' | 'WAREHOUSE';
  /** 计划收货门店ID */
  plannedStoreId?: string;
  /** 计划收货仓库ID */
  plannedWarehouseId?: string;
}

export interface PurchaseRequest {
  requestId: string;
  requestNo: string;
  title: string;
  requestType: string;
  departmentId: string;
  departmentName: string;
  /** 申请人ID（统一命名） */
  createBy: string;
  /** 申请人名称（统一命名） */
  createByName: string;
  totalAmount: number;
  status: string;
  priority: string;
  expectedDate: string;
  description: string;
  rejectReason: string;
  /** 驳回次数（达到 3 次后禁止重新提交，需管理员重置） */
  rejectCount: number;
  /** 审批人名称（统一命名） */
  approvedByName: string;
  approvedTime: string;
  budgetId: string;
  budgetStatus: string;
  createTime: string;
  /** 更新人（后端 DTO 当前未返回，由 converter 填充默认空串） */
  updateBy: string;
  updateTime: string;
  /** 删除人（后端 DTO 当前未返回，由 converter 填充默认空串） */
  deletedBy: string;
  /** 删除时间（后端 DTO 当前未返回，由 converter 填充默认空串） */
  deletedTime: string;
  items: PurchaseRequestItem[];
}

export interface PurchaseRequestCreateParams {
  title: string;
  requestType?: string;
  departmentId?: string;
  departmentName?: string;
  createBy?: string;
  createByName?: string;
  priority?: string;
  expectedDate?: string;
  description?: string;
  budgetId?: string;
  items: {
    materialId?: string;
    materialName: string;
    materialCode?: string;
    specification?: string;
    quantity?: number;
    unit?: string;
    estimatedPrice?: number;
    remark?: string;
    /** 计划收货方类型：STORE / WAREHOUSE */
    plannedReceiverType?: 'STORE' | 'WAREHOUSE';
    /** 计划收货门店ID */
    plannedStoreId?: string;
    /** 计划收货仓库ID */
    plannedWarehouseId?: string;
  }[];
}

export interface PurchaseRequestUpdateParams {
  requestId: string;
  title?: string;
  requestType?: string;
  departmentId?: string;
  departmentName?: string;
  priority?: string;
  expectedDate?: string;
  description?: string;
  budgetId?: string;
  items?: {
    itemId?: string;
    materialId?: string;
    materialName: string;
    materialCode?: string;
    specification?: string;
    quantity?: number;
    unit?: string;
    estimatedPrice?: number;
    remark?: string;
    /** 计划收货方类型：STORE / WAREHOUSE */
    plannedReceiverType?: 'STORE' | 'WAREHOUSE';
    /** 计划收货门店ID */
    plannedStoreId?: string;
    /** 计划收货仓库ID */
    plannedWarehouseId?: string;
  }[];
}

export interface PurchaseRequestQueryParams {
  page?: number;
  size?: number;
  requestNo?: string;
  status?: string;
  departmentId?: string;
  createBy?: string;
  createByName?: string;
  startDate?: string;
  endDate?: string;
}

export interface PurchaseRequestApproveParams {
  status: string;
  remark?: string;
  /** 审批人名称（由调用方传入，避免API层硬编码） */
  approvedByName?: string;
}

/** 采购申请信息（PurchaseRequest 的别名，用于业务语义区分） */
export type PurchaseRequestInfo = PurchaseRequest
