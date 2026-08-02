/**
 * 管理端路由清单
 * 从 frontend/src/router/index.ts 提取的业务页面路由
 * 排除：登录页、重定向、详情页（带动态参数）、公开H5页面
 */
export interface RouteItem {
  path: string;
  title: string;
  domain: string;
}

export const routes: RouteItem[] = [
  // 首页
  { path: '/home', title: '工作台', domain: 'public' },

  // 产品中心
  { path: '/product/food', title: '菜品管理', domain: 'product' },
  { path: '/product/category', title: '菜品分类', domain: 'product' },
  { path: '/product/combo', title: '套餐管理', domain: 'product' },
  { path: '/product/pricing', title: '菜品定价', domain: 'product' },
  { path: '/product/cost-analysis', title: '菜品成本分析', domain: 'product' },

  // 订单管理
  { path: '/order/query', title: '订单查询', domain: 'order' },
  { path: '/order/statistics', title: '订单统计', domain: 'order' },
  { path: '/order/refund', title: '退款管理', domain: 'order' },
  { path: '/order/reservation', title: '预约管理', domain: 'order' },

  // 运营中心
  { path: '/operations', title: '运营总览', domain: 'operations' },
  { path: '/operations/live-monitor', title: '实时监控', domain: 'operations' },
  { path: '/operations/decision-board', title: '经营分析', domain: 'operations' },
  { path: '/operations/strategy-workshop', title: '运营策略', domain: 'operations' },
  { path: '/operations/alert-command-center', title: '预警管理', domain: 'operations' },
  { path: '/operations/reports', title: '经营报表', domain: 'operations' },
  { path: '/operations/store-archive', title: '门店档案', domain: 'operations' },

  // 门店管理
  { path: '/store-management/pending-tasks', title: '待办事项', domain: 'store-ops' },
  { path: '/store-management/daily-settlement', title: '日结对账', domain: 'store-ops' },
  { path: '/store-management/certificate', title: '证件管理', domain: 'store-ops' },
  { path: '/store-management/recruitment', title: '门店招聘', domain: 'store-ops' },
  { path: '/store-management/table-usage', title: '桌台记录', domain: 'store-ops' },
  { path: '/store-management/queue-call-history', title: '叫号记录', domain: 'store-ops' },
  { path: '/store-management/shift', title: '排班管理', domain: 'store-ops' },
  { path: '/store-management/material-request', title: '物资需求', domain: 'store-ops' },
  { path: '/store-management/store-inventory', title: '门店库存', domain: 'store-ops' },

  // 采购管理
  { path: '/purchase/orders', title: '采购订单', domain: 'purchase' },
  { path: '/purchase/archive', title: '商品档案', domain: 'purchase' },
  { path: '/purchase/material-category', title: '商品分类', domain: 'purchase' },
  { path: '/purchase/plan', title: '采购计划', domain: 'purchase' },
  { path: '/purchase/stockin', title: '采购收货', domain: 'purchase' },
  { path: '/purchase/contract', title: '采购合同', domain: 'purchase' },
  { path: '/purchase/electronic-contract', title: '电子合同', domain: 'purchase' },
  { path: '/purchase/request', title: '采购申请', domain: 'purchase' },
  { path: '/purchase/settlement', title: '采购结算', domain: 'purchase' },
  { path: '/purchase/report', title: '采购报表', domain: 'purchase' },
  { path: '/purchase/analysis', title: '采购数据分析', domain: 'purchase' },
  { path: '/purchase/supplier', title: '供应商档案', domain: 'purchase' },
  { path: '/purchase/material-request', title: '物资需求提报', domain: 'purchase' },

  // 仓储管理
  { path: '/warehouse/overview', title: '库存概览', domain: 'warehouse' },
  { path: '/warehouse/store-inventory', title: '门店库存查看', domain: 'warehouse' },
  { path: '/warehouse/inventory', title: '库存管理', domain: 'warehouse' },
  { path: '/warehouse/warning', title: '库存预警', domain: 'warehouse' },
  { path: '/warehouse/check', title: '库存盘点', domain: 'warehouse' },
  { path: '/warehouse/report', title: '库存报表', domain: 'warehouse' },
  { path: '/warehouse/location', title: '库位管理', domain: 'warehouse' },
  { path: '/warehouse/transfer', title: '库存调拨', domain: 'warehouse' },
  { path: '/warehouse/outbound', title: '库存出库', domain: 'warehouse' },
  { path: '/warehouse/adjust', title: '库存调整', domain: 'warehouse' },
  { path: '/warehouse/inventory-loss', title: '库存报损', domain: 'warehouse' },
  { path: '/warehouse/smart-restock', title: '智能补货建议', domain: 'warehouse' },

  // 会员管理
  { path: '/marketing/member-overview', title: '会员概览', domain: 'member' },
  { path: '/marketing/member-list', title: '会员列表', domain: 'member' },
  { path: '/marketing/member-level', title: '会员等级', domain: 'member' },
  { path: '/marketing/recharge', title: '储值管理', domain: 'member' },
  { path: '/marketing/recharge-settings', title: '储值系统设置', domain: 'member' },

  // 财务中心
  { path: '/finance/ledger', title: '财务总账', domain: 'finance' },
  { path: '/finance/receivable', title: '应收账款', domain: 'finance' },
  { path: '/finance/payable', title: '应付账款', domain: 'finance' },
  { path: '/finance/cost', title: '成本管理', domain: 'finance' },
  { path: '/finance/budget', title: '预算管理', domain: 'finance' },
  { path: '/finance/fund', title: '资金管理', domain: 'finance' },
  { path: '/finance/tax', title: '税务管理', domain: 'finance' },
  { path: '/finance/invoice-reimbursement', title: '发票报销', domain: 'finance' },
  { path: '/finance/report', title: '财务报表', domain: 'finance' },
  { path: '/finance/approval', title: '财务审批', domain: 'finance' },
  { path: '/finance/auto-voucher', title: '自动凭证管理', domain: 'finance' },
  { path: '/finance/period', title: '会计期间管理', domain: 'finance' },
  { path: '/finance/subject', title: '会计科目管理', domain: 'finance' },

  // 资产管理
  { path: '/asset/overview', title: '资产概览', domain: 'asset' },
  { path: '/asset/ledger', title: '资产台账', domain: 'asset' },
  { path: '/asset/category', title: '资产分类', domain: 'asset' },
  { path: '/asset/depreciation', title: '资产折旧', domain: 'asset' },
  { path: '/asset/inventory-check', title: '资产盘点', domain: 'asset' },
  { path: '/asset/maintenance', title: '资产维护', domain: 'asset' },
  { path: '/asset/disposal', title: '资产处置', domain: 'asset' },
  { path: '/asset/report', title: '资产报表', domain: 'asset' },

  // 人事管理
  { path: '/hr/employee', title: '员工管理', domain: 'hr' },
  { path: '/hr/organization', title: '组织架构', domain: 'hr' },
  { path: '/hr/attendance', title: '考勤排班', domain: 'hr' },
  { path: '/hr/salary', title: '薪资管理', domain: 'hr' },
  { path: '/hr/recruitment', title: '招聘管理', domain: 'hr' },
  { path: '/hr/knowledge-base', title: '知识库管理', domain: 'hr' },
  { path: '/hr/knowledge-intelligence', title: '知识库智能', domain: 'hr' },
  { path: '/hr/training', title: '培训管理', domain: 'hr' },
  { path: '/hr/health-certificate', title: '健康证管理', domain: 'hr' },
  { path: '/hr/contract', title: '合同管理', domain: 'hr' },
  { path: '/hr/contract-dashboard', title: '合同智能管理', domain: 'hr' },
  { path: '/hr/contract-template', title: '合同模板', domain: 'hr' },
  { path: '/hr/contract-template-library', title: '合同模板库', domain: 'hr' },
  { path: '/hr/config-center', title: '配置中心', domain: 'hr' },
  { path: '/hr/approval', title: '审批管理', domain: 'hr' },
  { path: '/hr/analytics', title: '人事分析', domain: 'hr' },
  { path: '/hr/employee-intelligence', title: '员工智能画像', domain: 'hr' },
  { path: '/hr/invitation-code', title: '邀请码管理', domain: 'hr' },
  { path: '/hr/position', title: '岗位管理', domain: 'hr' },
  { path: '/hr/job-level', title: '职级体系', domain: 'hr' },
  { path: '/hr/onboarding', title: '入职办理', domain: 'hr' },

  // 溯源管理
  { path: '/traceability/query', title: '追溯查询', domain: 'traceability' },
  { path: '/traceability/material-code', title: '原料追溯', domain: 'traceability' },
  { path: '/traceability/food-trace-code', title: '食品追溯', domain: 'traceability' },
  { path: '/traceability/chain', title: '追溯链展示', domain: 'traceability' },
  { path: '/traceability/expiry-warning', title: '临期预警', domain: 'traceability' },
  { path: '/traceability/recall', title: '召回管理', domain: 'traceability' },
  { path: '/traceability/quality', title: '质量追溯', domain: 'traceability' },
  { path: '/traceability/label-template', title: '标签模板', domain: 'traceability' },
  { path: '/traceability/supplier', title: '供应商追溯', domain: 'traceability' },
  { path: '/traceability/inspection', title: '检验记录', domain: 'traceability' },

  // 设备管理
  { path: '/device/list', title: '设备列表', domain: 'device' },
  { path: '/device/monitor', title: '设备监控', domain: 'device' },
  { path: '/device/alerts', title: '设备告警', domain: 'device' },
  { path: '/device/status-history', title: '状态历史', domain: 'device' },

  // 系统管理
  { path: '/system/operation-audit', title: '操作审计', domain: 'system' },
  { path: '/system/permission', title: '系统配置', domain: 'system' },
  { path: '/system/permission-center', title: '权限中心', domain: 'system' },
  { path: '/system/ai-model-config', title: 'AI模型配置', domain: 'system' },

  // 电子签章
  { path: '/seal/management', title: '印章管理', domain: 'system' },

  // 供应商签署门户
  { path: '/supplier-portal/links', title: '签署链接管理', domain: 'purchase' },

  // 个人中心
  { path: '/personal-center', title: '个人中心', domain: 'public' },
];
