/**
 * 组织架构共享类型定义
 *
 * 用于 HROrganization.vue、OrgTreeNode.vue、OrgFormDialog.vue、
 * OrgDetailPanel.vue 等组件间的类型复用，避免重复定义。
 */

/** 组织类型 */
export type OrgType = 'company' | 'department' | 'store' | 'warehouse' | 'group' | 'office' | 'team'

/** 组织节点 */
export interface OrgNode {
  id: string
  name: string
  type: OrgType
  manager?: string
  description?: string
  phone?: string
  location?: string
  headcount?: number
  level?: number
  code?: string
  parentId?: string | null
  sort?: number
  status?: string
  employees?: EmployeeInfo[]
  children?: OrgNode[]
}

/** 组织下属员工简要信息 */
export interface EmployeeInfo {
  id: string
  name: string
  position: string
  status: string
}

/** 组织表单数据 */
export interface OrgFormData {
  name: string
  type: OrgType
  parentId: string | null
  manager: string
  phone: string
  location: string
  description: string
}
