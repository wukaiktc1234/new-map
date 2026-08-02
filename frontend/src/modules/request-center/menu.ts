import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

/**
 * 物资需求提报入口（跨部门，置于工作台域）
 *
 * 【定位】各部门/门店员工统一在此提报采购需求（走采购申请链路）：
 * 提报 → 审批（采购部） → 生成采购订单。申请人/部门由后端绑定（OA 化）。
 */
export const requestCenterMenu: MenuGroupConfig = {
  id: 'request-center',
  title: '物资需求提报',
  icon: 'Goods',
  path: '/workspace/material-request',
  order: 5,
  category: 'operation',
  visibleRoles: [
    UserRole.OWNER,
    UserRole.ADMIN,
    UserRole.STORE_MANAGER,
    UserRole.TEAM_LEADER,
    UserRole.DEPARTMENT_MANAGER,
    UserRole.EMPLOYEE,
  ],
  children: [
    { title: '物资需求提报', icon: 'Goods', path: '/workspace/material-request' },
  ],
}
