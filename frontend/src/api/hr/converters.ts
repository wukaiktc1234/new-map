/**
 * HR模块数据转换器
 * ============================================================
 *
 * 【定位】
 * 在API边界处完成数据格式转换，遵循项目规范第十六章。
 * 组件层禁止直接做状态映射或金额元分转换，必须通过Converter。
 *
 * 【转换规则】
 * - 金额：后端分(Long) ↔ 前端元(string)，使用 fenToYuan / yuanToFen
 * - 状态：后端数字 ↔ 前端语义字符串
 * - 日期：后端ISO 8601 ↔ 前端 YYYY-MM-DD HH:mm:ss
 *
 * 【使用方式】
 * ```typescript
 * // API层调用
 * const backendData = await request.get('/v1/hr/employees/1')
 * const frontendData = EmployeeDataConverter.toFrontend(backendData)
 *
 * // 表单提交前转换
 * const dto = EmployeeDataConverter.toCreateDTO(formData)
 * await request.post('/v1/hr/employees', dto)
 * ```
 */

import { fenToYuan, yuanToFen, fenToWan } from '@/utils/money'

// ============================================================
// 金额转换工具（统一委托给 utils/money，保留导出以兼容已有调用方）
// ============================================================

/**
 * 分转元（保留2位小数）
 * @param fen - 金额（分）
 * @returns 金额（元，字符串格式）
 */
export { fenToYuan, yuanToFen, fenToWan }

// ============================================================
// 员工状态映射
// ============================================================

/** 员工状态：后端数字 ↔ 前端语义字符串 */
export const EmployeeStatusMap = {
  // 后端 → 前端
  toFrontend: {
    1: 'active' as const,      // 在职
    0: 'inactive' as const,    // 离职
    2: 'probation' as const,   // 试用期
  } as Record<number, string>,
  // 前端 → 后端
  toBackend: {
    active: 1,
    inactive: 0,
    probation: 2,
  } as Record<string, number>,
}

/** 用工类型映射 */
export const EmploymentTypeMap = {
  toFrontend: {
    1: 'full_time' as const,    // 全职
    2: 'part_time' as const,    // 兼职
    3: 'intern' as const,       // 实习生
    4: 'dispatch' as const,     // 劳务派遣
    5: 'over_age' as const,     // 超龄返聘
  } as Record<number, string>,
  toBackend: {
    full_time: 1,
    part_time: 2,
    intern: 3,
    dispatch: 4,
    over_age: 5,
  } as Record<string, number>,
}

// ============================================================
// 合同状态映射
// ============================================================

/** 合同状态映射 */
export const ContractStatusMap = {
  toFrontend: {
    1: 'draft' as const,           // 草稿
    2: 'pending_approval' as const,// 待审批
    3: 'pending_sign' as const,    // 待签署
    4: 'company_signed' as const,  // 公司已签
    5: 'signed' as const,          // 双方已签
    6: 'active' as const,          // 生效中
    7: 'expiring' as const,        // 即将到期
    8: 'expired' as const,         // 已到期
    9: 'terminated' as const,      // 已终止
    10: 'renewed' as const,        // 已续签
    11: 'archived' as const,       // 已归档
  } as Record<number, string>,
  toBackend: {
    draft: 1,
    pending_approval: 2,
    pending_sign: 3,
    company_signed: 4,
    signed: 5,
    active: 6,
    expiring: 7,
    expired: 8,
    terminated: 9,
    renewed: 10,
    archived: 11,
  } as Record<string, number>,
}

/** 合同类型映射 */
export const ContractTypeMap = {
  toFrontend: {
    1: 'fixed_term' as const,      // 固定期限
    2: 'unfixed_term' as const,    // 无固定期限
    3: 'probation' as const,       // 试用期
    4: 'internship' as const,      // 实习协议
    5: 'service' as const,         // 劳务协议
    6: 'dispatch' as const,        // 派遣协议
    7: 'outsourcing' as const,     // 外包协议
    8: 'supplementary' as const,   // 补充协议
  } as Record<number, string>,
  toBackend: {
    fixed_term: 1,
    unfixed_term: 2,
    probation: 3,
    internship: 4,
    service: 5,
    dispatch: 6,
    outsourcing: 7,
    supplementary: 8,
  } as Record<string, number>,
}

// ============================================================
// 考勤状态映射
// ============================================================

