import request from './request'
import type { LeaveRecord, LeaveForm, LeaveBalance, LeaveStatus } from '@/types/leave'
import type { PaginationParams } from '@/types/common'

export const leaveApi = {
  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getBalances(): Promise<LeaveBalance[]> {
    return request.get<LeaveBalance[]>('/v1/employee/leave/balances') as any
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  submit(form: LeaveForm): Promise<LeaveRecord> {
    return request.post<LeaveRecord>('/v1/employee/leave', form) as any
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getList(params: PaginationParams & { status?: LeaveStatus }): Promise<{ records: LeaveRecord[]; total: number }> {
    return request.get('/v1/employee/leave/list', params as unknown as Record<string, unknown>) as any
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getById(id: string): Promise<LeaveRecord> {
    return request.get<LeaveRecord>(`/v1/employee/leave/${id}`) as any
  },

  withdraw(id: string): Promise<void> {
    return request.post(`/v1/employee/leave/${id}/withdraw`)
  },

  cancel(id: string): Promise<void> {
    return request.post(`/v1/employee/leave/${id}/cancel`)
  },
}
