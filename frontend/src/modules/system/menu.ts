import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const systemMenu: MenuGroupConfig = {
  id: 'system',
  title: '系统管理',
  icon: 'Setting',
  path: '/system',
  order: 130,
  category: 'system',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN],
  children: [
      { title: '权限中心', icon: 'Lock', path: '/system/permission-center' },
    { title: '系统配置', icon: 'Tools', path: '/system/permission' },
    { title: 'AI模型配置', icon: 'Cpu', path: '/system/ai-model-config' },
    { title: '操作审计', icon: 'List', path: '/system/operation-audit' },
  ],
}

