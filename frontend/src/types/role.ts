/**
 * 角色模块类型定义
 * 从 api/role.ts 迁移而来
 */

export interface Role {
  roleId: string;
  roleCode: string;
  roleName: string;
  roleDescription?: string;
  roleLevel?: number;
  status?: number | string;
  roleType?: number | string;
  systemBuilt?: boolean;
  dataScope?: string;
  accessibleStores?: string;
  accessibleDepartments?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface RoleListResponse {
  records: Role[];
  total: number;
  page: number;
  pageSize: number;
}
