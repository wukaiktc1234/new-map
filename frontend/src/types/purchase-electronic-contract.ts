/**
 * 电子合同相关类型定义
 * 对应后端实体: ElectronicContract (electronic_contract)
 */

/**
 * 电子合同状态
 */
export type ElectronicContractStatus =
  | 'draft'       // 草稿
  | 'pending_sign' // 待签署
  | 'signed'      // 已签署
  | 'expired'     // 已过期
  | 'cancelled';  // 已取消

/**
 * 电子合同信息
 */
export interface ElectronicContractInfo {
  /** 电子合同ID */
  eContractId: string;
  /** 电子合同编号 */
  eContractNo: string;
  /** 关联采购合同ID（可选） */
  contractId?: string;
  /** 关联采购合同编号（可选） */
  contractNo?: string;
  /** 合同名称 */
  contractName: string;
  /** 甲方 */
  partyA?: string;
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName: string;
  /** 合同金额（元） */
  totalAmount: number;
  /** 签署方式 */
  signMethod?: string;
  /** 合同开始日期 */
  startDate?: string;
  /** 签署日期 */
  signDate: string;
  /** 到期日期 */
  expireDate: string;
  /** 状态 */
  status: ElectronicContractStatus;
  /** 签署链接 */
  signUrl: string;
  /** 备注 */
  remark: string;
  /** 合同正文 */
  contractContent: string;
  /** 是否已归档 */
  archived?: boolean;
  /** 归档时间 */
  archiveTime?: string;
  /** 归档人 */
  archivedBy?: string;
  /** 归档位置 */
  archiveLocation?: string;
  /** 创建人 */
  createBy: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 电子合同查询参数
 */
export interface ElectronicContractQueryForm {
  /** 电子合同编号 */
  eContractNo?: string;
  /** 状态 */
  status?: ElectronicContractStatus | null;
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
 * 电子合同表单数据（新建/编辑）
 */
export interface ElectronicContractFormData {
  /** 合同名称 */
  contractName: string;
  /** 电子合同编号 */
  eContractNo?: string;
  /** 甲方 */
  partyA?: string;
  /** 供应商名称 */
  supplierName: string;
  /** 合同金额（元） */
  totalAmount: number;
  /** 签署方式 */
  signMethod?: string;
  /** 合同开始日期 */
  startDate?: string;
  /** 到期日期 */
  expireDate: string;
  /** 关联采购合同ID（可选） */
  relatedContractId?: string;
  /** 合同模板ID */
  templateId?: string;
  /** 自定义模板名称 */
  customTemplateName?: string;
  /** 合同正文 */
  contractContent?: string;
  /** 备注 */
  remark?: string;
}

/** 电子合同状态选项 */
export const ElectronicContractStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待签署', value: 'pending_sign' },
  { label: '已签署', value: 'signed' },
  { label: '已过期', value: 'expired' },
  { label: '已取消', value: 'cancelled' },
] as const;
