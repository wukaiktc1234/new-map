import request from './request'
import { mockDelay, mockLightDelay } from './mock/delays'

/** 考勤状态枚举 */
export type AttendanceStatus =
  | 'normal'       // 正常出勤
  | 'late'         // 迟到
  | 'early_leave'  // 早退
  | 'absent'       // 缺勤/旷工
  | 'not_clocked_in' // 未打卡
  | 'overtime'     // 加班中

/** 打卡类型 */
export type PunchType = 'in' | 'out'

/** WiFi网络信息（浏览器隐私限制下通过连接类型间接判断） */
export interface WiFiInfo {
  /** 网络连接类型：wifi / cellular / ethernet / unknown / offline */
  connectionType: string
  /** 是否已连接网络 */
  isConnected: boolean
  /** 有效网络类型（用于判断是否在门店内网环境） */
  effectiveType?: string
}

/** 打卡约束模式 */
export type PunchConstraintMode = 'location_only' | 'wifi_only' | 'both' | 'none'

/** 设备信息（防作弊基础数据） */
export interface DeviceInfo {
  userAgent: string
  platform: string
  screenWidth: number
  screenHeight: number
  devicePixelRatio: number
  timezone: string
  timezoneOffset: number
  language: string
}

/** 地理位置（用于考勤定位） */
export interface GeoLocation {
  latitude: number
  longitude: number
  accuracy?: number
}

/** 打卡结果 */
export interface PunchResult {
  success: boolean
  punchTime: string    // HH:mm:ss 格式
  punchType: PunchType
  message: string
  /** 与门店的距离（米） */
  distanceToStore?: number
  /** 是否在围栏内 */
  withinFence: boolean
  /** 约束检查失败原因（success=false 时填充） */
  constraintReason?: string
  /** 定位约束是否满足 */
  locationSatisfied?: boolean
  /** 网络约束是否满足 */
  networkSatisfied?: boolean
}

/** 打卡约束配置（后续从管理端API获取） */
export interface PunchConfig {
  /** 约束模式 */
  constraintMode: PunchConstraintMode
  /** 围栏半径（米） */
  fenceRadiusMeters: number
  /** 允许的网络连接类型列表（空表示不限制） */
  allowedConnectionTypes?: string[]
  /** 门店位置 */
  storeLocation: GeoLocation
}

/** 今日考勤数据 */
export interface TodayAttendance {
  /** 考勤状态 */
  status: AttendanceStatus
  /** 上班打卡时间（HH:mm） */
  clockInTime: string | null
  /** 下班打卡时间（HH:mm） */
  clockOutTime: string | null
  /** 当前已工作时长（小时） */
  currentHours: number
  /** 应出勤工时（小时） */
  expectedHours: number
  /** 班次名称 */
  shiftName: string
  /** 异常类型描述 */
  exceptionType?: string
  /** 是否可执行上班打卡 */
  canPunchIn: boolean
  /** 是否可执行下班打卡 */
  canPunchOut: boolean
}

// ==================== Mock 门店坐标 ====================

/** 模拟门店位置（北京市朝阳区某餐饮门店） */
const STORE_LOCATION: GeoLocation = {
  latitude: 39.9042,
  longitude: 116.4074,
}

/** 围栏半径：500米 */
const FENCE_RADIUS_METERS = 500

/**
 * 检测当前网络连接类型
 *
 * 浏览器隐私限制下无法直接获取WiFi SSID/BSSID，
 * 通过 Network Information API 间接判断连接类型。
 *
 * @returns WiFiInfo 网络信息对象
 */
async function detectNetworkType(): Promise<WiFiInfo> {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const conn: any =
    (navigator as any).connection ||
    (navigator as any).mozConnection ||
    (navigator as any).webkitConnection

  if (!conn) {
    return {
      connectionType: navigator.onLine ? 'unknown' : 'offline',
      isConnected: navigator.onLine,
      effectiveType: undefined,
    }
  }

  // Network Information API 可用：获取连接类型和有效类型
  const type = conn.type || 'unknown'
  const effectiveType = conn.effectiveType || undefined

  return {
    connectionType: type,
    isConnected: navigator.onLine && type !== 'none',
    effectiveType,
  }
}

/**
 * 采集设备信息（防作弊基础数据）
 *
 * 收集客户端环境信息供服务端校验，用于检测异常打卡行为。
 *
 * @returns DeviceInfo 设备信息对象
 */
function collectDeviceInfo(): DeviceInfo {
  return {
    userAgent: navigator.userAgent,
    platform: navigator.platform,
    screenWidth: screen.width,
    screenHeight: screen.height,
    devicePixelRatio: window.devicePixelRatio || 1,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    timezoneOffset: new Date().getTimezoneOffset(),
    language: navigator.language,
  }
}

