/**
 * 标签模板 API
 * 对应后端: LabelTemplateController (/v1/label-template)
 *
 * 改造说明（2026-06-30）：
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 * - 标签模板无状态/金额转换需求，直接返回 LabelTemplate 类型
 * - Blob 响应（预览图片）通过 options.responseType 处理
 * - 路径参数使用 encodeURIComponent 编码，防止特殊字符破坏 URL
 */
import { get, post, put, del } from '../request'

import type {
  LabelTemplate,
  LabelLayoutConfig,
  LabelPrintData,
  PreviewData,
} from '@/types/traceability'

// ============================================================
// 后端响应类型
// ============================================================

/** 后端列表响应（Spring Data 风格分页） */
interface LabelTemplateListBackend {
  content: LabelTemplate[] | null
  totalElements: number
  totalPages: number
}

/** 打印结果 */
interface PrintResult {
  success: boolean
  message: string
  successCount?: number
  failCount?: number
}

/** 布局校验结果 */
interface ValidateResult {
  valid: boolean
  message: string
  elementId?: string
}

export const labelTemplateApi = {
  /**
   * 查询标签模板列表
   * GET /v1/label-template/list
   */
  async getList(params?: {
    templateType?: string
    enabled?: boolean
  }): Promise<{ content: LabelTemplate[]; totalElements: number; totalPages: number }> {
    const res = await get<LabelTemplateListBackend | null>(
      '/v1/label-template/list',
      params as unknown as Record<string, unknown>,
    )
    return {
      content: res?.content ?? [],
      totalElements: res?.totalElements ?? 0,
      totalPages: res?.totalPages ?? 0,
    }
  },

  /**
   * 查询标签模板详情
   * GET /v1/label-template/{id}
   */
  async getById(id: string): Promise<LabelTemplate | null> {
    return await get<LabelTemplate | null>(`/v1/label-template/${encodeURIComponent(id)}`)
  },

  /**
   * 创建标签模板
   * POST /v1/label-template
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已创建）
   */
  async create(data: LabelTemplate): Promise<LabelTemplate> {
    return await post<LabelTemplate>('/v1/label-template', data)
  },

  /**
   * 更新标签模板
   * PUT /v1/label-template/{id}
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已更新）
   */
  async update(id: string, data: LabelTemplate): Promise<LabelTemplate> {
    return await put<LabelTemplate>(`/v1/label-template/${encodeURIComponent(id)}`, data)
  },

  /**
   * 删除标签模板
   * DELETE /v1/label-template/{id}
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已删除）
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/label-template/${encodeURIComponent(id)}`)
  },

  /**
   * 设置默认模板
   * PUT /v1/label-template/{id}/default
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已设置默认）
   */
  async setDefault(id: string): Promise<void> {
    await put<void>(`/v1/label-template/${encodeURIComponent(id)}/default`)
  },

  /**
   * 生成预览HTML
   * POST /v1/label-template/preview
   */
  async generatePreview(data: { layoutConfig: LabelLayoutConfig; labelData: LabelPrintData }): Promise<string> {
    return await post<string>('/v1/label-template/preview', data)
  },

  /**
   * 生成预览数据
   * POST /v1/label-template/preview-data
   */
  async generatePreviewData(data: {
    layoutConfig: LabelLayoutConfig
    labelData: LabelPrintData
  }): Promise<PreviewData> {
    return await post<PreviewData>('/v1/label-template/preview-data', data)
  },

  /**
   * 获取模板预览图片
   * GET /v1/label-template/{id}/preview
   * 返回 Blob（PNG 图片），通过 responseType: 'blob' 处理
   */
  async getTemplatePreviewImage(
    id: string,
    data?: Partial<LabelPrintData>,
  ): Promise<Blob> {
    return await get<Blob>(
      `/v1/label-template/${encodeURIComponent(id)}/preview`,
      data as unknown as Record<string, unknown>,
      { responseType: 'blob' },
    )
  },

  /**
   * 生成 TSPL 打印指令
   * POST /v1/label-template/generate-tspl
   */
  async generateTspl(data: { layoutConfig: LabelLayoutConfig; labelData: LabelPrintData }): Promise<string> {
    return await post<string>('/v1/label-template/generate-tspl', data)
  },

  /**
   * 按模板ID打印
   * POST /v1/label-template/{templateId}/print
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已打印）
   */
  async printByTemplateId(
    templateId: string,
    request: { traceCodeIds?: string[]; labelDataList?: LabelPrintData[]; printerId?: number },
  ): Promise<PrintResult> {
    return await post<PrintResult>(
      `/v1/label-template/${encodeURIComponent(templateId)}/print`,
      request,
    )
  },

  /**
   * 直接打印标签
   * POST /v1/label-template/print
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已打印）
   */
  async printLabel(data: {
    layoutConfig: LabelLayoutConfig
    labelData: LabelPrintData
    printerName?: string
  }): Promise<{ success: boolean; message: string }> {
    return await post<{ success: boolean; message: string }>('/v1/label-template/print', data)
  },

  /**
   * 批量打印标签
   * POST /v1/label-template/batch-print
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已批量打印）
   */
  async batchPrint(data: {
    layoutConfig: LabelLayoutConfig
    labelDataList: LabelPrintData[]
    printerName?: string
  }): Promise<PrintResult> {
    return await post<PrintResult>('/v1/label-template/batch-print', data)
  },

  /**
   * 验证布局配置
   * POST /v1/label-template/validate
   */
  async validateLayout(layoutConfig: LabelLayoutConfig): Promise<ValidateResult> {
    return await post<ValidateResult>('/v1/label-template/validate', layoutConfig)
  },

  /**
   * 获取默认模板
   * GET /v1/label-template/default/{templateType}
   */
  async getDefaultTemplate(templateType: string): Promise<LabelTemplate | null> {
    return await get<LabelTemplate | null>(
      `/v1/label-template/default/${encodeURIComponent(templateType)}`,
    )
  },
}

export default labelTemplateApi
