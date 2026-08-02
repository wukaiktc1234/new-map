export interface TTSConfig {
  provider: 'browser' | 'baidu' | 'xunfei' | 'aliyun' | 'tencent' | 'azure'
  enabled: boolean
  volume: number
  rate: number
  voiceURI?: string
  template: string
  useDifferentTemplates?: boolean
  templates?: {
    dineIn: string
    takeout: string
    pickup: string
    pack: string
  }
  naturalSpeech: boolean
  repeatCount: number
  repeatInterval: number
  apiKey?: string
  apiSecret?: string
  appId?: string
}

const chineseNumbers: Record<string, string> = {
  '0': '零', '1': '一', '2': '二', '3': '三', '4': '四',
  '5': '五', '6': '六', '7': '七', '8': '八', '9': '九'
}

const ORDER_TYPE_MAP: Record<string, string> = {
  '堂食': 'dineIn',
  '外卖': 'takeout',
  '自提': 'pickup',
  '打包': 'pack',
  'dineIn': 'dineIn',
  'takeout': 'takeout',
  'pickup': 'pickup',
  'pack': 'pack'
}

const defaultTemplates = {
  dineIn: '请{tableNumber}的顾客注意，您的堂食订单已完成制作，请到前台取餐！',
  takeout: '外卖订单 {orderNumber} 已准备好，请骑手尽快到取餐区领取！',
  pickup: '自提订单 {orderNumber} 已完成，请顾客到自提区取餐！',
  pack: '打包订单 {orderNumber} 已做好，请到收银台领取！'
}

const defaultConfig: TTSConfig = {
  provider: 'browser',
  enabled: true,
  volume: 80,
  rate: 1,
  voiceURI: '',
  template: '请注意，{orderType}订单 {orderNumber} 已完成制作，请到前台取餐！',
  useDifferentTemplates: false,
  templates: { ...defaultTemplates },
  naturalSpeech: true,
  repeatCount: 2,
  repeatInterval: 3
}

export class TTSService {
  private config: TTSConfig

  constructor() {
    this.config = { ...defaultConfig }
    this.loadConfig()
  }

  loadConfig() {
    try {
      const saved = localStorage.getItem('callNumberSettings')
      if (saved) {
        const parsed = JSON.parse(saved)
        if (parsed.voice) {
          this.config = { ...defaultConfig, ...parsed.voice }
          if (!this.config.templates) {
            this.config.templates = { ...defaultTemplates }
          }
        }
      }
    } catch (e) {
      console.error('加载TTS配置失败:', e)
    }
  }

  updateConfig(config: Partial<TTSConfig>) {
    this.config = { ...this.config, ...config }
  }

  convertToNaturalSpeech(text: string): string {
    if (!this.config.naturalSpeech) return text

    let result = text
    
    result = result.replace(/([A-Za-z]\d{3,})/g, (_: string, p1: string) => {
      const letter = p1[0].toUpperCase()
      const numbers = p1.slice(1).split('').map((n: string) => chineseNumbers[n] || n).join('')
      return `${letter} ${numbers}号`
    })

    result = result.replace(/订单\s*([A-Za-z]?\d+)/g, (_match: string, p1: string) => {
      if (/^[A-Za-z]/.test(p1)) {
        const letter = p1[0].toUpperCase()
        const numbers = p1.slice(1).split('').map((n: string) => chineseNumbers[n] || n).join('')
        return `订单 ${letter} ${numbers}号`
      }
      const numbers = p1.split('').map((n: string) => chineseNumbers[n] || n).join('')
      return `订单 ${numbers}号`
    })

    result = result.replace(/([A-Za-z]\d{1,2})桌/g, (_: string, p1: string) => {
      const letter = p1[0].toUpperCase()
      const numbers = p1.slice(1).split('').map((n: string) => chineseNumbers[n] || n).join('')
      return `${letter}区${numbers}号桌`
    })
    
    return result
  }

