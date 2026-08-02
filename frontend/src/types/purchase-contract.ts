/**
 * 采购合同相关类型定义
 * 对应后端实体: PurchaseContract (purchase_contract)
 */

/**
 * 采购合同状态
 */
export type PurchaseContractStatus =
  | 'draft'      // 草稿
  | 'pending'    // 待审批
  | 'active'     // 生效中
  | 'expired'    // 已过期
  | 'terminated'; // 已终止

/**
 * 采购合同信息
 */
export interface PurchaseContractInfo {
  /** 合同ID */
  contractId: string;
  /** 合同编号 */
  contractNo: string;
  /** 合同名称 */
  contractName: string;
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName: string;
  /** 合同总金额（元） */
  totalAmount: number;
  /** 合同开始日期 */
  startDate: string;
  /** 合同结束日期 */
  endDate: string;
  /** 签订日期 */
  signDate: string;
  /** 状态 */
  status: PurchaseContractStatus;
  /** 付款条款 */
  paymentTerms: string;
  /** 交货条款 */
  deliveryTerms: string;
  /** 备注 */
  remark: string;
  /** 合同正文 */
  contractContent?: string;
  /** 是否已归档 */
  archived?: boolean;
  /** 归档时间 */
  archiveTime?: string;
  /** 归档人 */
  archivedBy?: string;
  /** 存放位置 */
  archiveLocation?: string;
  /** 调阅次数 */
  viewCount?: number;
  /** 最近调阅时间 */
  lastViewTime?: string;
  /** 自定义模板名称 */
  customTemplateName?: string;
  /** 创建人 */
  createBy: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 采购合同查询参数
 */
export interface PurchaseContractQueryForm {
  /** 合同编号 */
  contractNo?: string;
  /** 状态 */
  status?: PurchaseContractStatus | null;
  /** 供应商ID */
  supplierId?: string;
  /** 开始日期 */
  startDate?: string;
  /** 结束日期 */
  endDate?: string;
  /** 搜索关键词 */
  keyword?: string;
}

/**
 * 采购合同表单数据（新建/编辑）
 */
export interface PurchaseContractFormData {
  /** 合同名称 */
  contractName: string;
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName?: string;
  /** 合同总金额（元） */
  totalAmount: number;
  /** 合同开始日期 */
  startDate: string;
  /** 合同结束日期 */
  endDate: string;
  /** 付款条款 */
  paymentTerms?: string;
  /** 交货条款 */
  deliveryTerms?: string;
  /** 备注 */
  remark?: string;
}

/** 采购合同状态选项 */
export const PurchaseContractStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审批', value: 'pending' },
  { label: '生效中', value: 'active' },
  { label: '已过期', value: 'expired' },
  { label: '已终止', value: 'terminated' },
] as const;
