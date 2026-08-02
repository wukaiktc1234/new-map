import { get, post } from '@/api/request'
import type {
  KpiSummary,
  ChartData,
  ExportTaskResponse,
  ExportTaskStatus
} from '@/views/operations/reports/types/report'

/**
 * 经营报表API（管理层视角 - 精简版）
 *
 * 核心端点：
 * - GET /v1/operations-reports/{reportType} 统一报表查询
 * - GET /v1/operations-reports/kpi-summary KPI汇总
 * - POST /v1/operations-reports/export 创建导出任务
 */

export const operationsReportApi = {
  /**
   * 统一报表数据查询
   * @param reportType 报表类型: daily/weekly/monthly/quarterly/yearly/profit
   * @param params 查询参数
   * @returns 报表数据（趋势图+门店排名）
   */
  getReportData(reportType: string, params: Record<string, unknown>): Promise<ChartData> {
    // 剔除多余的 reportType 字段（后端通过 @PathVariable 接收，DTO 无该字段）
    const { reportType: _omit, ...rest } = params
    return get<ChartData>(`/v1/operations-reports/${reportType}`, rest)
  },

  /**
   * 获取KPI汇总数据
   * @param params 查询参数
   * @returns KPI汇总数据
   */
  getKpiSummary(params: Record<string, unknown>): Promise<KpiSummary> {
    // 剔除多余的 reportType 字段（后端 DTO 无该字段）
    const { reportType: _omit, ...rest } = params
    return get<KpiSummary>('/v1/operations-reports/kpi-summary', rest)
  },

  /**
   * 创建导出任务
   * 后端签名：createExportTask(@RequestParam Integer taskType, @RequestParam Integer reportType, @RequestBody OperationsReportQueryDTO queryDTO)
   * 注意：taskType 和 reportType 必须作为 URL Query 参数传递，body 仅传 queryDTO 参数
   * @param taskType 任务类型：1-Excel 2-PDF
   * @param reportType 报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析
   * @param params 查询参数（作为 request body 传递）
   * @returns 导出任务信息
   */
  createExportTask(
    taskType: number,
    reportType: number,
    params: Record<string, unknown>
  ): Promise<ExportTaskResponse> {
    // 剔除 body 中多余的 reportType 字段（后端通过 @RequestParam 接收，DTO 无该字段）
    const { reportType: _omit, ...rest } = params
    // taskType 和 reportType 通过 query 参数传递，params 作为 request body
    return post<ExportTaskResponse>(
      '/v1/operations-reports/export',
      rest,
      { params: { taskType, reportType } }
    )
  },

  /**
   * 获取导出任务状态
   * @param taskId 任务ID
   * @returns 任务状态
   */
  getExportTaskStatus(taskId: number): Promise<ExportTaskStatus> {
    return get<ExportTaskStatus>(`/v1/operations-reports/export/${taskId}/status`)
  },

  /**
   * 下载导出文件
   * @param taskId 任务ID
   * @returns Blob（文件二进制数据，用于触发浏览器下载）
   */
  downloadExportFile(taskId: number): Promise<Blob> {
    return get<Blob>(`/v1/operations-reports/export/${taskId}/download`, undefined, {
      responseType: 'blob'
    })
  }
}
