/**
 * 财务模块公共工具函数
 * @module utils/finance-utils
 *
 * 注：金额转换（yuanToFen / fenToYuan）已统一委托给 utils/money，本文件仅保留 re-export
 * 以兼容已有调用方。新增代码请直接 import from '@/utils/money'。
 */
import { yuanToFen, fenToYuanNumber as fenToYuan } from './money'

// 重新导出，保持对外 API 兼容
export { yuanToFen, fenToYuan }

/**
 * 金额单位转换常量
 */
export const AMOUNT_UNIT = {
  YUAN_TO_FEN: 100,
  FEN_TO_YUAN: 0.01
} as const;

/**
 * 日期格式正则表达
 */
export const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
export const PAY_PERIOD_REGEX = /^\d{4}-(0[1-9]|1[0-2])$/;

/**
 * 工作流ID格式正则表达 * 格式: WF-{模块
}-{时间戳
}-{随机数
} UUID格式
 */
export const WORKFLOW_ID_REGEX = /^(WF-[A-Z]+-\d{13}-[A-Z0-9]{6}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$/i;

/**
 * UUID格式正则表达
 */
export const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * 最大日期范围（天）
 */
export const MAX_DATE_RANGE_DAYS = 365;

/**
 * HTML转义映射
 */
const HTML_ESCAPE_MAP: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#039;'
};

/**
 * HTML转义处理，防止XSS攻击
 * @param text - 需要转义的文本
 * @returns 转义后的安全文本
 * @example
 * escapeHtml('<script>alert("xss")<</script>')
 * // 返回: '&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;'
 */
export function escapeHtml(text: string | null | undefined): string {
  if (!text) return '';
  return text.replace(/[&<>"']/g, char => HTML_ESCAPE_MAP[char]);
}

// 金额转换 yuanToFen / fenToYuan 已上移至 utils/money（见文件顶部 import + re-export）

/**
 * 格式化金额显示（元格式化，保位小数，千分位）
 * @param value - 金额（元 * @returns 格式化后的金额字符串
 * @example
 * formatMoney(10050) // 返回: '10,050.00'
 * formatMoney(100.5) // 返回: '100.50'
 */
export function formatMoney(value: number): string {
  if (value === null || value === undefined) return '0.00';
  if (typeof value !== 'number' || !Number.isFinite(value)) return '0.00';
  return value.toLocaleString('zh-CN', { 
    minimumFractionDigits: 2, 
    maximumFractionDigits: 2 
  });
}

/**
 * 格式化金额显示（分转元，保留2位小数）
 * @param fen - 金额（分 * @param prefix - 前缀，默¥'
 * @returns 格式化后的金额字符串
 * @example
 * formatAmount(10050) // 返回: '¥100.50'
 */
export function formatAmount(fen: number, prefix = '¥'): string {
  return `${prefix}${formatMoney(fenToYuan(fen))}`;
}

/**
 * 验证日期格式（YYYY-MM-DD * @param date - 日期字符 * @returns 是否有效
 */
export function isValidDateFormat(date: string): boolean {
  return DATE_REGEX.test(date);
}

/**
 * 验证薪资周期格式（YYYY-MM * @param payPeriod - 薪资周期字符 * @returns 是否有效
 */
export function isValidPayPeriod(payPeriod: string): boolean {
  return PAY_PERIOD_REGEX.test(payPeriod);
}

/**
 * 验证工作流ID格式
 * @param workflowId - 工作流ID
 * @returns 是否有效
 */
export function isValidWorkflowId(workflowId: string): boolean {
  if (!workflowId || typeof workflowId !== 'string') {
    return false;
  }
  return WORKFLOW_ID_REGEX.test(workflowId.trim());
}

/**
 * 验证UUID格式
 * @param uuid - UUID字符 * @returns 是否有效
 */
export function isValidUUID(uuid: string): boolean {
  if (!uuid || typeof uuid !== 'string') {
    return false;
  }
  return UUID_REGEX.test(uuid.trim());
}

/**
 * 生成工作流ID
 * @param module - 模块名称（大写）
 * @returns 工作流ID
 */
export function generateWorkflowId(module: string): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 8).toUpperCase();
  const normalizedModule = module.toUpperCase().replace(/[^A-Z]/g, '').substring(0, 4);
  return `WF-${normalizedModule}-${timestamp}-${random}`;
}

