/**
 * 财务模块统一错误处理
 * @module utils/finance-error-handler
 */

import { ElMessage } from 'element-plus';
import logger from './logger';

/**
 * 错误类型枚举
 */
export enum FinanceErrorType {
  NETWORK_ERROR = 'NETWORK_ERROR',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  BUSINESS_ERROR = 'BUSINESS_ERROR',
  AUTH_ERROR = 'AUTH_ERROR',
  PERMISSION_ERROR = 'PERMISSION_ERROR',
  NOT_FOUND_ERROR = 'NOT_FOUND_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

/**
 * 财务错误
 */
export class FinanceError extends Error {
  type: FinanceErrorType;
  code?: string;
  details?: Record<string, unknown>;

  constructor(
    message: string,
    type: FinanceErrorType = FinanceErrorType.UNKNOWN_ERROR,
    code?: string,
    details?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'FinanceError';
    this.type = type;
    this.code = code;
    this.details = details;
  }
}

/**
 * 敏感字段配置
 */
const SENSITIVE_FIELDS = [
  'password',
  'pwd',
  'token',
  'secret',
  'apiKey',
  'api_key',
  'accessToken',
  'access_token',
  'refreshToken',
  'refresh_token',
  'idCard',
  'id_card',
  'phone',
  'mobile',
  'email',
  'bankAccount',
  'bank_account',
  'cardNumber',
  'card_number',
  'cvv',
  'amount',
  'balance',
  'salary'
] as const;

/**
 * 敏感信息脱敏
 */
export class SensitiveDataMasker {
  private static readonly MASK_CHAR = '*';

  /**
   * 脱敏手机   * @param phone - 手机   * @returns 脱敏后的手机
 */
  static maskPhone(phone: string): string {
    if (!phone || phone.length < 7) return phone;
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
  }

  /**
   * 脱敏邮箱
   * @param email - 邮箱
   * @returns 脱敏后的邮箱
   */
  static maskEmail(email: string): string {
    if (!email || !email.includes('@')) return email;
    const [localPart, domain] = email.split('@');
    const maskedLocal = localPart.length > 2
      ? localPart[0] + this.MASK_CHAR.repeat(localPart.length - 2) + localPart[localPart.length - 1]
      : this.MASK_CHAR.repeat(localPart.length);
    return `${maskedLocal}@${domain}`;
  }

  /**
   * 脱敏身份证号
   * @param idCard - 身份证号
   * @returns 脱敏后的身份证号
   */
  static maskIdCard(idCard: string): string {
    if (!idCard || idCard.length < 8) return idCard;
    return idCard.replace(/(.{4}).*(.{4})/, `$1${this.MASK_CHAR.repeat(idCard.length - 8)}$2`);
  }

  /**
   * 脱敏银行卡号
   * @param cardNumber - 银行卡号
   * @returns 脱敏后的银行卡号
   */
  static maskBankCard(cardNumber: string): string {
    if (!cardNumber || cardNumber.length < 8) return cardNumber;
    return cardNumber.replace(/(.{4}).*(.{4})/, `$1${this.MASK_CHAR.repeat(cardNumber.length - 8)}$2`);
  }

  /**
   * 脱敏金额（保留前两位   * @param amount - 金额
   * @returns 脱敏后的金额
   */
  static maskAmount(amount: string | number): string {
    const str = String(amount);
    if (!str || str.length < 3) return '***';
    return str[0] + this.MASK_CHAR.repeat(str.length - 1);
  }

  /**
   * 完全脱敏
   * @param value -    * @returns 脱敏后的
 */
  static maskFully(value: string): string {
    if (!value) return value;
    return this.MASK_CHAR.repeat(Math.min(value.length, 8));
  }

  /**
   * 根据字段名自动选择脱敏方式
   * @param fieldName - 字段   * @param value -    * @returns 脱敏后的
 */
static maskByFieldName(fieldName: string, value: unknown):
unknown {
    if (value === null || value === undefined) return value;
    
    const lowerFieldName = fieldName.toLowerCase();
    const strValue = String(value);

    if (lowerFieldName.includes('phone') || lowerFieldName.includes('mobile')) {
      return this.maskPhone(strValue);
    }
    if (lowerFieldName.includes('email')) {
      return this.maskEmail(strValue);
    }
    if (lowerFieldName.includes('idcard') || lowerFieldName.includes('id_card')) {
      return this.maskIdCard(strValue);
    }
    if (lowerFieldName.includes('bank') || lowerFieldName.includes('card')) {
      return this.maskBankCard(strValue);
    }
    if (lowerFieldName.includes('amount') || lowerFieldName.includes('balance') || lowerFieldName.includes('salary')) {
      return this.maskAmount(strValue);
    }
    if (lowerFieldName.includes('password') || lowerFieldName.includes('token') || lowerFieldName.includes('secret')) {
      return this.maskFully(strValue);
    }

    return value;
  }

