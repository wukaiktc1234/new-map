/**
 * 设备 WebSocket 实时推送组合式函数
 *
 * 基于 STOMP + SockJS 协议，订阅后端 /topic/device/* 主题，
 * 实时接收设备状态变化与连接状态推送，替代 HTTP 轮询。
 *
 * 特性：
 * - 自动重连（指数退避，最多 5 次）
 * - 连接状态响应式 ref（isConnected）
 * - 连接失败时 isConnected 降为 false，调用方可据此回退到轮询
 * - 页面卸载时自动断开连接
 *
 * 使用方式：
 * ```ts
 * const { isConnected, connect, disconnect, subscribeDeviceStatus } = useDeviceWebSocket()
 * subscribeDeviceStatus((msg) => { /* ... *\/ })
 * connect()
 * ```
 */
import { ref, onBeforeUnmount } from 'vue'
import { Client } from '@stomp/stompjs'
import type { IFrame, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * 设备状态推送消息（对应后端 DeviceStatus 实体）
 * 后端 Jackson 配置 default-property-inclusion: non_null，未设置的字段会被省略。
 */
export interface DeviceStatusMessage {
  /** 设备类型（如 PRINTER/SCANNER/SCALE/LOCKER/OTHER） */
  deviceType?: string
  /** 是否在线 */
  online?: boolean
  /** 设备响应时间（毫秒） */
  responseTime?: number
  /** 设备型号 */
  deviceModel?: string
  /** 固件版本 */
  firmwareVersion?: string
  /** 连接类型（NETWORK/SERIAL/BLUETOOTH） */
  connectionType?: string
  /** 网络IP地址（仅网络设备） */
  ipAddress?: string
  /** 网络端口（仅网络设备） */
  port?: string
  /** 最后检测时间（YYYY-MM-DD HH:mm:ss） */
  lastCheckTime?: string
  /** 检测结果详情 */
  details?: string
  /** 错误信息 */
  errorMessage?: string
  /** 设备名称 */
  deviceName?: string
  /** 设备ID */
  deviceId?: number
}

/** 状态推送回调 */
type StatusCallback = (message: DeviceStatusMessage) => void

/** 最大重连次数 */
const MAX_RECONNECT_ATTEMPTS = 5
/** 初始重连延迟（毫秒） */
const INITIAL_RECONNECT_DELAY = 1000
/** 最大重连延迟（毫秒） */
const MAX_RECONNECT_DELAY = 30000

/** 后端开发环境地址（context-path 为 /api） */
const DEV_BACKEND_ORIGIN = 'http://localhost:8081'
/** WebSocket 端点路径（相对 context-path） */
const WS_ENDPOINT = '/ws/device'
/** 设备状态主题 */
const TOPIC_DEVICE_STATUS = '/topic/device/status'
/** 设备连接状态主题 */
const TOPIC_DEVICE_CONNECTION = '/topic/device/connection'

/**
 * 构建 SockJS 端点 URL
 * - 若 VITE_API_BASE_URL 为完整 URL（生产环境），直接拼接端点
 * - 否则（开发环境仅路径如 /api，走代理），直连后端开发地址
 */
function buildWebSocketUrl(): string {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const normalizedBase = baseUrl.replace(/\/$/, '')
  if (/^https?:\/\//i.test(baseUrl)) {
    return `${normalizedBase}${WS_ENDPOINT}`
  }
  return `${DEV_BACKEND_ORIGIN}${normalizedBase}${WS_ENDPOINT}`
}

/** 解析 STOMP 消息体为设备状态对象，失败返回 null */
function parseMessage(body: string): DeviceStatusMessage | null {
  try {
    return JSON.parse(body) as DeviceStatusMessage
  } catch (error) {
    console.error('[DeviceWebSocket] 解析消息体失败:', error)
    return null
  }
}

/**
 * 设备 WebSocket 组合式函数
 * 必须在 setup 顶层调用（内部注册了 onBeforeUnmount 清理）。
 */
export function useDeviceWebSocket() {
  /** 连接状态（true=已连接，可接收推送） */
  const isConnected = ref(false)

  let client: Client | null = null
  let statusSubscription: StompSubscription | null = null
  let connectionSubscription: StompSubscription | null = null
  let statusCallback: StatusCallback | null = null
  let connectionCallback: StatusCallback | null = null

  let reconnectAttempts = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  /** 标记是否为主动断开，避免主动断开触发重连 */
  let manuallyDisconnected = false

  /** 计算指数退避延迟 */
  function getReconnectDelay(): number {
    const delay = INITIAL_RECONNECT_DELAY * Math.pow(2, reconnectAttempts)
    return Math.min(delay, MAX_RECONNECT_DELAY)
  }

  /** 调度一次重连（指数退避，达上限后停止） */
  function scheduleReconnect(): void {
    if (manuallyDisconnected) return
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.warn(`[DeviceWebSocket] 已达最大重连次数 (${MAX_RECONNECT_ATTEMPTS})，停止重连`)
      return
    }
    const delay = getReconnectDelay()
    reconnectAttempts++
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      if (manuallyDisconnected) return
      client?.activate()
    }, delay)
  }

  /** 创建消息分发处理器（统一解析与异常兜底） */
  function createMessageHandler(callback: StatusCallback): (message: IMessage) => void {
    return (message: IMessage) => {
      const data = parseMessage(message.body)
      if (data) callback(data)
    }
  }

  /** 重建所有主题订阅（连接成功后调用） */
  function resubscribeAll(): void {
    if (!client || !client.connected) return
    // 清理旧订阅，避免重复
    statusSubscription?.unsubscribe()
    connectionSubscription?.unsubscribe()
    statusSubscription = null
    connectionSubscription = null

    if (statusCallback) {
      statusSubscription = client.subscribe(TOPIC_DEVICE_STATUS, createMessageHandler(statusCallback))
    }
    if (connectionCallback) {
      connectionSubscription = client.subscribe(TOPIC_DEVICE_CONNECTION, createMessageHandler(connectionCallback))
    }
  }

  /** 建立连接（幂等，重复调用安全） */
  function connect(): void {
    if (client) return
    manuallyDisconnected = false
    const url = buildWebSocketUrl()
    client = new Client({
      webSocketFactory: () => new SockJS(url),
      reconnectDelay: 0, // 禁用内置重连，手动实现指数退避
      connectHeaders: {},
      debug: () => { /* 屏蔽 STOMP 内置 debug 日志，避免污染控制台 */ },
      onConnect: (_frame: IFrame) => {
        isConnected.value = true
        reconnectAttempts = 0
        resubscribeAll()
      },
      onDisconnect: () => {
        isConnected.value = false
      },
      onStompError: (frame: IFrame) => {
        console.error('[DeviceWebSocket] STOMP 协议错误:', frame.headers['message'] || frame.body)
      },
      onWebSocketError: () => {
        isConnected.value = false
        scheduleReconnect()
      },
      onWebSocketClose: () => {
        isConnected.value = false
        scheduleReconnect()
      },
    })
    client.activate()
  }

  /**
   * 订阅设备状态变化主题
   * 可在 connect() 之前调用（回调会暂存，连接成功后自动订阅）。
   */
  function subscribeDeviceStatus(callback: StatusCallback): void {
    statusCallback = callback
    if (client?.connected) {
      statusSubscription?.unsubscribe()
      statusSubscription = client.subscribe(TOPIC_DEVICE_STATUS, createMessageHandler(callback))
    }
  }

  /**
   * 订阅设备连接状态主题
   * 可在 connect() 之前调用（回调会暂存，连接成功后自动订阅）。
   */
  function subscribeDeviceConnection(callback: StatusCallback): void {
    connectionCallback = callback
    if (client?.connected) {
      connectionSubscription?.unsubscribe()
      connectionSubscription = client.subscribe(TOPIC_DEVICE_CONNECTION, createMessageHandler(callback))
    }
  }

  /** 主动断开连接（幂等） */
  function disconnect(): void {
    manuallyDisconnected = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    statusSubscription?.unsubscribe()
    connectionSubscription?.unsubscribe()
    statusSubscription = null
    connectionSubscription = null
    statusCallback = null
    connectionCallback = null
    if (client) {
      client.deactivate().catch((error: unknown) => {
        console.error('[DeviceWebSocket] 断开连接失败:', error)
      })
      client = null
    }
    isConnected.value = false
    reconnectAttempts = 0
  }

  // 页面卸载时自动清理，防止连接泄漏
  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    disconnect,
    subscribeDeviceStatus,
    subscribeDeviceConnection,
  }
}
