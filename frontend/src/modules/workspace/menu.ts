import type { MenuGroupConfig } from '@/types/permission'

export const workspaceMenu: MenuGroupConfig = {
  id: 'workspace',
  title: '工作台',
  icon: 'Odometer',
  path: '/home',
  order: 0,
  children: [
    { title: '工作台', icon: 'Monitor', path: '/home' },
  ],
}

