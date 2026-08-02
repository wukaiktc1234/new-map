/**
 * 托盘管理 API
 * 对应后端: /v1/tray (TrayController)
 *
 * 【端权责分离】（见 docs/design/terminal-responsibility-spec.md）
 * - POS端：bindOrder（idle → bound）
 * - 后厨端：scanKitchenIn（bound → making）、scanKitchenOut（making → ready）
 * - 取餐口：scanServe（ready → served → idle）
 * - 管理端：仅查询（list/idle/stats/{trayCode}）
 */
import { get, post } from './request'

/** 托盘接口基础路径 */
const TRAY_BASE = '/v1/tray'

/** 托盘状态枚举（5状态机 + 2扩展状态） */
export type TrayStatus =
  | 'idle'       // 空闲
  | 'bound'      // POS已绑定
  | 'making'     // 后厨制作中
  | 'ready'      // 后厨已出餐
  | 'served'     // 已取餐
  | 'cleaning'   // 清洁中
  | 'damaged'    // 损坏

/** 托盘实体（与后端 Tray.java 对应） */
export interface Tray {
  id?: number
  trayCode?: string
  trayName?: string
  trayType?: string
  status?: TrayStatus
  currentOrderId?: string
  currentKitchenOrderId?: number
  bindTime?: string
  lastUseTime?: string
  useCount?: number
  storeId?: number
  storeName?: string
  remark?: string
  lastStateChangeTime?: string
  lastScanDeviceId?: number
  lastScanTime?: string
  cameraSnapshotUrl?: string
  cameraSnapshotTime?: string
  yoloVerified?: number
  yoloVerifyTime?: string
  createTime?: string
  updateTime?: string
  createBy?: string
  updateBy?: string
}

/** 托盘统计（5状态机计数） */
export interface TrayStats {
  idle: number
  bound: number
  making: number
  ready: number
  served: number
  cleaning: number
  damaged: number
  total: number
}

/** 托盘绑定订单请求 */
export interface TrayBindOrderRequest {
  trayCode: string
  orderId: string
  kitchenOrderId?: number
}

/** 扫码操作可选参数 */
export interface ScanOptions {
  scanDeviceId?: number
  scanDeviceCode?: string
  operatorId?: string
  operatorName?: string
}

const trayApi = {
  /**
   * 获取所有托盘列表（管理端）
   * GET /v1/tray/list
   */
  async getAll(): Promise<Tray[]> {
    return get<Tray[]>(`${TRAY_BASE}/list`)
  },

  /**
   * 按订单ID查询托盘（管理端订单详情使用）
   * 后端无专用接口，前端在 getAll() 基础上按 currentOrderId 过滤
   * @param orderId - 订单ID
   */
  async getByOrderId(orderId: string): Promise<Tray[]> {
    const all = await this.getAll()
    return all.filter((t) => t.currentOrderId === orderId)
  },

  /**
   * 获取空闲托盘列表（POS端选择托盘用）
   * GET /v1/tray/idle
   */
  async getIdle(limit = 10): Promise<Tray[]> {
    return get<Tray[]>(`${TRAY_BASE}/idle`, { limit })
  },

  /**
   * 获取托盘统计（管理端大屏）
   * GET /v1/tray/stats
   */
  async getStats(): Promise<TrayStats> {
    return get<TrayStats>(`${TRAY_BASE}/stats`)
  },

  /**
   * 根据托盘码查询托盘
   * GET /v1/tray/{trayCode}
   */
  async getByTrayCode(trayCode: string): Promise<Tray> {
    return get<Tray>(`${TRAY_BASE}/${trayCode}`)
  },

  /**
   * 创建托盘（管理端）
   * POST /v1/tray/create
   */
  async create(tray: Partial<Tray>): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/create`, tray)
  },

  /**
   * 批量创建托盘（管理端）
   * POST /v1/tray/batch-create
   */
  async batchCreate(params: {
    count: number
    prefix?: string
    trayType?: string
    storeId?: number
    storeName?: string
  }): Promise<string> {
    return post<string>(`${TRAY_BASE}/batch-create`, undefined, { params })
  },

  /**
   * 扫描托盘码绑定订单（POS端：idle → bound）
   * POST /v1/tray/bind-order
   * @param request - 绑定请求（trayCode + orderId）
   */
  async bindOrder(request: TrayBindOrderRequest): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/bind-order`, request)
  },

  /**
   * 后厨一次扫码（后厨端：bound → making，防抖≥3s）
   * POST /v1/tray/scan-kitchen-in
   */
  async scanKitchenIn(trayCode: string, options?: ScanOptions): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/scan-kitchen-in`, undefined, {
      params: { trayCode, ...options },
    })
  },

  /**
   * 后厨二次扫码（后厨端：making → ready，防抖≥5s，联动摄像头+YOLO）
   * POST /v1/tray/scan-kitchen-out
   */
  async scanKitchenOut(trayCode: string, options?: ScanOptions): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/scan-kitchen-out`, undefined, {
      params: { trayCode, ...options },
    })
  },

  /**
   * 取餐口确认出餐（取餐口：ready → served → idle，防抖≥2s）
   * POST /v1/tray/scan-serve
   */
  async scanServe(trayCode: string, options?: ScanOptions): Promise<unknown> {
    return post<unknown>(`${TRAY_BASE}/scan-serve`, undefined, {
      params: { trayCode, ...options },
    })
  },

  /**
   * 释放托盘（管理端）
   * POST /v1/tray/release
   */
  async release(trayCode: string): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/release`, undefined, { params: { trayCode } })
  },

  /**
   * 开始清洁托盘（管理端）
   * POST /v1/tray/start-cleaning
   */
  async startCleaning(trayCode: string): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/start-cleaning`, undefined, { params: { trayCode } })
  },

  /**
   * 完成清洁托盘（管理端）
   * POST /v1/tray/finish-cleaning
   */
  async finishCleaning(trayCode: string): Promise<Tray> {
    return post<Tray>(`${TRAY_BASE}/finish-cleaning`, undefined, { params: { trayCode } })
  },
}

export default trayApi
