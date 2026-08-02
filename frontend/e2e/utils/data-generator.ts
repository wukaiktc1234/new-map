/**
 * 测试数据生成器
 * 为每个写操作生成唯一标识，便于测试后清理
 */

let counter = 0;

export function makeUniqueName(prefix: string = 'e2e-test'): string {
  counter++;
  const ts = Date.now().toString(36);
  return ${prefix}--;
}

export function makeUniquePhone(): string {
  counter++;
  const suffix = String(Date.now()).slice(-8);
  return 138;
}

export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