/**
 * 验证约束条件是否满足
 *
 * 根据约束模式检查定位和网络是否满足要求。
 *
 * @param mode - 约束模式
 * @param locationResult - 定位结果 { success, distance, withinFence }
 * @param networkInfo - 网络信息
 * @param config - 打卡配置
 * @returns 约束检查结果 { satisfied, reason, locationSatisfied, networkSatisfied }
 */
function validateConstraints(
  mode: PunchConstraintMode,
  locationResult: { success: boolean; withinFence: boolean; distance?: number },
  networkInfo: WiFiInfo,
  config: PunchConfig,
): { satisfied: boolean; reason: string; locationSatisfied: boolean; networkSatisfied: boolean } {
  let locationSatisfied = true
  let networkSatisfied = true
  const reasons: string[] = []

  // 定位约束检查
  if (mode === 'location_only' || mode === 'both') {
    if (!locationResult.success) {
      locationSatisfied = false
      reasons.push('请开启定位服务后再打卡')
    } else if (!locationResult.withinFence) {
      locationSatisfied = false
      reasons.push(`当前位置距门店 ${locationResult.distance ?? '?'}m，超出允许范围`)
    }
  }

  // 网络约束检查
  if (mode === 'wifi_only' || mode === 'both') {
    if (!networkInfo.isConnected) {
      networkSatisfied = false
      reasons.push('请连接网络后再打卡')
    } else if (
      config.allowedConnectionTypes &&
      config.allowedConnectionTypes.length > 0 &&
      !config.allowedConnectionTypes.includes(networkInfo.connectionType)
    ) {
      networkSatisfied = false
      const allowedNames = config.allowedConnectionTypes.join(' / ')
      reasons.push(`请使用${allowedNames}网络打卡（当前：${networkInfo.connectionType}）`)
    }
  }

  const satisfied = locationSatisfied && networkSatisfied
  return {
    satisfied,
    reason: reasons.length > 0 ? reasons.join('；') : '',
    locationSatisfied,
    networkSatisfied,
  }
}

/**
 * 计算两点间距离（Haversine 公式，单位：米）
 */
function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000 // 地球半径（米）
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

/**
 * 获取今日考勤状态
 *
 * 后端 B-13 就绪后替换为: GET /v1/attendance/today
 *
 * Mock 阶段行为：
 * - 根据当前时间动态生成合理的考勤数据
 * - 工作日返回正常/进行中状态，周末返回休息
 * - 模拟迟到、早退等异常场景（可配置）
 * - canPunchIn / canPunchOut 控制打卡按钮可用性
 */
