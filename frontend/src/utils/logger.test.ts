/**
 * 日志系统测试
 * 用于验证日志系统的基本功能
 */

import { describe, it, expect, beforeEach } from 'vitest';
import logger from "@/utils/logger";

describe('日志系统测试', () => {
  beforeEach(() => {
    // 重置logger状态
    const logUtil = logger.constructor.prototype.constructor.getInstance();
    logUtil.setLogLevel("INFO");
    logger.setRequestId("");
    logger.setUserId("");
  });

  it('应该能够记录不同级别的日志而不抛出异常', () => {
    // 测试基本日志功能，验证它们能够正常执行而不抛出异常
    expect(() => {
      logger.debug("TEST", "这是调试日志");
      logger.info("TEST", "这是信息日志");
      logger.warn("TEST", "这是警告日志");
      logger.error("TEST", "这是错误日志", new Error("测试错误"));
    }).not.toThrow();
  });

  it('应该能够记录业务日志而不抛出异常', () => {
    // 测试业务日志，验证它们能够正常执行而不抛出异常
    expect(() => {
      logger.business("用户登录", "用户尝试登录系统");
      logger.businessSuccess("用户登录", "用户登录成功", { userId: "12345" });
      logger.businessError("用户登录", "用户登录失败", new Error("密码错误"), {
        username: "test",
      });
    }).not.toThrow();
  });

  it('应该能够记录系统日志而不抛出异常', () => {
    // 测试系统日志，验证它们能够正常执行而不抛出异常
    expect(() => {
      logger.system("系统启动", "应用系统启动完成");
      logger.logSystemError("数据库连接", "数据库连接失败", new Error("连接超时"));
    }).not.toThrow();
  });

  it('应该能够记录API日志而不抛出异常', () => {
    // 测试API日志，验证它们能够正常执行而不抛出异常
    expect(() => {
      logger.api("GET", "/api/users", "获取用户列表");
      logger.logApiError("POST", "/api/login", "登录接口错误");
    }).not.toThrow();
  });

  it('应该能够测试日志级别设置', () => {
    const logUtil = logger.constructor.prototype.constructor.getInstance();

    // 设置为DEBUG级别
    expect(() => {
      logUtil.setLogLevel("DEBUG");
      logger.debug("LEVEL_TEST", "应该显示的DEBUG日志");
      logger.info("LEVEL_TEST", "应该显示的INFO日志");
    }).not.toThrow();

    // 设置为ERROR级别
    expect(() => {
      logUtil.setLogLevel("ERROR");
      logger.debug("LEVEL_TEST", "不应该显示的DEBUG日志");
      logger.info("LEVEL_TEST", "不应该显示的INFO日志");
      logger.error("LEVEL_TEST", "应该显示的ERROR日志", new Error("测试错误"));
    }).not.toThrow();

    // 恢复为INFO级别
    expect(() => {
      logUtil.setLogLevel("INFO");
    }).not.toThrow();
  });

  it('应该能够设置和使用请求ID和用户ID', () => {
    // 设置请求ID
    const requestId = logger.generateRequestId();
    expect(requestId).toBeDefined();
    expect(requestId.length).toBeGreaterThan(0);

    // 设置用户ID
    expect(() => {
      logger.setRequestId(requestId);
      logger.setUserId("user123");
      // 记录带有上下文的日志
      logger.info("CONTEXT_TEST", "带有请求ID和用户ID的日志");
    }).not.toThrow();
  });

  it('应该能够生成唯一的请求ID', () => {
    // 生成两个请求ID并验证它们不同
    const requestId1 = logger.generateRequestId();
    const requestId2 = logger.generateRequestId();

    expect(requestId1).not.toBe(requestId2);
    expect(requestId1.length).toBeGreaterThan(0);
    expect(requestId2.length).toBeGreaterThan(0);
  });

  it('应该能够记录性能日志和慢查询日志', () => {
    // 测试性能日志，验证它们能够正常执行而不抛出异常
    expect(() => {
      const startTime = Date.now();
      logger.logPerformance("数据处理", startTime, { dataSize: 1000 });
      logger.logSlowQuery("数据库查询", startTime, 100, {
        query: "SELECT * FROM users",
      });
    }).not.toThrow();
  });

  it('应该能够记录路由导航日志', () => {
    // 测试路由导航日志，验证它们能够正常执行而不抛出异常
    expect(() => {
      logger.logNavigation("/login", "/dashboard", { userId: "12345" });
    }).not.toThrow();
  });

  it('应该能够记录用户行为日志', () => {
    // 测试用户行为日志，验证它们能够正常执行而不抛出异常
    expect(() => {
      logger.logUserAction("点击按钮", "用户点击了提交按钮", "12345");
    }).not.toThrow();
  });
});
