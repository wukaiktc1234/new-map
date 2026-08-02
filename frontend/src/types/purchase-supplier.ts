/**
 * 供应商档案相关类型定义
 * 对应后端实体: Supplier (supplier)
 */

/**
 * 供应商类型
 */
export type SupplierType =
  | 'raw_material' // 原材料
  | 'auxiliary'    // 辅料
  | 'equipment'    // 设备
  | 'service';     // 服务

/**
 * 供应商等级
 */
export type SupplierLevel =
  | 'strategic'   // 战略
  | 'qualified'   // 合格
  | 'alternative' // 备选
  | 'temporary';  // 临时

/**
 * 供应商状态
 */
export type SupplierStatus =
  | 'active'    // 正常
  | 'inactive'  // 停用
  | 'frozen'    // 冻结
  | 'eliminated'; // 淘汰

/**
 * 结算方式
 */
export type SettlementMethod =
  | 'monthly'   // 月结
  | 'immediate' // 现结
  | 'prepaid';  // 预付

/**
 * 供应商信息
 */
export interface SupplierInfo {
  /** 供应商ID */
  supplierId: string;
  /** 供应商编码 */
  supplierCode: string;
  /** 供应商名称 */
  supplierName: string;
  /** 供应商简称 */
  shortName: string;
  /** 统一社会信用代码 */
  unifiedSocialCode: string;
  /** 供应商类型 */
  supplierType: SupplierType;
  /** 供应商等级 */
  supplierLevel: SupplierLevel;
  /** 所属行业 */
  industry: string;
  /** 注册资本（元） */
  registeredCapital: number;
  /** 成立日期 */
  establishDate: string;
  /** 联系人 */
  contactPerson: string;
  /** 联系电话 */
  contactPhone: string;
  /** 联系邮箱 */
  email: string;
  /** 地址 */
  address: string;
  /** 开户银行 */
  bankName: string;
  /** 银行账号 */
  bankAccount: string;
  /** 结算方式 */
  settlementMethod: SettlementMethod;
  /** 账期（天） */
  paymentTerms: number;
  /** 供货品类 */
  supplyCategories: string[];
  /** 主要品类（展示用，逗号分隔） */
  mainCategories: string;
  /** 最低起订量 */
  minOrderQuantity: number;
  /** 配送区域 */
  deliveryArea: string;
  /** 经营范围 */
  businessScope: string;
  /** 综合评分 */
  overallScore: number;
  /** 质量评分(0-100) */
  qualityScore: number;
  /** 交期评分(0-100) */
  deliveryScore: number;
  /** 价格评分(0-100) */
  priceScore: number;
  /** 服务评分(0-100) */
  serviceScore: number;
  /** 评估等级 */
  evaluationGrade: 'A' | 'B' | 'C' | 'D';
  /** 最近评估日期 */
  lastEvaluationDate: string;
  /** 状态 */
  status: SupplierStatus;
  /** 备注 */
  remark: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 供应商查询参数
 */
export interface SupplierQueryForm {
  /** 搜索关键词（名称或编码） */
  keyword?: string;
  /** 供应商类型 */
  supplierType?: SupplierType | null;
  /** 供应商等级 */
  supplierLevel?: SupplierLevel | null;
  /** 状态 */
  status?: SupplierStatus | null;
}

/**
 * 供应商表单数据（新建/编辑）
 */
export interface SupplierFormData {
  /** 供应商编码（新建时可手动输入，留空则后端自动生成；编辑时不可修改） */
  supplierCode?: string;
  /** 供应商名称 */
  supplierName: string;
  /** 供应商简称 */
  shortName?: string;
  /** 统一社会信用代码 */
  unifiedSocialCode?: string;
  /** 供应商类型 */
  supplierType: SupplierType;
  /** 供应商等级 */
  supplierLevel: SupplierLevel;
  /** 所属行业 */
  industry?: string;
  /** 注册资本（元） */
  registeredCapital?: number;
  /** 成立日期 */
  establishDate?: string;
  /** 联系人 */
  contactPerson: string;
  /** 联系电话 */
  contactPhone: string;
  /** 联系邮箱 */
  email?: string;
  /** 地址 */
  address?: string;
  /** 开户银行 */
  bankName?: string;
  /** 银行账号 */
  bankAccount?: string;
  /** 结算方式 */
  settlementMethod?: SettlementMethod;
  /** 账期（天） */
  paymentTerms?: number;
  /** 供货品类 */
  supplyCategories?: string[];
  /** 最低起订量 */
  minOrderQuantity?: number;
  /** 配送区域 */
  deliveryArea?: string;
  /** 备注 */
  remark?: string;
}

/** 供应商类型选项 */
export const SupplierTypeOptions = [
  { label: '原材料', value: 'raw_material' },
  { label: '辅料', value: 'auxiliary' },
  { label: '设备', value: 'equipment' },
  { label: '服务', value: 'service' },
] as const;

/** 供应商等级选项 */
export const SupplierLevelOptions = [
  { label: '战略', value: 'strategic' },
  { label: '合格', value: 'qualified' },
  { label: '备选', value: 'alternative' },
  { label: '临时', value: 'temporary' },
] as const;

/** 供应商状态选项 */
export const SupplierStatusOptions = [
  { label: '正常', value: 'active' },
  { label: '停用', value: 'inactive' },
  { label: '冻结', value: 'frozen' },
  { label: '淘汰', value: 'eliminated' },
] as const;

/** 结算方式选项 */
export const SettlementMethodOptions = [
  { label: '月结', value: 'monthly' },
  { label: '现结', value: 'immediate' },
  { label: '预付', value: 'prepaid' },
] as const;

/** 供货品类选项（模拟） */
export const SupplyCategoryOptions = [
  { label: '蔬菜', value: '蔬菜' },
  { label: '肉类', value: '肉类' },
  { label: '水产', value: '水产' },
  { label: '粮油', value: '粮油' },
  { label: '调料', value: '调料' },
  { label: '饮品', value: '饮品' },
  { label: '冷冻食品', value: '冷冻食品' },
  { label: '干货', value: '干货' },
  { label: '设备', value: '设备' },
  { label: '包装材料', value: '包装材料' },
] as const;
