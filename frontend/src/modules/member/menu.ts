import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const memberMenu: MenuGroupConfig = {
  id: 'member',
  title: '会员管理',
  icon: 'UserFilled',
  path: '/marketing/member-overview',
  order: 70,
  category: 'management',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  children: [
    { title: '会员概览', icon: 'DataAnalysis', path: '/marketing/member-overview' },
    { title: '会员列表', icon: 'User', path: '/marketing/member-list' },
    { title: '会员等级', icon: 'Medal', path: '/marketing/member-level' },
    { title: '储值管理', icon: 'Wallet', path: '/marketing/recharge' },
    { title: '储值系统设置', icon: 'Setting', path: '/marketing/recharge-settings' },
  ],
}
