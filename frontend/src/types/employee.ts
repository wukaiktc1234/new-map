/**
 * 员工管理模块 - TypeScript类型定义
 *
 * 【层级】L2 - 基础设施层
 * 【职责】定义员工相关的所有TypeScript接口和类型
 */

// ========== 后端数据类型 ==========

/** 后端返回的员工数据（原始格式） */
export interface EmployeeBackend {
  /** 员工ID（可能是数字或雪花ID） */
  id?: number | string
  employeeId?: number | string
  /** 工号 */
  code?: string
  employeeCode?: string
  /** 姓名 */
  name: string
  /** 部门ID */
  departmentId?: number | string
  /** 部门名称 */
  departmentName?: string
  department?: string
  /** 职位 */
  position: string
  /** 手机号 */
  phone: string
  /** 状态：'active'=正常, 'inactive'=停用, 'probation'=试用（符合项目规范§24前端语义化） */
  status: string
  /** 入职日期 */
  joinDate?: string
  createTime?: string
  updateTime?: string
  /** 薪资（单位：分） */
  salary?: number
}

// ========== 前端展示类型 ==========

/** 前端展示用的员工数据（经过DataConverter转换后） */
export interface EmployeeItem {
  /** 员工ID（统一字符串格式） */
  id: string
  /** 工号 */
  code: string
  /** 姓名 */
  name: string
  /** 部门名称 */
  department: string
  /** 职位 */
  position: string
  /** 手机号 */
  phone: string
  /**
   * 状态（语义字符串）
   * - 'active': 正常
   * - 'inactive': 停用
   * - 'probation': 试用
   */
  status: 'active' | 'inactive' | 'probation'
  /** 原始状态值（用于提交回后端，现在也是字符串格式） */
  statusRaw?: string
  /** 入职日期 (ISO 8601) */
  joinDate: string
  /** 薪资（单位：元，浮点数） */
  salary: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

// ========== 表单数据类型 ==========

/** 新增/编辑员工的表单数据 */
export interface EmployeeFormData {
  /** 姓名 */
  name: string
  /** 工号 */
  code: string
  /** 部门ID */
  departmentId: string | number
  /** 职位 */
  position: string
  /** 手机号 */
  phone: string
  /** 状态 */
  status: 'active' | 'inactive' | 'probation'
  /** 入职日期 */
  joinDate: string
  /** 薪资（单位：元） */
  salary: number | null
}

// ========== 查询参数类型 ==========

/** 员工列表查询参数 */
export interface EmployeeQueryForm extends Record<string, unknown> {
  /** 关键词搜索（姓名/工号/手机号） */
  keyword?: string
  /** 部门筛选 */
  department?: string
  /** 状态筛选 */
  status?: string
}

// ========== 统计卡片类型 ==========

/** 员工统计卡片数据 */
export interface EmployeeStatCard {
  /** 图标名称（Element Plus图标组件名） */
  icon: string
  /** 标题文本 */
  label: string
  /** 数值 */
  value: string | number
  /** 趋势百分比（正=上升，负=下降） */
  trend?: number
  /** 颜色类型 */
  colorType?: 'primary' | 'success' | 'warning' | 'error'
}

// ========== 表格列配置类型 ==========

// ========== 高级查询表单类型 ==========

/** 员工高级查询表单 */
export interface EmployeeAdvancedQueryForm {
  /** 关键词搜索 */
  keyword?: string
  /** 部门筛选（多选） */
  departments?: string[]
  /** 状态筛选（多选） */
  statuses?: ('active' | 'inactive' | 'probation')[]
  /** 入职日期范围 - 开始 */
  joinDateStart?: string
  /** 入职日期范围 - 结束 */
  joinDateEnd?: string
  /** 薪资范围 - 最低 */
  salaryMin?: number
  /** 薪资范围 - 最高 */
  salaryMax?: number
}

// ========== 头像上传类型 ==========

/** 头像上传结果 */
export interface AvatarUploadResult {
  /** 本地预览URL（Blob URL或Base64） */
  localUrl: string
  /** 文件对象 */
  file: File
}

// ========== Excel导入类型 ==========

/** Excel导入错误项 */
export interface EmployeeImportError {
  /** 行号 */
  row: number
  /** 错误字段 */
  field: string
  /** 错误原因 */
  message: string
  /** 原始值 */
  value: string
}

/** Excel导入结果 */
export interface EmployeeImportResult {
  /** 成功导入条数 */
  successCount: number
  /** 失败条数 */
  failCount: number
  /** 错误详情列表 */
  errors: EmployeeImportError[]
}

// ========== 组织架构类型 ==========

/** 部门树节点 */
export interface DepartmentNode {
  /** 部门ID */
  id: string
  /** 部门名称 */
  name: string
  /** 子节点（员工或子部门） */
  children?: EmployeeNode[]
  /** 节点类型 */
  type: 'department'
}

/** 员工树节点 */
export interface EmployeeNode {
  /** 员工ID */
  id: string
  /** 姓名 */
  name: string
  /** 职位 */
  position: string
  /** 状态 */
  status: 'active' | 'inactive' | 'probation'
  /** 节点类型 */
  type: 'employee'
}

// ========== 财务数据类型 ==========

/** 财务明细项 */
export interface FinanceDetailItem {
  /** 员工ID */
  id: string
  /** 姓名 */
  name: string
  /** 部门 */
  department: string
  /** 基本工资（元） */
  baseSalary: number
  /** 绩效（元） */
  performance: number
  /** 奖金（元） */
  bonus: number
  /** 扣款（元） */
  deduction: number
  /** 实发工资（元） */
  netSalary: number
}

/** 薪资图表数据 */
export interface SalaryChartData {
  /** 部门名称 */
  department: string
  /** 薪资总额（元） */
  totalSalary: number
  /** 员工人数 */
  employeeCount: number
}

/** 薪资区间分布数据 */
export interface SalaryRangeData {
  /** 区间名称 */
  range: string
  /** 人数 */
  count: number
  /** 占比 */
  percentage: number
}

/** 财务统计卡片 */
export interface FinanceStatCard {
  /** 图标 */
  icon: string
  /** 标题 */
  label: string
  /** 数值 */
  value: string | number
  /** 趋势 */
  trend?: number
  /** 颜色类型 */
  colorType: 'primary' | 'success' | 'warning' | 'error' | 'info'
}

// ========== 表格列配置类型 ==========

/** 表格列配置 */
export interface EmployeeColumnConfig {
  /** 字段名 */
  prop: string
  /** 列标题 */
  label: string
  /** 最小宽度 */
  minWidth?: number
  /** 是否使用插槽 */
  slot?: string
}