/** 考勤状态映射 */
export const AttendanceStatusMap = {
  toFrontend: {
    1: 'normal' as const,       // 正常
    2: 'late' as const,         // 迟到
    3: 'early_leave' as const,  // 早退
    4: 'absent' as const,       // 缺勤
    5: 'leave' as const,        // 请假
    6: 'overtime' as const,     // 加班
  } as Record<number, string>,
  toBackend: {
    normal: 1,
    late: 2,
    early_leave: 3,
    absent: 4,
    leave: 5,
    overtime: 6,
  } as Record<string, number>,
}

/** 请假类型映射 */
export const LeaveTypeMap = {
  toFrontend: {
    1: 'sick' as const,        // 病假
    2: 'personal' as const,    // 事假
    3: 'annual' as const,      // 年假
    4: 'maternity' as const,   // 产假
    5: 'marriage' as const,    // 婚假
    6: 'funeral' as const,     // 丧假
    7: 'other' as const,       // 其他
  } as Record<number, string>,
  toBackend: {
    sick: 1,
    personal: 2,
    annual: 3,
    maternity: 4,
    marriage: 5,
    funeral: 6,
    other: 7,
  } as Record<string, number>,
}

// ============================================================
// 招聘状态映射
// ============================================================

/** 招聘职位状态映射 */
export const RecruitmentStatusMap = {
  toFrontend: {
    1: 'published' as const,   // 已发布
    2: 'paused' as const,      // 暂停
    3: 'closed' as const,      // 已关闭
  } as Record<number, string>,
  toBackend: {
    published: 1,
    paused: 2,
    closed: 3,
  } as Record<string, number>,
}

/** 简历状态映射 */
export const ResumeStatusMap = {
  toFrontend: {
    1: 'pending' as const,        // 待筛选
    2: 'screening' as const,      // 筛选中
    3: 'interviewing' as const,   // 面试中
    4: 'offered' as const,        // 已发Offer
    5: 'hired' as const,          // 已录用
    6: 'rejected' as const,       // 已拒绝
  },
  toBackend: {
    pending: 1,
    screening: 2,
    interviewing: 3,
    offered: 4,
    hired: 5,
    rejected: 6,
  } as Record<string, number>,
}

// ============================================================
// 健康证状态映射
// ============================================================

/** 健康证状态映射 */
export const HealthCertStatusMap = {
  toFrontend: {
    1: 'valid' as const,      // 有效
    2: 'expiring' as const,   // 即将到期
    3: 'expired' as const,    // 已过期
    4: 'revoked' as const,    // 已撤销
  } as Record<number, string>,
  toBackend: {
    valid: 1,
    expiring: 2,
    expired: 3,
    revoked: 4,
  } as Record<string, number>,
}

/** 审批状态映射 */
export const ApprovalStatusMap = {
  toFrontend: {
    1: 'draft' as const,     // 草稿
    2: 'pending' as const,   // 待审批
    3: 'approved' as const,  // 已批准
    4: 'rejected' as const,  // 已拒绝
  } as Record<number, string>,
  toBackend: {
    draft: 1,
    pending: 2,
    approved: 3,
    rejected: 4,
  } as Record<string, number>,
}

// ============================================================
// Converter 类
// ============================================================

/**
 * 员工数据转换器
 */
