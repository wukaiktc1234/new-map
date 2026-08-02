import type { KnowledgeArticle } from '@/types/knowledge'

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

/** 知识库全部文章 Mock 数据 */
export const ALL_ARTICLES: KnowledgeArticle[] = [
  kb001,
  kb002,
  kb003,
  kb004,
  kb005,
  kb006,
  kb007,
  kb008,
  kb009,
  kb010,
  kb011,
  kb012,
  kb014,
]
