/**
 * 采购计划相关类型定义
 * 对应后端实体: PurchasePlan (purchase_plan, PurchasePlanItem)
 */

/**
 * 采购计划状态
 */
export type PurchasePlanStatus =
  | 'draft'     // 草稿
  | 'pending'   // 待审批
  | 'approved'  // 已审批
  | 'executing' // 执行中
  | 'completed' // 已完成
  | 'rejected'; // 已拒绝

/**
 * 采购计划明细
 */
export interface PurchasePlanItem {
  /** 物料ID */
  materialId: string;
  /** 物料名称 */
  materialName: string;
  /** 规格型号 */
  specification?: string;
  /** 数量 */
  quantity: number;
  /** 单位 */
  unit: string;
  /** 预估单价（元） */
  estimatedPrice: number;
  /** 是否临时物料 */
  isTempMaterial?: boolean;
  /** 供应商ID */
  supplierId?: string;
  /** 供应商名称 */
  supplierName?: string;
  /** 备注 */
  remark: string;
}

/**
 * 采购计划信息
 */
export interface PurchasePlanInfo {
  /** 计划ID */
  planId: string;
  /** 计划编号 */
  planNo: string;
  /** 计划日期 */
  planDate: string;
  /** 部门ID */
  departmentId: string;
  /** 部门名称 */
  departmentName: string;
  /** 总金额（元） */
  totalAmount: number;
  /** 物料项数 */
  itemCount: number;
  /** 创建人ID */
  createBy: string;
  /** 创建人名称 */
  createByName: string;
  /** 状态 */
  status: PurchasePlanStatus;
  /** 备注 */
  remark: string;
  /** 审批人名称 */
  approvedByName: string;
  /** 审批时间 */
  approvedTime: string;
  /** 拒绝原因 */
  rejectReason: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 采购计划查询参数
 */
export interface PurchasePlanQueryForm {
  /** 计划编号 */
  planNo?: string;
  /** 状态 */
  status?: PurchasePlanStatus | null;
  /** 部门ID */
  departmentId?: string;
  /** 开始日期 */
  startDate?: string;
  /** 结束日期 */
  endDate?: string;
  /** 搜索关键词 */
  keyword?: string;
}

/**
 * 采购计划表单数据（新建/编辑）
 */
export interface PurchasePlanFormData {
  /** 部门ID */
  departmentId: string;
  /** 计划日期 */
  planDate: string;
  /** 备注 */
  remark: string;
  /** 计划明细列表 */
  items: PurchasePlanItem[];
}

/** 采购计划状态选项 */
export const PurchasePlanStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审批', value: 'pending' },
  { label: '已审批', value: 'approved' },
  { label: '执行中', value: 'executing' },
  { label: '已完成', value: 'completed' },
  { label: '已拒绝', value: 'rejected' },
] as const;
