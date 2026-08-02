import { usePermissionStore, UserRole } from '@/stores/permission';

export function hasPermission(permission: string): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.hasPermission(permission);
}

export function hasAnyPermission(permissions: string[]): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.hasAnyPermission(permissions);
}

export function hasAllPermissions(permissions: string[]): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.hasAllPermissions(permissions);
}

export function hasRole(role: UserRole): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.hasRole(role);
}

export function hasAnyRole(roles: UserRole[]): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.hasAnyRole(roles);
}

export function hasAllRoles(roles: UserRole[]): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.hasAllRoles(roles);
}

export function isSuperAdmin(): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.isAdmin;
}

export function isSystemAdmin(): boolean {
  return hasRole(UserRole.ADMIN) || hasRole(UserRole.OWNER);
}

export function canAccessStore(storeId: string): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.canAccessStore(storeId);
}

export function canAccessDepartment(deptId: string): boolean {
  const permissionStore = usePermissionStore();
  return permissionStore.canAccessDepartment(deptId);
}

export function canAccessData(data: { storeId?: string; departmentId?: string; createdBy?: string }):
boolean {
  const permissionStore = usePermissionStore();
  // 超级管理员可访问所有数据
  if (permissionStore.isAdmin) return true;

  // 检查门店访问权限
  if (data.storeId && !permissionStore.canAccessStore(data.storeId)) return false;

  // 检查部门访问权限
  if (data.departmentId && !permissionStore.canAccessDepartment(data.departmentId)) return false;

  return true;
}

export function formatPermission(permission: string): string {
  const permissionMap: Record<string, string> = {
    'product:manage': '产品管理权限',
    'order:manage': '订单管理权限',
    'warehouse:manage': '仓储管理权限',
    'purchase:manage': '采购管理权限',
    'marketing:manage': '营销管理权限',
    'finance:manage': '财务管理权限',
    'hr:manage': '人事管理权限',
    'traceability:manage': '食品追溯权限',
    'system:manage': '系统管理权限',
    'user:manage': '用户管理权限',
    'role:manage': '角色管理权限',
    'permission:manage': '权限管理权限',
  };
  
  return permissionMap[permission] || permission;
}

export function getDataScopeLabel(scope: string): string {
  const scopeMap: Record<string, string> = {
    all: '全部数据',
    company: '本公司数',
    store: '本门店数',
    department: '本部门数',
    stores: '指定门店',
    departments: '指定部门',
    self: '仅本人数据'
};
  return scopeMap[scope] || scope;
}
