/**
 * 面试记录时间线 - 类型定义
 * 
 * 用于 InterviewHistoryTimeline 组件的数据结构
 */

export enum InterviewType {
  STORE = 'store',
  HR = 'hr'
}

export enum InterviewResult {
  PASSED = 'passed',
  PENDING = 'pending',
  REJECTED = 'rejected',
  CANCELLED = 'cancelled',
  SCHEDULED = 'scheduled'
}

export interface AssessmentItem {
  id: string
  name: string
  category: 'skill' | 'attitude' | 'experience' | 'other'
  score: number
  maxScore: number
  weight: number
  comment?: string
}

export interface InterviewRecord {
  interviewId: string
  applicantId: string
  round: number
  type: InterviewType
  name: string
  scheduledTime: string
  actualStartTime?: string
  actualEndTime?: string
  duration?: number
  interviewerId: string
  interviewerName: string
  interviewerRole: string
  interviewerDepartment?: string
  panelInterviewers?: Array<{
    id: string
    name: string
    role: string
    department?: string
  }>
  result: InterviewResult
  totalScore?: number
  assessmentItems?: AssessmentItem[]
  overallComment: string
  strengths?: string[]
  weaknesses?: string
  recommendation?: string
  nextStep?: string
  followUpDate?: string
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

export interface InterviewTimeline {
  applicantId: string
  applicantName: string
  positionId: string
  positionName: string
  records: InterviewRecord[]
  totalRounds: number
  currentRound?: number
  finalResult?: InterviewResult
  overallProgress: number
}
