/**
 * 培训管理API
 * 对应后端: /v1/hr/training
 *
 * 数据来源：直接对接员工端(employee-frontend)已配置的13门培训课程，
 * 非管理端新造数据。课程与员工端知识库文章一一关联。
 */
import { get, post, put, del } from '../request'
import type {
  TrainingCourse,
  TrainingCourseFormData,
  TrainingCourseQueryParams,
  StudyRecord,
  StudyRecordQueryParams,
  CertificateInfo,
  TrainingStatistics,
} from '../../types/hr/training'
import { ALL_ARTICLES } from './mock/knowledge/articles'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/** 文章ID→标题映射（用于课程关联文章标题） */
const ARTICLE_TITLE_MAP: Record<string, string> = Object.fromEntries(
  ALL_ARTICLES.map(a => [a.id, a.title])
)

// ============================================================
// 数据源：员工端真实培训课程（13门，与知识库文章一一关联）
// ============================================================

/** 员工端课程原始数据 */
const employeeCourses = [
  { id: 'tr-001', title: '食品安全操作规范', type: 'required' as const, duration: '120分钟', totalLessons: 8, instructor: '培训部·品控部', certificateEligible: true, relatedArticleId: 'kb-001', description: '涵盖食材验收、储存、加工、留样全流程的食品安全操作标准，全体员工必读。', deadline: '2026-09-30' },
  { id: 'tr-002', title: '门店服务与顾客沟通', type: 'required' as const, duration: '90分钟', totalLessons: 6, instructor: '前厅部', certificateEligible: true, relatedArticleId: 'kb-002', description: '3-5-7服务法则、LISTEN沟通技巧、投诉分级处理、开放式提问技巧等核心服务能力培训。', deadline: '2026-08-31' },
  { id: 'tr-003', title: 'POS收银系统操作', type: 'required' as const, duration: '60分钟', totalLessons: 5, instructor: '财务部', certificateEligible: false, relatedArticleId: 'kb-003', description: 'POS开机检查、点餐退菜流程、现金管理、反结账权限链等收银岗位必备技能。', deadline: '2026-08-20' },
  { id: 'tr-004', title: '消防安全与应急疏散', type: 'elective' as const, duration: '40分钟', totalLessons: 4, instructor: '安保部', certificateEligible: false, relatedArticleId: 'kb-005', description: 'PASS灭火法、燃气泄漏处置、火灾疏散路线、触电/烫伤急救等全员安全培训。', deadline: '2026-12-31' },
  { id: 'tr-006', title: '考勤与休假管理制度', type: 'elective' as const, duration: '50分钟', totalLessons: 4, instructor: '人事部', certificateEligible: false, relatedArticleId: 'kb-004', description: '打卡规则、迟到早退处理、请假流程、加班费计算、代打卡红线等考勤制度解读。', deadline: '2026-12-31' },
  { id: 'tr-007', title: '员工权益与劳动法基础', type: 'required' as const, duration: '90分钟', totalLessons: 6, instructor: '人力资源部·法务部', certificateEligible: true, relatedArticleId: 'kb-007', description: '劳动合同、工资社保、工伤认定、延迟退休新政、维权途径等法定权利保障。', deadline: '2026-09-18' },
  { id: 'tr-008', title: '交通安全培训', type: 'required' as const, duration: '60分钟', totalLessons: 5, instructor: '安全管理部', certificateEligible: true, relatedArticleId: 'kb-008', description: '电动车骑行安全、步行规范、交通事故应急处理、酒驾危害——保护生命安全。', deadline: '2026-10-05' },
  { id: 'tr-009', title: '生活法律常识', type: 'elective' as const, duration: '75分钟', totalLessons: 5, instructor: '法务部', certificateEligible: false, relatedArticleId: 'kb-009', description: '租房避坑、借贷陷阱识别、合同签订要点、个人信息保护、人身损害赔偿。', deadline: '2026-12-31' },
  { id: 'tr-010', title: '财务常识与报销规范', type: 'required' as const, duration: '60分钟', totalLessons: 4, instructor: '财务部', certificateEligible: true, relatedArticleId: 'kb-010', description: '读懂工资条、个税专项附加扣除、费用报销流程、个人理财基础——管好钱袋子。', deadline: '2026-10-15' },
  { id: 'tr-011', title: '急救技能与工伤防护', type: 'required' as const, duration: '75分钟', totalLessons: 5, instructor: '安全管理部·人力资源部', certificateEligible: true, relatedArticleId: 'kb-011', description: '心肺复苏(CPR)、海姆立克急救法、烧烫伤处理、厨房工伤预防——关键时刻救命技能。', deadline: '2026-10-20' },
  { id: 'tr-012', title: '职业健康与工作疲劳防护', type: 'required' as const, duration: '60分钟', totalLessons: 6, instructor: '安全管理部·人力资源部', certificateEligible: true, relatedArticleId: 'kb-012', description: '久坐/久站危害识别、科学休息方法、睡眠管理、职业病预防——保护身体从了解风险开始。', deadline: '2026-10-25' },
  { id: 'tr-013', title: '新员工入职指南', type: 'required' as const, duration: '45分钟', totalLessons: 7, instructor: '行政人事部·培训部', certificateEligible: false, relatedArticleId: 'kb-013', description: '企业文化、价值观、行为规范、入职须知、沟通机制、职业发展路径——新员工第一课。', deadline: '入职1周内' },
  { id: 'tr-014', title: '职场反霸凌与员工权益保障', type: 'required' as const, duration: '90分钟', totalLessons: 7, instructor: '人力资源部·法务部', certificateEligible: true, relatedArticleId: 'kb-014', description: '识别职场霸凌行为、掌握应对方法、了解公司零容忍政策、学习心理健康自护——每一位员工的尊严都值得被保护。', deadline: '2026-09-30' },
]

