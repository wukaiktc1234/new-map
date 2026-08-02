/**
 * 日历视图 API
 * 对应后端: /v1/schedule/calendar
 */
import { get } from '@/api/request'
import type { TimelineResponse } from '@/types/schedule'

export const calendarApi = {
  /**
   * 获取周视图时间线数据
   * @param weekStart - 周起始日期 (YYYY-MM-DD)
   * @returns 时间线响应数据
   */
  async getTimeline(weekStart: string): Promise<TimelineResponse> {
    return get<TimelineResponse>(
      '/v1/schedule/calendar/timeline',
      { weekStart }
    )
  },

  /**
   * 获取日历数据（列表查询）
   * 对应后端 GET /v1/schedule/calendar
   * @param planId - 方案ID
   * @param weekStart - 周起始日期 (YYYY-MM-DD)
   * @returns 时间线响应数据
   */
  async getEntries(
    planId: string,
    weekStart: string
  ): Promise<TimelineResponse> {
    return get<TimelineResponse>(
      '/v1/schedule/calendar',
      { planId, weekStart }
    )
  },
}

export default calendarApi
