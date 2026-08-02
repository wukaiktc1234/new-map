interface BroadcastMessage {
  event: string
  data: unknown
  timestamp: number
  source?: string
}

type BroadcastCallback = (event: string, data: unknown) => void

class PosBroadcastService {
  private channel: BroadcastChannel | null = null
  private listeners: Map<string, Set<BroadcastCallback>> = new Map()
  private useLocalStorage = false
  private channelName: string

  constructor(channelName: string = 'pos-system') {
    this.channelName = channelName
    this.init()
  }

  private init(): void {
    try {
      this.channel = new BroadcastChannel(this.channelName)
      this.channel.onmessage = (event: MessageEvent<BroadcastMessage>) => {
        const { event: eventName, data } = event.data
        this.notifyListeners(eventName, data)
      }
    } catch {
      console.warn('[BroadcastChannel] 不支持，降级为localStorage')
      this.useLocalStorage = true
      window.addEventListener('storage', this.handleStorageEvent.bind(this))
    }
  }

  private handleStorageEvent(event: StorageEvent): void {
    if (event.key === `broadcast-${this.channelName}` && event.newValue) {
      try {
        const msg: BroadcastMessage = JSON.parse(event.newValue)
        this.notifyListeners(msg.event, msg.data)
      } catch { /* ignore */ }
    }
  }

  private notifyListeners(event: string, data: unknown): void {
    const cbs = this.listeners.get(event)
    if (cbs) {
      cbs.forEach(cb => { try { cb(event, data) } catch (e) { console.error(e) } })
    }
    const allCbs = this.listeners.get('*')
    if (allCbs) {
      allCbs.forEach(cb => { try { cb(event, data) } catch (e) { console.error(e) } })
    }
  }

  send(event: string, data?: unknown): void {
    const msg: BroadcastMessage = { event, data: data ?? null, timestamp: Date.now(), source: 'pos-main' }

    if (!this.useLocalStorage && this.channel) {
      this.channel.postMessage(msg)
    } else {
      const key = `broadcast-${this.channelName}`
      localStorage.setItem(key, JSON.stringify(msg))
      localStorage.removeItem(key)
      localStorage.setItem(key, JSON.stringify(msg))
    }
  }

  onMessage(callback: BroadcastCallback): () => void {
    if (!this.listeners.has('*')) {
      this.listeners.set('*', new Set())
    }
    this.listeners.get('*')!.add(callback)
    return () => { this.listeners.get('*')?.delete(callback) }
  }

  on(event: string, callback: BroadcastCallback): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event)!.add(callback)
    return () => { this.listeners.get(event)?.delete(callback) }
  }

  off(event: string, callback: BroadcastCallback): void {
    this.listeners.get(event)?.delete(callback)
  }

  destroy(): void {
    if (this.channel) {
      this.channel.close()
      this.channel = null
    }
    if (this.useLocalStorage) {
      window.removeEventListener('storage', this.handleStorageEvent.bind(this))
    }
    this.listeners.clear()
  }
}

export const posBroadcast = new PosBroadcastService()
