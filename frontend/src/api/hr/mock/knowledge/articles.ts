/**
 * 员工端知识库文章聚合（管理端引用）
 *
 * 直接对接员工端(employee-frontend)已写好的13篇文章，
 * 非管理端新造数据。文章内容维护在员工端，管理端通过此文件引用。
 *
 * 文章来源：employee-frontend/src/api/mock/knowledge/
 */
import type { KnowledgeArticle as EmployeeArticle } from './types'

import { article as kb001 } from './kb-001-laws'
import { article as kb002 } from './kb-002-hygiene'
import { article as kb003 } from './kb-003-management'
import { article as kb004 } from './kb-004-receiving'
import { article as kb005 } from './kb-005-cooking'
import { article as kb006 } from './kb-006-cleaning'
import { article as kb007 } from './kb-007-poisoning'
import { article as kb008 } from './kb-008-complaint'
import { article as kb009 } from './kb-009-teamwork'
import { article as kb010 } from './kb-010-salary'
import { article as kb011 } from './kb-011-firstaid'
import { article as kb012 } from './kb-012-health'
import { article as kb014 } from './kb-014-anti-bullying'

/** 员工端原始文章列表 */
const employeeArticles: EmployeeArticle[] = [
  kb001, kb002, kb003, kb004, kb005, kb006, kb007,
  kb008, kb009, kb010, kb011, kb012, kb014,
]

/**
 * 将员工端文章转换为管理端知识库文章格式
 * 添加管理端必需字段：publishStatus, viewCount(默认值), createTime
 * 确保 quiz[].difficulty 有默认值（员工端为可选，管理端为必需）
 */
export function convertToManagementArticles(
  articles: EmployeeArticle[]
): import('@/types/hr/knowledge-base').KnowledgeArticle[] {
  return articles.map(article => ({
    id: article.id,
    title: article.title,
    summary: article.summary,
    category: article.category,
    icon: article.icon,
    content: article.content,
    coverUrl: article.coverUrl,
    author: article.author,
    publishStatus: 'published' as const,
    viewCount: article.viewCount ?? 0,
    tags: article.tags,
    quiz: article.quiz?.map(q => ({
      ...q,
      difficulty: q.difficulty ?? 'normal',
    })),
    publishTime: article.publishTime,
    updateTime: article.updateTime,
    createTime: article.publishTime || article.updateTime || '2026-01-01 00:00:00',
  }))
}

/** 管理端格式的全部文章（从员工端转换） */
export const ALL_ARTICLES = convertToManagementArticles(employeeArticles)

/** 员工端原始文章（供需要原始结构的场景使用） */
export { employeeArticles }
