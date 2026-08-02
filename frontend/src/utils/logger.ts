/**
 * 日志工具
 * 提供统一的日志记录接口，支持多种日志级别和输出方式
 */

import {
  DEFAULT_LOG_CONFIG,
  LOG_LEVEL_NAMES,
  LogLevel,
  ENV_CONFIG,
} from "./logger.config";

export { LogLevel };

export interface LogContext {
  userId?: string;
  requestId?: string;
  timestamp: number;
  level: LogLevel;
  category: string;
  message: string;
  data?: unknown;
  error?: Error;
}

class LogUtil {
  private static instance: LogUtil;
  private logLevel: LogLevel = LogLevel.INFO;
  private enableConsoleLog: boolean = true;
  private enableServerLog: boolean = false;
  private requestId: string = "";
  private userId: string = "";

  private constructor() {
    // 根据环境变量设置日志级别
    const env = import.meta.env.MODE || "development";
    const envConfig = ENV_CONFIG[env as keyof typeof ENV_CONFIG];
    if (envConfig) {
      this.logLevel = envConfig.level;
    }
  }

  public static getInstance(): LogUtil {
    if (!LogUtil.instance) {
      LogUtil.instance = new LogUtil();
    }
    return LogUtil.instance;
  }

  /**
   * 设置日志级别
   */
  public setLogLevel(level: LogLevel): void {
    this.logLevel = level;
  }

  /**
   * 设置是否启用控制台日志
   */
  public setEnableConsoleLog(enable: boolean): void {
    this.enableConsoleLog = enable;
  }

  /**
   * 设置是否启用服务端日志
   */
  public setEnableServerLog(enable: boolean): void {
    this.enableServerLog = enable;
  }

  /**
   * 设置请求ID
   */
  public setRequestId(requestId: string): void {
    this.requestId = requestId;
  }

  /**
   * 设置用户ID
   */
  public setUserId(userId: string): void {
    this.userId = userId;
  }

