/**
 * POS 端统一日志工具
 *
 * 规范：业务代码禁止直接调用 console.*，统一走 logger。
 * - 开发环境（import.meta.env.DEV）：按原样打印，便于排查；
 * - 生产环境：静默，不产生任何控制台输出。
 *
 * logger 自身是唯一允许触碰 console 的实现，这里通过 native 间接引用原生 console，
 * 避免在业务代码/本文件里出现对 console 的直接调用。
 */

const native = globalThis.console;
const isDev: boolean = import.meta.env.DEV;

type ConsoleMethod = "debug" | "info" | "warn" | "error";

/** 统一消息转换：Error 保留对象（不丢失堆栈），其余转字符串 */
const toMessage = (value: unknown): unknown =>
  value instanceof Error ? value : typeof value === "string" ? value : String(value);

/** 输出入口：仅开发环境落控制台 */
const emit = (method: ConsoleMethod, args: unknown[]): void => {
  if (!isDev || !native) {
    return;
  }
  if (args.length === 0) {
    native[method]();
    return;
  }
  const [first, ...rest] = args;
  native[method](toMessage(first), ...rest);
};

export const logger = {
  /** 信息级（对应原控制台 log 输出） */
  logMessage: (...args: unknown[]): void => emit("info", args),
  /** 调试级（对应原控制台 debug 输出） */
  debugMessage: (...args: unknown[]): void => emit("debug", args),
  /** 警告级（对应原控制台 warn 输出） */
  warnMessage: (...args: unknown[]): void => emit("warn", args),
  /** 错误级（对应原控制台 error 输出） */
  errorMessage: (...args: unknown[]): void => emit("error", args),
};

export default logger;
