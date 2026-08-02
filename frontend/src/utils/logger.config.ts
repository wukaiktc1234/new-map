/**
 * 日志系统配置
 * 提供日志系统的配置选项和常量
 */

// 日志级别枚举
/* eslint-disable no-unused-vars
 */
export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
}
/* eslint-enable no-unused-vars
 */

// 日志配置接口
export interface LogConfig {
  level: LogLevel;
  enableConsole: boolean;
  enableServer: boolean;
  enablePerformance: boolean;
  enableUserAction: boolean;
  enableNavigation: boolean;
  serverEndpoint: string;
  maxLogSize: number;
  batchSize: number;
  flushInterval: number;
}

// 默认日志配置
export const DEFAULT_LOG_CONFIG: LogConfig = {
  level: LogLevel.INFO,
  enableConsole: true,
  enableServer: false,
  enablePerformance: true,
  enableUserAction: true,
  enableNavigation: true,
  serverEndpoint: "/api/logs",
  maxLogSize: 1000,
  batchSize: 50,
  flushInterval: 5000,
};

// 日志级别名称映射
export const LOG_LEVEL_NAMES = {
  [LogLevel.DEBUG]: "DEBUG",
  [LogLevel.INFO]: "INFO",
  [LogLevel.WARN]: "WARN",
  [LogLevel.ERROR]: "ERROR",
};

// 日志类型常量
export const LOG_TYPES = {
  BUSINESS: "BUSINESS",
  SYSTEM: "SYSTEM",
  API: "API",
  PERFORMANCE: "PERFORMANCE",
  USER_ACTION: "USER_ACTION",
  NAVIGATION: "NAVIGATION",
  ERROR: "ERROR",
} as const;

// 业务日志子类型
export const BUSINESS_LOG_SUBTYPES = {
  LOGIN: "LOGIN",
  LOGOUT: "LOGOUT",
  CREATE: "CREATE",
  UPDATE: "UPDATE",
  DELETE: "DELETE",
  SEARCH: "SEARCH",
  EXPORT: "EXPORT",
  IMPORT: "IMPORT",
} as const;

// 系统日志子类型
export const SYSTEM_LOG_SUBTYPES = {
  STARTUP: "STARTUP",
  SHUTDOWN: "SHUTDOWN",
  CONFIG_CHANGE: "CONFIG_CHANGE",
  ERROR: "ERROR",
  WARNING: "WARNING",
  INFO: "INFO",
} as const;

// 性能监控阈值（毫秒）
export const PERFORMANCE_THRESHOLDS = {
  SLOW_QUERY: 100,
  SLOW_API: 500,
  SLOW_PAGE_LOAD: 2000,
  SLOW_OPERATION: 1000,
} as const;

// 错误类型常量
export const ERROR_TYPES = {
  VALIDATION: "VALIDATION",
  BUSINESS: "BUSINESS",
  SYSTEM: "SYSTEM",
  NETWORK: "NETWORK",
  AUTH: "AUTH",
  PERMISSION: "PERMISSION",
} as const;

// 环境配置
export const ENV_CONFIG = {
  development: {
    level: LogLevel.DEBUG,
    enableConsole: true,
    enableServer: false,
  },
  production: {
    level: LogLevel.INFO,
    enableConsole: false,
    enableServer: true,
  },
  test: {
    level: LogLevel.ERROR,
    enableConsole: true,
    enableServer: false,
  },
};
