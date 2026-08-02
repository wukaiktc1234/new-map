/**
 * 权限模板 API
 * 对应后端: /v1/permission-templates (PermissionTemplateController)
 *
 * 4 种模式编码：
 * - centralized-single: 集中式单店模式（5-50人，年营收50万-500万）
 * - standard-chain: 标准连锁模式（2-5家门店，年营收500万-2000万）
 * - large-chain: 大型连锁模式（多门店，年营收2000万+）
 * - custom: 自定义（admin 全开，用户自行编辑）
 */
import { get, post, put, del } from '../request'

/** 后端返回的权限模板 DTO */
export interface PermissionTemplateDTO {
  id: number
  name: string
  code: string
  description: string
  enterpriseType: string
  scaleRange: string
  /** 角色配置 JSON 字符串（{ admin: [...], hr_director: [...], ... }） */
  roleConfig: string
  isSystem: boolean
  status: number
  createdAt: string
  updatedAt: string
}

/**
 * 解析 roleConfig JSON 字符串为 domainMatrix 对象
 * @param roleConfig JSON 字符串
 * @returns 角色→域列表 映射
 */
export function parseRoleConfig(roleConfig: string): Record<string, string[]> {
  if (!roleConfig) return {}
  try {
    const parsed = JSON.parse(roleConfig)
    if (parsed && typeof parsed === 'object') {
      return parsed as Record<string, string[]>
    }
  } catch (e) {
    console.error('[permission-template] 解析 roleConfig 失败:', e)
  }
  return {}
}

/**
 * 将 domainMatrix 对象序列化为 roleConfig JSON 字符串
 */
export function stringifyRoleConfig(domainMatrix: Record<string, string[]>): string {
  return JSON.stringify(domainMatrix)
}

export const permissionTemplateApi = {
  /**
   * 查询所有启用的权限模板
   */
  async getAllEnabled(): Promise<PermissionTemplateDTO[]> {
    return await get<PermissionTemplateDTO[]>('/v1/permission-templates') || []
  },

  /**
   * 查询所有系统模板（4 种模式）
   */
  async getSystemTemplates(): Promise<PermissionTemplateDTO[]> {
    return await get<PermissionTemplateDTO[]>('/v1/permission-templates/system') || []
  },

  /**
   * 获取当前激活的权限模板（服务端持久化，多终端一致）
   * 无配置时后端默认返回 centralized-single
   */
  async getActiveTemplate(): Promise<string> {
    const data = await get<{ templateCode: string }>('/v1/system-config/permission-template')
    return data?.templateCode || ''
  },

  /**
   * 保存当前激活的权限模板（"应用模板"时同步持久化到服务端）
   */
  async setActiveTemplate(templateCode: string): Promise<void> {
    await put<void>('/v1/system-config/permission-template', { templateCode })
  },

  /**
   * 根据编码查询模板
   */
  async getByCode(code: string): Promise<PermissionTemplateDTO | null> {
    return await get<PermissionTemplateDTO | null>(`/v1/permission-templates/${code}`)
  },

  /**
   * 根据企业类型和规模查询匹配的模板
   */
  async search(enterpriseType = 'restaurant', scaleRange = 'all'): Promise<PermissionTemplateDTO[]> {
    return await get<PermissionTemplateDTO[]>('/v1/permission-templates/search', { enterpriseType, scaleRange }) || []
  },

  /**
   * 创建自定义模板
   */
  async create(data: Omit<PermissionTemplateDTO, 'id' | 'isSystem' | 'status' | 'createdAt' | 'updatedAt'>): Promise<PermissionTemplateDTO> {
    return await post<PermissionTemplateDTO>('/v1/permission-templates', data)
  },

  /**
   * 更新模板
   */
  async update(id: number, data: Partial<PermissionTemplateDTO>): Promise<PermissionTemplateDTO> {
    return await put<PermissionTemplateDTO>(`/v1/permission-templates/${id}`, data)
  },

  /**
   * 删除模板（系统模板不允许删除）
   */
  async delete(id: number): Promise<void> {
    await del<void>(`/v1/permission-templates/${id}`)
  },
}

export default permissionTemplateApi