/** 将员工端课程时长字符串转为分钟数 */
function parseDuration(durationStr: string): number {
  const match = durationStr.match(/(\d+)/)
  return match ? parseInt(match[1], 10) : 60
}

/** 将员工端课程转换管理端格式 */
function convertToManagementCourses(): TrainingCourse[] {
  return employeeCourses.map(c => ({
    id: c.id,
    title: c.title,
    description: c.description,
    courseType: c.type,
    relatedArticleId: c.relatedArticleId,
    relatedArticleTitle: ARTICLE_TITLE_MAP[c.relatedArticleId] || '',
    instructor: c.instructor,
    totalLessons: c.totalLessons,
    duration: parseDuration(c.duration),
    deadline: c.deadline,
    certificateEligible: c.certificateEligible,
    certificateValidityDays: c.certificateEligible ? 365 : 0,
    publishStatus: 'published' as const,
    assignedCount: Math.floor(Math.random() * 10) + 5,
    completedCount: Math.floor(Math.random() * 5) + 1,
    averageScore: Math.floor(Math.random() * 20) + 75,
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 10:00:00',
  }))
}

let courseList: TrainingCourse[] = convertToManagementCourses()

// ============================================================
// Mock 数据 - 学习记录（文章/课程ID已对接员工端真实ID）
// ============================================================

let mockStudyRecordList: StudyRecord[] = [
  {
    id: 'TSR001',
    employeeId: 'EMP001',
    employeeName: '张伟',
    employeeCode: 'E2024001',
    departmentName: '前厅部',
    articleId: 'kb-001',
    articleTitle: '食品安全操作规范（2026版）',
    courseId: 'tr-001',
    courseTitle: '食品安全操作规范',
    startTime: '2026-05-10 09:00:00',
    endTime: '2026-05-10 12:00:00',
    durationSeconds: 10800,
    completed: true,
    score: 92,
    certificateId: 'CERT-2026-001',
    certificateExpiry: '2027-05-10',
  },
  {
    id: 'TSR002',
    employeeId: 'EMP002',
    employeeName: '李娜',
    employeeCode: 'E2024002',
    departmentName: '前厅部',
    articleId: 'kb-001',
    articleTitle: '食品安全操作规范（2026版）',
    courseId: 'tr-001',
    courseTitle: '食品安全操作规范',
    startTime: '2026-05-12 14:00:00',
    endTime: '2026-05-12 17:00:00',
    durationSeconds: 10800,
    completed: true,
    score: 88,
    certificateId: 'CERT-2026-002',
    certificateExpiry: '2027-05-12',
  },
  {
    id: 'TSR003',
    employeeId: 'EMP003',
    employeeName: '王强',
    employeeCode: 'E2024003',
    departmentName: '后厨部',
    articleId: 'kb-011',
    articleTitle: '急救技能与工伤防护',
    courseId: 'tr-011',
    courseTitle: '急救技能与工伤防护',
    startTime: '2026-05-15 09:00:00',
    endTime: '2026-05-15 10:30:00',
    durationSeconds: 5400,
    completed: true,
    score: 95,
    certificateId: 'CERT-2026-003',
    certificateExpiry: '2027-05-15',
  },
  {
    id: 'TSR004',
    employeeId: 'EMP010',
    employeeName: '郑浩',
    employeeCode: 'E2024010',
    departmentName: '前厅部',
    articleId: 'kb-014',
    articleTitle: '职场反霸凌与员工权益保障',
    courseId: 'tr-014',
    courseTitle: '职场反霸凌与员工权益保障',
    startTime: '2026-05-20 09:00:00',
    durationSeconds: 7200,
    completed: false,
    score: 0,
  },
  {
    id: 'TSR005',
    employeeId: 'EMP004',
    employeeName: '刘洋',
    employeeCode: 'E2024004',
    departmentName: '后厨部',
    articleId: 'kb-002',
    articleTitle: '门店服务标准手册',
    courseId: 'tr-002',
    courseTitle: '门店服务与顾客沟通',
    startTime: '2026-06-01 14:00:00',
    endTime: '2026-06-01 18:00:00',
    durationSeconds: 14400,
    completed: true,
    score: 78,
  },
]

