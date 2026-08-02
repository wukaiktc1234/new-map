/**
 * 招聘管理类型定义
 * 包含招聘职位、简历、面试、录用全流程类型
 */

/** 招聘状态 */
export enum RecruitmentStatus {
  DRAFT = 'draft',           // 草稿
  PUBLISHED = 'published',   // 已发布
  CLOSED = 'closed',         // 已关闭
  CANCELLED = 'cancelled',   // 已取消
}

/** 招聘状态 → StatusTag映射 */
export const RecruitmentStatusTagMap: Record<string, { status: string; label: string }> = {
  draft: { status: 'info', label: '草稿' },
  published: { status: 'active', label: '招聘中' },
  closed: { status: 'inactive', label: '已关闭' },
  cancelled: { status: 'error', label: '已取消' },
}

/** 招聘状态选项 */
export const RecruitmentStatusOptions = Object.entries(RecruitmentStatusTagMap).map(
  ([value, { label }]) => ({ value, label }),
)

/** 简历状态 */
export enum ResumeStatus {
  SUBMITTED = 'submitted',     // 已投递
  SCREENING = 'screening',     // 简历筛选
  INTERVIEWING = 'interviewing', // 面试中
  OFFERED = 'offered',         // 已发offer
  HIRED = 'hired',             // 已录用
  REJECTED = 'rejected',       // 已拒绝
}

/** 简历状态 → StatusTag映射 */
export const ResumeStatusTagMap: Record<string, { status: string; label: string }> = {
  submitted: { status: 'info', label: '已投递' },
  screening: { status: 'warning', label: '筛选中' },
  interviewing: { status: 'info', label: '面试中' },
  offered: { status: 'warning', label: '已发offer' },
  hired: { status: 'active', label: '已录用' },
  rejected: { status: 'error', label: '已拒绝' },
}

/** 简历状态选项 */
export const ResumeStatusOptions = Object.entries(ResumeStatusTagMap).map(
  ([value, { label }]) => ({ value, label }),
)

/** 面试结果 */
export type InterviewResult = 'pending' | 'pass' | 'fail' | 'pending_next'

/** 面试结果 → StatusTag映射 */
export const InterviewResultTagMap: Record<InterviewResult, { status: string; label: string }> = {
  pending: { status: 'warning', label: '待面试' },
  pass: { status: 'active', label: '通过' },
  fail: { status: 'error', label: '未通过' },
  pending_next: { status: 'info', label: '待下一轮' },
}

/** 面试结果选项 */
export const InterviewResultOptions = Object.entries(InterviewResultTagMap).map(
  ([value, { label }]) => ({ value, label }),
)

/** 面试类型 */
export type InterviewType = 'phone' | 'video' | 'onsite'

/** 面试类型选项 */
export const InterviewTypeOptions: { value: InterviewType; label: string }[] = [
  { value: 'phone', label: '电话面试' },
  { value: 'video', label: '视频面试' },
  { value: 'onsite', label: '现场面试' },
]

/** 人事面谈状态 */
export type HrInterviewStatus = 'pending' | 'scheduled' | 'completed' | 'pass' | 'fail'

/** 人事面谈状态 → StatusTag 映射 */
export const HrInterviewStatusTagMap: Record<HrInterviewStatus, { status: string; label: string }> = {
  pending: { status: 'warning', label: '待人事面谈' },
  scheduled: { status: 'info', label: '已安排' },
  completed: { status: 'info', label: '已完成' },
  pass: { status: 'active', label: '通过' },
  fail: { status: 'error', label: '未通过' },
}

/** 人事面谈状态选项 */
export const HrInterviewStatusOptions: { value: HrInterviewStatus; label: string }[] = [
  { value: 'pending', label: '待人事面谈' },
  { value: 'scheduled', label: '已安排' },
  { value: 'completed', label: '已完成' },
  { value: 'pass', label: '通过' },
  { value: 'fail', label: '未通过' },
]

/** 核验结果 */
export type VerificationResult = 'pending' | 'pass' | 'fail'

/** 核验结果选项 */
export const VerificationResultOptions: { value: VerificationResult; label: string }[] = [
  { value: 'pending', label: '待核验' },
  { value: 'pass', label: '通过' },
  { value: 'fail', label: '未通过' },
]

/** 学历要求 */
export type EducationRequirement = 'none' | 'high_school' | 'college' | 'bachelor' | 'master'

/** 学历要求选项 */
export const EducationOptions: { value: EducationRequirement; label: string }[] = [
  { value: 'none', label: '不限' },
  { value: 'high_school', label: '高中' },
  { value: 'college', label: '大专' },
  { value: 'bachelor', label: '本科' },
  { value: 'master', label: '硕士' },
]

/** 录用状态 */
export type HireStatus = 'offered' | 'accepted' | 'declined' | 'onboarded'

/** 录用状态 → StatusTag映射 */
export const HireStatusTagMap: Record<HireStatus, { status: string; label: string }> = {
  offered: { status: 'warning', label: '已发Offer' },
  accepted: { status: 'info', label: '已接受' },
  declined: { status: 'error', label: '已拒绝' },
  onboarded: { status: 'active', label: '已入职' },
}

/** 录用状态选项 */
export const HireStatusOptions = Object.entries(HireStatusTagMap).map(
  ([value, { label }]) => ({ value, label }),
)

