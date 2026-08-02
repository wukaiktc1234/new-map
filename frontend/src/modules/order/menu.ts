import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const orderMenu: MenuGroupConfig = {
  id: 'order',
  title: '订单管理',
  icon: 'Operation',
  path: '/order',
  order: 20,
  category: 'management',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  children: [
    { title: '订单查询', icon: 'Search', path: '/order/query' },
    { title: '订单统计', icon: 'TrendCharts', path: '/order/statistics' },
    { title: '退款管理', icon: 'Money', path: '/order/refund' },
    { title: '预约管理', icon: 'Calendar', path: '/order/reservation' },
  ],
}

