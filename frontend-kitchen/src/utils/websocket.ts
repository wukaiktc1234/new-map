import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { OrderStatusNotification, WebSocketConfig } from '@/types/websocket';

const getWebSocketUrl = (): string => {
  const host = window.location.hostname
  const apiPort = (import.meta.env as Record<string, string>).VITE_API_PORT || '8081'
  return `http://${host}:${apiPort}/api/ws`
}

// 后端 STOMP CONNECT 帧强制 JWT 鉴权（WebSocketConfig 拦截器只读帧头，不读 HTTP 头）
const getConnectHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('token') || localStorage.getItem('access_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const defaultConfig: WebSocketConfig = {
  url: getWebSocketUrl(),
  reconnectInterval: 3000
};

class WebSocketService {
  private client: Client | null = null;
  private config: WebSocketConfig;
  private subscriptions: Map<string, any> = new Map();
  private isConnected = false;
  private connectionPromise: Promise<void> | null = null;
  private onConnectionChange: ((connected: boolean) => void) | null = null;
  // 应用层监督重连：stompjs v7 的内部重连只在 ws-close 事件续期，
  // 重试以 ws-error（无 close）收场时链路会永久停摆（P1-STOMP-RECONNECT-001），
  // 因此关闭内部重连，由本类统一调度。
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(config?: Partial<WebSocketConfig>) {
    this.config = { ...defaultConfig, ...config };
  }

  setOnConnectionChange(callback: (connected: boolean) => void) {
    this.onConnectionChange = callback;
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private scheduleReconnect() {
    if (this.isConnected || this.reconnectTimer !== null) {
      return;
    }
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.isConnected) {
        return;
      }
      console.log('WebSocket监督重连...');
      const oldClient = this.client;
      this.client = null;
      oldClient?.deactivate().catch(() => {});
      this.connect().catch(() => {});
    }, this.config.reconnectInterval);
  }

  async connect(): Promise<void> {
    if (this.isConnected && this.client?.connected) {
      console.log('WebSocket已连接，跳过');
      return;
    }

    if (this.connectionPromise) {
      console.log('WebSocket正在连接中，等待...');
      return this.connectionPromise;
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      try {
        console.log('正在连接WebSocket:', this.config.url);
        const socket = new SockJS(this.config.url);
        this.client = new Client({
          webSocketFactory: () => socket,
          connectHeaders: getConnectHeaders(),
          reconnectDelay: 0, // 内部重连已停用：由 scheduleReconnect 接管
          debug: (str) => console.log('STOMP Debug:', str),
          heartbeatIncoming: 10000,
          heartbeatOutgoing: 10000,
          connectionTimeout: 10000
        });

        this.client.onConnect = (frame) => {
          console.log('WebSocket连接成功:', frame);
          this.isConnected = true;
          this.clearReconnectTimer();
          this.connectionPromise = null;
          this.onConnectionChange?.(true);
          resolve();
        };

        this.client.onStompError = (frame) => {
          console.error('STOMP错误:', frame);
          this.isConnected = false;
          this.connectionPromise = null;
          this.onConnectionChange?.(false);
          // STOMP ERROR 后连接通常随即关闭，由 close 分支调度重连；
          // 若无 close（同 ws-error 停摆场景），这里兜底调度（含去重护栏）
          this.scheduleReconnect();
          reject(new Error('STOMP error'));
        };

        this.client.onWebSocketError = (event) => {
          console.error('WebSocket错误:', event);
          this.isConnected = false;
          this.onConnectionChange?.(false);
        };

        this.client.onWebSocketClose = () => {
          console.log('WebSocket已关闭');
          this.isConnected = false;
          this.connectionPromise = null;
          this.onConnectionChange?.(false);
          this.scheduleReconnect();
        };

        this.client.onDisconnect = () => {
          console.log('WebSocket已断开连接');
          this.isConnected = false;
          this.connectionPromise = null;
          this.onConnectionChange?.(false);
        };

        this.client.activate();
      } catch (error) {
        console.error('WebSocket初始化失败:', error);
        this.isConnected = false;
        this.connectionPromise = null;
        this.onConnectionChange?.(false);
        this.scheduleReconnect();
        reject(error);
      }
    });

    return this.connectionPromise;
  }

  async subscribe(
    destination: string,
    callback: (message: OrderStatusNotification) => void
  ): Promise<string> {
    await this.connect();

    if (!this.client?.connected) {
      throw new Error('WebSocket未连接');
    }

    console.log('订阅主题:', destination);
    const subscription = this.client.subscribe(destination, (message) => {
      try {
        console.log('收到消息:', destination, message.body);
        const data = JSON.parse(message.body) as OrderStatusNotification;
        callback(data);
      } catch (error) {
        console.error('解析WebSocket消息失败:', error);
      }
    });

    this.subscriptions.set(subscription.id, subscription);
    console.log('订阅成功, subscriptionId:', subscription.id);
    return subscription.id;
  }

  async subscribeToNewOrders(callback: (message: OrderStatusNotification) => void): Promise<string> {
    return this.subscribe('/topic/orders/new', callback);
  }

  async subscribeToOrderStatusChanges(callback: (message: OrderStatusNotification) => void): Promise<string> {
    return this.subscribe('/topic/orders/status', callback);
  }

  async subscribeToReadyForPickup(callback: (message: OrderStatusNotification) => void): Promise<string> {
    return this.subscribe('/topic/orders/ready', callback);
  }

  async subscribeToOrderRefund(callback: (message: OrderStatusNotification) => void): Promise<string> {
    return this.subscribe('/topic/orders/refund', callback);
  }

  unsubscribe(subscriptionId: string): void {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
    }
  }

  disconnect(): void {
    this.clearReconnectTimer();
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions.clear();

    if (this.client?.connected) {
      this.client.deactivate();
    }

    this.isConnected = false;
    this.client = null;
    this.connectionPromise = null;
  }

  isConnectedStatus(): boolean {
    return this.isConnected && this.client?.connected === true;
  }
}

export const webSocketService = new WebSocketService();