  /**
   * 脱敏对象中的敏感字段
   * @param obj - 原始对象
   * @returns 脱敏后的对象副本
   */
  static maskObject<T extends Record<string, unknown>>(obj: T): Record<string, unknown> {
    if (!obj || typeof obj !== 'object') return obj;

    const masked: Record<string, unknown> = {};
    
    for (const [key, value] of Object.entries(obj)) {
      const lowerKey = key.toLowerCase();
      const isSensitive = SENSITIVE_FIELDS.some(field => lowerKey.includes(field.toLowerCase()));

      if (isSensitive) {
        masked[key] = this.maskByFieldName(key, value);
      } else if (value && typeof value === 'object' && !Array.isArray(value)) {
        masked[key] = this.maskObject(value as Record<string, unknown>);
      } else if (Array.isArray(value)) {
        masked[key] = value.map(item => 
          item && typeof item === 'object' ? this.maskObject(item as Record<string, unknown>) 
            : item
        );
      } else {
        masked[key] = value;
      }
    }

    return masked;
  }
}

/**
 * 错误消息映射
 */
const ERROR_MESSAGES: Record<FinanceErrorType, string> = {
  [FinanceErrorType.NETWORK_ERROR]: '网络连接失败，请检查网络设',
  [FinanceErrorType.VALIDATION_ERROR]: '数据验证失败',
  [FinanceErrorType.BUSINESS_ERROR]: '业务处理失败',
  [FinanceErrorType.AUTH_ERROR]: '认证失败，请重新登录',
  [FinanceErrorType.PERMISSION_ERROR]: '权限不足，无法执行此操作',
  [FinanceErrorType.NOT_FOUND_ERROR]: '请求的资源不存在',
  [FinanceErrorType.TIMEOUT_ERROR]: '请求超时，请稍后重试',
  [FinanceErrorType.UNKNOWN_ERROR]: '未知错误，请联系管理员'
};

/**
 * 根据HTTP状态码获取错误类型
 * @param statusCode - HTTP状态码
 * @returns 错误类型
 */
function getErrorTypeByStatusCode(statusCode: number): FinanceErrorType {
  const mapping: Record<number, FinanceErrorType> = {
    400: FinanceErrorType.VALIDATION_ERROR,
    401: FinanceErrorType.AUTH_ERROR,
    403: FinanceErrorType.PERMISSION_ERROR,
    404: FinanceErrorType.NOT_FOUND_ERROR,
    408: FinanceErrorType.TIMEOUT_ERROR,
    500: FinanceErrorType.BUSINESS_ERROR,
    502: FinanceErrorType.NETWORK_ERROR,
    503: FinanceErrorType.NETWORK_ERROR,
    504: FinanceErrorType.TIMEOUT_ERROR
  };
  return mapping[statusCode] || FinanceErrorType.UNKNOWN_ERROR;
}

/**
 * 解析错误对象
 * @param error - 原始错误对象
 * @returns 财务错误对象
 */
export function parseError(error: unknown): FinanceError {
  if (error instanceof FinanceError) {
    return error;
  }

  if (error instanceof Error) {
    if (error.name === 'AxiosError') {
const axiosError = error as Error & { response?: {
status?: number;
data?: {
message?: string } } };
      const statusCode = axiosError.response?.status;
      const message = axiosError.response?.data?.message || error.message;
      return new FinanceError(
        message,
        getErrorTypeByStatusCode(statusCode || 0),
        statusCode?.toString()
      );
    }

    return new FinanceError(error.message, FinanceErrorType.UNKNOWN_ERROR);
  }

  if (typeof error === 'string') {
    return new FinanceError(error, FinanceErrorType.UNKNOWN_ERROR);
  }

  return new FinanceError('未知错误', FinanceErrorType.UNKNOWN_ERROR);
}

/**
 * 错误处理器配
 */
interface ErrorHandlerConfig {
  showMessage?: boolean;
  logError?: boolean;
  reportError?: boolean;
}

/**
 * 统一错误处理
 */
export class FinanceErrorHandler {
  private static defaultConfig: ErrorHandlerConfig = {
    showMessage: true,
    logError: true,
    reportError: false
  };

