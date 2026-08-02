/**
 * 叫号系统API模块
 * 封装叫号系统相关的所有API请求
 *
 * 注意：response拦截器已自动提取 response.data.data，
 * 因此 request 调用的实际返回值已经是解包后的数据。
 */
import request from './request'

/** 硬件语音播报响应 */
export interface HardwareVoiceResponse {
  orderNumber: string
  orderType: string
  hardwareAvailable: boolean
  message: string
  response?: string
}

/** LED显示屏响应 */
export interface HardwareDisplayResponse {
  orderNumber: string
  orderType: string
  hardwareAvailable: boolean
  message: string
  displayText?: string
}

/** 硬件状态响应 */
export interface HardwareStatusResponse {
  speakerAvailable: boolean
  displayAvailable: boolean
  anyHardwareAvailable: boolean
}

/** 硬件语音播报请求参数 */
interface VoiceRequestParams {
  orderNumber: string
  orderType: string
}

/** LED显示屏请求参数 */
interface DisplayRequestParams {
  orderNumber: string
  orderType: string
  tableNumber?: string
}

/**
 * 叫号系统硬件API
 */
export const callNumberHardwareApi = {
  /**
   * 通过硬件设备播报叫号语音
   * @param params - 叫号信息（订单号、订单类型）
   * @returns 硬件播报结果，包含hardwareAvailable字段指示是否成功使用硬件
   */
  broadcastVoice(params: VoiceRequestParams): Promise<HardwareVoiceResponse> {
    return request.post('/v1/call-number/hardware/voice', params) as unknown as Promise<HardwareVoiceResponse>
  },

  /**
   * 向LED显示屏发送叫号信息
   * @param params - 显示信息（订单号、订单类型、桌号）
   * @returns 发送结果
   */
  sendToDisplay(params: DisplayRequestParams): Promise<HardwareDisplayResponse> {
    return request.post('/v1/call-number/hardware/display', params) as unknown as Promise<HardwareDisplayResponse>
  },

  /**
   * 获取硬件连接状态
   * @returns 各类硬件设备的连接状态
   */
  getStatus(): Promise<HardwareStatusResponse> {
    return request.get('/v1/call-number/hardware/status') as unknown as Promise<HardwareStatusResponse>
  }
}
