/**
 * 模拟网络延迟工具
 * 随机延迟 80-400ms，模拟真实API响应时间
 */
export function mockDelay(min = 80, max = 400): Promise<void> {
  const delay = Math.floor(Math.random() * (max - min + 1)) + min
  return new Promise(resolve => setTimeout(resolve, delay))
}

export function mockLightDelay(): Promise<void> {
  return mockDelay(30, 120)
}