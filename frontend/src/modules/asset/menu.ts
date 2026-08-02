import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const assetMenu: MenuGroupConfig = {
  id: 'asset',
  title: '资产管理',
  icon: 'Coin',
  path: '/asset',
  order: 90,
  category: 'finance',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN],
  children: [
    { title: '资产概览', icon: 'DataBoard', path: '/asset/overview' },
    { title: '资产台账', icon: 'Document', path: '/asset/ledger' },
    { title: '资产分类', icon: 'Folder', path: '/asset/category' },
    { title: '资产折旧', icon: 'TrendCharts', path: '/asset/depreciation' },
    { title: '资产盘点', icon: 'Check', path: '/asset/inventory-check' },
    { title: '资产维护', icon: 'SetUp', path: '/asset/maintenance' },
    { title: '资产处置', icon: 'SwitchButton', path: '/asset/disposal' },
    { title: '资产报表', icon: 'DataAnalysis', path: '/asset/report' },
  ],
}

