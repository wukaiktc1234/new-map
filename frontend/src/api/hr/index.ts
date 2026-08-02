/**
 * HR模块 API 统一导出
 */

// 员工管理
export { default as employeeApi } from './employee'

// 部门管理
export { departmentApi } from './department'

// 合同管理
export { contractApi } from './contract'

// 考勤排班
export { attendanceApi, scheduleApi } from './attendance'

// 健康证管理
export { healthCertificateApi } from './health-certificate'

// 知识库管理
export { knowledgeBaseApi } from './knowledge-base'

// 培训管理
export { trainingApi } from './training'

// 超龄劳动者管理
export { overAgeWorkerApi } from './over-age-worker'

// 薪资管理
export { salaryApi, salaryBatchApi } from './salary'

// HR配置中心
export { hrConfigApi } from './config'

// 招聘管理
export { recruitmentApi, positionApi, resumeApi, interviewApi, hireApi, recruitmentStatsApi } from './recruitment'

// 入职管理
export { onboardingApi } from './onboarding'
export type { OnboardingListParams, OnboardingFormData } from './onboarding'