  /**
   * 生成请求ID
   */
  public generateRequestId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }

  /**
   * 记录调试日志
   */
  public debug(category: string, message: string, data?: unknown): void {
    this.log(LogLevel.DEBUG, category, message, data);
  }

  /**
   * 记录信息日志
   */
  public info(category: string, message: string, data?: unknown): void {
    this.log(LogLevel.INFO, category, message, data);
  }

  /**
   * 记录警告日志
   */
  public warn(
    category: string,
    message: string,
    data?: unknown,
    error?: Error,
  ): void {
    this.log(LogLevel.WARN, category, message, data, error);
  }

  /**
   * 记录错误日志
   */
  public error(
    category: string,
    message: string,
    error?: Error,
    data?: unknown,
  ): void {
    this.log(LogLevel.ERROR, category, message, data, error);
  }

  /**
   * 记录业务操作日志
   */
  public logBusiness(operation: string, description: string, data?: unknown): void {
    this.info("BUSINESS", `[BUSINESS] ${operation} - ${description}`, data);
  }

  /**
   * 记录业务操作成功日志
   */
  public logBusinessSuccess(
    operation: string,
    description: string,
    data?: unknown,
  ): void {
    this.info(
      "BUSINESS",
      `[BUSINESS] [成功] ${operation} - ${description}`,
      data,
    );
  }

  /**
   * 记录业务操作失败日志
   */
  public logBusinessError(
    operation: string,
    description: string,
    error: Error,
    data?: unknown,
  ): void {
    this.error(
      "BUSINESS",
      `[BUSINESS] [失败] ${operation} - ${description}`,
      error,
      data,
    );
  }

  /**
   * 业务日志快捷方法
   */
  public business(operation: string, description: string, data?: unknown): void {
    this.logBusiness(operation, description, data);
  }

  /**
   * 业务成功日志快捷方法
   */
  public businessSuccess(
    operation: string,
    description: string,
    data?: unknown,
  ): void {
    this.logBusinessSuccess(operation, description, data);
  }

  /**
   * 业务失败日志快捷方法
   */
  public businessError(
    operation: string,
    description: string,
    error: Error,
    data?: unknown,
  ): void {
    this.logBusinessError(operation, description, error, data);
  }

  /**
   * 记录系统操作日志
   */
  public logSystem(operation: string, description: string, data?: unknown): void {
    this.info("SYSTEM", `[SYSTEM] ${operation} - ${description}`, data);
  }

  /**
   * 记录系统错误日志
   */
  public logSystemError(
    operation: string,
    description: string,
    error: Error,
    data?: unknown,
  ): void {
    this.error(
      "SYSTEM",
      `[SYSTEM] [错误] ${operation} - ${description}`,
      error,
      data,
    );
  }

  /**
   * 系统日志快捷方法
   */
  public system(operation: string, description: string, data?: unknown): void {
    this.logSystem(operation, description, data);
  }

  /**
   * 记录安全相关日志
   */
  public logSecurity(
    operation: string,
    description: string,
    userId?: string,
    data?: unknown,
  ): void {
    this.warn(
      "SECURITY",
      `[SECURITY] ${operation} - ${description} - 用户: ${userId || this.userId || "未知"}`,
      data,
    );
  }

  /**
   * 记录安全警告日志
   */
  public logSecurityWarning(
    operation: string,
    description: string,
    warning: string,
    userId?: string,
    data?: unknown,
  ): void {
    this.warn(
      "SECURITY",
      `[SECURITY] [警告] ${operation} - ${description} - 用户: ${userId || this.userId || "未知"} - 警告: ${warning}`,
      data,
    );
  }

  /**
   * 记录性能相关日志
   */
  public logPerformance(
    operation: string,
    startTime: number,
    data?: unknown,
  ): void {
    const duration = Date.now() - startTime;
    this.info(
      "PERFORMANCE",
      `[PERFORMANCE] ${operation} - 耗时: ${duration}ms`,
      { duration, ...data as Record<string, unknown> },
    );
  }

  /**
   * 记录慢查询日志
   */
  public logSlowQuery(
    operation: string,
    startTime: number,
    threshold: number,
    data?: unknown,
  ): void {
    const duration = Date.now() - startTime;
    if (duration > threshold) {
      this.warn(
        "SLOW_QUERY",
        `[SLOW_QUERY] ${operation} - 耗时: ${duration}ms (阈值: ${threshold}ms)`,
        { duration, threshold, ...data as Record<string, unknown> },
      );
    }
  }

  /**
   * 记录API调用日志
   */
  public logApi(
    method: string,
    uri: string,
    description: string,
    data?: unknown,
  ): void {
    this.info("API", `[API] ${method} ${uri} - ${description}`, data);
  }

  /**
   * 记录API错误日志
   */
  public logApiError(
    method: string,
    uri: string,
    error: string,
    requestId?: string,
    data?: unknown,
  ): void {
    this.error(
      "API",
      `[API] [错误] ${method} ${uri} - 错误: ${error}`,
      new Error(error),
      { requestId: requestId || this.requestId, ...data as Record<string, unknown> },
    );
  }

  /**
   * API日志快捷方法
   */
  public api(
    method: string,
    uri: string,
    description: string,
    data?: unknown,
  ): void {
    this.logApi(method, uri, description, data);
  }

  /**
   * 记录用户行为日志
   */
  public logUserAction(action: string, description: string, data?: unknown): void {
    this.info("USER_ACTION", `[USER_ACTION] ${action} - ${description}`, data);
  }

  /**
   * 记录导航日志
   */
  public logNavigation(from: string, to: string, data?: unknown): void {
    this.info("NAVIGATION", `[NAVIGATION] ${from} -> ${to}`, data);
  }

  /**
   * 记录表单验证错误
   */
  public logValidationError(formName: string, errors: unknown, data?: unknown): void {
    this.warn("VALIDATION", `[VALIDATION] ${formName} 表单验证失败`, {
      errors,
      ...data as Record<string, unknown>,
    });
  }

  /**
   * 记录数据库操作日志
   */
  public logDatabase(
    operation: string,
    table: string,
    description: string,
    data?: unknown,
  ): void {
    this.info(
      "DATABASE",
      `[DATABASE] ${operation} ${table} - ${description}`,
      data,
    );
  }

  /**
   * 记录数据库错误日志
   */
  public logDatabaseError(
    operation: string,
    table: string,
    error: Error,
    data?: unknown,
  ): void {
    this.error(
      "DATABASE",
      `[DATABASE] [错误] ${operation} ${table}`,
      error,
      data,
    );
  }

  /**
   * 核心日志方法
   */
  private log(
    level: LogLevel,
    category: string,
    message: string,
    data?: unknown,
    error?: Error,
  ): void {
    if (level < this.logLevel) {
      return;
    }

    const context: LogContext = {
      timestamp: Date.now(),
      level,
      category,
      message,
      data,
      error,
      userId: this.userId,
      requestId: this.requestId,
    };

    if (this.enableConsoleLog) {
      this.outputToConsole(context);
    }

    if (this.enableServerLog) {
      this.sendToServer(context);
    }
  }

  /**
   * 输出到控制台
   */
  private outputToConsole(context: LogContext): void {
    if (!context) return;

    const { level, category, message, data, error } = context;
    const levelName = LOG_LEVEL_NAMES[level] || "UNKNOWN";
    const time = new Date(context.timestamp || Date.now()).toLocaleString();

    let consoleMessage = `[${time}] [${levelName}] [${category}] ${message}`;

    if (context.userId || context.requestId) {
      consoleMessage += ` (用户: ${context.userId || "未知"}, 请求: ${context.requestId || "未知"})`;
    }

    let safeData = data;
    try {
      if (data) {
        if (data instanceof Error) {
          safeData = {
            name: data.name,
            message: data.message,
            stack: data.stack,
          };
        } else if (typeof data === 'object' && data !== null) {
          safeData = data;
        }
      }
    } catch (_e) {
      safeData = "[Data non-serializable]";
    }

    switch (level) {
      case LogLevel.DEBUG:
        break;
      case LogLevel.INFO:
        break;
      case LogLevel.WARN:
        console.warn(consoleMessage, safeData || '', error || '');
        break;
      case LogLevel.ERROR:
        console.error(consoleMessage, safeData || '', error || '');
        break;
      default:
        break;
    }
  }

  /**
   * 清理对象中的不可序列化属性
   */
  private sanitizeObject(obj: unknown): unknown {
    if (obj === null || typeof obj !== 'object') {
      return obj;
    }

    // 处理数组
    if (Array.isArray(obj)) {
      return obj.map(item => this.sanitizeObject(item));
    }

    // 处理日期对象
    if (obj instanceof Date) {
      return obj.toISOString();
    }

    // 处理特殊对象类型
    if (
      obj.constructor &&
      typeof (obj as object).constructor.name === 'string' &&
      [(obj as object).constructor.name === 'AbortSignal' ||
       (obj as object).constructor.name === 'Headers' ||
       (obj as object).constructor.name === 'FormData'].some(Boolean)
    ) {
      return `[${(obj as object).constructor.name}]`;
    }

    // 处理普通对象
    const sanitized: Record<string, unknown> = {};
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        try {
          if (key === 'signal' && (obj as Record<string, unknown>)[key] instanceof AbortSignal) {
            sanitized[key] = '[AbortSignal]';
          } else if (key === 'headers' && typeof (obj as Record<string, unknown>)[key] === 'object') {
            // 特殊处理headers对象
            sanitized[key] = this.sanitizeHeaders((obj as Record<string, unknown>)[key]);
          } else {
            sanitized[key] = this.sanitizeObject((obj as Record<string, unknown>)[key]);
          }
        } catch (_e) {
          sanitized[key] = '[Value not serializable]';
        }
      }
    }
    return sanitized;
  }

  /**
   * 清理headers对象
   */
  private sanitizeHeaders(headers: unknown): unknown {
    if (!headers) return {};

    try {
      if (typeof headers === 'object' && headers !== null) {
        const headersObj = headers as Record<string, unknown>;

        if (typeof headersObj.entries === 'function') {
          // 处理Headers对象
          const sanitized: Record<string, unknown> = {};
          for (const [key, value] of headersObj.entries() as Iterable<[string, unknown]>) {
            sanitized[key] = value;
          }
          return sanitized;
        } else {
          // 处理普通对象形式的headers
          return this.sanitizeObject(headers);
        }
      }
    } catch (_e) {
      return '[Headers not serializable]';
    }
    return {};
  }

  /**
   * 发送日志到服务端
   * 日志上报使用原生fetch，避免request拦截器循环调用：
   * request.ts 依赖 logger（拦截器中调用 logger.api/error/warn），
   * 若 logger 再调用 request 会导致循环依赖
   * （request拦截器中调用logger -> logger调用request -> 触发拦截器 -> 调用logger...）
   * 这是唯一允许使用原生fetch的例外场景
   */
  private sendToServer(context: LogContext): void {
    if (this.enableServerLog && DEFAULT_LOG_CONFIG.serverEndpoint) {
      // eslint-disable-next-line no-restricted-syntax -- 日志上报使用原生fetch，避免request拦截器循环调用
      fetch(DEFAULT_LOG_CONFIG.serverEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(context),
      }).catch((_err) => {
        // 如果发送到服务器失败，静默处理
      });
    }
  }
}

