import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const storeManagementMenu: MenuGroupConfig = {
  id: 'store-management',
  title: '门店管理',
  icon: 'Shop',
  path: '/store-management',
  order: 60,
  category: 'operation',
  visibleRoles: [UserRole.STORE_MANAGER, UserRole.TEAM_LEADER, UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR],
  children: [
    { title: '待办事项', icon: 'Bell', path: '/store-management/pending-tasks' },
    { title: '门店收货', icon: 'Box', path: '/store-management/store-receiving' },
    { title: '门店库存', icon: 'Box', path: '/store-management/store-inventory' },
    { title: '日结对账', icon: 'Money', path: '/store-management/daily-settlement' },
    { title: '证件管理', icon: 'Medal', path: '/store-management/certificate' },
    { title: '排班管理', icon: 'Timer', path: '/store-management/shift' },
    { title: '门店招聘', icon: 'UserFilled', path: '/store-management/recruitment' },
    { title: '桌台记录', icon: 'Grid', path: '/store-management/table-usage' },
    { title: '叫号记录', icon: 'List', path: '/store-management/queue-call-history' },
  ],
}

