import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { OrderStatusNotification, WebSocketConfig } from '@/types/websocket';

const getWebSocketUrl = (): string => {
  const host = window.location.hostname
  const apiPort = (import.meta.env as Record<string, string>).VITE_API_PORT || '8081'
  return `http://${host}:${apiPort}/api/ws`
}

const defaultConfig: WebSocketConfig = {
  url: getWebSocketUrl(),
  reconnectInterval: 3000,
  maxReconnectAttempts: 10
};

class WebSocketService {
  private client: Client | null = null;
  private reconnectAttempts = 0;
  private config: WebSocketConfig;
  private subscriptions: Map<string, any> = new Map();
  private isConnected = false;
  private connectionPromise: Promise<void> | null = null;
  private onConnectionChange: ((connected: boolean) => void) | null = null;

  constructor(config?: Partial<WebSocketConfig>) {
    this.config = { ...defaultConfig, ...config };
  }

  setOnConnectionChange(callback: (connected: boolean) => void) {
    this.onConnectionChange = callback;
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
          debug: (str) => console.log('STOMP Debug:', str),
          reconnectDelay: this.config.reconnectInterval,
          heartbeatIncoming: 10000,
          heartbeatOutgoing: 10000,
          connectionTimeout: 10000
        });

        this.client.onConnect = (frame) => {
          console.log('WebSocket连接成功:', frame);
          this.isConnected = true;
          this.reconnectAttempts = 0;
          this.connectionPromise = null;
          this.onConnectionChange?.(true);
          resolve();
        };

        this.client.onStompError = (frame) => {
          console.error('STOMP错误:', frame);
          this.isConnected = false;
          this.connectionPromise = null;
          this.onConnectionChange?.(false);
          this.tryReconnect();
          reject(new Error('STOMP error'));
        };

        this.client.onWebSocketError = (event) => {
          console.error('WebSocket错误:', event);
          this.isConnected = false;
          this.onConnectionChange?.(false);
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
        this.tryReconnect();
        reject(error);
      }
    });

    return this.connectionPromise;
  }

  private tryReconnect(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      console.error('WebSocket重连次数已达上限，停止重连');
      return;
    }

    this.reconnectAttempts++;
    console.log(`WebSocket尝试重连 (${this.reconnectAttempts}/${this.config.maxReconnectAttempts})...`);

    setTimeout(() => {
      this.connect().catch(err => console.error('重连失败:', err));
    }, this.config.reconnectInterval);
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
