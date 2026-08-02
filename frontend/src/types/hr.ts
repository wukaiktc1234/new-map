/**
 * HR模块类型定义
 * 从 api/hr.ts 迁移而来
 */

// 员工基本信息DTO (对应后端 EmployeeBasicInfo)
export interface EmployeeBasicInfo {
  employeeId: string;
  employeeName: string;
  employeeCode?: string;
  gender?: string;
  phone?: string;
  email?: string;
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

// 部门基本信息DTO (对应后端 DepartmentBasicInfo)
export interface DepartmentBasicInfo {
  departmentId: string;
  departmentName: string;
  departmentCode?: string;
  parentId?: string;
  managerId?: string;
  managerName?: string;
  status?: string;
  storeId?: string;
  version?: number;
  updateTime?: string;
}

// 职位基本信息DTO (对应后端 PositionBasicInfo)
export interface PositionBasicInfo {
  positionId: string;
  positionName: string;
  positionCode?: string;
  departmentId?: string;
  departmentName?: string;
  status?: string;
  version?: number;
  updateTime?: string;
}

export interface PositionCodeMapping {
  id?: number;
  keyword: string;
  code: string;
}

export interface StoreBasicInfo {
  storeId: string;
  storeName: string;
  storeCode?: string;
  address?: string;
  phone?: string;
  managerId?: string;
  managerName?: string;
  status?: string;
  companyId?: string;
  region?: string;
  storeType?: string;
  version?: number;
  updateTime?: string;
}

// 门店实体 (对应后端 Entity)
export interface HrStore {
  id?: string;
  storeName: string;
  storeCode?: string;
  address?: string;
  phone?: string;
  managerId?: string;
  managerName?: string;
  status?: string;
  companyId?: string;
  region?: string;
  storeType?: string;
  description?: string;
  createdTime?: string;
  updatedTime?: string;
  [key: string]: string | number | boolean | undefined | null;
}

// 部门实体 (对应后端 Entity)
export interface Department {
  departmentId?: string;
  departmentName: string;
  departmentCode?: string;
  parentId?: string;
  level?: number;
  status?: string;
  managerId?: string;
  managerName?: string;
  storeId?: string;
  description?: string;
  sortOrder?: number;
  children?: Department[];
  [key: string]: unknown;
}

// 部门DTO (对应后端 DepartmentDTO, 用于树形展示)
export interface DepartmentDTO {
  id: string | null;
  name: string;
  code?: string;
  parentId?: string | null;
  level?: number;
  employeeCount?: number;
  manager?: string;
  type?: string;
  status?: string;
  sort?: number;
  createdAt?: string;
  remark?: string;
  children?: DepartmentDTO[];
  hasChildren?: boolean;
  [key: string]: unknown;
}

// 职位实体
export interface Position {
  id?: string;
  positionName: string;
  positionCode?: string;
  departmentId?: string;
  department?: string;
  description?: string;
  employeeCount?: number;
  status?: string;
  createdTime?: string;
  updatedTime?: string;
  [key: string]: unknown;
}

export interface PositionPageResult {
  items: Position[];
  total: number;
  current: number;
  size: number;
  pages?: number;
}

// 员工实体 (对应后端 Entity)
export interface Employee {
  id?: string;
  employeeCode?: string;
  employeeName?: string;
  gender?: string;
  phone?: string;
  email?: string;
  departmentId?: string;
  departmentName?: string;
  positionId?: string;
  positionName?: string;
  storeId?: string;
  storeName?: string;
  createdTime?: string;
  updatedTime?: string;
  status?: string;
  hireDate?: string;
  resignDate?: string;
  [key: string]: unknown;
}

export interface EmployeePageResult {
  records: Employee[];
  total: number;
  current: number;
  pages: number;
  size: number;
}

/**
 * 员工全局统计数据
 * 用于员工管理页面顶部统计卡片展示（不受列表筛选条件影响）
 */
export interface EmployeeStatistics {
  /** 在职员工数 */
  active: number;
  /** 试用期员工数 */
  probation: number;
  /** 已离职员工数 */
  inactive: number;
  /** 本月新增员工数 */
  newThisMonth: number;
  /** 超龄返聘员工数 */
  overAge: number;
  /** 员工总数 */
  total: number;
}

// 员工调动
export interface Transfer {
  id: string;
  employeeName: string;
  employeeId: string;
  oldDepartment: string;
  oldPosition: string;
  newDepartment: string;
  newPosition: string;
  transferDate: string;
  reason: string;
  status: string;
}

// 部门查询参数
export interface DepartmentQueryParams {
  page?: number;
  pageSize?: number;
  departmentName?: string;
  departmentCode?: string;
  status?: string;
  storeId?: string;
  parentId?: string;
}

// 职位查询参数
export interface PositionQueryParams {
  page?: number;
  pageSize?: number;
  positionName?: string;
  positionCode?: string;
  departmentId?: string;
  status?: string;
}

// 员工查询参数
export interface EmployeeQueryParams {
  page?: number;
  pageSize?: number;
  employeeName?: string;
  employeeCode?: string;
  departmentId?: string;
  positionId?: string;
  storeId?: string;
  status?: string;
  phone?: string;
  email?: string;
}

// 员工更新数据
export interface EmployeeUpdateData {
  employeeCode?: string;
  employeeName?: string;
  gender?: string;
  phone?: string;
  email?: string;
  departmentId?: string;
  positionId?: string;
  storeId?: string;
  status?: string;
  hireDate?: string;
  resignDate?: string;
}

// 员工调岗数据
export interface EmployeeTransferData {
  employeeId: string;
  oldDepartmentId: string;
  oldPositionId: string;
  newDepartmentId: string;
  newPositionId: string;
  transferDate: string;
  reason?: string;
}

// 员工离职数据
export interface EmployeeResignData {
  employeeId: string;
  resignDate: string;
  reason?: string;
  handoverPersonId?: string;
  handoverDate?: string;
}

// 员工调动查询参数
export interface TransferQueryParams {
  page?: number;
  pageSize?: number;
  employeeName?: string;
  employeeId?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}

// 薪资计算结果接口
export interface SalaryCalculationResult {
  success: boolean;
  message?: string;
  employeeId?: string;
  employeeName?: string;
  level?: number;
  levelName?: string;
  coefficient?: number;
  departmentId?: number;
  departmentName?: string;
  departmentBaseSalary?: number;
  calculatedSalary?: number;
  baseSalary?: number;
}
