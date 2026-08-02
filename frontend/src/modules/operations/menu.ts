import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const operationsMenu: MenuGroupConfig = {
  id: 'operations',
  title: '运营中心',
  icon: 'DataBoard',
  path: '/operations',
  order: 30,
  category: 'operation',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR],
  children: [
    { title: '运营总览', icon: 'Odometer', path: '/operations' },
    { title: '门店档案', icon: 'OfficeBuilding', path: '/operations/store-archive' },
    { title: '实时监控', icon: 'Monitor', path: '/operations/live-monitor' },
    { title: '经营分析', icon: 'TrendCharts', path: '/operations/decision-board' },
    { title: '运营策略', icon: 'MagicStick', path: '/operations/strategy-workshop' },
    { title: '预警管理', icon: 'WarningFilled', path: '/operations/alert-command-center' },
    { title: '经营报表', icon: 'Document', path: '/operations/reports' },
  ],
}

