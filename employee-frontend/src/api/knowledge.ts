import request from './request'
import { mockDelay, mockLightDelay } from './mock/delays'
import type {
  KnowledgeArticle,
  KnowledgeCategory,
  KnowledgeQueryParams,
  KnowledgeListResponse,
  QuizQuestion,
} from '@/types/knowledge'
import { KNOWLEDGE_CATEGORY_LABELS } from '@/types/knowledge'

import { ALL_ARTICLES } from './mock/knowledge/articles'

/** Mock 文章数据（从独立文件聚合，保持向后兼容引用） */
const MOCK_ARTICLES = ALL_ARTICLES

export const knowledgeApi = {
  /**
   * 获取文章列表
   */
  async getList(params?: KnowledgeQueryParams): Promise<KnowledgeListResponse> {
    await mockDelay(80, 200)

    let filtered = [...MOCK_ARTICLES]

    // 关键词搜索
    if (params?.keyword) {
      const kw = params.keyword.toLowerCase()
      filtered = filtered.filter(
        a => a.title.toLowerCase().includes(kw) || a.summary.toLowerCase().includes(kw),
      )
    }

    // 分类筛选
    if (params?.category && params.category !== 'all') {
      filtered = filtered.filter(a => a.category === params.category)
    }

    const total = filtered.length
    const page = params?.page || 1
    const size = params?.size || 100
    const start = (page - 1) * size
    const records = filtered.slice(start, start + size)

    return { records, total }
  },

  /**
   * 获取文章详情
   */
  async getById(id: string): Promise<KnowledgeArticle | null> {
    await mockLightDelay()

    // 模拟阅读量+1
    const article = MOCK_ARTICLES.find(a => a.id === id)
    if (article) {
      article.viewCount = (article.viewCount ?? 0) + 1
      return { ...article }
    }
    return null
  },

  /**
   * 切换收藏状态
   */
  async toggleFavorite(id: string): Promise<boolean> {
    await mockDelay(50, 120)

    const article = MOCK_ARTICLES.find(a => a.id === id)
    if (article) {
      article.isFavorited = !article.isFavorited
      return article.isFavorited
    }
    return false
  },

  /** 获取分类列表（用于筛选Tab） */
  getCategories(): Array<{ key: KnowledgeCategory | 'all'; label: string }> {
    return [
      { key: 'all', label: '全部' },
      ...Object.entries(KNOWLEDGE_CATEGORY_LABELS).map(([key, label]) => ({
        key: key as KnowledgeCategory,
        label,
      })),
    ]
  },
}