// 创建单例实例
const logger = LogUtil.getInstance();

// 导出便捷方法
const {
  debug,
  info,
  warn,
  error,
  logBusiness,
  logBusinessSuccess,
  logBusinessError,
  business,
  businessSuccess,
  businessError,
  logSystem,
  logSystemError,
  logSecurity,
  logSecurityWarning,
  logPerformance,
  logSlowQuery,
  logApi,
  logApiError,
  logUserAction,
  logNavigation,
  logValidationError,
  logDatabase,
  logDatabaseError,
  setLogLevel,
  setEnableConsoleLog,
  setEnableServerLog,
  setRequestId,
  setUserId,
  generateRequestId,
} = logger;

export {
  debug,
  info,
  warn,
  error,
  logBusiness,
  logBusinessSuccess,
  logBusinessError,
  business,
  businessSuccess,
  businessError,
  logSystem,
  logSystemError,
  logSecurity,
  logSecurityWarning,
  logPerformance,
  logSlowQuery,
  logApi,
  logApiError,
  logUserAction,
  logNavigation,
  logValidationError,
  logDatabase,
  logDatabaseError,
  setLogLevel,
  setEnableConsoleLog,
  setEnableServerLog,
  setRequestId,
  setUserId,
  generateRequestId,
};

export { LogUtil };
export default logger;
