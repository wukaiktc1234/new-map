export type TableStatus = 'idle' | 'dining' | 'reserved' | 'maintenance' | 'disabled'
export type TableType = 'hall' | 'private_room' | 'bar' | 'outdoor'
export type OrderSource = 'cashier' | 'miniapp'

export interface DiningTable {
  tableId: number
  storeId: number
  tableCode: string
  tableName: string
  tableType: TableType
  seatsCount: number
  status: TableStatus
  qrCode?: string
  sortOrder: number
  createTime: string
  updateTime: string
}

export interface DiningTableFormData {
  tableCode: string
  tableName?: string
  tableType?: TableType
  seatsCount: number
  sortOrder?: number
}

export interface DiningTableQueryForm {
  status?: TableStatus | ''
  tableType?: TableType | ''
  keyword?: string
  page: number
  size: number
}

export interface DiningTableStats {
  total: number
  idle: number
  dining: number
  disabled: number
}

export interface TableUsageRecord {
  orderId: number
  orderNumber: string
  tableCode: string
  tableName: string
  orderSource: OrderSource
  diningPeopleCount: number
  finalAmount: string
  orderStatus: string
  createTime: string
  completeTime?: string
  durationMinutes?: number
}

export interface DiningTableBackend {
  tableId: number
  storeId: number
  tableCode: string
  tableName: string
  tableType: number
  seatsCount: number
  status: number
  qrCode?: string
  sortOrder: number
  createTime: string
  updateTime: string
}
