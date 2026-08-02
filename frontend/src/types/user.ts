/**
 * 用户模块类型定义
 * 从 api/user.ts 迁移而来
 */

// 用户基本信息DTO (对应后端 UserBasicInfo)
export interface UserBasicInfo {
  userId: string;
  username: string;
  fullName?: string;
  email?: string;
  phone?: string;
  departmentId?: string;
  departmentName?: string;
  positionId?: string;
  positionName?: string;
  storeId?: string;
  storeName?: string;
  status?: string;
  version?: number;
  updateTime?: string;
}

export interface User {
  id?: number;
  username: string;
  name: string;
  email: string;
  phone?: string;
  department?: string;
  position?: string;
  role?: string;
  status?: string;
  storeId?: string;
  storeName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserListResponse {
  records: User[];
  total: number;
  current: number;
  size: number;
}

export interface UserStore {
  storeId: string;
  storeName: string;
  storeCode: string;
  address?: string;
  phone?: string;
  status?: string;
}
