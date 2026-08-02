import { get, isMockMode } from '../request'
import { callNumberQueueDataConverter } from './converters'
import type {
  CallNumberQueue,
  CallNumberQueueBackend,
  CallNumberQueueQueryForm,
  CallNumberQueueStats,
  PageResponse,
} from '@/types/store-operation'

const mockQueueList: CallNumberQueue[] = [
  { queueId: 1, storeId: 1, ticketNumber: 'A001', queueType: 'dine_in', peopleCount: 2, status: 'waiting', calledCount: 0, waitMinutes: 15, createTime: '2026-05-16 12:00:00' },
  { queueId: 2, storeId: 1, ticketNumber: 'A002', queueType: 'dine_in', peopleCount: 4, status: 'called', calledCount: 1, callTime: '2026-05-16 12:10:00', waitMinutes: 10, createTime: '2026-05-16 12:00:00' },
  { queueId: 3, storeId: 1, ticketNumber: 'A003', queueType: 'dine_in', peopleCount: 6, status: 'waiting', calledCount: 0, waitMinutes: 25, createTime: '2026-05-16 11:45:00' },
  { queueId: 4, storeId: 1, ticketNumber: 'B001', queueType: 'takeout', peopleCount: 1, status: 'dined', calledCount: 1, callTime: '2026-05-16 11:30:00', waitMinutes: 8, createTime: '2026-05-16 11:20:00' },
  { queueId: 5, storeId: 1, ticketNumber: 'A004', queueType: 'dine_in', peopleCount: 3, status: 'expired', calledCount: 2, callTime: '2026-05-16 11:00:00', waitMinutes: 20, createTime: '2026-05-16 10:40:00' },
  { queueId: 6, storeId: 1, ticketNumber: 'C001', queueType: 'pickup', peopleCount: 1, status: 'dined', calledCount: 1, callTime: '2026-05-16 11:15:00', waitMinutes: 5, createTime: '2026-05-16 11:10:00' },
  { queueId: 7, storeId: 1, ticketNumber: 'A005', queueType: 'dine_in', peopleCount: 2, status: 'cancelled', calledCount: 0, waitMinutes: 0, createTime: '2026-05-16 10:30:00' },
  { queueId: 8, storeId: 1, ticketNumber: 'A006', queueType: 'dine_in', peopleCount: 4, status: 'waiting', calledCount: 0, waitMinutes: 30, createTime: '2026-05-16 11:30:00' },
  { queueId: 9, storeId: 1, ticketNumber: 'B002', queueType: 'takeout', peopleCount: 1, status: 'called', calledCount: 1, callTime: '2026-05-16 12:05:00', waitMinutes: 5, createTime: '2026-05-16 12:00:00' },
  { queueId: 10, storeId: 1, ticketNumber: 'A007', queueType: 'dine_in', peopleCount: 8, status: 'dined', calledCount: 1, callTime: '2026-05-16 10:00:00', waitMinutes: 12, createTime: '2026-05-16 09:48:00' },
]

const mockQueueStats: CallNumberQueueStats = { waitingCount: 3, dinedCount: 3, avgWaitMinutes: 13, cancelledCount: 1 }

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export const queueApi = {
  async getRecords(params: CallNumberQueueQueryForm): Promise<PageResponse<CallNumberQueue>> {
    if (isMockMode()) {
      await delay(300)
      let filtered = [...mockQueueList]
      if (params.status) filtered = filtered.filter((q) => q.status === params.status)
      if (params.queueType) filtered = filtered.filter((q) => q.queueType === params.queueType)
      if (params.keyword) filtered = filtered.filter((q) => q.ticketNumber.includes(params.keyword!))
      const page = params.page || 1
      const size = params.size || 10
      const start = (page - 1) * size
      return { records: filtered.slice(start, start + size), total: filtered.length, current: page, size, pages: Math.ceil(filtered.length / size) }
    }
    // 构建后端查询参数：status/queueType 需转换为数字编码
    const query: Record<string, unknown> = {
      page: params.page,
      size: params.size,
    }
    if (params.status) query.status = callNumberQueueDataConverter.convertToStatusNum(params.status)
    if (params.queueType) query.queueType = callNumberQueueDataConverter.convertToQueueTypeNum(params.queueType)
    if (params.keyword) query.keyword = params.keyword
    const res = await get<PageResponse<CallNumberQueueBackend>>(
      '/v1/store-management/queue/records',
      query,
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: (res.records || []).map((r) => callNumberQueueDataConverter.toFrontend(r)),
    }
  },

  async getStats(): Promise<CallNumberQueueStats> {
    if (isMockMode()) {
      await delay(200)
      return { ...mockQueueStats }
    }
    return get<CallNumberQueueStats>('/v1/store-management/queue/stats')
  },
}
