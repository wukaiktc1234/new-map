import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const hrMenu: MenuGroupConfig = {
  id: 'hr',
  title: '人事管理',
  icon: 'UserFilled',
  path: '/hr',
  order: 100,
  category: 'hr',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.HR_DIRECTOR],
  children: [
    // ===== 组织与员工（基础数据） =====
    { title: '员工管理', icon: 'User', path: '/hr/employee' },
    { title: '组织架构', icon: 'OfficeBuilding', path: '/hr/organization' },
    { title: '岗位管理', icon: 'Postcard', path: '/hr/position' },
    // ===== 日常运营 =====
    { title: '考勤排班', icon: 'Clock', path: '/hr/attendance' },
    // ===== 人才获取与发展 =====
    { title: '招聘管理', icon: 'UserFilled', path: '/hr/recruitment' },
    { title: '入职办理', icon: 'DocumentChecked', path: '/hr/onboarding' },
    { title: '培训发展', icon: 'Reading', path: '/hr/training' },
    { title: '学习记录', icon: 'Notebook', path: '/hr/study-records', permissions: ['hr:study-record:view'] },
    // ===== 合规与薪酬 =====
    { title: '健康证管理', icon: 'Stamp', path: '/hr/health-certificate' },
    { title: '合同智能管理', icon: 'Connection', path: '/hr/contract-dashboard' },
    { title: '合同管理', icon: 'Document', path: '/hr/contract' },
    { title: '合同模板', icon: 'Files', path: '/hr/contract-template' },
    { title: '合同模板库', icon: 'FolderOpened', path: '/hr/contract-template-library' },
    { title: '薪资管理', icon: 'Money', path: '/hr/salary' },
    { title: '合规审批', icon: 'Check', path: '/hr/approval' },
    // ===== 知识与分析 =====
    { title: '知识库智能', icon: 'MagicStick', path: '/hr/knowledge-intelligence' },
    { title: '人事分析', icon: 'DataAnalysis', path: '/hr/analytics' },
    { title: '员工画像', icon: 'Aim', path: '/hr/employee-intelligence' },
    // ===== 系统配置（放最后） =====
    { title: '邀请码管理', icon: 'Key', path: '/hr/invitation-code' },
    { title: '配置中心', icon: 'Setting', path: '/hr/config-center' },
  ],
}

