/**
 * 排班数据导出 API
 * 对应后端: /v1/schedule/export
 */
import { post } from '@/api/request'

/** 导出格式类型 */
type ExportFormat = 'xlsx' | 'csv' | 'pdf'

/** 排班计划导出参数 */
interface ScheduleExportParams {
  /** 计划ID */
  planId: string
  /** 导出格式 */
  format?: ExportFormat
  /** 是否包含员工联系方式 */
  includeContact?: boolean
  /** 是否包含工时统计 */
  includeHoursSummary?: boolean
}

/** 换班记录导出参数 */
interface SwapExportParams {
  /** 计划ID（可选） */
  planId?: string
  /** 状态筛选 */
  status?: string
  /** 时间范围 - 开始 */
  startDateStart?: string
  /** 时间范围 - 结束 */
  endDateEnd?: string
  /** 导出格式 */
  format?: ExportFormat
}

export const exportApi = {
  /**
   * 导出排班计划
   * @param params - 导出参数
   * @returns 文件Blob数据
   */
  async exportSchedule(params: ScheduleExportParams): Promise<Blob> {
    return post<Blob>(
      '/v1/schedule/export/schedule',
      params,
      { responseType: 'blob' }
    )
  },

  /**
   * 导出换班记录
   * @param params - 导出参数
   * @returns 文件Blob数据
   */
  async exportSwapRecords(params: SwapExportParams): Promise<Blob> {
    return post<Blob>(
      '/v1/schedule/export/swap',
      undefined,
      { params, responseType: 'blob' }
    )
  },
}

export default exportApi
