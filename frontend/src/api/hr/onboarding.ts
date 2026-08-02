/**
 * 入职档案 API
 * 对应后端: /v1/onboarding/archive (OnboardingArchiveController)
 *
 * 说明：
 * - 对接真实后端，已移除 HROnboarding.vue 中的 Mock 降级
 * - 后端使用 Spring 分页，返回 { records, total, current, size }
 * - 状态值保持后端语义字符串（CREATED/PENDING_HR/...），页面展示层做中文映射
 */
import { get, post, put, del } from '../request'
import type { OnboardingArchive, PersonnelType } from '@/types/onboarding'

/** 分页查询参数 */
export interface OnboardingListParams {
  page?: number
  size?: number
  status?: string
  keyword?: string
}

/** 分页查询结果 */
export interface OnboardingListResult {
  records: OnboardingArchive[]
  total: number
  current: number
  size: number
}

/** 创建/更新入职档案参数 */
export interface OnboardingFormData {
  candidateName: string
  email: string
  phone?: string
  idCard?: string
  position: string
  positionLevel: string
  departmentId: string
  departmentName?: string
  expectedSalary?: number
  finalSalary?: number
  onboardDate?: string
  /** 现住址 */
  address?: string
  /** 人员类型 */
  personnelType?: PersonnelType
}

const BASE_URL = '/v1/onboarding/archive'

export const onboardingApi = {
  /**
   * 分页查询入职档案列表
   * GET /v1/onboarding/archive/list
   */
  async getList(params: OnboardingListParams = {}): Promise<OnboardingListResult> {
    const query: Record<string, unknown> = {
      page: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.status) query.status = params.status
    if (params.keyword) query.keyword = params.keyword

    const res = await get<OnboardingListResult | null>(`${BASE_URL}/list`, query)
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
      current: res?.current ?? params.page ?? 1,
      size: res?.size ?? params.size ?? 10,
    }
  },

  /**
   * 根据 ID 获取入职档案详情
   * GET /v1/onboarding/archive/{id}
   */
  async getById(id: number): Promise<OnboardingArchive | null> {
    return get<OnboardingArchive | null>(`${BASE_URL}/${id}`)
  },

  /**
   * 创建入职档案
   * POST /v1/onboarding/archive
   */
  async create(data: OnboardingFormData): Promise<OnboardingArchive> {
    return post<OnboardingArchive>(BASE_URL, data)
  },

  /**
   * 更新入职档案
   * PUT /v1/onboarding/archive/{id}
   */
  async update(id: number, data: OnboardingFormData): Promise<OnboardingArchive> {
    return put<OnboardingArchive>(`${BASE_URL}/${id}`, data)
  },

  /**
   * 删除入职档案（逻辑删除）
   * DELETE /v1/onboarding/archive/{id}
   */
  async delete(id: number): Promise<void> {
    await del<void>(`${BASE_URL}/${id}`)
  },

  /**
   * 提交档案审批
   * POST /v1/onboarding/archive/{id}/submit
   */
  async submit(id: number): Promise<void> {
    await post<void>(`${BASE_URL}/${id}/submit`)
  },

  /**
   * 获取可用职位级别
   * GET /v1/onboarding/archive/position-levels
   */
  async getPositionLevels(): Promise<string[]> {
    const res = await get<string[] | null>(`${BASE_URL}/position-levels`)
    return res ?? []
  },

  /**
   * 获取可用档案状态
   * GET /v1/onboarding/archive/statuses
   */
  async getStatuses(): Promise<string[]> {
    const res = await get<string[] | null>(`${BASE_URL}/statuses`)
    return res ?? []
  },
}

export default onboardingApi
