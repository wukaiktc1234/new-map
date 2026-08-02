import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const sealMenu: MenuGroupConfig = {
  id: 'seal',
  title: '电子签章',
  icon: 'Stamp',
  path: '/seal',
  order: 125,
  category: 'system',
  // 电子签章是跨部门基础服务：人事合同、采购合同、财务审批、行政文件等场景均需使用
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.HR_DIRECTOR, UserRole.FINANCE_DIRECTOR, UserRole.OPS_DIRECTOR],
  children: [
    { title: '印章管理', icon: 'Collection', path: '/seal/management' },
  ],
}
