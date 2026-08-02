/**
 * 培训管理类型定义
 * 对接员工端(employee-frontend)培训模块，提供管理端CRUD能力
 */

/** 课程状态 */
export type CourseStatus = 'not_started' | 'in_progress' | 'completed'

/** 课程类型 */
export type CourseType = 'required' | 'elective'

/** 课程发布状态（管理端） */
export type CoursePublishStatus = 'draft' | 'published' | 'archived'

/** 课程类型选项 */
export const CourseTypeOptions: { value: CourseType; label: string }[] = [
  { value: 'required', label: '必修' },
  { value: 'elective', label: '选修' },
]

/** 课程发布状态选项 */
export const CoursePublishStatusOptions: { value: CoursePublishStatus; label: string }[] = [
  { value: 'draft', label: '草稿' },
  { value: 'published', label: '已发布' },
  { value: 'archived', label: '已归档' },
]

/** 课程发布状态到StatusTag映射 */
export const CoursePublishStatusTagMap: Record<CoursePublishStatus, string> = {
  draft: 'default',
  published: 'active',
  archived: 'inactive',
}

/** 培训课程（管理端版本） */
export interface TrainingCourse {
  id: string
  title: string
  description?: string
  /** 课程类型：必修/选修 */
  courseType: CourseType
  /** 关联知识库文章ID */
  relatedArticleId?: string
  relatedArticleTitle?: string
  /** 讲师/部门 */
  instructor?: string
  /** 总课时数 */
  totalLessons?: number
  /** 时长（分钟） */
  duration: number
  /** 截止日期 */
  deadline?: string
  /** 是否可获得证书 */
  certificateEligible: boolean
  /** 证书有效期天数 */
  certificateValidityDays: number
  /** 发布状态 */
  publishStatus: CoursePublishStatus
  /** 已分配员工数 */
  assignedCount?: number
  /** 已完成员工数 */
  completedCount?: number
  /** 平均得分 */
  averageScore?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 培训课程表单数据 */
export interface TrainingCourseFormData {
  title: string
  description?: string
  courseType: CourseType
  relatedArticleId?: string
  instructor?: string
  totalLessons?: number
  duration: number
  deadline?: string
  certificateEligible: boolean
  certificateValidityDays: number
  publishStatus: CoursePublishStatus
}

/** 培训课程查询参数 */
export interface TrainingCourseQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  courseType?: CourseType
  publishStatus?: CoursePublishStatus
}

/** 学习记录（管理端查看） */
export interface StudyRecord {
  id: string
  employeeId: string
  employeeName: string
  employeeCode?: string
  departmentName?: string
  articleId: string
  articleTitle?: string
  courseId?: string
  courseTitle?: string
  /** 开始时间 */
  startTime: string
  /** 结束时间 */
  endTime?: string
  /** 学习时长（秒） */
  durationSeconds: number
  /** 是否通过测验 */
  completed: boolean
  /** 得分 0-100 */
  score: number
  /** 证书ID */
  certificateId?: string
  /** 证书到期日 */
  certificateExpiry?: string
}

/** 证书信息 */
export interface CertificateInfo {
  id: string
  employeeId: string
  employeeName: string
  courseId: string
  courseTitle: string
  /** 发证日期 */
  issueDate: string
  /** 到期日期 */
  expiryDate: string
  /** 证书状态 */
  status: 'valid' | 'expiring' | 'expired'
}

/** 学习记录查询参数 */
export interface StudyRecordQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  courseId?: string
  employeeId?: string
  completed?: boolean
  startDate?: string
  endDate?: string
}

/** 培训统计 */
export interface TrainingStatistics {
  totalCourses: number
  publishedCourses: number
  totalAssigned: number
  totalCompleted: number
  completionRate: number
  averageScore: number
  expiringCertificates: number
}
