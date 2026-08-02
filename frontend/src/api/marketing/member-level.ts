/**
 * 会员等级API
 * 对应后端: /v1/member-levels
 */
import { get, post, put, del } from '../request'
import type { MemberLevelInfo, MemberLevelForm, MemberLevelQueryForm } from '@/types/member-level'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 * 同时转换 status 字段（前端语义字符串 → 后端数字编码）
 *
 * 后端 MemberLevelController.getList 参数为 Integer status（1启用 0停用），
 * 与 com.foodtraceability.entity.MemberLevel 实体（Integer status）一致。
 * 前端 MemberLevelQueryForm.status 为语义字符串（active/inactive），需转换。
 */
const LEVEL_STATUS_TO_BACKEND: Record<string, number> = {
  active: 1,
  inactive: 0,
}

function mapQueryParams(params?: MemberLevelQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && status !== null && typeof status === 'string') {
    result.status = LEVEL_STATUS_TO_BACKEND[status]
  }
  return result
}

/**
 * 映射表单数据到后端格式：转换 status 字段（前端语义字符串 → 后端数字编码）
 * 后端实体 com.foodtraceability.entity.MemberLevel.status 为 Integer（1启用 0停用）
 */
function mapFormToBackend(data: MemberLevelForm): Record<string, unknown> {
  // 先转 unknown 再转 Record，避免 TS2352（MemberLevelForm 缺索引签名）
  const { status, ...rest } = data as unknown as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (typeof status === 'string') {
    result.status = LEVEL_STATUS_TO_BACKEND[status] ?? 1
  }
  return result
}

export const memberLevelApi = {
  /** 获取等级列表 */
  async getList(params?: MemberLevelQueryForm): Promise<MemberLevelInfo[]> {
    return await get<MemberLevelInfo[]>('/v1/member-levels', mapQueryParams(params))
  },

  /** 获取等级详情 */
  async getById(id: string): Promise<MemberLevelInfo | null> {
    return await get<MemberLevelInfo | null>(`/v1/member-levels/${id}`)
  },

  /** 创建等级 */
  async create(data: MemberLevelForm): Promise<MemberLevelInfo | null> {
    return await post<MemberLevelInfo | null>('/v1/member-levels', mapFormToBackend(data))
  },

  /** 更新等级 */
  async update(id: string, data: MemberLevelForm): Promise<MemberLevelInfo | null> {
    return await put<MemberLevelInfo | null>(`/v1/member-levels/${id}`, mapFormToBackend(data))
  },

  /** 切换等级状态 */
  async toggleStatus(id: string): Promise<void> {
    await put(`/v1/member-levels/${id}/toggle-status`)
  },

  /** 删除等级 */
  async delete(id: string): Promise<void> {
    await del(`/v1/member-levels/${id}`)
  },
}