export const EmployeeDataConverter = {
  /** 后端数据 → 前端展示 */
  toFrontend<T extends Record<string, unknown>>(backend: T): T {
    return {
      ...backend,
      status: EmployeeStatusMap.toFrontend[backend.status as number] ?? backend.status,
      employmentType: EmploymentTypeMap.toFrontend[backend.employmentType as number] ?? backend.employmentType,
    } as T
  },

  /** 前端表单 → 创建DTO */
  toCreateDTO<T extends Record<string, unknown>>(form: T): T {
    return {
      ...form,
      status: EmployeeStatusMap.toBackend[form.status as string] ?? form.status,
      employmentType: EmploymentTypeMap.toBackend[form.employmentType as string] ?? form.employmentType,
    } as T
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO<T extends Record<string, unknown>>(form: T): T {
    return this.toCreateDTO(form)
  },
}

/**
 * 合同数据转换器
 */
export const ContractDataConverter = {
  /** 后端数据 → 前端展示 */
  toFrontend<T extends Record<string, unknown>>(backend: T): T {
    return {
      ...backend,
      status: ContractStatusMap.toFrontend[backend.status as number] ?? backend.status,
      contractType: ContractTypeMap.toFrontend[backend.contractType as number] ?? backend.contractType,
    } as T
  },

  /** 前端表单 → 创建DTO */
  toCreateDTO<T extends Record<string, unknown>>(form: T): T {
    return {
      ...form,
      status: ContractStatusMap.toBackend[form.status as string] ?? form.status,
      contractType: ContractTypeMap.toBackend[form.contractType as string] ?? form.contractType,
    } as T
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO<T extends Record<string, unknown>>(form: T): T {
    return this.toCreateDTO(form)
  },
}

/**
 * 薪资数据转换器
 * 处理多字段金额分↔元转换
 */
export const SalaryDataConverter = {
  /** 金额字段清单（分↔元） */
  amountFields: [
    'baseSalary', 'performanceBonus', 'overtimePay', 'deductions',
    'socialInsurance', 'housingFund', 'taxAmount', 'netSalary', 'grossSalary',
    'totalAmount', 'paidAmount', 'pendingAmount', 'avgSalary',
  ] as const,

  /** 后端数据 → 前端展示（金额分→元） */
  toFrontend<T extends Record<string, unknown>>(backend: T): T {
    const result: Record<string, unknown> = { ...backend }
    SalaryDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = fenToYuan(result[field] as number)
      }
    })
    return result as T
  },

  /** 前端表单 → 创建DTO（金额元→分） */
  toCreateDTO<T extends Record<string, unknown>>(form: T): T {
    const result: Record<string, unknown> = { ...form }
    SalaryDataConverter.amountFields.forEach(field => {
      if (result[field] !== undefined && result[field] !== null) {
        result[field] = yuanToFen(result[field] as string | number)
      }
    })
    return result as T
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO<T extends Record<string, unknown>>(form: T): T {
    return this.toCreateDTO(form)
  },
}

/**
 * 考勤数据转换器
 */
export const AttendanceDataConverter = {
  /** 后端数据 → 前端展示 */
  toFrontend<T extends Record<string, unknown>>(backend: T): T {
    return {
      ...backend,
      status: AttendanceStatusMap.toFrontend[backend.status as number] ?? backend.status,
      leaveType: backend.leaveType ? LeaveTypeMap.toFrontend[backend.leaveType as number] : backend.leaveType,
    } as T
  },

  /** 前端表单 → 创建DTO */
  toCreateDTO<T extends Record<string, unknown>>(form: T): T {
    return {
      ...form,
      status: AttendanceStatusMap.toBackend[form.status as string] ?? form.status,
      leaveType: form.leaveType ? LeaveTypeMap.toBackend[form.leaveType as string] : form.leaveType,
    } as T
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO<T extends Record<string, unknown>>(form: T): T {
    return this.toCreateDTO(form)
  },
}

/**
 * 招聘数据转换器
 */
export const RecruitmentDataConverter = {
  /** 后端数据 → 前端展示 */
  toFrontend<T extends Record<string, unknown>>(backend: T): T {
    return {
      ...backend,
      status: RecruitmentStatusMap.toFrontend[backend.status as number] ?? backend.status,
    } as T
  },

  /** 前端表单 → 创建DTO */
  toCreateDTO<T extends Record<string, unknown>>(form: T): T {
    return {
      ...form,
      status: RecruitmentStatusMap.toBackend[form.status as string] ?? form.status,
    } as T
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO<T extends Record<string, unknown>>(form: T): T {
    return this.toCreateDTO(form)
  },
}

/**
 * 健康证数据转换器
 */
export const HealthCertificateDataConverter = {
  /** 后端数据 → 前端展示 */
  toFrontend<T extends Record<string, unknown>>(backend: T): T {
    return {
      ...backend,
      status: HealthCertStatusMap.toFrontend[backend.status as number] ?? backend.status,
      approvalStatus: ApprovalStatusMap.toFrontend[backend.approvalStatus as number] ?? backend.approvalStatus,
    } as T
  },

  /** 前端表单 → 创建DTO */
  toCreateDTO<T extends Record<string, unknown>>(form: T): T {
    return {
      ...form,
      status: HealthCertStatusMap.toBackend[form.status as string] ?? form.status,
      approvalStatus: ApprovalStatusMap.toBackend[form.approvalStatus as string] ?? form.approvalStatus,
    } as T
  },

  /** 前端表单 → 更新DTO */
  toUpdateDTO<T extends Record<string, unknown>>(form: T): T {
    return this.toCreateDTO(form)
  },
}
