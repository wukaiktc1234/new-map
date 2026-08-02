/**
 * 商品分类相关类型定义
 * 对应后端实体: MaterialCategory (material_category)
 */

/**
 * 商品分类状态
 */
export type MaterialCategoryStatus =
  | 'active'    // 启用
  | 'inactive'; // 停用

/**
 * 商品分类信息
 */
export interface MaterialCategoryInfo {
  /** 分类ID */
  categoryId: string;
  /** 分类名称 */
  categoryName: string;
  /** 分类编码 */
  categoryCode: string;
  /** 父级分类ID */
  parentId: string;
  /** 父级分类名称 */
  parentName: string;
  /** 排序号 */
  sortOrder: number;
  /** 分类下商品数量 */
  materialCount: number;
  /** 状态 */
  status: MaterialCategoryStatus;
  /** 备注 */
  remark: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 商品分类查询参数
 */
export interface MaterialCategoryQueryForm {
  /** 搜索关键词 */
  keyword?: string;
  /** 状态 */
  status?: MaterialCategoryStatus | null;
}

/**
 * 商品分类表单数据（新建/编辑）
 */
export interface MaterialCategoryFormData {
  /** 分类名称 */
  categoryName: string;
  /** 分类编码 */
  categoryCode: string;
  /** 父级分类ID */
  parentId?: string;
  /** 排序号 */
  sortOrder?: number;
  /** 备注 */
  remark?: string;
}

/** 商品分类状态选项 */
export const MaterialCategoryStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' },
] as const;
