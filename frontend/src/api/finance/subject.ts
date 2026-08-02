/**
 * 会计科目API
 * 对应后端: /v1/finance/subjects
 *
 * 【设计说明】
 * - 后端 SubjectController 7 个端点：getTree/getLeafSubjects/create/update/getDetail/getPage/toggleStatus
 * - 无 DELETE 端点，前端 delete 方法已移除（避免 405）
 * - toggleStatus 后端为 @PatchMapping，前端使用 patch
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/patch（替代 silentGet/silentPost 等）
 * - 内置 DataConverter 处理：
 *   - 科目状态数字↔字符串（后端 0/1 ↔ 前端 'inactive'/'active'）
 *   - 科目类型数字↔字符串（后端 1~6 ↔ 前端 'asset'/'liability'/'equity'/'cost'/'income'/'profit'）
 */
import { get, post, put, patch } from '../request'
import { SubjectDataConverter, SubjectStatusMap, SubjectTypeMap } from './converters'
import type {
  FinanceSubject,
  SubjectFormData,
  SubjectQueryForm,
  SubjectTreeNode,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 status 数字（1启用 0停用）
 * - 前端 subjectType 字符串 → 后端 subjectType 数字（1资产 2负债 3权益 4成本 5损益 6利润）
 */
function mapQueryParams(params?: SubjectQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, subjectType, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = SubjectStatusMap.toBackend[status]
  }
  if (subjectType !== undefined && typeof subjectType === 'string') {
    result.subjectType = SubjectTypeMap.toBackend[subjectType]
  }
  return result
}

/** 后端 IPage 分页响应（records 为原始后端对象，状态/类型为数字） */
interface SubjectPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const subjectApi = {
  /**
   * 分页查询会计科目列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型为前端语义字符串）
   */
  async getList(params?: SubjectQueryForm): Promise<PageResponse<FinanceSubject>> {
    const query = mapQueryParams(params)
    const res = await get<SubjectPageBackend | null>('/v1/finance/subjects', query)
    const records = (res?.records || []).map(item =>
      SubjectDataConverter.toFrontend(item) as unknown as FinanceSubject
    )
    return {
      records,
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取会计科目详情
   * @param id - 科目ID
   * @returns 科目详情（已转换状态/类型为前端语义字符串）
   */
  async getById(id: string): Promise<FinanceSubject> {
    const res = await get<Record<string, unknown>>(`/v1/finance/subjects/${id}`)
    return SubjectDataConverter.toFrontend(res) as unknown as FinanceSubject
  },

  /**
   * 创建会计科目
   * @param data - 科目表单数据（前端语义字符串）
   * @returns 创建后的科目（已转换状态/类型为前端语义字符串）
   */
  async create(data: SubjectFormData): Promise<FinanceSubject> {
    const dto = SubjectDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/subjects', dto)
    return SubjectDataConverter.toFrontend(res) as unknown as FinanceSubject
  },

  /**
   * 更新会计科目
   * @param id - 科目ID
   * @param data - 科目表单数据（前端语义字符串）
   * @returns 更新后的科目（已转换状态/类型为前端语义字符串）
   */
  async update(id: string, data: SubjectFormData): Promise<FinanceSubject> {
    const dto = SubjectDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/subjects/${id}`, dto)
    return SubjectDataConverter.toFrontend(res) as unknown as FinanceSubject
  },

  // 【设计说明】后端 SubjectController 无 DELETE 端点
  // delete 方法已移除（原前端调用 DELETE /v1/finance/subjects/{id} 后端会返回 405）

  /**
   * 获取科目树形结构
   * @returns 科目树（后端返回的树节点 status/subjectType 为数字，按需转换）
   */
  async getTree(): Promise<SubjectTreeNode[]> {
    const res = await get<SubjectTreeNode[] | null>('/v1/finance/subjects/tree')
    return res ?? []
  },

  /**
   * 获取叶子节点科目列表（用于凭证录入下拉选择）
   * 对应后端 GET /v1/finance/subjects/leaves
   * @returns 叶子科目列表（已转换状态/类型为前端语义字符串）
   */
  async getLeafSubjects(): Promise<FinanceSubject[]> {
    const res = await get<Record<string, unknown>[] | null>('/v1/finance/subjects/leaves')
    return (res ?? []).map(item =>
      SubjectDataConverter.toFrontend(item) as unknown as FinanceSubject
    )
  },

  /**
   * 启用/禁用科目
   * 对应后端 PATCH /v1/finance/subjects/{id}/status
   *
   * @param id - 科目ID
   * @returns 操作结果
   */
  async toggleStatus(id: string): Promise<boolean> {
    const res = await patch<boolean>(`/v1/finance/subjects/${id}/status`)
    return res !== false
  },
}

export default subjectApi
