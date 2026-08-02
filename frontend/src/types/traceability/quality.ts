/**
 * 质量管理类型定义
 *
 * 对齐后端 QualityController (/v1/quality) 的 QualityRecordVO 和 QualityStandardVO。
 * 异常等级、处理状态、标准类型使用大写枚举字符串，与后端保持一致。
 */

/** 异常等级（对齐后端枚举） */
export type AbnormalLevel = 'NORMAL' | 'WARNING' | 'CRITICAL'

/** 质量处理状态（对齐后端枚举） */
export type QualityHandlingStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

/** 标准类型（对齐后端枚举） */
export type StandardType = 'NATIONAL' | 'INDUSTRY' | 'ENTERPRISE' | 'LOCAL'

/** 标准状态（对齐后端枚举） */
export type StandardStatus = 'ACTIVE' | 'INACTIVE'

/** 质量记录（对齐后端 QualityRecordVO） */
export interface QualityRecord {
  /** 质量记录ID */
  qualityRecordId: number
  /** 记录编号 */
  recordNo: string
  /** 追溯码ID */
  traceCodeId?: number
  /** 追溯码 */
  traceCode?: string
  /** 批次号 */
  batchNo?: string
  /** 质量标准ID */
  standardId?: number
  /** 质量标准名称 */
  standardName?: string
  /** 物料ID */
  materialId?: string
  /** 物料名称 */
  materialName: string
  /** 检验数据JSON字符串 */
  inspectionData: string
  /** 异常等级: NORMAL/WARNING/CRITICAL */
  abnormalLevel: AbnormalLevel
  /** 异常等级中文名 */
  abnormalLevelName: string
  /** 处理状态: PENDING/PROCESSING/RESOLVED/CLOSED */
  handlingStatus: QualityHandlingStatus
  /** 处理状态中文名 */
  handlingStatusName: string
  /** 处理结果 */
  handlingResult?: string
  /** 处理人ID */
  handledById?: number
  /** 处理人姓名 */
  handledByName?: string
  /** 处理时间 */
  handledTime?: string
  /** 根本原因 */
  rootCause?: string
  /** 受影响追溯码JSON字符串 */
  affectedTraceCodes?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 质量标准（对齐后端 QualityStandardVO） */
export interface QualityStandard {
  /** 标准ID */
  standardId: number
  /** 标准编号 */
  standardNo: string
  /** 标准名称 */
  standardName: string
  /** 标准类型: NATIONAL/INDUSTRY/ENTERPRISE/LOCAL */
  standardType: StandardType
  /** 标准类型中文名 */
  standardTypeName: string
  /** 分类 */
  category?: string
  /** 目标物料ID */
  targetMaterialId?: string
  /** 目标物料名称 */
  targetMaterialName?: string
  /** 指标项JSON字符串 */
  indicatorItems: string
  /** 状态: ACTIVE/INACTIVE */
  status: StandardStatus
  /** 状态中文名 */
  statusName: string
  /** 生效日期 */
  effectiveDate?: string
  /** 失效日期 */
  expiryDate?: string
  /** 发布机构 */
  issuingAuthority?: string
  /** 版本 */
  version?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 质量记录创建表单 */
export interface QualityRecordFormData {
  /** 追溯码ID */
  traceCodeId?: number
  /** 追溯码 */
  traceCode?: string
  /** 批次号 */
  batchNo?: string
  /** 质量标准ID */
  standardId?: number
  /** 物料ID */
  materialId?: string
  /** 物料名称 */
  materialName: string
  /** 检验数据JSON字符串 */
  inspectionData: string
  /** 异常等级 */
  abnormalLevel: AbnormalLevel
  /** 备注 */
  remark?: string
}

/** 质量标准创建表单 */
export interface QualityStandardFormData {
  /** 标准编号 */
  standardNo?: string
  /** 标准名称 */
  standardName: string
  /** 标准类型 */
  standardType: StandardType
  /** 分类 */
  category?: string
  /** 目标物料ID */
  targetMaterialId?: string
  /** 目标物料名称 */
  targetMaterialName?: string
  /** 指标项JSON字符串 */
  indicatorItems: string
  /** 状态 */
  status?: StandardStatus
  /** 生效日期 */
  effectiveDate?: string
  /** 失效日期 */
  expiryDate?: string
  /** 发布机构 */
  issuingAuthority?: string
  /** 版本 */
  version?: string
  /** 备注 */
  remark?: string
}

/** 质量查询参数 */
export interface QualityQuery {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 标准名称 */
  standardName?: string
  /** 标准类型 */
  standardType?: StandardType
  /** 分类 */
  category?: string
  /** 目标物料名称 */
  targetMaterialName?: string
  /** 状态 */
  status?: StandardStatus
}

/** 异常等级映射 */
export const AbnormalLevelMap: Record<AbnormalLevel, { label: string; status: string }> = {
  NORMAL: { label: '正常', status: 'success' },
  WARNING: { label: '警告', status: 'warning' },
  CRITICAL: { label: '严重', status: 'error' },
}

/** 质量处理状态映射 */
export const QualityHandlingStatusMap: Record<QualityHandlingStatus, { label: string; status: string }> = {
  PENDING: { label: '待处理', status: 'warning' },
  PROCESSING: { label: '处理中', status: 'primary' },
  RESOLVED: { label: '已解决', status: 'success' },
  CLOSED: { label: '已关闭', status: 'info' },
}

/** 标准类型映射 */
export const StandardTypeMap: Record<StandardType, { label: string; status: string }> = {
  NATIONAL: { label: '国标', status: 'error' },
  INDUSTRY: { label: '行标', status: 'warning' },
  ENTERPRISE: { label: '企标', status: 'primary' },
  LOCAL: { label: '地方标准', status: 'info' },
}
