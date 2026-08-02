/**
 * 物料分类模块类型定义
 * 从 api/materialCategory.ts 迁移而来
 */

/**
 * 商品分类数据接口
 */
export interface MaterialCategory {
  id: string;
  categoryName: string;
  categoryCode: string;
  description: string;
  sortOrder: number;
  status: string;
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
}

/**
 * 商品分类创建DTO
 */
export interface MaterialCategoryCreateDTO {
  categoryName: string;
  categoryCode: string;
  description: string;
  sortOrder: number;
  status: string;
}

/**
 * 商品分类更新DTO
 */
export interface MaterialCategoryUpdateDTO {
  categoryName: string;
  categoryCode: string;
  description: string;
  sortOrder: number;
  status: string;
}

/**
 * 商品分类查询参数
 */
export interface MaterialCategoryQueryParams {
  page: number;
  size: number;
  categoryName: string;
  status: string;
}
