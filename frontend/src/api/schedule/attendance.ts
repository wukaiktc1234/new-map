/**
 * 考勤同步 API
 * 对应后端: /v1/schedule/attendance
 */
import { get, post } from '@/api/request'
import type { AttendanceDiffReport, SyncStatus } from '@/types/schedule'

/** 同步状态响应 */
interface SyncStatusResponse {
  planId: string
  status: string | number
  lastSyncTime?: string
  errorMessage?: string
}

export const attendanceApi = {
  /**
   * 同步排班数据到考勤系统
   * @param planId - 计划ID
   * @returns 同步结果
   */
  async syncToAttendance(planId: string): Promise<SyncStatusResponse> {
    return post<SyncStatusResponse>(
      `/v1/schedule/attendance/${planId}/sync`
    )
  },

  /**
   * 获取考勤同步状态
   * @param planId - 计划ID
   * @returns 同步状态信息
   */
  async getSyncStatus(planId: string): Promise<{
    status: SyncStatus
    lastSyncTime?: string
    errorMessage?: string
  }> {
    const res = await get<SyncStatusResponse>(
      `/v1/schedule/attendance/${planId}/status`
    )
    if (!res) {
      return { status: 'not_synced' }
    }
    const rawStatus = String(res.status ?? 'not_synced')
    const validStatuses: SyncStatus[] = [
      'not_synced',
      'synced',
      'failed',
    ]
    return {
      status: validStatuses.includes(rawStatus as SyncStatus)
        ? (rawStatus as SyncStatus)
        : 'not_synced',
      lastSyncTime: res.lastSyncTime,
      errorMessage: res.errorMessage,
    }
  },

  /**
   * 获取考勤差异报告
   * @param planId - 计划ID
   * @returns 差异报告
   */
  async getAttendanceDiff(planId: string): Promise<AttendanceDiffReport> {
    return get<AttendanceDiffReport>(
      `/v1/schedule/attendance/${planId}/diff`
    )
  },
}

export default attendanceApi
