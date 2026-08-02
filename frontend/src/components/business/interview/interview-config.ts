/**
 * 面试记录时间线 - 配置文件
 *
 * 包含面试类型、结果、图标等视觉配置
 *
 * 注意：var() fallback为防御性编码，确保CSS变量未定义时有默认样式
 */
import type { Component } from 'vue'
import {
  CircleCheckFilled,
  Clock,
  CircleCloseFilled,
  WarningFilled,
  Timer,
  Shop,
  User
} from '@element-plus/icons-vue'
import { InterviewType, InterviewResult } from './types'

export { InterviewType, InterviewResult }

export interface InterviewTypeConfig {
  label: string
  icon: Component
  color: string
  bgColor: string
  borderColor: string
  textColor: string
  trackLabel: string
  dotColor: string
  lineColor: string
}

export interface InterviewResultConfig {
  label: string
  status: string
  icon: Component
  description: string
}

export const INTERVIEW_TYPE_CONFIG: Record<InterviewType, InterviewTypeConfig> = {
  [InterviewType.STORE]: {
    label: '门店面试',
    icon: Shop,
    color: 'var(--fts-success)',
    bgColor: 'var(--fts-success-bg, rgba(103, 194, 58, 0.08))',
    borderColor: 'var(--fts-success-border, #e1f3d8)',
    textColor: 'var(--fts-success-dark, #529b2e)',
    trackLabel: '门店轨道',
    dotColor: 'var(--fts-success)',
    lineColor: 'var(--fts-success-light, #95d475)'
  },
  [InterviewType.HR]: {
    label: '人事面试',
    icon: User,
    color: 'var(--fts-primary)',
    bgColor: 'var(--fts-primary-bg, rgba(64, 158, 255, 0.08))',
    borderColor: 'var(--fts-primary-border, #d9ecff)',
    textColor: 'var(--fts-primary-dark, #337ecc)',
    trackLabel: '人事轨道',
    dotColor: 'var(--fts-primary)',
    lineColor: 'var(--fts-primary-light, #a0cfff)'
  }
}

export const INTERVIEW_RESULT_CONFIG: Record<InterviewResult, InterviewResultConfig> = {
  [InterviewResult.PASSED]: {
    label: '通过',
    status: 'passed',
    icon: CircleCheckFilled,
    description: '本轮面试通过，可进入下一阶段'
  },
  [InterviewResult.PENDING]: {
    label: '待定',
    status: 'warning',
    icon: Clock,
    description: '需补充材料或安排加试'
  },
  [InterviewResult.REJECTED]: {
    label: '淘汰',
    status: 'failed',
    icon: CircleCloseFilled,
    description: '不符合要求，终止招聘流程'
  },
  [InterviewResult.CANCELLED]: {
    label: '已取消',
    status: 'info',
    icon: WarningFilled,
    description: '应聘者或公司取消了本次面试'
  },
  [InterviewResult.SCHEDULED]: {
    label: '已安排',
    status: 'pending',
    icon: Timer,
    description: '已安排时间，等待进行中'
  }
}
