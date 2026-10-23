/**
 * 图片上传 API
 * 对应后端: POST /v1/upload/image
 */
import { post } from './request'

export interface UploadImageResult {
  url: string
  fileName: string
  originalName: string
  fileSize: number
}

/** 上传图片（FormData，后端存 uploads 目录并返回访问 URL） */
export async function uploadImage(file: Blob, fileName?: string): Promise<string> {
  const formData = new FormData()
  formData.append('file', file, fileName ?? `evidence-${Date.now()}.jpg`)
  const res = await post<UploadImageResult>('/v1/upload/image', formData)
  return res?.url ?? ''
}
