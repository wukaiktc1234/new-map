// 数据缓存工具
class DataCache {

  private readonly CACHE_PREFIX = 'food_traceability_';

  // 获取缓存数据
  get<T>(key: string, level: 'memory' | 'session' | 'local' = 'memory'): T | null {
    switch (level) {
      case 'memory':
        // 从Pinia store获取 - 暂时返回null，需要结合具体store实现
        return this.getFromMemory(key);
      case 'session':
        return this.getFromSession(key);
      case 'local':
        return this.getFromLocal(key);
      default:
        return null;
    }
  }

  // 设置缓存数据
  set<T>(key: string, data: T, level: 'memory' | 'session' | 'local' = 'memory', expiresIn?: number): void {
    switch (level) {
      case 'memory':
        // 设置到Pinia store - 暂时不实现，需要结合具体store
        this.setToMemory(key, data);
        break;
      case 'session':
        this.setToSession(key, data, expiresIn);
        break;
      case 'local':
        this.setToLocal(key, data, expiresIn);
        break;
    }
  }

  // 从Session Storage获取
  private getFromSession<T>(key: string): T | null {
    const fullKey = this.CACHE_PREFIX + key;
    const item = sessionStorage.getItem(fullKey);
    if (!item) return null;

    try {
      const parsed = JSON.parse(item);
      // 检查是否过期
      if (parsed.expires && Date.now() > parsed.expires) {
        sessionStorage.removeItem(fullKey);
        return null;
      }
      return parsed.data as T;
    } catch (error) {
      return null;
    }
  }

  // 设置到Session Storage
  private setToSession<T>(key: string, data: T, expiresIn?: number): void {
    const fullKey = this.CACHE_PREFIX + key;
    const item = {
      data,
      expires: expiresIn ? Date.now() + expiresIn : undefined
    };
    sessionStorage.setItem(fullKey, JSON.stringify(item));
  }

  // 从Local Storage获取
  private getFromLocal<T>(key: string): T | null {
    const fullKey = this.CACHE_PREFIX + key;
    const item = localStorage.getItem(fullKey);
    if (!item) return null;

    try {
      const parsed = JSON.parse(item);
      // 检查是否过期
      if (parsed.expires && Date.now() > parsed.expires) {
        localStorage.removeItem(fullKey);
        return null;
      }
      return parsed.data as T;
    } catch (error) {
      return null;
    }
  }

  // 设置到Local Storage
  private setToLocal<T>(key: string, data: T, expiresIn?: number): void {
    const fullKey = this.CACHE_PREFIX + key;
    const item = {
      data,
      expires: expiresIn ? Date.now() + expiresIn : undefined
    };
    localStorage.setItem(fullKey, JSON.stringify(item));
  }

  // 从内存获取（Pinia store）
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private getFromMemory<T>(_key: string): T | null {
    // 实现从Pinia store获取数据的逻辑
    // 这里需要根据具体的store结构实现
    // 参数 _key 暂时未使用，等待后续实现
    return null;
  }

  // 设置到内存（Pinia store）
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private setToMemory<T>(_key: string, _data: T): void {
    // 实现设置到Pinia store的逻辑
    // 这里需要根据具体的store结构实现
    // 参数 _key _data 暂时未使用，等待后续实现
  }

  // 清除缓存
  clear(key?: string, level?: 'memory' | 'session' | 'local'): void {
    if (key) {
      const fullKey = this.CACHE_PREFIX + key;
      if (!level || level === 'session') {
        sessionStorage.removeItem(fullKey);
      }
      if (!level || level === 'local') {
        localStorage.removeItem(fullKey);
      }
      if (!level || level === 'memory') {
        // 清除内存缓存 - 暂时不实现
      }
    } else {
      if (!level || level === 'session') {
        // 清除所有Session Storage中以前缀开头的缓存
        Object.keys(sessionStorage).forEach(k => {
          if (k.startsWith(this.CACHE_PREFIX)) {
            sessionStorage.removeItem(k);
          }
        });
      }
      if (!level || level === 'local') {
        // 清除所有Local Storage中以前缀开头的缓存
        Object.keys(localStorage).forEach(k => {
          if (k.startsWith(this.CACHE_PREFIX)) {
            localStorage.removeItem(k);
          }
        });
      }
      if (!level || level === 'memory') {
        // 清除所有内存缓存 - 暂时不实现
      }
    }
  }
}

export const dataCache = new DataCache();
