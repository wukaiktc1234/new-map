/**
 * 面试记录时间线 - Mock 数据
 * 
 * 用于开发和测试的示例数据
 */
import { InterviewType, InterviewResult } from './types'
import type { InterviewTimeline } from './types'

export const mockInterviewTimeline: InterviewTimeline = {
  applicantId: 'APP001',
  applicantName: '张师傅',
  positionId: 'REC001',
  positionName: '川菜主厨',
  overallProgress: 75,
  totalRounds: 4,
  currentRound: 4,
  records: [
    {
      interviewId: 'INT-20260512-001',
      applicantId: 'APP001',
      round: 1,
      type: InterviewType.HR,
      name: '初筛面谈',
      scheduledTime: '2026-05-12T14:00:00Z',
      actualStartTime: '2026-05-12T14:02:00Z',
      actualEndTime: '2026-05-12T14:32:00Z',
      duration: 30,
      interviewerId: 'USR-HR-001',
      interviewerName: '王HR',
      interviewerRole: 'HR_STAFF',
      interviewerDepartment: '人力资源部',
      result: InterviewResult.PASSED,
      totalScore: 85,
      assessmentItems: [
        {
          id: 'AI-001',
          name: '基本沟通',
          category: 'attitude',
          score: 88,
          maxScore: 100,
          weight: 0.3,
          comment: '表达清晰流畅'
        },
        {
          id: 'AI-002',
          name: '工作经历验证',
          category: 'experience',
          score: 82,
          maxScore: 100,
          weight: 0.4,
          comment: '经历真实可信'
        },
        {
          id: 'AI-003',
          name: '薪资期望确认',
          category: 'other',
          score: 85,
          maxScore: 100,
          weight: 0.3,
          comment: '期望合理'
        }
      ],
      overallComment:
        '沟通表达清晰，工作经验真实，薪资期望合理，建议进入门店技能面试环节。',
      strengths: ['8年川菜经验', '态度端正诚恳', '稳定性强'],
      weaknesses: '对新技术接受度一般',
      recommendation: '推荐',
      nextStep: '安排门店试菜考核',
      followUpDate: '2026-05-13',
      createdAt: '2026-05-12T14:35:00Z',
      updatedAt: '2026-05-12T14:35:00Z',
      createdBy: 'USR-HR-001',
      updatedBy: 'USR-HR-001'
    },
    {
      interviewId: 'INT-20260514-001',
      applicantId: 'APP001',
      round: 2,
      type: InterviewType.STORE,
      name: '试菜考核',
      scheduledTime: '2026-05-14T10:00:00Z',
      actualStartTime: '2026-05-14T10:05:00Z',
      actualEndTime: '2026-05-14T12:05:00Z',
      duration: 120,
      interviewerId: 'USR-MGR-001',
      interviewerName: '刘店长',
      interviewerRole: 'STORE_MANAGER',
      interviewerDepartment: '后厨部',
      panelInterviewers: [
        { id: 'USRCHEF-001', name: '张厨师长', role: 'CHEF', department: '后厨部' }
      ],
      result: InterviewResult.PASSED,
      totalScore: 92,
      assessmentItems: [
        {
          id: 'AI-004',
          name: '刀工测试',
          category: 'skill',
          score: 95,
          maxScore: 100,
          weight: 0.3,
          comment: '刀工娴熟，切丝均匀'
        },
        {
          id: 'AI-005',
          name: '调味能力',
          category: 'skill',
          score: 90,
          maxScore: 100,
          weight: 0.25,
          comment: '调味精准，层次分明'
        },
        {
          id: 'AI-006',
          name: '出菜速度',
          category: 'skill',
          score: 88,
          maxScore: 100,
          weight: 0.2,
          comment: '出菜快速且质量稳定'
        },
        {
          id: 'AI-007',
          name: '卫生习惯',
          category: 'attitude',
          score: 95,
          maxScore: 100,
          weight: 0.15,
          comment: '操作规范，卫生意识强'
        },
        {
          id: 'AI-008',
          name: '团队配合',
          category: 'attitude',
          score: 92,
          maxScore: 100,
          weight: 0.1,
          comment: '与小厨配合默契'
        }
      ],
      overallComment:
        '刀工娴熟，调味精准，出菜速度快且质量稳定，特别擅长川菜系列，卫生意识强，建议录用。',
      strengths: ['刀工精湛', '调味功底扎实', '出菜效率高', '卫生习惯好'],
      weaknesses: '创意菜研发经验稍欠缺',
      recommendation: '强烈推荐',
      nextStep: '安排实战演练（可选）',
      createdAt: '2026-05-14T12:10:00Z',
      updatedAt: '2026-05-14T12:10:00Z',
      createdBy: 'USR-MGR-001',
      updatedBy: 'USR-MGR-001'
    },
    {
      interviewId: 'INT-20260515-001',
      applicantId: 'APP001',
      round: 3,
      type: InterviewType.STORE,
      name: '实战演练',
      scheduledTime: '2026-05-15T14:00:00Z',
      actualStartTime: '2026-05-15T14:00:00Z',
      actualEndTime: '2026-05-15T17:00:00Z',
      duration: 180,
      interviewerId: 'USR-MGR-001',
      interviewerName: '刘店长',
      interviewerRole: 'STORE_MANAGER',
      panelInterviewers: [
        { id: 'USRCHEF-001', name: '张厨师长', role: 'CHEF', department: '后厨部' },
        { id: 'USR-SVR-001', name: '李领班', role: 'TEAM_LEADER', department: '服务部' }
      ],
      result: InterviewResult.PASSED,
      totalScore: 88,
      assessmentItems: [
        {
          id: 'AI-009',
          name: '高峰期应对',
          category: 'skill',
          score: 86,
          maxScore: 100,
          weight: 0.4,
          comment: '应对有序，稍有紧张'
        },
        {
          id: 'AI-010',
          name: '团队配合',
          category: 'attitude',
          score: 90,
          maxScore: 100,
          weight: 0.35,
          comment: '协作顺畅'
        },
        {
          id: 'AI-011',
          name: '成本控制意识',
          category: 'other',
          score: 88,
          maxScore: 100,
          weight: 0.25,
          comment: '用料合理'
        }
      ],
      overallComment: '高峰期能保持出品质量，团队配合默契，成本控制意识较好。',
      strengths: ['抗压能力强', '团队协作好'],
      weaknesses: '高峰期初期略显紧张',
      recommendation: '推荐',
      createdAt: '2026-05-15T17:05:00Z',
      updatedAt: '2026-05-15T17:05:00Z',
      createdBy: 'USR-MGR-001',
      updatedBy: 'USR-MGR-001'
    },
    {
      interviewId: 'INT-20260516-001',
      applicantId: 'APP001',
      round: 4,
      type: InterviewType.HR,
      name: '录用洽谈',
      scheduledTime: '2026-05-16T15:00:00Z',
      interviewerId: 'USR-RM-001',
      interviewerName: '陈区域经理',
      interviewerRole: 'REGIONAL_MANAGER',
      interviewerDepartment: '运营管理部',
      result: InterviewResult.SCHEDULED,
      overallComment: '',
      createdAt: '2026-05-15T18:00:00Z',
      updatedAt: '2026-05-15T18:00:00Z',
      createdBy: 'USR-HR-001',
      updatedBy: 'USR-HR-001'
    }
  ]
}