/**
 * 解析工作流ID
 * @param workflowId - 工作流ID
 * @returns 解析结果或null
 */
export function parseWorkflowId(workflowId: string): {
  module: string;
  timestamp: number;
  random: string;
} | null {
  if (!isValidWorkflowId(workflowId)) {
    return null;
  }
  
  const match = workflowId.match(/^WF-([A-Z]+)-(\d{13})-([A-Z0-9]{6})$/i);
  if (!match) {
    return null;
  }
  
  return {
    module: match[1].toUpperCase(),
    timestamp: parseInt(match[2], 10),
    random: match[3].toUpperCase()
  };
}

/**
 * 验证日期范围
 * @param startDate - 开始日 * @param endDate - 结束日期
 * @returns 验证结果
 */
export function validateDateRange(
  startDate: Date | string,
  endDate: Date | string
): { valid: boolean; message?: string } {
  const start = typeof startDate === 'string' ? new Date(startDate) : startDate;
  const end = typeof endDate === 'string' ? new Date(endDate) : endDate;
  const now = new Date();

  if (isNaN(start.getTime()) || isNaN(end.getTime())) {
return { valid: false,
message: '无效的日期'
};
  }

  if (start > end) {
return { valid: false,
message: '开始日期不能大于结束日期'
};
  }

  if (end > now) {
return { valid: false,
message: '结束日期不能大于当前日期'
};
  }

  const daysDiff = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  if (daysDiff > MAX_DATE_RANGE_DAYS) {
return { valid: false,
message: `日期范围不能超过${MAX_DATE_RANGE_DAYS}天` };
  }

  return { valid: true };
}

/**
 * 格式化日期为字符 * @param date - 日期对象或字符串
 * @param format - 格式类型
 * @returns 格式化后的日期字符串
 */
export function formatDate(
  date: Date | string,
  format: 'date' | 'datetime' = 'date'
): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  
  if (isNaN(d.getTime())) {
    return '-';
  }

  if (format === 'date') {
    return d.toISOString().split('T')[0];
  }
  
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

/**
 * 获取当前月份的薪资周 * @returns 薪资周期字符串（YYYY-MM
 */
export function getCurrentPayPeriod(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

/**
 * 获取当前月份的第一天和最后一 * @returns 包含第一天和最后一天的日期范围
 */
export function getCurrentMonthRange(): { startDate: Date;
endDate: Date } {
  const now = new Date();
  const startDate = new Date(now.getFullYear(), now.getMonth(), 1);
  const endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { startDate, endDate };
}

/**
 * 深拷贝对 * @param obj - 需要拷贝的对象
 * @returns 拷贝后的新对
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(item => deepClone(item)) as T;
  }
  
  const cloned = {} as T;
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      cloned[key] = deepClone(obj[key]);
    }
  }
  
  return cloned;
}

/**
 * 防抖函数
 * @param fn - 需要防抖的函数
 * @param delay - 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null;
  
  return function (this: unknown, ...args: Parameters<T>) {
    if (timer) {
      clearTimeout(timer);
    }
    timer = setTimeout(() => {
      fn.apply(this, args);
      timer = null;
    }, delay);
  };
}

/**
 * 节流函数
 * @param fn - 需要节流的函数
 * @param interval - 间隔时间（毫秒）
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: unknown[]) => unknown>(
  fn: T,
  interval: number
): (...args: Parameters<T>) => void {
  let lastTime = 0;
  
  return function (this: unknown, ...args: Parameters<T>) {
    const now = Date.now();
    if (now - lastTime >= interval) {
      fn.apply(this, args);
      lastTime = now;
    }
  };
}