// ============================================================
// Mock 数据 - 证书
// ============================================================

let mockCertificateList: CertificateInfo[] = [
  {
    id: 'CERT-2026-001',
    employeeId: 'EMP001',
    employeeName: '张伟',
    courseId: 'tr-001',
    courseTitle: '食品安全操作规范',
    issueDate: '2026-05-10',
    expiryDate: '2027-05-10',
    status: 'valid',
  },
  {
    id: 'CERT-2026-002',
    employeeId: 'EMP002',
    employeeName: '李娜',
    courseId: 'tr-001',
    courseTitle: '食品安全操作规范',
    issueDate: '2026-05-12',
    expiryDate: '2027-05-12',
    status: 'valid',
  },
  {
    id: 'CERT-2026-003',
    employeeId: 'EMP003',
    employeeName: '王强',
    courseId: 'tr-011',
    courseTitle: '急救技能与工伤防护',
    issueDate: '2026-05-15',
    expiryDate: '2027-05-15',
    status: 'valid',
  },
  {
    id: 'CERT-2026-004',
    employeeId: 'EMP004',
    employeeName: '刘洋',
    courseId: 'tr-007',
    courseTitle: '员工权益与劳动法基础',
    issueDate: '2026-04-20',
    expiryDate: '2026-10-20',
    status: 'expiring',
  },
]

/**
 * 映射分页查询参数：前端 page/pageSize → 后端 current/size
 */
function mapQueryParams<T extends object>(params?: T): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  return result
}

export const trainingApi = {
  /** 分页查询培训课程 */
  async getList(params?: TrainingCourseQueryParams): Promise<{ records: TrainingCourse[]; total: number }> {
    const res = await get<{ records: TrainingCourse[]; total: number } | TrainingCourse[]>('/v1/hr/training', mapQueryParams(params))
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: TrainingCourse[]; total: number }
  },

  /** 根据ID获取课程详情 */
  async getById(id: string): Promise<TrainingCourse> {
    return await get<TrainingCourse>(`/v1/hr/training/${id}`)
  },

  /** 创建课程 */
  async create(data: TrainingCourseFormData): Promise<TrainingCourse> {
    return await post<TrainingCourse>('/v1/hr/training', data)
  },

  /** 更新课程 */
  async update(id: string, data: Partial<TrainingCourseFormData>): Promise<TrainingCourse> {
    return await put<TrainingCourse>(`/v1/hr/training/${id}`, data)
  },

  /** 删除课程 */
  async delete(id: string): Promise<void> {
    await del(`/v1/hr/training/${id}`)
  },

  /** 发布课程 */
  async publish(id: string): Promise<void> {
    await post(`/v1/hr/training/${id}/publish`, {})
  },

  /** 归档课程 */
  async archive(id: string): Promise<void> {
    await post(`/v1/hr/training/${id}/archive`, {})
  },

  /** 获取学习记录 */
  async getStudyRecords(params?: StudyRecordQueryParams): Promise<{ records: StudyRecord[]; total: number }> {
    const res = await get<{ records: StudyRecord[]; total: number } | StudyRecord[]>('/v1/hr/training/study-records', mapQueryParams(params))
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: StudyRecord[]; total: number }
  },

  /** 获取证书列表 */
  async getCertificates(params?: { page?: number; pageSize?: number; keyword?: string }): Promise<{ records: CertificateInfo[]; total: number }> {
    const res = await get<{ records: CertificateInfo[]; total: number } | CertificateInfo[]>('/v1/hr/training/certificates', mapQueryParams(params))
    if (Array.isArray(res)) return { records: res, total: res.length }
    return res as { records: CertificateInfo[]; total: number }
  },

  /** 获取培训统计 */
  async getStatistics(): Promise<TrainingStatistics> {
    return await get<TrainingStatistics>('/v1/hr/training/statistics')
  },
}
