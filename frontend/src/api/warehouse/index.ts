/**
 * 仓储管理 API 模块 - 统一导出
 */

// 仓库管理
export { warehouseApi } from './warehouse'

// 库存管理
export { inventoryApi } from './inventory'

// 库存盘点
export { inventoryCheckApi } from './inventory-check'

// 库存调拨
export { inventoryTransferApi } from './inventory-transfer'

// 库存预警
export { inventoryWarningApi, inventoryWarningRuleApi } from './inventory-warning'

// 库存报损
export { inventoryLossApi } from './inventory-loss'

// 库存出库
export { inventoryOutboundApi } from './inventory-outbound'

// 库存调整
export { inventoryAdjustApi } from './inventory-adjust'

// 库存统计
export { inventoryStatsApi } from './inventory-stats'

// 库存分析
export { inventoryAnalysisApi } from './inventory-analysis'

// 门店库存已迁移至 @/api/store-ops/store-inventory（归属门店运营模块）

// 库位管理
export { inventoryLocationApi } from './inventory-location'

// 库存日志
export { inventoryLogApi } from './inventory-log'

// 数据转换器
export {
  warehouseConverter,
  inventoryConverter,
  locationConverter,
  inventoryCheckConverter,
  inventoryTransferConverter,
  inventoryWarningConverter,
  inventoryLossConverter,
  inventoryOutboundConverter,
  inventoryAdjustConverter,
  inventoryLogConverter,
  inventoryStatsConverter,
  inventoryAnalysisConverter,
} from './converters'
