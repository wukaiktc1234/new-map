/**
 * LRU (Least Recently Used) 缓存实现
 * @module utils/lru-cache
 */

/**
 * LRU缓存节点
 */
interface LRUCacheNode<K, V> {
  key: K;
  value: V;
  timestamp?: number;
  prev: LRUCacheNode<K, V> | null;
  next: LRUCacheNode<K, V> | null;
}

/**
 * LRU缓存配置
 */
export interface LRUCacheOptions {
  maxSize: number;
  maxAge?: number;
}

/**
 * LRU缓存 * @template K - 键类 * @template V - 值类
 */
export class LRUCache<K = string, V = unknown> {
  private maxSize: number;
  private maxAge: number;
  private cache: Map<K, LRUCacheNode<K, V>>;
  private head: LRUCacheNode<K, V> | null = null;
  private tail: LRUCacheNode<K, V> | null = null;

  constructor(options: LRUCacheOptions) {
    this.maxSize = options.maxSize;
    this.maxAge = options.maxAge || 0;
    this.cache = new Map();
  }

  /**
   * 获取缓存   * @param key - 缓存   * @returns 缓存值，不存在或已过期返回undefined
   */
  get(key: K): V | undefined {
    const node = this.cache.get(key);
    if (!node) return undefined;

    if (this.isExpired(node)) {
      this.delete(key);
      return undefined;
    }

    this.moveToHead(node);
    return node.value;
  }

  /**
   * 设置缓存   * @param key - 缓存   * @param value - 缓存
 */
set(key: K, value: V):
void {
    let node = this.cache.get(key);
    const timestamp = this.maxAge > 0 ? Date.now() : undefined;

    if (node) {
      node.value = value;
      node.timestamp = timestamp;
      this.moveToHead(node);
    } else {
      node = {
        key,
        value,
        timestamp,
        prev: null,
        next: null
      };

      this.cache.set(key, node);
      this.addToHead(node);

      if (this.cache.size > this.maxSize) {
        this.removeTail();
      }
    }
  }

  /**
   * 检查键是否存在
   * @param key - 缓存   * @returns 是否存在
   */
  has(key: K): boolean {
    const node = this.cache.get(key);
    if (!node) return false;

    if (this.isExpired(node)) {
      this.delete(key);
      return false;
    }

    return true;
  }

  /**
   * 删除缓存
   * @param key - 缓存   * @returns 是否删除成功
   */
  delete(key: K): boolean {
    const node = this.cache.get(key);
    if (!node) return false;

    this.removeNode(node);
    this.cache.delete(key);
    return true;
  }

  /**
   * 清空缓存
   */
  clear(): void {
    this.cache.clear();
    this.head = null;
    this.tail = null;
  }

  /**
   * 获取缓存大小
   */
  get size(): number {
    return this.cache.size;
  }

  /**
   * 获取所有键
   */
  keys(): K[] {
    return Array.from(this.cache.keys());
  }

  /**
   * 获取所有
 */
  values(): V[] {
    const values: V[] = [];
    let node = this.head;
    while (node) {
      if (!this.isExpired(node)) {
        values.push(node.value);
      }
      node = node.next;
    }
    return values;
  }

  /**
   * 检查节点是否过
 */
  private isExpired(node: LRUCacheNode<K, V>): boolean {
    if (this.maxAge <= 0) return false;
    if (!node.timestamp) return false;
    return Date.now() - node.timestamp > this.maxAge;
  }

  /**
   * 将节点移到头
 */
  private moveToHead(node: LRUCacheNode<K, V>): void {
    this.removeNode(node);
    this.addToHead(node);
  }

  /**
   * 将节点添加到头部
   */
  private addToHead(node: LRUCacheNode<K, V>): void {
    node.prev = null;
    node.next = this.head;

    if (this.head) {
      this.head.prev = node;
    }
    this.head = node;

    if (!this.tail) {
      this.tail = node;
    }
  }

  /**
   * 移除节点
   */
  private removeNode(node: LRUCacheNode<K, V>): void {
    if (node.prev) {
      node.prev.next = node.next;
    } else {
      this.head = node.next;
    }

    if (node.next) {
      node.next.prev = node.prev;
    } else {
      this.tail = node.prev;
    }
  }

  /**
   * 移除尾部节点
   */
  private removeTail(): void {
    if (!this.tail) return;

    const key = this.tail.key;
    this.removeNode(this.tail);
    this.cache.delete(key);
  }
}

/**
 * 创建LRU缓存实例
 * @param options - 缓存配置
 * @returns LRU缓存实例
 */
export function createLRUCache<K = string, V = unknown>(
  options: LRUCacheOptions
): LRUCache<K, V> {
  return new LRUCache<K, V>(options);
}

/**
 * 默认缓存配置
 */
export const DEFAULT_CACHE_OPTIONS: LRUCacheOptions = {
  maxSize: 50,
  maxAge: 30 * 60 * 1000
};
