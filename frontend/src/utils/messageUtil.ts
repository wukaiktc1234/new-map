import { ElMessage, type MessageParams } from "element-plus";

/**
 * 消息提示工具，用于解决重复提示问题
 * 支持节流功能，防止短时间内出现大量相同的消息提示
 */
export class MessageUtil {
  // 用于节流的消息记录，格式：{ messageKey: 时间戳 }

  private static messageThrottleMap: Map<string, number> = new Map();

  // 节流时间隔（毫秒）

  private static readonly throttleInterval = 2000;

  /**
   * 显示成功消息
   */
  public static success(options: string | MessageParams) {
    return this.showMessage(options, "success");
  }

  /**
   * 显示警告消息
   */
  public static warning(options: string | MessageParams) {
    return this.showMessage(options, "warning");
  }

  /**
   * 显示信息消息
   */
  public static info(options: string | MessageParams) {
    return this.showMessage(options, "info");
  }

  /**
   * 显示错误消息
   */
  public static error(options: string | MessageParams) {
    return this.showMessage(options, "error");
  }

  /**
   * 显示消息的核心方法
   */
  private static showMessage(
    options: string | MessageParams,
    type: "success" | "warning" | "info" | "error",
  ) {
    let message: string;
    let messageOptions: MessageParams;

    if (typeof options === "string") {
      message = options;
      messageOptions = { message: options };
    } else {
      // 确保options是一个对象类型且不是VNode
if (typeof options === "object" && options !== null && !('type' in options)) {
        // 安全访问message属性

        message = (options as { message?: string }).message || "";
        messageOptions = options;
      } else {
        // 如果是VNode或其他类型，直接使用

        message = "";
        messageOptions = options;
      }
    }

    // 生成唯一一的消息键，使用时间戳作为备用

    const messageKey = `${type}:${message || Date.now()}`;
    const now = Date.now();

    // 检查是否需要节流
if (this.messageThrottleMap.has(messageKey)) {
      const lastShown = this.messageThrottleMap.get(messageKey) || 0;
      if (now - lastShown < this.throttleInterval) {
        return;
      }
    }

    // 更新节流时间 ? this.messageThrottleMap.set(messageKey, now);

    // 2秒后清除记录，允许再次显示相同消息
    setTimeout(() => {
      this.messageThrottleMap.delete(messageKey);
    }, this.throttleInterval);

    // 显示消息
return ElMessage[type](messageOptions);
  }
}

// 导出便捷的消息方法
export const { success, warning, info, error } = MessageUtil;


export default MessageUtil;
