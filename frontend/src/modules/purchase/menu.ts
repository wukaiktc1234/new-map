import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const purchaseMenu: MenuGroupConfig = {
  id: 'purchase',
  title: '采购管理',
  icon: 'ShoppingCart',
  path: '/purchase',
  order: 50,
  category: 'management',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.PURCHASE_MANAGER, UserRole.STORE_MANAGER],
  children: [
    { title: '商品分类', icon: 'Folder', path: '/purchase/material-category' },
    { title: '商品档案', icon: 'Files', path: '/purchase/archive' },
    { title: '供应商档案', icon: 'OfficeBuilding', path: '/purchase/supplier' },
    { title: '采购申请', icon: 'Edit', path: '/purchase/request' },
    { title: '采购计划', icon: 'Calendar', path: '/purchase/plan' },
    { title: '采购订单', icon: 'Document', path: '/purchase/orders' },
    { title: '到货登记', icon: 'Box', path: '/purchase/stockin' },
    { title: '采购退货', icon: 'RefreshLeft', path: '/purchase/return' },
    { title: '采购合同', icon: 'DocumentCopy', path: '/purchase/contract' },
    { title: '电子合同', icon: 'DocumentChecked', path: '/purchase/electronic-contract' },
    { title: '签署链接', icon: 'Link', path: '/supplier-portal/links' },
    { title: '采购结算', icon: 'Money', path: '/purchase/settlement' },
    { title: '采购报表', icon: 'DataAnalysis', path: '/purchase/report' },
    { title: '采购数据分析', icon: 'TrendCharts', path: '/purchase/analysis' },
  ],
}

