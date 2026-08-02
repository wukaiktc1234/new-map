export type CourseStatus = 'not_started' | 'in_progress' | 'completed' | 'expired'

export const COURSE_STATUS_LABEL_MAP: Record<CourseStatus, string> = {
  not_started: '未开始',
  in_progress: '学习中',
  completed: '已完成',
  expired: '已过期',
}

export interface TrainingProgress {
  currentLesson: number
  totalLessons: number
  percent: number
  lastAccessAt: string
}

export interface TrainingCourse {
  id: string
  title: string
  description: string
  coverImage?: string
  category: string
  duration: number
  status: CourseStatus
  progress?: TrainingProgress
  score?: number
  certificateId?: string
  requiredBy: Date
  publishedAt: string
}
