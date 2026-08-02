/**
 * 知识库管理API
 * 对应后端: KnowledgeBaseController (/v1/hr/knowledge-base)
 *
 * 数据来源：直接对接员工端(employee-frontend)已写好的文章。
 * 后端端点已全部实现，前端通过标准 get/post/put/del 调用。
 */
import { get, post, put, del } from '../request'
import type {
  KnowledgeArticle,
  KnowledgeArticleFormData,
  KnowledgeArticleQueryParams,
  KnowledgeCategoryStat,
  ArticleStudyRecord,
  StudyRecordQueryParams,
  StudyRecordStatistics,
  ReviewRecord,
  SubmitReviewForm,
  DeptReviewForm,
  FinalReviewForm,
} from '../../types/hr/knowledge-base'

/**
 * 映射分页查询参数：前端 page/pageSize → 后端 current/size
 */
function mapQueryParams<T extends object>(params?: T): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  return result
}

export const knowledgeBaseApi = {
  /** 分页查询知识库文章 */
  async getList(params?: KnowledgeArticleQueryParams): Promise<{ records: KnowledgeArticle[]; total: number }> {
    const res = await get<{ records: KnowledgeArticle[]; total: number } | KnowledgeArticle[]>(
      '/v1/hr/knowledge-base',
      mapQueryParams(params),
    )
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: KnowledgeArticle[]; total: number }
  },

  /** 根据ID获取文章详情 */
  async getById(id: string): Promise<KnowledgeArticle> {
    return get<KnowledgeArticle>(`/v1/hr/knowledge-base/${id}`)
  },

  /** 创建文章 */
  async create(data: KnowledgeArticleFormData): Promise<KnowledgeArticle> {
    return post<KnowledgeArticle>('/v1/hr/knowledge-base', data)
  },

  /** 更新文章 */
  async update(id: string, data: Partial<KnowledgeArticleFormData>): Promise<KnowledgeArticle> {
    return put<KnowledgeArticle>(`/v1/hr/knowledge-base/${id}`, data)
  },

  /** 删除文章 */
  async delete(id: string): Promise<void> {
    await del(`/v1/hr/knowledge-base/${id}`)
  },

  /** 发布文章 */
  async publish(id: string): Promise<void> {
    await post(`/v1/hr/knowledge-base/${id}/publish`, {})
  },

  /** 归档文章 */
  async archive(id: string): Promise<void> {
    await post(`/v1/hr/knowledge-base/${id}/archive`, {})
  },

  /* ============================================================
   * 联邦式审核流程（部门初审 + 店长终审）
   * 状态流转：draft → pending_review → pending_final → published
   *                                          ↘ rejected
   * ============================================================ */

  /** 提交审核（作者提交，draft → pending_review） */
  async submitReview(id: string, data: SubmitReviewForm): Promise<void> {
    await post(`/v1/hr/knowledge-base/${id}/submit-review`, data)
  },

  /** 部门初审（部门负责人审核，pending_review → pending_final/rejected） */
  async deptReview(id: string, data: DeptReviewForm): Promise<void> {
    await post(`/v1/hr/knowledge-base/${id}/dept-review`, data)
  },

  /** 店长终审（店长/HR审核，pending_final → published/rejected） */
  async finalReview(id: string, data: FinalReviewForm): Promise<void> {
    await post(`/v1/hr/knowledge-base/${id}/final-review`, data)
  },

  /** 获取文章审核记录 */
  async getReviewRecords(id: string): Promise<ReviewRecord[]> {
    return get<ReviewRecord[]>(`/v1/hr/knowledge-base/${id}/review-records`)
  },

  /** 获取分类统计 */
  async getCategoryStats(): Promise<KnowledgeCategoryStat[]> {
    return get<KnowledgeCategoryStat[]>('/v1/hr/knowledge-base/category-stats')
  },

  /* ============================================================
   * 员工学习记录相关方法（对接 employee-frontend 学习数据）
   * ============================================================ */

  /** 分页查询员工学习记录 */
  async getStudyRecords(params?: StudyRecordQueryParams): Promise<{ records: ArticleStudyRecord[]; total: number }> {
    const res = await get<{ records: ArticleStudyRecord[]; total: number } | ArticleStudyRecord[]>(
      '/v1/hr/knowledge-base/study-records',
      mapQueryParams(params),
    )
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: ArticleStudyRecord[]; total: number }
  },

  /** 获取学习记录统计 */
  async getStudyRecordStatistics(articleId?: string): Promise<StudyRecordStatistics> {
    return get<StudyRecordStatistics>('/v1/hr/knowledge-base/study-records/statistics', { articleId })
  },

  /** 根据文章ID获取学习记录 */
  async getStudyRecordsByArticle(articleId: string): Promise<ArticleStudyRecord[]> {
    return get<ArticleStudyRecord[]>(`/v1/hr/knowledge-base/articles/${articleId}/study-records`)
  },

  /** 重置员工学习记录（管理员操作，重置某员工对某文章的学习进度） */
  async resetStudyRecord(recordId: string): Promise<void> {
    await post(`/v1/hr/knowledge-base/study-records/${recordId}/reset`, {})
  },
}
