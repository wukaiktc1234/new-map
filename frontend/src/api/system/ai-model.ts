/**
 * AI 模型配置 API
 * 对应后端: /v1/ai-models (AIModelConfigController)
 *
 * 用于管理智能补货建议的 AI 模型接入，支持本地模型和 API 服务两种类型
 */
import { get, post, put, del } from '../request'
import type {
  AIModelVO,
  AIModelCreateDTO,
  AIModelUpdateDTO,
  AIModelQueryParams,
  AIModelPageResponse,
  AIModelStatistics,
} from '@/types/ai-model'

export const aiModelApi = {
  /**
   * 分页查询 AI 模型列表
   */
  async getList(params: AIModelQueryParams = {}): Promise<AIModelPageResponse> {
    return await get<AIModelPageResponse>('/v1/ai-models', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 10,
    }
  },

  /**
   * 查询所有模型（不分页，最多1000条）
   */
  async getAll(): Promise<AIModelVO[]> {
    const resp = await get<AIModelPageResponse>('/v1/ai-models', { size: 1000 })
    return resp?.records || []
  },

  /**
   * 根据 ID 查询模型详情
   */
  async getById(id: number): Promise<AIModelVO | null> {
    return await get<AIModelVO | null>(`/v1/ai-models/${id}`)
  },

  /**
   * 根据编码查询模型
   */
  async getByCode(modelCode: string): Promise<AIModelVO | null> {
    return await get<AIModelVO | null>(`/v1/ai-models/code/${modelCode}`)
  },

  /**
   * 创建 AI 模型
   */
  async create(data: AIModelCreateDTO): Promise<AIModelVO> {
    return await post<AIModelVO>('/v1/ai-models', data)
  },

  /**
   * 更新 AI 模型（部分更新）
   */
  async update(id: number, data: AIModelUpdateDTO): Promise<AIModelVO> {
    return await put<AIModelVO>(`/v1/ai-models/${id}`, data)
  },

  /**
   * 删除 AI 模型（逻辑删除）
   */
  async delete(id: number): Promise<void> {
    await del<void>(`/v1/ai-models/${id}`)
  },

  /**
   * 更新模型状态（启用/停用）
   */
  async updateStatus(id: number, status: 'active' | 'inactive'): Promise<AIModelVO> {
    return await put<AIModelVO>(`/v1/ai-models/${id}/status`, { status })
  },

  /**
   * 触发模型同步（更新 lastSyncTime）
   */
  async syncModel(id: number): Promise<AIModelVO> {
    return await post<AIModelVO>(`/v1/ai-models/${id}/sync`)
  },

  /**
   * 获取统计信息
   */
  async getStatistics(): Promise<AIModelStatistics> {
    return await get<AIModelStatistics>('/v1/ai-models/statistics') || {
      total: 0,
      active: 0,
      local: 0,
      api: 0,
    }
  },

  /**
   * 测试 AI 模型连接是否可用
   * - local 类型：检查 modelPath 是否存在（内置规则引擎视为可用）
   * - api 类型：发送 HTTP GET 到 endpoint，根据响应状态码判断
   * @param id 模型ID
   * @returns 测试结果：{ success, message, responseTimeMs?, httpStatus?, modelType?, modelName? }
   */
  async testConnection(id: number): Promise<AIModelTestResult> {
    return await post<AIModelTestResult>(`/v1/ai-models/${id}/test-connection`)
  },
}

export default aiModelApi