  getTemplateByOrderType(orderType: string): string {
    if (this.config.useDifferentTemplates && this.config.templates) {
      const typeKey = ORDER_TYPE_MAP[orderType] || 'dineIn'
      const templates = this.config.templates as Record<string, string>
      return templates[typeKey] || this.config.template || defaultConfig.template
    }
    return this.config.template || defaultConfig.template
  }

  replaceVariables(template: string, data: { orderNumber: string; tableNumber: string; orderType: string; itemCount: number }): string {
    return template
      .replace(/{orderNumber}/g, data.orderNumber)
      .replace(/{tableNumber}/g, data.tableNumber)
      .replace(/{orderType}/g, data.orderType)
      .replace(/{itemCount}/g, String(data.itemCount))
  }

  async speak(orderData: { orderNumber: string; tableNumber: string; orderType: string; itemCount: number }): Promise<void> {
    if (!this.config.enabled) return

    const template = this.getTemplateByOrderType(orderData.orderType)
    const text = this.replaceVariables(template, orderData)
    const naturalText = this.convertToNaturalSpeech(text)

    for (let i = 0; i < this.config.repeatCount; i++) {
      if (i > 0) {
        await new Promise(resolve => setTimeout(resolve, this.config.repeatInterval * 1000))
      }
      await this.playVoice(naturalText)
    }
  }

  private async playVoice(text: string): Promise<void> {
    if (this.config.provider === 'browser') {
      return this.playWithBrowser(text)
    } else {
      return this.playWithCloudTTS(text)
    }
  }

  private async playWithBrowser(text: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!('speechSynthesis' in window)) {
        reject(new Error('浏览器不支持语音合成'))
        return
      }

      speechSynthesis.cancel()
      
      const utterance = new SpeechSynthesisUtterance(text)
      utterance.volume = this.config.volume / 100
      utterance.rate = this.config.rate
      utterance.pitch = 1
      
      const voices = speechSynthesis.getVoices()
      
      if (this.config.voiceURI) {
        const selectedVoice = voices.find(v => v.voiceURI === this.config.voiceURI)
        if (selectedVoice) {
          utterance.voice = selectedVoice
          utterance.lang = selectedVoice.lang
        }
      } else {
        const zhVoice = voices.find(v => v.lang.includes('zh') || v.lang.includes('CN'))
        if (zhVoice) {
          utterance.voice = zhVoice
          utterance.lang = zhVoice.lang
        } else {
          utterance.lang = 'zh-CN'
        }
      }
      
      utterance.onend = () => resolve()
      utterance.onerror = (e) => reject(new Error(`语音播放失败: ${e.error}`))
      
      setTimeout(() => speechSynthesis.speak(utterance), 100)
    })
  }

  private async playWithCloudTTS(text: string): Promise<void> {
    if (!this.config.apiKey) {
      console.warn('未配置API密钥，回退到浏览器语音')
      return this.playWithBrowser(text)
    }

    try {
      const response = await fetch(`/api/tts/${this.config.provider}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          text,
          voice: this.config.voiceURI,
          speed: this.config.rate,
          volume: this.config.volume,
          apiKey: this.config.apiKey,
          apiSecret: this.config.apiSecret,
          appId: this.config.appId
        })
      })

      if (!response.ok) {
        throw new Error('语音合成失败')
      }

      const audioBlob = await response.blob()
      const audio = new Audio(URL.createObjectURL(audioBlob))
      audio.volume = this.config.volume / 100
      
      audio.onended = () => URL.revokeObjectURL(audio.src)
      
      await audio.play()
    } catch (error) {
      console.error('云端TTS错误，回退到浏览器语音:', error)
      return this.playWithBrowser(text)
    }
  }

  stop() {
    if ('speechSynthesis' in window) {
      speechSynthesis.cancel()
    }
  }
}

export const ttsService = new TTSService()
