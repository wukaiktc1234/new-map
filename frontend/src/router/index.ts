import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'
import { UserRole } from '@/stores/permission'

/**
 * 路由权限域映射
 * 每个业务路由通过 domain 字段关联到权限域，由路由守卫统一检查
 * admin 角色自动通过所有域检查
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ========== 公开路由 ==========
    { path: '/login', name: 'Login', component: () => import('@/views/login/LoginPage.vue'), meta: { title: '登录', requireAuth: false } },
    { path: '/', redirect: '/home' },
    { path: '/home', name: 'Home', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
    { path: '/company-init', name: 'CompanyInit', component: () => import('@/views/company/CompanyInit.vue'), meta: { title: '公司初始化向导', domain: 'system' } },
    { path: '/demo/component-gallery', name: 'ComponentGallery', component: () => import('@/demo/ComponentGallery.vue'), meta: { title: 'UI组件参考手册', requireAuth: false } },

    // ========== 产品中心 ==========
    { path: '/product/food', name: 'ProductFood', component: () => import('@/views/product/FoodManagement.vue'), meta: { title: '菜品管理', domain: 'product' } },
    { path: '/product/category', name: 'ProductCategory', component: () => import('@/views/product/CategoryManagement.vue'), meta: { title: '菜品分类', domain: 'product' } },
    { path: '/product/combo', name: 'ProductCombo', component: () => import('@/views/product/DishCombo.vue'), meta: { title: '套餐管理', domain: 'product' } },
    // { path: '/product/bom', name: 'ProductBOM', redirect: '/product/food' }, // 配方管理(BOM)已废弃
    { path: '/product/pricing', name: 'ProductPricing', component: () => import('@/views/product/DishPricing.vue'), meta: { title: '菜品定价', domain: 'product' } },
    { path: '/product/cost-analysis', name: 'ProductCostAnalysis', component: () => import('@/views/product/DishCostAnalysis.vue'), meta: { title: '菜品成本分析', domain: 'product' } },

    // ========== 订单管理 ==========
    { path: '/order/query', name: 'OrderQuery', component: () => import('@/views/order/OrderQuery.vue'), meta: { title: '订单查询', domain: 'order' } },
    { path: '/order/statistics', name: 'OrderStatistics', component: () => import('@/views/order/OrderStatistics.vue'), meta: { title: '订单统计', domain: 'order' } },
    { path: '/order/refund', name: 'OrderRefund', component: () => import('@/views/order/OrderRefund.vue'), meta: { title: '退款管理', domain: 'order' } },
    { path: '/order/reservation', name: 'OrderReservation', component: () => import('@/views/order/OrderReservation.vue'), meta: { title: '预约管理', domain: 'order' } },

    // ========== 运营中心（公司级·数据聚合+分析决策+策略+预警） ==========
    { path: '/operations', name: 'OperationsDashboard', component: () => import('@/views/operations/OperationsDashboard.vue'), meta: { title: '运营总览', domain: 'operations' } },
    { path: '/operations/live-monitor', name: 'LiveMonitor', component: () => import('@/views/operations/LiveMonitor.vue'), meta: { title: '实时监控', domain: 'operations' } },
    { path: '/operations/decision-board', name: 'DecisionBoard', component: () => import('@/views/operations/DecisionBoard.vue'), meta: { title: '经营分析', domain: 'operations' } },
    { path: '/operations/strategy-workshop', name: 'StrategyWorkshop', component: () => import('@/views/operations/StrategyWorkshop.vue'), meta: { title: '运营策略', domain: 'operations' } },
    { path: '/operations/alert-command-center', name: 'AlertCommandCenter', component: () => import('@/views/operations/AlertCommandCenter.vue'), meta: { title: '预警管理', domain: 'operations' } },

    // ========== 经营报表中心（统一入口） ==========
    { path: '/operations/reports', name: 'ReportCenter', component: () => import('@/views/operations/reports/ReportCenter.vue'), meta: { title: '经营报表', domain: 'operations' } },

    // ========== 门店管理（门店级） ==========
    { path: '/operations/store-archive', name: 'StoreArchive', component: () => import('@/views/store-ops/StoreArchive.vue'), meta: { title: '门店档案', domain: 'operations' } },
    { path: '/store-management/pending-tasks', name: 'StorePendingTasks', component: () => import('@/views/store-ops/StorePendingTasks.vue'), meta: { title: '待办事项', domain: 'store-ops' } },
    { path: '/store-management/daily-settlement', name: 'StoreDailySettlement', component: () => import('@/views/store-ops/StoreDailySettlement.vue'), meta: { title: '日结对账', domain: 'store-ops' } },
    { path: '/store-management/certificate', name: 'StoreCertificate', component: () => import('@/views/store-ops/StoreCertificate.vue'), meta: { title: '证件管理', domain: 'store-ops' } },
    { path: '/store-management/recruitment', name: 'StoreRecruitment', component: () => import('@/views/store-ops/StoreRecruitment.vue'), meta: { title: '门店招聘', domain: 'store-ops' } },
    { path: '/store-management/table-usage', name: 'StoreTableUsage', component: () => import('@/views/store-ops/StoreTableUsage.vue'), meta: { title: '桌台记录', domain: 'store-ops' } },
    { path: '/store-management/queue-call-history', name: 'StoreQueueHistory', component: () => import('@/views/store-ops/StoreQueueHistory.vue'), meta: { title: '叫号记录', domain: 'store-ops' } },
    { path: '/store-management/shift', name: 'ShiftManagement', component: () => import('@/views/store-ops/ShiftManagement.vue'), meta: { title: '排班管理', domain: 'store-ops' } },
    { path: '/workspace/material-request', name: 'StoreMaterialRequest', component: () => import('@/views/store-ops/StoreMaterialRequest.vue'), meta: { title: '物资需求提报', domain: 'workspace' } },
    { path: '/store-management/store-inventory', name: 'StoreManagementInventory', component: () => import('@/views/store-ops/StoreInventory.vue'), meta: { title: '门店库存', domain: 'store-ops' } },
    { path: '/store-management/store-receiving', name: 'StoreReceiving', component: () => import('@/views/store-ops/StoreReceiving.vue'), meta: { title: '门店收货', domain: 'store-ops' } },

    // ========== URL重定向规则（旧路径 → 新路径，确保平滑过渡） ==========
    { path: '/operations/multi-store-monitor', redirect: '/operations/live-monitor' },
    { path: '/operations/business-report', redirect: '/operations/decision-board' },
    { path: '/operations/alert-center', redirect: '/operations/alert-command-center' },
    { path: '/store-ops/pending-tasks', redirect: '/store-management/pending-tasks' },
    { path: '/store-ops/daily-settlement', redirect: '/store-management/daily-settlement' },
    { path: '/store-ops/certificate', redirect: '/store-management/certificate' },
    { path: '/store-ops/recruitment', redirect: '/store-management/recruitment' },
    { path: '/store-ops/table-usage', redirect: '/store-management/table-usage' },
    { path: '/store-ops/queue-history', redirect: '/store-management/queue-call-history' },
    { path: '/store-ops/operation-log', redirect: '/system/operation-audit' },

    // ========== 采购管理 ==========
    { path: '/purchase/orders', name: 'PurchaseOrderList', component: () => import('@/views/purchase/PurchaseOrder.vue'), meta: { title: '采购订单', domain: 'purchase', permissions: ['purchase:order:view'] } },
    { path: '/purchase/archive', name: 'PurchaseArchive', component: () => import('@/views/purchase/PurchaseArchive.vue'), meta: { title: '商品档案', domain: 'purchase' } },
    { path: '/purchase/material-category', name: 'MaterialCategory', component: () => import('@/views/purchase/MaterialCategory.vue'), meta: { title: '商品分类', domain: 'purchase' } },
    { path: '/purchase/plan', name: 'PurchasePlan', component: () => import('@/views/purchase/PurchasePlan.vue'), meta: { title: '采购计划', domain: 'purchase' } },
    { path: '/purchase/stockin', name: 'PurchaseStockin', component: () => import('@/views/purchase/PurchaseStockin.vue'), meta: { title: '到货登记', domain: 'purchase' } },
    { path: '/purchase/return', name: 'PurchaseReturn', component: () => import('@/views/purchase/PurchaseReturn.vue'), meta: { title: '采购退货', domain: 'purchase' } },
    { path: '/purchase/contract', name: 'PurchaseContract', component: () => import('@/views/purchase/PurchaseContract.vue'), meta: { title: '采购合同', domain: 'purchase', permissions: ['purchase:contract:view'] } },
    { path: '/purchase/electronic-contract', name: 'ElectronicContract', component: () => import('@/views/purchase/ElectronicContract.vue'), meta: { title: '电子合同', domain: 'purchase', permissions: ['purchase:electronic-contract:view'] } },
    { path: '/purchase/request', name: 'PurchaseRequest', component: () => import('@/views/purchase/PurchaseRequest.vue'), meta: { title: '采购申请', domain: 'purchase' } },
    { path: '/purchase/settlement', name: 'PurchaseSettlement', component: () => import('@/views/purchase/PurchaseSettlement.vue'), meta: { title: '采购结算', domain: 'purchase', permissions: ['purchase:settlement:view'] } },
    { path: '/purchase/report', name: 'PurchaseReport', component: () => import('@/views/purchase/PurchaseReport.vue'), meta: { title: '采购报表', domain: 'purchase' } },
    { path: '/purchase/analysis', name: 'PurchaseAnalysis', component: () => import('@/views/purchase/PurchaseAnalysis.vue'), meta: { title: '采购数据分析', domain: 'purchase' } },
    { path: '/purchase/supplier', name: 'SupplierArchive', component: () => import('@/views/purchase/SupplierArchive.vue'), meta: { title: '供应商档案', domain: 'purchase' } },
    // 【下线】采购模块物资需求提报：统一由门店运营「物资需求」入口（/store-management/material-request）提报

    // ========== 仓储管理 ==========
    { path: '/warehouse/overview', name: 'WarehouseOverview', component: () => import('@/views/warehouse/WarehouseOverview.vue'), meta: { title: '库存概览', domain: 'warehouse' } },
    { path: '/warehouse/inventory-stockin', name: 'InventoryStockin', component: () => import('@/views/warehouse/InventoryStockin.vue'), meta: { title: '库存入库', domain: 'warehouse' } },
    { path: '/warehouse/store-inventory', name: 'StoreInventory', component: () => import('@/views/warehouse/StoreInventory.vue'), meta: { title: '门店库存查看', domain: 'warehouse' } },
    { path: '/warehouse/inventory', name: 'WarehouseInventory', component: () => import('@/views/product/Inventory.vue'), meta: { title: '库存管理', domain: 'warehouse' } },
    { path: '/warehouse/warning', name: 'InventoryWarning', component: () => import('@/views/warehouse/InventoryWarning.vue'), meta: { title: '库存预警', domain: 'warehouse' } },
    { path: '/warehouse/check', name: 'InventoryCheck', component: () => import('@/views/warehouse/InventoryCheck.vue'), meta: { title: '库存盘点', domain: 'warehouse' } },
    { path: '/warehouse/report', name: 'InventoryReport', component: () => import('@/views/warehouse/InventoryReport.vue'), meta: { title: '库存报表', domain: 'warehouse' } },
    { path: '/warehouse/location', name: 'InventoryLocation', component: () => import('@/views/warehouse/InventoryLocation.vue'), meta: { title: '库位管理', domain: 'warehouse' } },
    { path: '/warehouse/transfer', name: 'InventoryTransfer', component: () => import('@/views/warehouse/InventoryTransfer.vue'), meta: { title: '库存调拨', domain: 'warehouse' } },
    { path: '/warehouse/outbound', name: 'WarehouseOutbound', component: () => import('@/views/warehouse/InventoryOutbound.vue'), meta: { title: '库存出库', domain: 'warehouse', permissions: ['warehouse:outbound:view'] } },
    { path: '/warehouse/adjust', name: 'WarehouseAdjust', component: () => import('@/views/warehouse/InventoryAdjust.vue'), meta: { title: '库存调整', domain: 'warehouse' } },
    { path: '/warehouse/inventory-loss', name: 'InventoryLoss', component: () => import('@/views/warehouse/InventoryLoss.vue'), meta: { title: '库存报损', domain: 'warehouse' } },
    { path: '/warehouse/smart-restock', name: 'SmartRestock', component: () => import('@/views/warehouse/SmartRestock.vue'), meta: { title: '智能补货建议', domain: 'warehouse' } },

    // ========== 会员管理 ==========
    { path: '/marketing/member-overview', name: 'MemberOverview', component: () => import('@/views/marketing/MemberOverview.vue'), meta: { title: '会员概览', domain: 'member' } },
    { path: '/marketing/member-list', name: 'MemberList', component: () => import('@/views/marketing/MemberList.vue'), meta: { title: '会员列表', domain: 'member' } },
    { path: '/marketing/member-detail/:id', name: 'MemberDetail', component: () => import('@/views/marketing/MemberDetail.vue'), meta: { title: '会员详情', domain: 'member' } },
    { path: '/marketing/member-level', name: 'MemberLevel', component: () => import('@/views/marketing/MemberLevel.vue'), meta: { title: '会员等级', domain: 'member' } },
    { path: '/marketing/recharge', name: 'RechargeManage', component: () => import('@/views/marketing/RechargeManage.vue'), meta: { title: '储值管理', domain: 'member' } },
    { path: '/marketing/recharge-settings', name: 'MarketingRechargeSettings', component: () => import('@/views/marketing/RechargeSettings.vue'), meta: { title: '储值系统设置', domain: 'member' } },

    // ========== 财务中心 ==========
    { path: '/finance', redirect: '/finance/ledger' },
    { path: '/finance/ledger', name: 'FinanceLedger', component: () => import('@/views/finance/FinanceLedger.vue'), meta: { title: '财务总账', domain: 'finance', permissions: ['finance:ledger:view'] } },
    { path: '/finance/receivable', name: 'FinanceReceivable', component: () => import('@/views/finance/FinanceReceivable.vue'), meta: { title: '应收账款', domain: 'finance' } },
    { path: '/finance/payable', name: 'FinancePayable', component: () => import('@/views/finance/FinancePayable.vue'), meta: { title: '应付账款', domain: 'finance' } },
    { path: '/finance/cost', name: 'FinanceCost', component: () => import('@/views/finance/FinanceCost.vue'), meta: { title: '成本管理', domain: 'finance' } },
    { path: '/finance/budget', name: 'FinanceBudget', component: () => import('@/views/finance/FinanceBudget.vue'), meta: { title: '预算管理', domain: 'finance' } },
    { path: '/finance/fund', name: 'FinanceFund', component: () => import('@/views/finance/FinanceFund.vue'), meta: { title: '资金管理', domain: 'finance' } },
    { path: '/finance/tax', name: 'FinanceTax', component: () => import('@/views/finance/FinanceTax.vue'), meta: { title: '税务管理', domain: 'finance' } },
    { path: '/finance/invoice-reimbursement', name: 'InvoiceReimbursement', component: () => import('@/views/finance/InvoiceReimbursement.vue'), meta: { title: '发票报销', domain: 'finance' } },
    { path: '/finance/report', name: 'FinanceReport', component: () => import('@/views/finance/FinanceReport.vue'), meta: { title: '财务报表', domain: 'finance', permissions: ['finance:report:view'] } },
    { path: '/finance/approval', name: 'FinanceApproval', component: () => import('@/views/finance/FinanceApproval.vue'), meta: { title: '财务审批', domain: 'finance', permissions: ['finance:approval:view'] } },
    { path: '/finance/auto-voucher', name: 'AutoVoucher', component: () => import('@/views/finance/AutoVoucher.vue'), meta: { title: '自动凭证管理', domain: 'finance' } },
    { path: '/finance/period', name: 'FinancePeriod', component: () => import('@/views/finance/FinancePeriod.vue'), meta: { title: '会计期间管理', domain: 'finance' } },
    { path: '/finance/subject', name: 'FinanceSubject', component: () => import('@/views/finance/FinanceSubject.vue'), meta: { title: '会计科目管理', domain: 'finance' } },

    // ========== 资产管理 ==========
    { path: '/asset/overview', name: 'AssetOverview', component: () => import('@/views/asset/AssetOverview.vue'), meta: { title: '资产概览', domain: 'asset' } },
    { path: '/asset/ledger', name: 'AssetLedger', component: () => import('@/views/asset/AssetLedger.vue'), meta: { title: '资产台账', domain: 'asset' } },
    { path: '/asset/category', name: 'AssetCategory', component: () => import('@/views/asset/AssetCategory.vue'), meta: { title: '资产分类', domain: 'asset' } },
    { path: '/asset/depreciation', name: 'AssetDepreciation', component: () => import('@/views/asset/AssetDepreciation.vue'), meta: { title: '资产折旧', domain: 'asset' } },
    { path: '/asset/inventory-check', name: 'AssetInventory', component: () => import('@/views/asset/AssetInventory.vue'), meta: { title: '资产盘点', domain: 'asset' } },
    { path: '/asset/maintenance', name: 'AssetMaintenance', component: () => import('@/views/asset/AssetMaintenance.vue'), meta: { title: '资产维护', domain: 'asset' } },
    { path: '/asset/disposal', name: 'AssetDisposal', component: () => import('@/views/asset/AssetDisposal.vue'), meta: { title: '资产处置', domain: 'asset' } },
    { path: '/asset/report', name: 'AssetReport', component: () => import('@/views/asset/AssetReport.vue'), meta: { title: '资产报表', domain: 'asset' } },

    // ========== 人事管理 ==========
    { path: '/hr/employee', name: 'HREmployee', component: () => import('@/views/hr/HREmployee.vue'), meta: { title: '员工管理', domain: 'hr' } },
    { path: '/hr/organization', name: 'HROrganization', component: () => import('@/views/hr/HROrganization.vue'), meta: { title: '组织架构', domain: 'hr' } },
    { path: '/hr/attendance', name: 'HRAttendance', component: () => import('@/views/hr/HRAttendance.vue'), meta: { title: '考勤排班', domain: 'hr' } },
    { path: '/hr/salary', name: 'HRSalary', component: () => import('@/views/hr/HRSalary.vue'), meta: { title: '薪资管理', domain: 'hr', permissions: ['hr:salary:view'] } },
    { path: '/hr/recruitment', name: 'HRRecruitment', component: () => import('@/views/hr/HRRecruitment.vue'), meta: { title: '招聘管理', domain: 'hr' } },
    { path: '/hr/knowledge-base', name: 'HRKnowledgeBase', component: () => import('@/views/hr/HRKnowledgeBase.vue'), meta: { title: '知识库管理', domain: 'hr' } },
    { path: '/hr/knowledge-intelligence', name: 'KnowledgeIntelligence', component: () => import('@/views/hr/KnowledgeIntelligence.vue'), meta: { title: '知识库智能', domain: 'hr' } },
    { path: '/hr/training', name: 'HRTraining', component: () => import('@/views/hr/HRTraining.vue'), meta: { title: '培训发展', domain: 'hr' } },
    { path: '/hr/study-records', name: 'KnowledgeStudyRecords', component: () => import('@/views/hr/KnowledgeStudyRecords.vue'), meta: { title: '学习记录', domain: 'hr', permissions: ['hr:study-record:view'] } },
    { path: '/hr/health-certificate', name: 'HRHealthCertificate', component: () => import('@/views/hr/HRHealthCertificate.vue'), meta: { title: '健康证管理', domain: 'hr' } },
    { path: '/hr/contract', name: 'HRContract', component: () => import('@/views/hr/HRContract.vue'), meta: { title: '合同管理', domain: 'hr', permissions: ['hr:contract:view'] } },
    { path: '/hr/contract-dashboard', name: 'ContractDashboard', component: () => import('@/views/hr/ContractDashboard.vue'), meta: { title: '合同智能管理', domain: 'hr' } },
    { path: '/hr/contract-template', name: 'HRContractTemplate', component: () => import('@/views/hr/HRContractTemplate.vue'), meta: { title: '合同模板', domain: 'hr' } },
    { path: '/hr/contract-template-library', name: 'ContractTemplateLibrary', component: () => import('@/views/hr/ContractTemplateLibrary.vue'), meta: { title: '合同模板库', domain: 'hr' } },
    { path: '/hr/config-center', name: 'HRConfigCenter', component: () => import('@/views/hr/HRConfigCenter.vue'), meta: { title: '配置中心', domain: 'hr' } },
    { path: '/hr/approval', name: 'HRApproval', component: () => import('@/views/hr/HRApproval.vue'), meta: { title: '合规审批', domain: 'hr' } },
    { path: '/hr/analytics', name: 'HRAnalytics', component: () => import('@/views/hr/HRAnalytics.vue'), meta: { title: '人事分析', domain: 'hr' } },
    { path: '/hr/employee-intelligence', name: 'EmployeeIntelligence', component: () => import('@/views/hr/EmployeeIntelligence.vue'), meta: { title: '员工画像', domain: 'hr' } },
    { path: '/hr/invitation-code', name: 'HRInvitationCode', component: () => import('@/views/hr/HRInvitationCode.vue'), meta: { title: '邀请码管理', domain: 'hr' } },
    { path: '/hr/position', name: 'HRPosition', component: () => import('@/views/hr/HRPosition.vue'), meta: { title: '岗位管理', domain: 'hr' } },
    { path: '/hr/onboarding', name: 'HROnboarding', component: () => import('@/views/hr/HROnboarding.vue'), meta: { title: '入职办理', domain: 'hr' } },

    // ========== 溯源管理 ==========
    { path: '/traceability/query', name: 'TraceabilityQuery', component: () => import('@/views/traceability/TraceQuery.vue'), meta: { title: '追溯查询', domain: 'traceability' } },
    { path: '/traceability/material-code', name: 'MaterialTraceCode', component: () => import('@/views/traceability/MaterialTraceCode.vue'), meta: { title: '原料追溯', domain: 'traceability' } },
    { path: '/traceability/food-trace-code', name: 'FoodTraceCode', component: () => import('@/views/traceability/FoodTraceCode.vue'), meta: { title: '食品追溯', domain: 'traceability' } },
    { path: '/traceability/chain', name: 'TraceChainView', component: () => import('@/views/traceability/TraceChainView.vue'), meta: { title: '追溯链展示', domain: 'traceability' } },
    { path: '/traceability/expiry-warning', name: 'ExpiryWarning', component: () => import('@/views/traceability/ExpiryWarning.vue'), meta: { title: '临期预警', domain: 'traceability' } },
    { path: '/traceability/recall', name: 'RecallManagement', component: () => import('@/views/traceability/RecallManagement.vue'), meta: { title: '召回管理', domain: 'traceability', permissions: ['traceability:recall:view'] } },
    { path: '/traceability/quality', name: 'TraceabilityQuality', component: () => import('@/views/traceability/TraceabilityQuality.vue'), meta: { title: '质量追溯', domain: 'traceability' } },
    { path: '/traceability/label-template', name: 'LabelTemplate', component: () => import('@/views/traceability/LabelTemplate.vue'), meta: { title: '标签模板', domain: 'traceability' } },
    { path: '/traceability/supplier', name: 'TraceabilitySupplier', component: () => import('@/views/traceability/SupplierTrace.vue'), meta: { title: '供应商追溯', domain: 'traceability' } },
    { path: '/traceability/inspection', name: 'TraceabilityInspection', component: () => import('@/views/traceability/TraceabilityInspection.vue'), meta: { title: '检验记录', domain: 'traceability' } },

    // ========== 旧路径重定向 ==========
    { path: '/analytics/dashboard', redirect: '/operations/decision-board' },
    { path: '/analytics/sales', redirect: '/operations/decision-board' },
    { path: '/marketing/dashboard', redirect: '/operations/strategy-workshop' },

    // ========== 设备管理 ==========
    { path: '/device/list', name: 'DeviceList', component: () => import('@/views/device/DeviceList.vue'), meta: { title: '设备列表', domain: 'device' } },
    { path: '/device/monitor', name: 'DeviceMonitor', component: () => import('@/views/device/DeviceMonitor.vue'), meta: { title: '设备监控', domain: 'device' } },
    { path: '/device/alerts', name: 'DeviceAlerts', component: () => import('@/views/device/DeviceAlerts.vue'), meta: { title: '设备告警', domain: 'device' } },
    { path: '/device/status-history', name: 'DeviceStatusHistory', component: () => import('@/views/device/DeviceStatusHistory.vue'), meta: { title: '状态历史', domain: 'device' } },

    // ========== 系统管理 ==========
    { path: '/system/operation-audit', name: 'OperationAudit', component: () => import('@/views/system/OperationAudit.vue'), meta: { title: '操作审计', domain: 'system', roles: [UserRole.OWNER, UserRole.ADMIN] } },
    { path: '/system/permission', name: 'SystemPermission', component: () => import('@/views/system/SystemSettings.vue'), meta: { title: '系统配置', domain: 'system', roles: [UserRole.OWNER, UserRole.ADMIN] } },
    { path: '/system/permission-center', name: 'PermissionCenter', component: () => import('@/views/system/PermissionCenter.vue'), meta: { title: '权限中心', domain: 'system', roles: [UserRole.OWNER, UserRole.ADMIN] } },
    { path: '/system/ai-model-config', name: 'AIModelConfig', component: () => import('@/views/system/AIModelConfig.vue'), meta: { title: 'AI模型配置', domain: 'system', roles: [UserRole.OWNER, UserRole.ADMIN] } },

    // ========== 电子签章 ==========
    { path: '/seal/management', name: 'SealManagement', component: () => import('@/views/seal/SealManagement.vue'), meta: { title: '印章管理', domain: 'system', roles: [UserRole.OWNER, UserRole.ADMIN, UserRole.HR_DIRECTOR, UserRole.FINANCE_DIRECTOR, UserRole.OPS_DIRECTOR] } },

    // ========== 供应商签署门户 ==========
    { path: '/supplier-portal/links', name: 'SignLinkManagement', component: () => import('@/views/supplier-portal/SignLinkManagement.vue'), meta: { title: '签署链接', domain: 'purchase' } },
    // H5签署页面（无需登录，通过token鉴权）
    { path: '/portal/sign', name: 'SupplierSignPortal', component: () => import('@/views/supplier-portal/SupplierSignPortal.vue'), meta: { title: '供应商签署', public: true } },

    // ========== 个人中心 ==========
    { path: '/personal-center', name: 'PersonalCenter', component: () => import('@/views/personal/PersonalCenter.vue'), meta: { title: '个人中心' } },
  ]
})

// 页面标题同步：路由变化时自动更新浏览器标签页标题
router.afterEach((to) => {
  const pageTitle = to.meta?.title as string | undefined
  document.title = pageTitle ? `${pageTitle} - 食品溯源系统` : '食品溯源系统'
})

// 注册路由权限守卫（必须在路由定义完成后调用）
setupRouterGuards(router)

export default router
