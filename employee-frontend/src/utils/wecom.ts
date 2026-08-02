/**
 * 企业微信（WeCom）JS-SDK 集成工具
 *
 * 提供企业微信环境检测、SDK初始化、OAuth登录、消息发送等功能。
 * 由于企业微信 JS-SDK 使用回调风格 API 且类型定义不完整，
 * 部分接口调用需要使用类型断言进行适配。
 */
import request from '@/api/request'

export interface WecomUserInfo {
  userId: string
  name: string
  mobile: string
  avatar: string
  department: string
  position: string
}

export interface JsApiConfig {
  corpId: string
  agentId: string
  timestamp: number
  nonceStr: string
  signature: string
}

declare global {
  interface Window {
    wx: WxJssdk | undefined
  }
}

interface WxJssdk {
  config(params: {
    beta?: boolean
    debug?: boolean
    appId: string
    timestamp: number
    nonceStr: string
    signature: string
    jsApiList: string[]
  }): void

  ready(callback: () => void): void
  error(callback: (res: { errMsg: string }) => void): void

  invoke(method: string, params: Record<string, unknown>, success?: (res: unknown) => void, fail?: (err: { errMsg: string }) => void, complete?: () => void): void
  checkJsApi(params: { jsApiList: string[] }, success?: (res: Record<string, boolean>) => void): void
}
const WECOM_JSAPI_LIST = [
  'runtime.info',
  'biz.contact.choose',
  'biz.util.uploadImage',
  'device.notification.show',
]

let wxSdkLoaded = false
let isWecomEnv = false

export async function initWecom(): Promise<boolean> {
  const ua = navigator.userAgent.toLowerCase()
  isWecomEnv = ua.includes('wxwork') || ua.includes('micromessenger')

  if (!isWecomEnv) return false

  if (!wxSdkLoaded && !window.wx) {
    await loadWxJssdk()
  }

  if (!window.wx) {
    console.warn('[WeCom] SDK加载失败')
    return false
  }

  try {
    const config = await fetchJsApiConfig()
    window.wx.config({
      beta: true,
      debug: import.meta.env.DEV,
      appId: config.corpId,
      timestamp: config.timestamp,
      nonceStr: config.nonceStr,
      signature: config.signature,
      jsApiList: WECOM_JSAPI_LIST,
    })

    return new Promise<boolean>((resolve) => {
      // 已在上方检查 window.wx 存在性，此处使用非空断言
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      window.wx!.ready(() => {
        resolve(true)
      })
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      window.wx!.error((res) => {
        console.warn('[WeCom] JSAPI鉴权失败:', res.errMsg)
        resolve(false)
      })
    })
  } catch (e) {
    console.warn('[WeCom] 初始化失败:', e)
    return false
  }
}

function loadWxJssdk(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.wx) { resolve(); return }
    const script = document.createElement('script')
    script.src = 'https://res.wx.qq.com/open/js/jweixin-1.2.0.js'
    script.async = true
    script.onload = () => {
      wxSdkLoaded = true
      resolve()
    }
    script.onerror = () => reject(new Error('企业微信SDK加载失败'))
    document.head.appendChild(script)
  })
}

async function fetchJsApiConfig(): Promise<JsApiConfig> {
  const response = await request.get<JsApiConfig>('/v1/wecom/jsapi-config', {
    url: location.href.split('#')[0],
  } as unknown as Record<string, string>)
  return response as unknown as JsApiConfig
}

export async function wecomLogin(): Promise<WecomUserInfo> {
  if (!isWecomEnv) throw new Error('不在企业微信环境内')

  let authCode = new URLSearchParams(location.search).get('code')

  if (!authCode) {
    authCode = await getAuthCodeFromWx()
  }

  if (!authCode) throw new Error('未获取到授权码')

  const result = await request.get<WecomUserInfo>('/v1/wecom/oauth-login', { code: authCode } as unknown as Record<string, string>)
  return result as unknown as WecomUserInfo
}

function getAuthCodeFromWx(): Promise<string | null> {
  return new Promise((resolve) => {
    if (!window.wx) { resolve(null); return }

    // invoke 回调参数类型与 WxJssdk.invoke 签名不完全匹配，
    // 使用 unknown 类型适配企业微信 JS-SDK 不完整的类型定义
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    window.wx.invoke('getCurExternalContact', {}, (res: any) => {
      if (res.err_code === 0 && res.user_id) {
        resolve(res.user_id)
      } else {
        console.warn('[WeCom] 获取外部联系人失败:', res.err_code)
        resolve(null)
      }
    }, () => {
        resolve(null)
      })
  })
}

export async function sendWecomMessage(userId: string, content: string): Promise<void> {
  await request.post('/v1/wecom/message/send-text', { userId, content })
}

export function isInWecomEnvironment(): boolean {
  return isWecomEnv || /wxwork|micromessenger/i.test(navigator.userAgent)
}