export const attendanceApi = {
  async getTodayAttendance(): Promise<TodayAttendance> {
    await mockLightDelay()

    const now = new Date()
    const hour = now.getHours()
    const minute = now.getMinutes()
    const dayOfWeek = now.getDay()
    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6

    // 周末：无排班，不可打卡
    if (isWeekend) {
      return {
        status: 'not_clocked_in',
        clockInTime: null,
        clockOutTime: null,
        currentHours: 0,
        expectedHours: 0,
        shiftName: '休息',
        canPunchIn: false,
        canPunchOut: false,
      }
    }

    // 工作日：根据当前时间段生成不同状态
    const clockInHour = 8
    const clockOutHour = 17

    // 未上班（8点前）→ 可上班打卡
    if (hour < clockInHour) {
      return {
        status: 'not_clocked_in',
        clockInTime: null,
        clockOutTime: null,
        currentHours: 0,
        expectedHours: 8,
        shiftName: '早班',
        canPunchIn: true,
        canPunchOut: false,
      }
    }

    // 已上班未下班（8:00 - 17:30）→ 可下班打卡
    if (hour < clockOutHour || (hour === clockOutHour && minute < 30)) {
      const workedMinutes = (hour - clockInHour) * 60 + minute - 55
      const currentHours = Math.max(0, Math.round(workedMinutes) / 60)

      // 模拟轻微迟到场景
      const isLate = hour === clockInHour && minute > 10

      return {
        status: isLate ? 'late' : 'normal',
        clockInTime: isLate ? `0${clockInHour}:${String(minute).padStart(2, '0')}` : `${clockInHour}:5${minute % 10}`,
        clockOutTime: null,
        currentHours: Math.min(currentHours, 9),
        expectedHours: 8,
        shiftName: '早班',
        exceptionType: isLate ? '迟到' : undefined,
        canPunchIn: false,
        canPunchOut: true,
      }
    }

    // 已下班（17:30后）→ 今日完成
    const isEarlyLeave = hour === clockOutHour && minute < 30
    return {
      status: isEarlyLeave ? 'early_leave' : 'normal',
      clockInTime: '08:55',
      clockOutTime: isEarlyLeave ? `${clockOutHour}:${String(minute).padStart(2, '0')}` : '17:32',
      currentHours: isEarlyLeave ? 7.5 : 8.6,
      expectedHours: 8,
      shiftName: '早班',
      exceptionType: isEarlyLeave ? '早退' : undefined,
      canPunchIn: false,
      canPunchOut: false,
    }
  },

  /**
   * 执行打卡操作（带约束检查）
   *
   * 后端 B-13 就绪后替换为: POST /v1/attendance/punch
   * Body: { type: 'in'|'out', latitude, longitude, accuracy, deviceInfo }
   *
   * 约束检查流程：
   * 1. 根据约束模式检查定位/网络条件
   * 2. 条件不满足时直接拒绝（返回 success=false）
   * 3. 条件满足后采集设备信息并提交
   *
   * @param type - 打卡类型（上班/下班）
   * @param constraintMode - 约束模式，默认 location_only
   */
  async punch(
    type: PunchType,
    constraintMode: PunchConstraintMode = 'location_only',
  ): Promise<PunchResult> {
    await mockDelay()

    // 获取默认配置（后续替换为API获取）
    const config: PunchConfig = {
      constraintMode,
      fenceRadiusMeters: FENCE_RADIUS_METERS,
      allowedConnectionTypes: ['wifi', 'ethernet'],
      storeLocation: STORE_LOCATION,
    }

    // ── 步骤1：获取定位信息 ──
    let locationResult: { success: boolean; withinFence: boolean; distance?: number; location?: GeoLocation } =
      { success: false, withinFence: false }

    try {
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error('浏览器不支持定位功能'))
          return
        }
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 60000,
        })
      })

      const userLocation: GeoLocation = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracy: position.coords.accuracy,
      }

      const distance = calculateDistance(
        userLocation.latitude,
        userLocation.longitude,
        STORE_LOCATION.latitude,
        STORE_LOCATION.longitude,
      )

      locationResult = {
        success: true,
        withinFence: distance <= FENCE_RADIUS_METERS,
        distance: Math.round(distance),
        location: userLocation,
      }
    } catch {
      // 定位失败：记录失败状态，由约束校验决定是否拒绝
      locationResult = { success: false, withinFence: false }
    }

    // ── 步骤2：检测网络连接类型 ──
    const networkInfo = await detectNetworkType()

    // ── 步骤3：约束条件校验 ──
    const constraintCheck = validateConstraints(constraintMode, locationResult, networkInfo, config)

    if (!constraintCheck.satisfied) {
      // 约束不满足，直接拒绝打卡
      return {
        success: false,
        punchTime: '',
        punchType: type,
        message: constraintCheck.reason,
        distanceToStore: locationResult.distance,
        withinFence: locationResult.withinFence,
        constraintReason: constraintCheck.reason,
        locationSatisfied: constraintCheck.locationSatisfied,
        networkSatisfied: constraintCheck.networkSatisfied,
      }
    }

    // ── 步骤4：采集设备信息（防作弊） ──
    const deviceInfo = collectDeviceInfo()

    // TODO: 后端对接时将 deviceInfo 一并提交

    // ── 步骤5：执行打卡 ──
    const now = new Date()
    const punchTime =
      `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`

    if (locationResult.withinFence) {
      return {
        success: true,
        punchTime,
        punchType: type,
        message: type === 'in' ? '上班打卡成功' : '下班打卡成功',
        distanceToStore: locationResult.distance,
        withinFence: true,
        locationSatisfied: true,
        networkSatisfied: true,
      }
    }

    // 围栏外但约束模式允许（如 none 模式或仅 wifi 模式）
    return {
      success: true,
      punchTime,
      punchType: type,
      message: `打卡成功（距离门店 ${locationResult.distance ?? '?'}m，已记录异常位置）`,
      distanceToStore: locationResult.distance,
      withinFence: false,
      locationSatisfied: true,
      networkSatisfied: true,
    }
  },

  /**
   * 获取打卡约束配置
   *
   * Mock 阶段返回本地配置，后端就绪后从 GET /v1/attendance/punch-config 获取。
   */
  async getPunchConfig(): Promise<PunchConfig> {
    await mockLightDelay()

    return {
      constraintMode: 'location_only',
      fenceRadiusMeters: FENCE_RADIUS_METERS,
      allowedConnectionTypes: ['wifi', 'ethernet'],
      storeLocation: STORE_LOCATION,
    }
  },

  /** 检测当前网络连接类型 */
  async detectNetwork(): Promise<WiFiInfo> {
    return detectNetworkType()
  },

  /** 获取门店坐标信息（用于前端展示） */
  getStoreLocation(): { location: GeoLocation; radiusMeters: number; address: string } {
    return {
      location: STORE_LOCATION,
      radiusMeters: FENCE_RADIUS_METERS,
      address: '北京市朝阳区xx路xx号餐饮门店',
    }
  },
}
