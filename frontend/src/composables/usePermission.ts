import { computed } from 'vue';
import { usePermissionStore, UserRole } from '@/stores/permission';
import type { DataScopeType } from '@/types/permission';

export function usePermission() {
  const permissionStore = usePermissionStore();

  const isAdmin = computed(() => permissionStore.isAdmin);

  /** 当前用户的权限列表 */
  const userPermissions = computed(() => permissionStore.userInfo?.permissions ?? []);

  /** 当前用户的角色列表 */
  const userRoles = computed(() => permissionStore.userInfo?.roles ?? []);

  const userDataScope = computed(() => permissionStore.userDataScope);

  const hasPermission = (permission: string): boolean => {
    return permissionStore.hasPermission(permission);
  };

  const hasAnyPermission = (permissions: string[]): boolean => {
    return permissionStore.hasAnyPermission(permissions);
  };

  const hasAllPermissions = (permissions: string[]): boolean => {
    return permissionStore.hasAllPermissions(permissions);
  };

  const hasRole = (role: UserRole): boolean => {
    return permissionStore.hasRole(role);
  };

  const hasAnyRole = (roles: UserRole[]): boolean => {
    return permissionStore.hasAnyRole(roles);
  };

  const hasAllRoles = (roles: UserRole[]): boolean => {
    return permissionStore.hasAllRoles(roles);
  };

  const canAccessStore = (storeId: string): boolean => {
    return permissionStore.canAccessStore(storeId);
  };

  const canAccessDepartment = (deptId: string): boolean => {
    return permissionStore.canAccessDepartment(deptId);
  };

  /**
   * 检查是否可以访问指定数据
   * 基于当前用户的数据范围和权限判断
   * @param data 包含门店ID、部门ID、创建者信息的数据对象
   */
  const canAccessData = (data: { storeId?: string; departmentId?: string; createdBy?: string }):
  boolean => {
    // 超级管理员可访问所有数据
    if (isAdmin.value) return true;

    // 检查门店访问权限
    if (data.storeId && !canAccessStore(data.storeId)) return false;

    // 检查部门访问权限
    if (data.departmentId && !canAccessDepartment(data.departmentId)) return false;

    return true;
  };

  /**
   * 设置用户数据范围
   * TODO: 数据范围由后端控制，前端暂不实现设置方法
   */
  const setDataScope = (_scope: DataScopeType): void => {
    console.warn('[usePermission] setDataScope: 数据范围由后端控制，前端暂不实现');
  };

  /**
   * 设置可访问门店列表
   * TODO: 可访问门店由后端控制，前端暂不实现设置方法
   */
  const setAccessibleStores = (_stores: string[]): void => {
    console.warn('[usePermission] setAccessibleStores: 可访问门店由后端控制，前端暂不实现');
  };

  /**
   * 设置可访问部门列表
   * TODO: 可访问部门由后端控制，前端暂不实现设置方法
   */
  const setAccessibleDepartments = (_depts: string[]): void => {
    console.warn('[usePermission] setAccessibleDepartments: 可访问部门由后端控制，前端暂不实现');
  };

  const canManageDepartment = computed(() => {
    return isAdmin.value || hasAnyRole([UserRole.ADMIN, UserRole.HR_DIRECTOR, UserRole.STORE_MANAGER]);
  });

  const canManagePosition = computed(() => {
    return isAdmin.value || hasAnyRole([UserRole.ADMIN, UserRole.HR_DIRECTOR, UserRole.STORE_MANAGER]);
  });

  const canManageEmployee = computed(() => {
    return isAdmin.value || hasAnyRole([UserRole.ADMIN, UserRole.HR_DIRECTOR]);
  });

  const canView = computed(() => {
    return isAdmin.value || hasAnyRole([UserRole.ADMIN, UserRole.HR_DIRECTOR, UserRole.STORE_MANAGER, UserRole.EMPLOYEE]);
  });

  const canManageProduct = computed(() => {
    return isAdmin.value || hasPermission('product:manage');
  });

  const canManageOrder = computed(() => {
    return isAdmin.value || hasPermission('order:manage');
  });

  const canManageWarehouse = computed(() => {
    return isAdmin.value || hasPermission('warehouse:manage');
  });

  const canManagePurchase = computed(() => {
    return isAdmin.value || hasPermission('purchase:manage');
  });

  const canManageFinance = computed(() => {
    return isAdmin.value || hasPermission('finance:manage');
  });

  const canManageSystem = computed(() => {
    return isAdmin.value || hasPermission('system:manage');
  });

  return {
    isAdmin,
    userPermissions,
    userRoles,
    userDataScope,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    canAccessStore,
    canAccessDepartment,
    canAccessData,
    setDataScope,
    setAccessibleStores,
    setAccessibleDepartments,
    canManageDepartment,
    canManagePosition,
    canManageEmployee,
    canView,
    canManageProduct,
    canManageOrder,
    canManageWarehouse,
    canManagePurchase,
    canManageFinance,
    canManageSystem
  };
}
