import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 操作反馈配置接口
 */
export interface OperationFeedback {
  type: 'success' | 'warning' | 'error' | 'info' | 'urgent'
  message: string
  sound?: boolean
  vibration?: boolean
  duration?: number
}

/**
 * 音频配置接口
 */
interface AudioConfig {
  frequency: number
  duration: number
  type: OscillatorType
  volume: number
}

/**
 * 操作反馈Composable - 提供统一的视觉、声音、震动反馈
 * 使用Web Audio API生成音效，无需外部音频文件
 */
export function useOperationFeedback() {
  const isPlaying = ref(false)
  let audioContext: AudioContext | null = null

  /**
   * 获取或创建AudioContext
   */
  const getAudioContext = (): AudioContext => {
    if (!audioContext) {
      audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()
    }
    return audioContext
  }

  /**
   * 不同类型的音频配置
   */
  const audioConfigs: Record<string, AudioConfig> = {
    success: {
      frequency: 880,
      duration: 150,
      type: 'sine',
      volume: 0.3
    },
    warning: {
      frequency: 440,
      duration: 200,
      type: 'triangle',
      volume: 0.3
    },
    error: {
      frequency: 220,
      duration: 300,
      type: 'sawtooth',
      volume: 0.4
    },
    info: {
      frequency: 660,
      duration: 100,
      type: 'sine',
      volume: 0.2
    },
    urgent: {
      frequency: 1000,
      duration: 500,
      type: 'square',
      volume: 0.5
    }
  }

  /**
   * 播放音效
   * @param config - 音频配置
   */
  const playSound = (config: AudioConfig): void => {
    try {
      if (isPlaying.value) return

      isPlaying.value = true
      const ctx = getAudioContext()

      // 创建振荡器
      const oscillator = ctx.createOscillator()
      const gainNode = ctx.createGain()

      // 配置振荡器
      oscillator.type = config.type
      oscillator.frequency.setValueAtTime(config.frequency, ctx.currentTime)

      // 配置音量
      gainNode.gain.setValueAtTime(config.volume, ctx.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + config.duration / 1000)

      // 连接节点
      oscillator.connect(gainNode)
      gainNode.connect(ctx.destination)

      // 播放和停止
      oscillator.start(ctx.currentTime)
      oscillator.stop(ctx.currentTime + config.duration / 1000)

      // 重置状态
      setTimeout(() => {
        isPlaying.value = false
      }, config.duration)
    } catch (error) {
      console.warn('播放音效失败:', error)
      isPlaying.value = false
    }
  }

  /**
   * 触发设备震动（移动端）
   * @param pattern - 震动模式
   */
  const triggerVibration = (pattern: number | number[] = 100): void => {
    if ('vibrate' in navigator && typeof navigator.vibrate === 'function') {
      navigator.vibrate(pattern)
    }
  }

  /**
   * 显示Toast提示
   * @param feedback - 反馈配置
   */
  const showToast = (feedback: OperationFeedback): void => {
    const options = {
      message: feedback.message,
      type: feedback.type === 'urgent' ? 'warning' : feedback.type as any,
      duration: feedback.duration || 2000,
      showClose: true
    }

    switch (feedback.type) {
      case 'success':
        ElMessage.success(options)
        break
      case 'warning':
        ElMessage.warning(options)
        break
      case 'error':
        ElMessage.error(options)
        break
      case 'info':
        ElMessage.info(options)
        break
      default:
        ElMessage(options)
    }
  }

  /**
   * 显示成功反馈
   * @param message - 提示信息
   * @param options - 可选配置
   */
  const showSuccess = (
    message: string,
    options?: { sound?: boolean; vibration?: boolean; duration?: number }
  ): void => {
    const feedback: OperationFeedback = {
      type: 'success',
      message,
      sound: options?.sound !== false,
      vibration: options?.vibration !== false,
      duration: options?.duration
    }
    triggerFeedback(feedback)
  }

  /**
   * 显示警告反馈
   * @param message - 提示信息
   * @param options - 可选配置
   */
  const showWarning = (
    message: string,
    options?: { sound?: boolean; vibration?: boolean; duration?: number }
  ): void => {
    const feedback: OperationFeedback = {
      type: 'warning',
      message,
      sound: options?.sound !== false,
      vibration: options?.vibration !== false,
      duration: options?.duration
    }
    triggerFeedback(feedback)
  }

  /**
   * 显示错误反馈
   * @param message - 提示信息
   * @param options - 可选配置
   */
  const showError = (
    message: string,
    options?: { sound?: boolean; vibration?: boolean; duration?: number }
  ): void => {
    const feedback: OperationFeedback = {
      type: 'error',
      message,
      sound: options?.sound !== false,
      vibration: options?.vibration !== false,
      duration: options?.duration
    }
    triggerFeedback(feedback)
  }

  /**
   * 显示紧急提醒（超时订单等）
   * @param message - 提示信息
   * @param options - 可选配置
   */
  const showUrgent = (
    message: string,
    options?: { sound?: boolean; vibration?: boolean; duration?: number }
  ): void => {
    const feedback: OperationFeedback = {
      type: 'urgent',
      message,
      sound: options?.sound !== false,
      vibration: options?.vibration !== false,
      duration: options?.duration || 3000
    }
    triggerFeedback(feedback)

    // 紧急提醒使用特殊震动模式
    if (feedback.vibration) {
      triggerVibration([200, 100, 200, 100, 200])
    }
  }

  /**
   * 触发完整的反馈（声音+震动+视觉）
   * @param feedback - 反馈配置
   */
  const triggerFeedback = (feedback: OperationFeedback): void => {
    // 播放音效
    if (feedback.sound) {
      const config = audioConfigs[feedback.type]
      if (config) {
        playSound(config)
      }
    }

    // 触发震动
    if (feedback.vibration) {
      triggerVibration()
    }

    // 显示Toast
    showToast(feedback)
  }

  /**
   * 清理资源
   */
  const cleanup = (): void => {
    if (audioContext && audioContext.state !== 'closed') {
      audioContext.close()
      audioContext = null
    }
  }

  return {
    isPlaying,
    showSuccess,
    showWarning,
    showError,
    showUrgent,
    triggerFeedback,
    cleanup
  }
}
