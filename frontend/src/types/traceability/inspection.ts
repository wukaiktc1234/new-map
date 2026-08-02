/**
 * 检验记录类型定义
 *
 * 对齐后端 InspectionController (/v1/inspections) 的 InspectionVO。
 * 状态/类型字段使用大写枚举字符串，与后端保持一致。
 */

/** 检验结果（对齐后端枚举） */
export type InspectionResult = 'QUALIFIED' | 'UNQUALIFIED' | 'CONDITIONAL'

/** 检验类型（对齐后端枚举） */
export type InspectionType = 'INCOMING' | 'PROCESS' | 'FINAL'

/** 检验项目明细（用于 inspectionItems JSON 字段解析） */
export interface InspectionItem {
  /** 项目名称 */
  itemName: string
  /** 标准值 */
  standard?: string
  /** 实测值 */
  value?: string
  /** 单位 */
  unit?: string
  /** 单项结果 */
  result: 'QUALIFIED' | 'UNQUALIFIED'
}

/** 检验记录（对齐后端 InspectionVO，camelCase） */
export interface InspectionRecord {
  /** 检验记录ID */
  inspectionId: number
  /** 检验编号 */
  inspectionNo: string
  /** 追溯码ID */
  traceCodeId: number
  /** 追溯码 */
  traceCode: string
  /** 批次号 */
  batchNo: string
  /** 供应商ID */
  supplierId: number
  /** 供应商名称 */
  supplierName: string
  /** 物料ID */
  materialId: string
  /** 物料名称 */
  materialName: string
  /** 检验类型: INCOMING/PROCESS/FINAL */
  inspectionType: InspectionType
  /** 检验类型中文名 */
  inspectionTypeName: string
  /** 检验结果: QUALIFIED/UNQUALIFIED/CONDITIONAL */
  inspectionResult: InspectionResult
  /** 检验结果中文名 */
  inspectionResultName: string
  /** 检验项目 */
  inspectionItem: string
  /** 检验值 */
  inspectionValue: string
  /** 标准值 */
  standardValue: string
  /** 检验单位 */
  inspectionUnit: string
  /** 检验员ID */
  inspectorId: number
  /** 检验员姓名 */
  inspectorName: string
  /** 检验时间（ISO 8601） */
  inspectionTime: string
  /** 检验地点 */
  inspectionLocation: string
  /** 检验报告URL */
  reportUrl: string
  /** 检验项明细JSON字符串 */
  inspectionItems: string
  /** 备注 */
  remark: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 检验记录创建表单 */
export interface InspectionFormData {
  /** 检验记录ID（编辑模式必填，对应后端 InspectionUpdateDTO.inspectionId） */
  inspectionId?: number
  /** 追溯码ID */
  traceCodeId: number
  /** 追溯码 */
  traceCode?: string
  /** 批次号 */
  batchNo?: string
  /** 供应商ID */
  supplierId?: number
  /** 供应商名称 */
  supplierName?: string
  /** 物料ID */
  materialId?: string
  /** 物料名称 */
  materialName?: string
  /** 检验类型 */
  inspectionType: InspectionType
  /** 检验结果 */
  inspectionResult: InspectionResult
  /** 检验项目 */
  inspectionItem?: string
  /** 检验值 */
  inspectionValue?: string
  /** 标准值 */
  standardValue?: string
  /** 检验单位 */
  inspectionUnit?: string
  /** 检验员ID */
  inspectorId?: number
  /** 检验员姓名 */
  inspectorName?: string
  /** 检验时间 */
  inspectionTime?: string
  /** 检验地点 */
  inspectionLocation?: string
  /** 检验报告URL */
  reportUrl?: string
  /** 检验项明细JSON字符串 */
  inspectionItems?: string
  /** 备注 */
  remark?: string
}

/** 检验记录查询参数 */
export interface InspectionQuery {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 批次号 */
  batchNo?: string
  /** 追溯码 */
  traceCode?: string
  /** 供应商ID */
  supplierId?: number
  /** 物料名称 */
  materialName?: string
  /** 检验类型 */
  inspectionType?: InspectionType
  /** 检验结果 */
  inspectionResult?: InspectionResult
  /** 关键字 */
  keyword?: string
  /** 检验时间起始 */
  inspectionTimeStart?: string
  /** 检验时间结束 */
  inspectionTimeEnd?: string
}

/** 检验统计（对齐后端 InspectionStatisticsVO） */
export interface InspectionStatistics {
  /** 检验总数 */
  total: number
  /** 合格数 */
  qualifiedCount: number
  /** 不合格数 */
  unqualifiedCount: number
  /** 有条件合格数 */
  conditionalCount: number
  /** 合格率（百分比） */
  qualificationRate: number
}

/**
 * 质量统计（兼容旧版，建议使用 InspectionStatistics）
 * @deprecated 请使用 InspectionStatistics
 */
export type QualityStatistics = InspectionStatistics

/** 检验结果映射 */
export const InspectionResultMap: Record<InspectionResult, { label: string; type: string }> = {
  QUALIFIED: { label: '合格', type: 'success' },
  UNQUALIFIED: { label: '不合格', type: 'danger' },
  CONDITIONAL: { label: '有条件合格', type: 'warning' },
}

/** 检验类型映射 */
export const InspectionTypeMap: Record<InspectionType, { label: string; type: string }> = {
  INCOMING: { label: '进货检验', type: 'primary' },
  PROCESS: { label: '过程检验', type: 'warning' },
  FINAL: { label: '成品检验', type: 'success' },
}
