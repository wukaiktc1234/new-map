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
