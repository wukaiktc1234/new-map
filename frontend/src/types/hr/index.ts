/**
 * HR模块类型统一导出
 */

// 超龄劳动者
export type {
  EmploymentType,
  EmployeeLifecycleStatus,
  OverAgeWorkerInfo,
  OverAgeWorkerFormData,
  OverAgeWorkerQueryParams,
} from './over-age-worker'
export {
  EmploymentTypeOptions,
  EmployeeLifecycleStatusOptions,
} from './over-age-worker'

// 合同管理
export type {
  ContractType,
  ContractStatus,
  LaborContractTerm,
  EmployeeContract,
  ContractFormData,
  ContractQueryParams,
  ContractRenewData,
  ContractTerminateData,
} from './contract'
export {
  ContractTypeOptions,
  ContractStatusOptions,
  ContractStatusTagMap,
  LaborContractTermOptions,
} from './contract'

// 考勤排班
export type {
  ShiftType,
  AttendanceStatus,
  LeaveType,
  ScheduleRecord,
  ScheduleTemplate,
  ScheduleTemplateRow,
  AttendanceRecord,
  AttendanceMonthlySummary,
  AttendanceQueryParams,
  ScheduleQueryParams,
  AttendanceSummaryQueryParams,
  StoreAttendanceSubmission,
} from './attendance'
export {
  ShiftTypeOptions,
  AttendanceStatusOptions,
  AttendanceStatusTagMap,
  LeaveTypeOptions,
} from './attendance'

// 知识库管理
export type {
  KnowledgeCategory,
  ArticlePublishStatus,
  QuizDifficulty,
  QuizOption,
  QuizQuestion,
  KnowledgeArticle,
  KnowledgeArticleFormData,
  KnowledgeArticleQueryParams,
  KnowledgeCategoryStat,
} from './knowledge-base'
export {
  KnowledgeCategoryOptions,
  ArticlePublishStatusOptions,
  ArticlePublishStatusTagMap,
} from './knowledge-base'

// 培训管理
export type {
  CourseStatus,
  CourseType,
  CoursePublishStatus,
  TrainingCourse,
  TrainingCourseFormData,
  TrainingCourseQueryParams,
  StudyRecord,
  CertificateInfo,
  StudyRecordQueryParams,
  TrainingStatistics,
} from './training'
export {
  CourseTypeOptions,
  CoursePublishStatusOptions,
  CoursePublishStatusTagMap,
} from './training'

// 跨模块流转
export type {
  SourceModule,
  TargetModule,
  CrossModuleEventStatus,
  CrossModuleEventType,
  CrossModuleEvent,
  PendingTask,
  ApprovalFlowConfig,
  ApprovalFlowStep,
} from './cross-module'

// 薪资管理
export type {
  SalaryRecord,
  SalaryQueryDTO,
  SalaryApprovalDTO,
  SalaryPaymentDTO,
  SalaryBatchPaymentDTO,
  SalaryStatisticsVO,
  SalaryFinanceSyncData,
} from './salary'
export {
  SalaryStatus,
  SalaryStatusTagMap,
  SalaryStatusOptions,
} from './salary'

// 招聘管理
export type {
  RecruitmentPosition,
  Resume,
  InterviewRecord,
  HireRecord,
  PositionCreateDTO,
  PositionUpdateDTO,
  PositionQueryParams,
  ResumeCreateDTO,
  ResumeQueryParams,
  InterviewCreateDTO,
  InterviewQueryParams,
  HireCreateDTO,
  HireQueryParams,
  RecruitmentStatistics,
  InterviewResult,
  InterviewType,
  EducationRequirement,
  HireStatus,
} from './recruitment'
export {
  RecruitmentStatus,
  RecruitmentStatusTagMap,
  RecruitmentStatusOptions,
  ResumeStatus,
  ResumeStatusTagMap,
  ResumeStatusOptions,
  InterviewResultTagMap,
  InterviewResultOptions,
  InterviewTypeOptions,
  EducationOptions,
  HireStatusTagMap,
  HireStatusOptions,
} from './recruitment'
