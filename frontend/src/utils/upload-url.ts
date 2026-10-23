/**
 * 上传文件 URL 解析
 * 后端 context-path 为 /api，上传返回的 /uploads/xxx 需加 /api 前缀才能访问
 */
export function resolveUploadUrl(url?: string | null): string {
  if (!url) return ''
  return url.startsWith('/uploads') ? `/api${url}` : url
}