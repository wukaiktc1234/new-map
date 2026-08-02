import type { AxiosError } from 'axios';

/** API错误响应接口 */
export interface ApiErrorResponse {
  code?: number;
  message?: string;
  data?: unknown;
}

/**
 * 获取错误消息
 * @param error - 错误对象
 * @param defaultMessage - 默认错误消息
 * @returns 错误消息字符串
 */
export function getErrorMessage(error: unknown, defaultMessage: string = '操作失败'): string {
  if (!error) return defaultMessage;
  if (typeof error === 'string') return error;

  const axiosError = error as AxiosError<ApiErrorResponse>;
  if (axiosError.response?.data?.message) {
    return axiosError.response.data.message;
  }

  if (axiosError.message) {
    return axiosError.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return defaultMessage;
}

/**
 * 判断是否为取消请求错误
 * @param error - 错误对象
 * @returns 是否为取消错误
 */
export function isCancelError(error: unknown): boolean {
  return error === 'cancel' ||
    (error instanceof Error && error.message === 'cancel');
}

/** API错误处理器对象 */
const ApiErrorHandler = {
  getErrorMessage,
  isCancelError
};

export default ApiErrorHandler;
