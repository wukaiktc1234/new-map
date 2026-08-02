/**
 * 入职管理模块类型定义
 * 从 api/onboarding.ts 迁移而来
 */

// 入职档案接口
/** 人员类型 */
export type PersonnelType = 'social' | 'school' | 'intern' | 'returnee'

export interface OnboardingArchive {
  id?: number;
  candidateName: string;
  email: string;
  phone?: string;
  idCard?: string;
  position: string;
  positionLevel: string;
  departmentId: string;
  departmentName?: string;
  roleId?: string;
  roleName?: string;
  expectedSalary?: number;
  finalSalary?: number;
  onboardDate?: string;
  /** 现住址 */
  address?: string;
  /** 人员类型：social-社招 school-校招/应届生 intern-实习生 returnee-返聘 */
  personnelType?: PersonnelType;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

// 审批记录接口
export interface OnboardingApprovalRecord {
  id?: number;
  archiveId: number;
  stepNumber: number;
  reviewerId?: number;
  reviewerName?: string;
  action?: string;
  comment?: string;
  actionTime?: string;
  status: string;
  createTime?: string;
  updateTime?: string;
}

// 审批记录与档案关联接口
export interface ApprovalRecordWithArchive {
  approvalRecord: OnboardingApprovalRecord;
  archive: OnboardingArchive;
}

// 邀请码记录接口
export interface InvitationRecord {
  id?: number;
  archiveId: number;
  invitationCode: string;
  boundEmail?: string;
  boundPhone?: string;
  boundName?: string;
  useCount: number;
  maxUseCount: number;
  status: string;
  validDays: number;
  expireTime: string;
  usedBy?: number;
  usedTime?: string;
  sendMethod?: string;
  sendStatus: string;
  sendTime?: string;
  errorMessage?: string;
  createTime?: string;
}
