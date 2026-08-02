/**
 * 商品档案相关类型定义
 * 对应后端实体: MaterialArchive (material_archive)
 */

/**
 * 商品档案状态
 */
export type MaterialArchiveStatus =
  | 'active'    // 启用
  | 'inactive'; // 停用

/**
 * 商品档案信息
 */
export interface MaterialArchiveInfo {
  /** 商品ID */
  materialId: string;
  /** 商品编码 */
  materialCode: string;
  /** 商品名称 */
  materialName: string;
  /** 分类名称 */
  categoryName: string;
  /** 分类ID */
  categoryId: string;
  /** 单位 */
  unit: string;
  /** 规格型号 */
  spec: string;
  /** 参考价（元） */
  referencePrice: number;
  /** 条码 */
  barcode: string;
  /** 产地 */
  origin: string;
  /** 保质期 */
  shelfLife: string;
  /** 存储条件 */
  storageCondition: string;
  /** 使用部门ID */
  departmentId: string;
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName: string;
  /** 状态 */
  status: MaterialArchiveStatus;
  /** 备注 */
  remark: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 商品档案查询参数
 */
export interface MaterialArchiveQueryForm {
  /** 分类名称 */
  categoryName?: string;
  /** 分类ID */
  categoryId?: string;
  departmentId?: string;
  /** 搜索关键词（名称或编码） */
  keyword?: string;
  /** 状态 */
  status?: MaterialArchiveStatus | null;
}

/**
 * 商品档案表单数据（新建/编辑）
 */
export interface MaterialArchiveFormData {
  /** 商品名称 */
  materialName: string;
  /** 分类ID */
  categoryId: string;
  /** 单位 */
  unit: string;
  /** 规格型号 */
  spec: string;
  /** 参考价（元） */
  referencePrice: number;
  /** 条码 */
  barcode: string;
  /** 产地 */
  origin: string;
  /** 保质期 */
  shelfLife: string;
  /** 存储条件 */
  storageCondition: string;
  /** 使用部门ID */
  departmentId: string;
  /** 供应商ID */
  supplierId: string;
  /** 备注 */
  remark: string;
}

/** 商品档案状态选项 */
export const MaterialArchiveStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' },
] as const;
