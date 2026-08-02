/**
 * 字典管理 API
 * 对应后端: /v1/dict (SysDictController)
 *
 * 管理字典类型（sys_dict）和字典项（sys_dict_item），
 * 支持分组管理、按编码查询字典项、缓存清理
 */
import { get, put, post, del } from '../request'

/** 分页响应通用结构（与后端 MyBatis-Plus IPage 序列化结构一致） */
export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 字典类型（对应 sys_dict 表） */
export interface DictTypeItem {
  dictId: number
  dictName: string
  dictCode: string
  dictGroup: string
  description: string
  /** 状态：0-禁用，1-启用 */
  status: number
  /** 是否系统内置：0-否，1-是 */
  isSystem: number
  sortOrder: number
  createUserId: string
  createUsername: string
  updateUserId: string
  updateUsername: string
  version: number
  createTime: string
  updateTime: string
}

/** 字典项（对应 sys_dict_item 表） */
export interface DictItem {
  itemId: number
  dictId: number
  itemLabel: string
  itemValue: string
  sortOrder: number
  cssClass: string
  listClass: string
  /** 颜色类型：primary/success/warning/danger/info */
  colorType: string
  /** 是否默认项：0-否，1-是 */
  isDefault: number
  /** 状态：0-禁用，1-启用 */
  status: number
  remark: string
  createUserId: string
  createUsername: string
  updateUserId: string
  updateUsername: string
  version: number
  createTime: string
  updateTime: string
}

/** 字典类型查询条件 */
export interface DictQueryForm {
  current?: number
  size?: number
  dictName?: string
  dictCode?: string
  dictGroup?: string
  /** 状态：0-禁用，1-启用 */
  status?: number
}

/** 字典项查询条件 */
export interface DictItemQueryForm {
  dictId: number
  current?: number
  size?: number
  itemLabel?: string
  /** 状态：0-禁用，1-启用 */
  status?: number
}

/** 字典类型创建参数（对应后端 DictCreateDTO） */
export interface DictTypeCreateDTO {
  dictName: string
  dictCode: string
  dictGroup: string
  description?: string
  sortOrder?: number
}

/** 字典类型更新参数（对应后端 DictUpdateDTO） */
export interface DictTypeUpdateDTO {
  dictName: string
  dictGroup?: string
  description?: string
  status?: number
  sortOrder?: number
}

/** 字典项创建参数（对应后端 DictItemCreateDTO） */
export interface DictItemCreateDTO {
  dictId: number
  itemLabel: string
  itemValue: string
  sortOrder?: number
  cssClass?: string
  listClass?: string
  colorType?: string
  isDefault?: number
  remark?: string
}

/** 字典项更新参数（对应后端 DictItemUpdateDTO） */
export interface DictItemUpdateDTO {
  itemLabel: string
  itemValue: string
  sortOrder?: number
  cssClass?: string
  listClass?: string
  colorType?: string
  isDefault?: number
  status?: number
  remark?: string
}

/** 字典分组信息（后端 /groups 返回 List<String>） */
export type DictGroupInfo = string

/** 空分页响应（用于异常回退） */
function emptyPage<T>(): PageResponse<T> {
  return { records: [], total: 0, current: 1, size: 20, pages: 0 }
}

export const sysDictApi = {
  // ==================== 字典类型 ====================

  /** 分页查询字典类型列表 */
  async getTypeList(params: DictQueryForm = {}): Promise<PageResponse<DictTypeItem>> {
    return await get<PageResponse<DictTypeItem>>('/v1/dict', params as Record<string, unknown>) || emptyPage<DictTypeItem>()
  },

  /** 获取字典类型详情 */
  async getTypeById(dictId: number): Promise<DictTypeItem | null> {
    return await get<DictTypeItem | null>(`/v1/dict/${dictId}`)
  },

  /** 根据编码获取字典类型 */
  async getTypeByCode(dictCode: string): Promise<DictTypeItem | null> {
    return await get<DictTypeItem | null>(`/v1/dict/code/${dictCode}`)
  },

  /** 创建字典类型 */
  async createType(data: DictTypeCreateDTO): Promise<DictTypeItem> {
    return await post<DictTypeItem>('/v1/dict', data)
  },

  /** 更新字典类型 */
  async updateType(dictId: number, data: DictTypeUpdateDTO): Promise<void> {
    await put<void>(`/v1/dict/${dictId}`, data)
  },

  /** 删除字典类型（逻辑删除） */
  async deleteType(dictId: number): Promise<void> {
    await del<void>(`/v1/dict/${dictId}`)
  },

  /** 启用字典类型 */
  async enableType(dictId: number): Promise<void> {
    await post<void>(`/v1/dict/${dictId}/enable`)
  },

  /** 禁用字典类型 */
  async disableType(dictId: number): Promise<void> {
    await post<void>(`/v1/dict/${dictId}/disable`)
  },

  // ==================== 字典项 ====================

  /** 分页查询字典项列表 */
  async getItemList(params: DictItemQueryForm): Promise<PageResponse<DictItem>> {
    return await get<PageResponse<DictItem>>('/v1/dict/item/list', params as unknown as Record<string, unknown>) || emptyPage<DictItem>()
  },

  /** 获取字典项详情 */
  async getItemById(itemId: number): Promise<DictItem | null> {
    return await get<DictItem | null>(`/v1/dict/item/${itemId}`)
  },

  /** 创建字典项 */
  async createItem(data: DictItemCreateDTO): Promise<DictItem> {
    return await post<DictItem>('/v1/dict/item', data)
  },

  /** 更新字典项 */
  async updateItem(itemId: number, data: DictItemUpdateDTO): Promise<void> {
    await put<void>(`/v1/dict/item/${itemId}`, data)
  },

  /** 删除字典项（逻辑删除） */
  async deleteItem(itemId: number): Promise<void> {
    await del<void>(`/v1/dict/item/${itemId}`)
  },

  /** 根据字典ID获取所有启用的字典项（用于前端组件下拉） */
  async getItemsByDictId(dictId: number): Promise<DictItem[]> {
    return await get<DictItem[]>(`/v1/dict/${dictId}/items`) || []
  },

  /** 根据字典编码获取所有启用的字典项（用于前端组件下拉） */
  async getItemsByCode(dictCode: string): Promise<DictItem[]> {
    return await get<DictItem[]>(`/v1/dict/code/${dictCode}/items`) || []
  },

  // ==================== 分组 / 缓存 / 统计 ====================

  /** 获取所有字典分组列表 */
  async getGroups(): Promise<DictGroupInfo[]> {
    return await get<DictGroupInfo[]>('/v1/dict/groups') || []
  },

  /** 刷新（清除）指定字典缓存 */
  async refreshCache(dictId: number): Promise<void> {
    await del<void>(`/v1/dict/cache/${dictId}`)
  },

  /** 清除所有字典缓存 */
  async clearCache(): Promise<void> {
    await del<void>('/v1/dict/cache/all')
  },

  /** 获取字典统计信息 */
  async getStats(): Promise<Record<string, unknown>> {
    return await get<Record<string, unknown>>('/v1/dict/stats') || {}
  },
}

export default sysDictApi
