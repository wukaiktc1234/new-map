import { get, post, put, isMockMode } from '../request'
import { diningTableDataConverter } from './converters'
import type {
  DiningTable,
  DiningTableBackend,
  DiningTableFormData,
  DiningTableQueryForm,
  DiningTableStats,
  TableUsageRecord,
  PageResponse,
} from '@/types/store-operation'

const mockTableList: DiningTable[] = [
  { tableId: 1, storeId: 1, tableCode: 'A01', tableName: '大厅A01桌', tableType: 'hall', seatsCount: 4, status: 'idle', sortOrder: 1, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 09:00:00' },
  { tableId: 2, storeId: 1, tableCode: 'A02', tableName: '大厅A02桌', tableType: 'hall', seatsCount: 4, status: 'dining', sortOrder: 2, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 12:30:00' },
  { tableId: 3, storeId: 1, tableCode: 'A03', tableName: '大厅A03桌', tableType: 'hall', seatsCount: 6, status: 'idle', sortOrder: 3, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 09:00:00' },
  { tableId: 4, storeId: 1, tableCode: 'A04', tableName: '大厅A04桌', tableType: 'hall', seatsCount: 2, status: 'reserved', sortOrder: 4, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 10:00:00' },
  { tableId: 5, storeId: 1, tableCode: 'B01', tableName: '包厢B01', tableType: 'private_room', seatsCount: 8, status: 'dining', sortOrder: 5, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 11:00:00' },
  { tableId: 6, storeId: 1, tableCode: 'B02', tableName: '包厢B02', tableType: 'private_room', seatsCount: 10, status: 'idle', sortOrder: 6, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 09:00:00' },
  { tableId: 7, storeId: 1, tableCode: 'C01', tableName: '吧台C01', tableType: 'bar', seatsCount: 2, status: 'idle', sortOrder: 7, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 09:00:00' },
  { tableId: 8, storeId: 1, tableCode: 'D01', tableName: '露台D01', tableType: 'outdoor', seatsCount: 4, status: 'maintenance', sortOrder: 8, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 08:00:00' },
  { tableId: 9, storeId: 1, tableCode: 'D02', tableName: '露台D02', tableType: 'outdoor', seatsCount: 4, status: 'disabled', sortOrder: 9, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-15 18:00:00' },
  { tableId: 10, storeId: 1, tableCode: 'A05', tableName: '大厅A05桌', tableType: 'hall', seatsCount: 4, status: 'idle', sortOrder: 10, createTime: '2026-05-16 09:00:00', updateTime: '2026-05-16 09:00:00' },
]

const mockTableStats: DiningTableStats = { total: 10, idle: 5, dining: 2, disabled: 1 }

const mockUsageRecords: TableUsageRecord[] = [
  { orderId: 1001, orderNumber: 'ORD20260516001', tableCode: 'A01', tableName: '大厅A01桌', orderSource: 'miniapp', diningPeopleCount: 3, finalAmount: '128.50', orderStatus: 'completed', createTime: '2026-05-16 11:30:00', completeTime: '2026-05-16 12:45:00', durationMinutes: 75 },
  { orderId: 1002, orderNumber: 'ORD20260516002', tableCode: 'A02', tableName: '大厅A02桌', orderSource: 'cashier', diningPeopleCount: 4, finalAmount: '256.00', orderStatus: 'in_progress', createTime: '2026-05-16 12:00:00' },
  { orderId: 1003, orderNumber: 'ORD20260516003', tableCode: 'B01', tableName: '包厢B01', orderSource: 'cashier', diningPeopleCount: 8, finalAmount: '688.00', orderStatus: 'in_progress', createTime: '2026-05-16 11:00:00' },
  { orderId: 1004, orderNumber: 'ORD20260516004', tableCode: 'A03', tableName: '大厅A03桌', orderSource: 'miniapp', diningPeopleCount: 2, finalAmount: '68.00', orderStatus: 'completed', createTime: '2026-05-16 10:00:00', completeTime: '2026-05-16 10:50:00', durationMinutes: 50 },
  { orderId: 1005, orderNumber: 'ORD20260516005', tableCode: 'A01', tableName: '大厅A01桌', orderSource: 'miniapp', diningPeopleCount: 2, finalAmount: '96.00', orderStatus: 'completed', createTime: '2026-05-16 09:00:00', completeTime: '2026-05-16 09:40:00', durationMinutes: 40 },
]

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export const tableApi = {
  async getList(params: DiningTableQueryForm): Promise<PageResponse<DiningTable>> {
    if (isMockMode()) {
      await delay(300)
      let filtered = [...mockTableList]
      if (params.status) filtered = filtered.filter((t) => t.status === params.status)
      if (params.tableType) filtered = filtered.filter((t) => t.tableType === params.tableType)
      if (params.keyword) filtered = filtered.filter((t) => t.tableCode.includes(params.keyword!) || t.tableName?.includes(params.keyword!))
      const page = params.page || 1
      const size = params.size || 10
      const start = (page - 1) * size
      return { records: filtered.slice(start, start + size), total: filtered.length, current: page, size, pages: Math.ceil(filtered.length / size) }
    }
    // 构建后端查询参数：status/tableType 需转换为数字编码
    const query: Record<string, unknown> = {
      page: params.page,
      size: params.size,
    }
    if (params.status) query.status = diningTableDataConverter.convertToStatusNum(params.status)
    if (params.tableType) query.tableType = diningTableDataConverter.convertToTableTypeNum(params.tableType)
    if (params.keyword) query.keyword = params.keyword
    const res = await get<PageResponse<DiningTableBackend>>(
      '/v1/store-management/tables',
      query,
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: (res.records || []).map((r: DiningTableBackend) => diningTableDataConverter.toFrontend(r)),
    }
  },

  async getById(tableId: number): Promise<DiningTable> {
    if (isMockMode()) {
      await delay(200)
      return mockTableList.find((t) => t.tableId === tableId) ?? mockTableList[0]
    }
    const res = await get<DiningTableBackend>(`/v1/store-management/tables/${tableId}`)
    return diningTableDataConverter.toFrontend(res)
  },

  async create(data: DiningTableFormData): Promise<DiningTable> {
    if (isMockMode()) {
      await delay(300)
      const newTable: DiningTable = {
        tableId: Date.now(), storeId: 1, tableCode: data.tableCode, tableName: data.tableName ?? '',
        tableType: data.tableType ?? 'hall', seatsCount: data.seatsCount, status: 'idle',
        sortOrder: data.sortOrder ?? 0, createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      }
      mockTableList.push(newTable)
      return newTable
    }
    const dto = diningTableDataConverter.toBackend(data)
    const res = await post<DiningTableBackend>('/v1/store-management/tables', dto)
    return diningTableDataConverter.toFrontend(res)
  },

  async update(tableId: number, data: Partial<DiningTableFormData>): Promise<DiningTable> {
    if (isMockMode()) {
      await delay(300)
      const idx = mockTableList.findIndex((t) => t.tableId === tableId)
      if (idx >= 0) {
        if (data.tableName) mockTableList[idx].tableName = data.tableName
        if (data.seatsCount) mockTableList[idx].seatsCount = data.seatsCount
        if (data.tableType) mockTableList[idx].tableType = data.tableType
        mockTableList[idx].updateTime = new Date().toISOString().replace('T', ' ').slice(0, 19)
        return mockTableList[idx]
      }
      return mockTableList[0]
    }
    const dto = diningTableDataConverter.toBackend(data)
    const res = await put<DiningTableBackend>(`/v1/store-management/tables/${tableId}`, dto)
    return diningTableDataConverter.toFrontend(res)
  },

  async updateStatus(tableId: number, status: number): Promise<void> {
    if (isMockMode()) {
      await delay(200)
      const idx = mockTableList.findIndex((t) => t.tableId === tableId)
      if (idx >= 0) {
        const statusMap: Record<number, DiningTable['status']> = { 1: 'idle', 4: 'maintenance', 5: 'disabled' }
        mockTableList[idx].status = statusMap[status] ?? 'idle'
      }
      return
    }
    await put(`/v1/store-management/tables/${tableId}/status`, { status })
  },

  async getStats(): Promise<DiningTableStats> {
    if (isMockMode()) {
      await delay(200)
      return { ...mockTableStats }
    }
    return get<DiningTableStats>('/v1/store-management/tables/stats')
  },

  async getUsageRecords(params: Record<string, unknown>): Promise<PageResponse<TableUsageRecord>> {
    if (isMockMode()) {
      await delay(300)
      let filtered = [...mockUsageRecords]
      if (params.source) filtered = filtered.filter((r) => r.orderSource === params.source)
      if (params.tableCode) filtered = filtered.filter((r) => r.tableCode.includes(params.tableCode as string))
      const page = (params.page as number) || 1
      const size = (params.size as number) || 10
      return { records: filtered, total: filtered.length, current: page, size, pages: 1 }
    }
    return get<PageResponse<TableUsageRecord>>('/v1/store-management/tables/usage-records', params)
  },
}