  /**
   * 处理错误
   * @param error - 错误对象
   * @param context - 错误上下   * @param config - 处理配置
   * @returns 格式化的错误结果
   */
  static handle(
    error: unknown,
    context: string,
    config: ErrorHandlerConfig = {}
  ): { success: false; message: string; error: FinanceError } {
    const finalConfig = { ...this.defaultConfig, ...config };
    const financeError = parseError(error);
    const message = financeError.message || ERROR_MESSAGES[financeError.type];

    if (finalConfig.logError) {
      const maskedDetails = financeError.details
        ? SensitiveDataMasker.maskObject(financeError.details)
        : undefined;
      logger.error(
        'FinanceErrorHandler',
        `[FinanceError][${context}]`,
        new Error(financeError.message),
        {
          type: financeError.type,
          code: financeError.code,
          details: maskedDetails,
        }
      );
    }

    if (finalConfig.showMessage) {
      this.showErrorMessage(financeError);
    }

    if (finalConfig.reportError && import.meta.env.PROD) {
      this.reportError(financeError, context);
    }

    return {
      success: false,
      message,
      error: financeError
    };
  }

  /**
   * 显示错误消息
   */
  private static showErrorMessage(error: FinanceError): void {
    const message = error.message || ERROR_MESSAGES[error.type];
    
    switch (error.type) {
      case FinanceErrorType.AUTH_ERROR:
        ElMessage.error({ message, duration: 5000 });
        break;
      case FinanceErrorType.NETWORK_ERROR:
        ElMessage.warning({ message, duration: 4000 });
        break;
      default:
        ElMessage.error(message);
    }
  }

  /**
   * 上报错误到监控系
 */
private static reportError(error: FinanceError, context: string):
void {
    const maskedDetails = error.details 
      ? SensitiveDataMasker.maskObject(error.details) 
      : undefined;
    logger.error('ErrorReport', context, undefined, {
      error: {
        type: error.type,
        message: error.message,
        code: error.code,
        details: maskedDetails
      },
      timestamp: new Date().toISOString()
    });
  }

  /**
   * 创建验证错误
   */
static createValidationError(message: string, details?: Record<string, unknown>):
FinanceError {
    return new FinanceError(message, FinanceErrorType.VALIDATION_ERROR, undefined, details);
  }

  /**
   * 创建业务错误
   */
static createBusinessError(message: string, code?: string):
FinanceError {
    return new FinanceError(message, FinanceErrorType.BUSINESS_ERROR, code);
  }

  /**
   * 判断是否为网络错
 */
  static isNetworkError(error: unknown): boolean {
    return parseError(error).type === FinanceErrorType.NETWORK_ERROR;
  }

  /**
   * 判断是否为认证错
 */
  static isAuthError(error: unknown): boolean {
    return parseError(error).type === FinanceErrorType.AUTH_ERROR;
  }

  /**
   * 判断是否为权限错
 */
  static isPermissionError(error: unknown): boolean {
    return parseError(error).type === FinanceErrorType.PERMISSION_ERROR;
  }
}

/**
 * 快捷错误处理函数
 */
export const handleError = FinanceErrorHandler.handle.bind(FinanceErrorHandler);

/**
 * API调用包装器，自动处理错误
 * @param fn - API调用函数
 * @param context - 错误上下 * @returns 包装后的函数
 */
export function withErrorHandling<T>(
  fn: () => Promise<T>,
  context: string
): Promise<{ success: true;
data: T } | {
success: false;
message: string }> {
  return fn()
    .then(data => ({ success: true as const, data }))
    .catch(error => handleError(error, context, { showMessage: true }));
}

/**
 * 重试包装 * @param fn - 需要重试的函数
 * @param maxRetries - 最大重试次 * @param delay - 重试延迟（毫秒）
 * @returns 包装后的函数
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: Error | null = null;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));

      if (i < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i)));
      }
    }
  }

  throw lastError;
}
