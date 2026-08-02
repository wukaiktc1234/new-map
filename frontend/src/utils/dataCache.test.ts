import { describe, it, expect, beforeEach } from 'vitest';
import { dataCache } from './dataCache';

describe('数据缓存测试', () => {
  beforeEach(() => {
    // 清除所有缓
    dataCache.clear();
  });
  
  it('应该能够设置和获取Session Storage缓存', () => {
    const testKey = 'testSessionCache';
const testData = { id: 'test001',
name: '测试数据'
};
    
    dataCache.set(testKey, testData, 'session');
    const retrievedData = dataCache.get(testKey, 'session');
    
    expect(retrievedData).toEqual(testData);
  });
  
  it('应该能够设置和获取Local Storage缓存', () => {
    const testKey = 'testLocalCache';
const testData = { id: 'test001',
name: '测试数据'
};
    
    dataCache.set(testKey, testData, 'local');
    const retrievedData = dataCache.get(testKey, 'local');
    
    expect(retrievedData).toEqual(testData);
  });
  
  it('应该能够处理过期缓存', () => {
    const testKey = 'testExpiredCache';
const testData = { id: 'test001',
name: '测试数据'
};
    
    // 设置1毫秒后过期的缓存
    
    dataCache.set(testKey, testData, 'session', 1);
    
    // 等待2毫秒确保缓存过期
return new Promise(resolve => {
      setTimeout(() => {
        const retrievedData = dataCache.get(testKey, 'session');
        expect(retrievedData).toBeNull();
        resolve(true);
      }, 2);
    });
  });
  
  it('应该能够清除指定缓存', () => {
    const testKey = 'testClearCache';
const testData = { id: 'test001',
name: '测试数据'
};
    
    dataCache.set(testKey, testData, 'session');
    dataCache.clear(testKey, 'session');
    const retrievedData = dataCache.get(testKey, 'session');
    
    expect(retrievedData).toBeNull();
  });
  
  it('应该能够清除所有缓', () => {
    const testKey1 = 'testClearAll1';
    const testKey2 = 'testClearAll2';
const testData = { id: 'test001',
name: '测试数据'
};
    
    dataCache.set(testKey1, testData, 'session');
    dataCache.set(testKey2, testData, 'local');
    dataCache.clear();
    
    const retrievedData1 = dataCache.get(testKey1, 'session');
    const retrievedData2 = dataCache.get(testKey2, 'local');
    
    expect(retrievedData1).toBeNull();
    expect(retrievedData2).toBeNull();
  });
});