/** 招聘职位 */
export interface RecruitmentPosition {
  id: string
  /** 职位名称 */
  positionName: string
  /** 部门ID */
  departmentId: string
  /** 部门名称 */
  departmentName: string
  /** 招聘人数 */
  headcount: number
  /** 招聘状态 */
  status: RecruitmentStatus
  /** 薪资范围 */
  salaryRange?: string
  /** 学历要求 */
  educationRequirement: EducationRequirement
  /** 经验要求 */
  experienceRequirement?: string
  /** 职位描述 */
  jobDescription: string
  /** 任职要求 */
  requirements: string
  /** 发布日期 */
  publishDate?: string
  /** 截止日期 */
  deadline: string
  /** 应聘人数 */
  applicantCount: number
  /** 已录用人数 */
  hiredCount: number
  /** 创建人 */
  creatorName: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 简历/应聘者 */
export interface Resume {
  id: string
  /** 职位ID */
  positionId: string
  /** 职位名称 */
  positionName: string
  /** 应聘者姓名 */
  applicantName: string
  /** 联系电话 */
  phone: string
  /** 邮箱 */
  email?: string
  /** 性别 */
  gender?: 'male' | 'female'
  /** 年龄 */
  age?: number
  /** 学历 */
  education: string
  /** 工作经验 */
  experience?: string
  /** 简历地址 */
  resumeUrl?: string
  /** 简历状态 */
  status: ResumeStatus
  /** 投递时间 */
  submitTime: string
  /** 备注 */
  remark?: string
}

/** 面试记录 */
export interface InterviewRecord {
  id: string
  /** 简历ID */
  resumeId: string
  /** 应聘者姓名 */
  applicantName: string
  /** 职位名称 */
  positionName: string
  /** 第几轮面试 */
  interviewRound: number
  /** 面试类型 */
  interviewType: InterviewType
  /** 预约时间 */
  scheduledTime: string
  /** 时长（分钟） */
  duration?: number
  /** 面试官姓名 */
  interviewerName: string
  /** 面试结果 */
  result: InterviewResult
  /** 评价 */
  evaluation?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 人事面试官ID */
  hrInterviewerId?: string
  /** 人事面试官姓名 */
  hrInterviewerName?: string
  /** 人事面评内容 */
  hrEvaluation?: string
  /** 人事面评提交时间 */
  hrEvaluationTime?: string
  /** 人事面评评分 */
  hrEvaluationScore?: number
  /** 学历核验结果 */
  educationVerification?: VerificationResult
  /** 学历核验备注 */
  educationVerificationRemark?: string
  /** 背调结果 */
  backgroundCheckResult?: VerificationResult
  /** 背调备注 */
  backgroundCheckRemark?: string
  /** 人事面谈状态 */
  hrInterviewStatus?: HrInterviewStatus
}

/** 人事面评提交DTO */
export interface HrEvaluationSubmitDTO {
  /** 面评内容 */
  evaluation: string
  /** 评分 */
  score: number
  /** 学历核验结果 */
  educationVerification?: VerificationResult
  /** 学历核验备注 */
  educationRemark?: string
  /** 背调结果 */
  backgroundCheck?: VerificationResult
  /** 背调备注 */
  backgroundRemark?: string
  /** 人事面谈状态 */
  status: HrInterviewStatus
}

/** 录用记录 */
export interface HireRecord {
  id: string
  /** 简历ID */
  resumeId: string
  /** 应聘者姓名 */
  applicantName: string
  /** 职位名称 */
  positionName: string
  /** 部门名称 */
  departmentName: string
  /** 发Offer日期 */
  offerDate: string
  /** 入职日期 */
  onboardDate: string
  /** 薪资（分） */
  salary: number
  /** 录用状态 */
  status: HireStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
}

/** 职位创建DTO */
export interface PositionCreateDTO {
  positionName: string
  departmentId: string
  departmentName: string
  headcount: number
  salaryRange?: string
  educationRequirement: EducationRequirement
  experienceRequirement?: string
  jobDescription: string
  requirements: string
  deadline: string
}

/** 职位更新DTO */
export interface PositionUpdateDTO extends Partial<PositionCreateDTO> {}

/** 简历创建DTO */
export interface ResumeCreateDTO {
  positionId: string
  applicantName: string
  phone: string
  email?: string
  gender?: 'male' | 'female'
  age?: number
  education: string
  experience?: string
  remark?: string
}

/** 面试创建DTO */
export interface InterviewCreateDTO {
  resumeId: string
  interviewRound: number
  interviewType: InterviewType
  scheduledTime: string
  interviewerName: string
}

/** 录用创建DTO */
export interface HireCreateDTO {
  resumeId: string
  offerDate: string
  onboardDate: string
  /** 薪资（分） */
  salary: number
  remark?: string
}

/** 职位查询参数 */
export interface PositionQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  departmentId?: string
  status?: RecruitmentStatus
  startDate?: string
  endDate?: string
}

/** 简历查询参数 */
export interface ResumeQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  positionId?: string
  status?: ResumeStatus
}

/** 面试查询参数 */
export interface InterviewQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  resumeId?: string
  result?: InterviewResult
}

/** 录用查询参数 */
export interface HireQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: HireStatus
}

/** 招聘统计 */
export interface RecruitmentStatistics {
  /** 在招职位数 */
  openPositions: number
  /** 总招聘人数 */
  totalHeadcount: number
  /** 总应聘人数 */
  totalApplicants: number
  /** 已录用人数 */
  totalHired: number
}
