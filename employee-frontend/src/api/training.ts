import { mockDelay } from './mock/delays'

type CourseStatus = 'not_started' | 'in_progress' | 'completed'
type CourseType = 'required' | 'elective'

export interface TrainingCourse {
  id: string
  title: string
  chapter: string
  duration: string
  progress: number
  status: CourseStatus
  type: CourseType
  /** 课程描述 */
  description?: string
  /** 截止日期（MM-DD 格式） */
  deadline?: string
  /** 讲师/部门 */
  instructor?: string
  /** 总课时数 */
  totalLessons?: number
  /** 是否可获得证书 */
  certificateEligible?: boolean
  /** 关联的知识库文章ID（学习材料来源） */
  relatedArticleId?: string
}

const mockCourses: TrainingCourse[] = [
  {
    id: 'tr-001', title: '食品安全操作规范',
    chapter: '8章·已完成', duration: '120分钟',
    progress: 100, status: 'completed', type: 'required',
    description: '涵盖食材验收、储存、加工、留样全流程的食品安全操作标准，全体员工必读。',
    instructor: '培训部·品控部', totalLessons: 8, certificateEligible: true,
    relatedArticleId: 'kb-001',
  },
  {
    id: 'tr-002', title: '门店服务与顾客沟通',
    chapter: '6章·进行中', duration: '90分钟',
    progress: 60, status: 'in_progress', type: 'required',
    description: '3-5-7服务法则、LISTEN沟通技巧、投诉分级处理、开放式提问技巧等核心服务能力培训。',
    deadline: '06-15', instructor: '前厅部', totalLessons: 6, certificateEligible: true,
    relatedArticleId: 'kb-002',
  },
  {
    id: 'tr-003', title: 'POS收银系统操作',
    chapter: '5章·未开始', duration: '60分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: 'POS开机检查、点餐退菜流程、现金管理、反结账权限链等收银岗位必备技能。',
    deadline: '06-20', instructor: '财务部', totalLessons: 5, certificateEligible: false,
    relatedArticleId: 'kb-003',
  },
  {
    id: 'tr-004', title: '消防安全与应急疏散',
    chapter: '4章·未开始', duration: '40分钟',
    progress: 0, status: 'not_started', type: 'elective',
    description: 'PASS灭火法、燃气泄漏处置、火灾疏散路线、触电/烫伤急救等全员安全培训。',
    deadline: '06-30', instructor: '安保部', totalLessons: 4, certificateEligible: false,
    relatedArticleId: 'kb-005',
  },
  {
    id: 'tr-006', title: '考勤与休假管理制度',
    chapter: '4章·未开始', duration: '50分钟',
    progress: 0, status: 'not_started', type: 'elective',
    description: '打卡规则、迟到早退处理、请假流程、加班费计算、代打卡红线等考勤制度解读。',
    deadline: '07-01', instructor: '人事部', totalLessons: 4, certificateEligible: false,
    relatedArticleId: 'kb-004',
  },
  {
    id: 'tr-007', title: '员工权益与劳动法基础',
    chapter: '6章·未开始', duration: '90分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '劳动合同、工资社保、工伤认定、延迟退休新政、维权途径等法定权利保障。',
    deadline: '06-18', instructor: '人力资源部·法务部', totalLessons: 6, certificateEligible: true,
    relatedArticleId: 'kb-007',
  },
  {
    id: 'tr-008', title: '交通安全培训',
    chapter: '5章·未开始', duration: '60分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '电动车骑行安全、步行规范、交通事故应急处理、酒驾危害——保护生命安全。',
    deadline: '07-05', instructor: '安全管理部', totalLessons: 5, certificateEligible: true,
    relatedArticleId: 'kb-008',
  },
  {
    id: 'tr-009', title: '生活法律常识',
    chapter: '5章·未开始', duration: '75分钟',
    progress: 0, status: 'not_started', type: 'elective',
    description: '租房避坑、借贷陷阱识别、合同签订要点、个人信息保护、人身损害赔偿。',
    deadline: '07-10', instructor: '法务部', totalLessons: 5, certificateEligible: false,
    relatedArticleId: 'kb-009',
  },
  {
    id: 'tr-010', title: '财务常识与报销规范',
    chapter: '4章·未开始', duration: '60分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '读懂工资条、个税专项附加扣除、费用报销流程、个人理财基础——管好钱袋子。',
    deadline: '07-15', instructor: '财务部', totalLessons: 4, certificateEligible: true,
    relatedArticleId: 'kb-010',
  },
  {
    id: 'tr-011', title: '急救技能与工伤防护',
    chapter: '5章·未开始', duration: '75分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '心肺复苏(CPR)、海姆立克急救法、烧烫伤处理、厨房工伤预防——关键时刻救命技能。',
    deadline: '07-20', instructor: '安全管理部·人力资源部', totalLessons: 5, certificateEligible: true,
    relatedArticleId: 'kb-011',
  },
  {
    id: 'tr-012', title: '职业健康与工作疲劳防护',
    chapter: '6章·未开始', duration: '60分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '久坐/久站危害识别、科学休息方法、睡眠管理、职业病预防——保护身体从了解风险开始。',
    deadline: '07-25', instructor: '安全管理部·人力资源部', totalLessons: 6, certificateEligible: true,
    relatedArticleId: 'kb-012',
  },
  {
    id: 'tr-013', title: '新员工入职指南',
    chapter: '7章·未开始', duration: '45分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '企业文化、价值观、行为规范、入职须知、沟通机制、职业发展路径——新员工第一课。',
    deadline: '入职1周内', instructor: '行政人事部·培训部', totalLessons: 7, certificateEligible: false,
    relatedArticleId: 'kb-013',
  },
  {
    id: 'tr-014', title: '职场反霸凌与员工权益保障',
    chapter: '7章·未开始', duration: '90分钟',
    progress: 0, status: 'not_started', type: 'required',
    description: '识别职场霸凌行为、掌握应对方法、了解公司零容忍政策、学习心理健康自护——每一位员工的尊严都值得被保护。',
    deadline: '06-30', instructor: '人力资源部·法务部', totalLessons: 7, certificateEligible: true,
    relatedArticleId: 'kb-014',
  },
]

export const trainingApi = {
  async getCourses(): Promise<TrainingCourse[]> {
    await mockDelay(60, 200)
    return mockCourses.map(c => ({ ...c }))
  },

  async updateProgress(id: string, progress: number): Promise<TrainingCourse> {
    await mockDelay(40, 120)
    const course = mockCourses.find(c => c.id === id)
    if (!course) throw new Error('课程不存在')
    course.progress = Math.min(100, progress)
    if (course.progress >= 100) {
      course.status = 'completed'
    } else if (course.progress > 0) {
      course.status = 'in_progress'
    }
    return { ...course }
  },
}