export type QueueStatus = 'waiting' | 'called' | 'expired' | 'dined' | 'cancelled'
export type QueueType = 'dine_in' | 'takeout' | 'pickup'

export interface CallNumberQueue {
  queueId: number
  storeId: number
  ticketNumber: string
  queueType: QueueType
  peopleCount: number
  tablePreference?: string
  status: QueueStatus
  callTime?: string
  calledCount: number
  waitMinutes?: number
  createTime: string
}

export interface CallNumberQueueQueryForm {
  status?: QueueStatus | ''
  queueType?: QueueType | ''
  keyword?: string
  dateRange?: [string, string] | null
  page: number
  size: number
}

export interface CallNumberQueueStats {
  waitingCount: number
  dinedCount: number
  avgWaitMinutes: number
  cancelledCount: number
}

export interface CallNumberQueueBackend {
  queueId: number
  storeId: number
  ticketNumber: string
  queueType: number
  peopleCount: number
  tablePreference?: string
  status: number
  callTime?: string
  calledCount: number
  createTime: string
  updateTime: string
}
