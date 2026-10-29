/**
 * 统一把「后端具体原因」取出来给用户看。
 *
 * 为什么需要它：POS 的 axios 拦截器已经把 `error.response.data.message` 提前，
 * 但很多 catch 里写死 ElMessage.error('xxx失败，请重试')，直接把服务端给的原因丢掉，
 * 用户只能看到通用文案（例如 422「金额不符：应收 2200 分，请求 9999 分」被吞掉）。
 *
 * 取值优先级：originalError.response.data.message → error.message → fallback
 */
export function resolveErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'string' && error.trim()) return error
  if (error && typeof error === 'object') {
    const err = error as {
      message?: unknown
      originalError?: { response?: { data?: { message?: unknown } } }
    }
    const backendMessage = err.originalError?.response?.data?.message
    if (typeof backendMessage === 'string' && backendMessage.trim()) return backendMessage
    if (typeof err.message === 'string' && err.message.trim()) return err.message
  }
  return fallback
}
