/**
 * 供应商相关类型定 * 对应后端实体: Supplier (suppliers
 */

/**
 * 供应商信
 */
export interface SupplierInfo {
  /** 主键ID
 */
  id: number;
  /** 供应商ID（业务编码） */
  supplierId: string;
  /** 供应商名
 */
  name: string;
  /** 联系
 */
  contactPerson: string;
  /** 联系电话
 */
  phone: string;
  /** 地址
 */
  address: string;
  /** 状态（active-启用，inactive-停用
 */
  status: string;
  /** 版本号（乐观锁） */
  version: number;
  /** 创建时间
 */
  createTime: string;
  /** 更新时间
 */
  updateTime: string;
}

/**
 * 供应商表单数据（用于新增/编辑
 */
export interface SupplierFormData {
  /** 供应商名
 */
  name: string;
  /** 联系
 */
  contactPerson: string;
  /** 联系电话
 */
  phone: string;
  /** 地址
 */
  address: string;
  /** 状
 */
  status: 'active' | 'inactive';
}

/**
 * 供应商查询参数
 */
export interface SupplierQueryForm {
  /** 供应商名称（模糊搜索
 */
  name?: string;
  /** 联系
 */
  contactPerson?: string;
  /** 联系电话
 */
  phone?: string;
  /** 状
 */
  status?: 'active' | 'inactive' | null;
}

/** 供应商状态选项
 */
export const SupplierStatusOptions = [
{ label: '启用',
value: 'active' },
{ label: '停用',
value: 'inactive' },
] as const;

/** 供应商状态文本映
 */
export const SupplierStatusText: Record<string, string> = {
  active: '启用',
  inactive: '停用',
};
