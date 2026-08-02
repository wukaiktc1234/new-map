/**
 * 岗位管理 API
 * 对接后端: /v1/positions
 *
 * 说明：本文件导出的 positionApi 与 recruitment.ts 中的 positionApi 语义不同（后者服务于招聘模块），
 * 因此 HRPosition.vue 通过文件路径直接导入（@/api/hr/position），不走 hr/index.ts 桶导出，避免命名冲突。
 */
import { get, post, put, del } from '../request'

/* ===== 后端 Position 实体（/v1/positions 返回的真实字段） ===== */
interface PositionBackend {
  id?: string
  positionName: string
  positionCode?: string
  departmentId?: string
  /** 后端 transient 字段：部门名称 */
  department?: string
  description?: string
  /** 在岗员工数（后端实时统计） */
  employeeCount?: number
  /** 编制人数 */
  headCount?: number
  /** 1: 启用, 0: 禁用 */
  status?: number
  createdAt?: string
  updatedAt?: string
}

/* ===== 前端岗位列表项（页面展示用） ===== */
export interface PositionItem {
  positionId: string
  positionCode: string
  positionName: string
  departmentId: string
  departmentName: string
  currentCount: number
  /** 编制人数（后端暂无此字段，默认 0 以保持 UI 兼容） */
  maxCount: number
  status: 'active' | 'inactive'
  description: string
  /** 平均薪资（后端暂无此字段，默认 0 以保持统计卡片兼容） */
  avgSalary: number
}

/* ===== 前端表单数据 ===== */
export interface PositionFormData {
  positionCode: string
  positionName: string
  departmentId: string
  maxCount: number
  description: string
  status: 'active' | 'inactive'
}

/* ===== 查询表单 ===== */
export interface PositionQueryForm {
  departmentId: string
  status: '' | 'active' | 'inactive'
  keyword: string
}

/* ===== 分页响应（后端 Map 结构，列表字段为 items） ===== */
interface PositionPageResponse {
  items: PositionBackend[]
  total: number
  current: number
  size: number
  pages: number
}

/* ===== 批量基本信息响应 ===== */
export interface PositionBasicInfoResponse {
  positionId: string
  positionName: string
  positionCode?: string
  departmentId?: string
  departmentName?: string
  description?: string
  employeeCount?: number
  status?: boolean
}

/* ===== 状态转换：后端数字编码 ↔ 前端语义字符串 ===== */
function toFrontendStatus(status?: number): 'active' | 'inactive' {
  return status === 1 ? 'active' : 'inactive'
}

function toBackendStatus(status: 'active' | 'inactive'): number {
  return status === 'active' ? 1 : 0
}

/* ===== 后端 Position → 前端 PositionItem ===== */
function toPositionItem(item: PositionBackend): PositionItem {
  return {
    positionId: String(item.id ?? ''),
    positionCode: item.positionCode ?? '',
    positionName: item.positionName ?? '',
    departmentId: String(item.departmentId ?? ''),
    departmentName: item.department ?? '',
    currentCount: item.employeeCount ?? 0,
    maxCount: item.headCount ?? 0,
    status: toFrontendStatus(item.status),
    description: item.description ?? '',
    avgSalary: 0,
  }
}

/* ===== 前端表单 → 后端 Position 创建/更新负载 ===== */
function toBackendPayload(form: PositionFormData): PositionBackend {
  return {
    positionName: form.positionName,
    positionCode: form.positionCode || undefined,
    departmentId: form.departmentId || undefined,
    headCount: form.maxCount,
    description: form.description,
    status: toBackendStatus(form.status),
  }
}

export const positionApi = {
  /** 获取所有岗位（后端 /v1/positions/all） */
  async getAll(): Promise<PositionItem[]> {
    const res = await get<PositionBackend[]>('/v1/positions/all')
    return (res || []).map(toPositionItem)
  },

  /** 获取岗位分页列表（后端 /v1/positions/page） */
  async getList(params: {
    current: number
    size: number
    departmentId?: string
    status?: 'active' | 'inactive'
    keyword?: string
  }): Promise<{ records: PositionItem[]; total: number }> {
    const res = await get<PositionPageResponse>('/v1/positions/page', {
      current: params.current,
      size: params.size,
      ...(params.departmentId ? { departmentId: params.departmentId } : {}),
      ...(params.status ? { status: params.status } : {}),
      ...(params.keyword ? { keyword: params.keyword } : {}),
    })
    return {
      records: (res?.items || []).map(toPositionItem),
      total: res?.total || 0,
    }
  },

  /** 根据ID获取岗位详情 */
  async getById(id: string): Promise<PositionItem> {
    const res = await get<PositionBackend>(`/v1/positions/${id}`)
    return toPositionItem(res)
  },

  /** 根据部门获取岗位列表 */
  async getByDepartment(departmentId: string): Promise<PositionItem[]> {
    const res = await get<PositionBackend[]>(`/v1/positions/departments/${departmentId}/positions`)
    return (res || []).map(toPositionItem)
  },

  /** 创建岗位 */
  async create(data: PositionFormData): Promise<PositionItem> {
    const res = await post<PositionBackend>('/v1/positions', toBackendPayload(data))
    return toPositionItem(res)
  },

  /** 更新岗位 */
  async update(id: string, data: PositionFormData): Promise<PositionItem> {
    const res = await put<PositionBackend>(`/v1/positions/${id}`, toBackendPayload(data))
    return toPositionItem(res)
  },

  /** 删除岗位 */
  async delete(id: string): Promise<void> {
    await del(`/v1/positions/${id}`)
  },

  /** 更新岗位状态（后端接收 'active'/'inactive' 字符串） */
  async updateStatus(id: string, status: 'active' | 'inactive'): Promise<void> {
    await put(`/v1/positions/${id}/status`, { status })
  },

  /** 生成岗位编码（positionName 必填，departmentId 可选） */
  async generateCode(params: { departmentId?: string; positionName: string }): Promise<string> {
    const res = await get<string>('/v1/positions/code/generate', {
      ...(params.departmentId ? { departmentId: params.departmentId } : {}),
      positionName: params.positionName,
    })
    return res ?? ''
  },

  /** 生成岗位编码序号（positionName 必填，departmentId 可选） */
  async generateSeq(params: { departmentId?: string; positionName: string }): Promise<number> {
    const res = await get<number>('/v1/positions/seq/generate', {
      ...(params.departmentId ? { departmentId: params.departmentId } : {}),
      positionName: params.positionName,
    })
    return res ?? 0
  },

  /** 批量获取岗位基本信息（后端 POST，body 为 ID 列表） */
  async getBatchBasicInfo(ids: string[]): Promise<Record<string, PositionBasicInfoResponse>> {
    return await post<Record<string, PositionBasicInfoResponse>>('/v1/positions/batch/basic-info', ids)
  },

  /** 批量更新岗位编码（后端 POST，无请求体） */
  async batchUpdateCodes(): Promise<Record<string, unknown>> {
    return await post<Record<string, unknown>>('/v1/positions/batch-update-codes')
  },

  /** 按编码前缀清理岗位（默认 POS_） */
  async cleanupByPrefix(prefix: string = 'POS_'): Promise<{ prefix: string; cleanedCount: number }> {
    return await del<{ prefix: string; cleanedCount: number }>('/v1/positions/cleanup', { prefix })
  },
}
