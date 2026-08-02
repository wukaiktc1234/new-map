/**
 * 采购管理 API 模块 - 统一导出
 *
 * 导出所有采购相关的API和数据转换器
 */

// 采购订单
export { purchaseOrderApi } from './order'

// 商品档案
export { materialArchiveApi } from './archive'

// 商品分类
export { materialCategoryApi } from './category'

// 采购计划
export { purchasePlanApi } from './plan'

// 采购收货（旧版，逐步替换为到货登记）
export { purchaseStockinApi } from './stockin'

// 采购到货单（到货登记）
export { purchaseArrivalApi, purchaseArrivalConverter } from './arrival'

// 采购合同
export { purchaseContractApi } from './contract'

// 电子合同
export { electronicContractApi } from './electronic-contract'

// 采购申请
export { purchaseRequestApi } from './request'

// 物资需求提报
export { materialRequestApi } from './material-request'

// 采购结算
export { purchaseSettlementApi } from './settlement'

// 采购退货
export { purchaseReturnApi } from './return'

// 供应商档案
export { supplierApi } from './supplier'

// 采购报表
export { purchaseReportApi, purchaseReportConverter } from './report'

// 采购数据分析
export { purchaseAnalysisApi, purchaseAnalysisConverter } from './analysis'

// 数据转换器（集中导出，定义在 converters.ts 中）
export {
  purchaseOrderConverter,
  materialArchiveConverter,
  materialCategoryConverter,
  purchasePlanConverter,
  purchaseStockinConverter,
  purchaseContractConverter,
  electronicContractConverter,
  purchaseRequestConverter,
  materialRequestConverter,
  purchaseSettlementConverter,
  purchaseReturnConverter,
  supplierConverter,
} from './converters'
