/**
 * 资产管理模块 API 统一导出
 */

// 资产管理
export { default as assetApi } from './asset'

// 资产分类
export { categoryApi } from './category'

// 折旧管理
export { depreciationApi } from './depreciation'

// 盘点管理
export { inventoryApi } from './inventory'

// 维修管理
export { maintenanceApi } from './maintenance'

// 处置管理
export { disposalApi } from './disposal'

// 调拨管理
export { transferApi } from './transfer'
