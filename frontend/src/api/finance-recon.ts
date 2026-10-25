/**
 * 财务对账工作台 API（P3-1）
 *
 * 对应后端 ReconController（base: /v1/finance）：
 * - R1 POST /recon-runs                     发起跑批（幂等，force=true 重建）
 * - R2 GET  /recon-runs                     跑批列表
 * - R3 GET  /recon-runs/{runId}             跑批详情
 * - R8 GET  /recon-differences              差异池列表（from/to 可选，按 create_time 半开区间）
 * - R9 POST /recon-differences/{id}/claim   认领
 * - R10 POST /recon-differences/{id}/resolve 处理（WRITE_OFF / EXEMPT / REVERSE）
 *
 * 沿用 `@/api/request` 的 get/post（响应拦截器已剥离 { code, message, data } 外壳）。
 */
import { get, post } from '@/api/request'
import type {
  ReconDifferenceQuery,
  ReconDifferenceVO,
  ReconPageResult,
  ReconResolvePayload,
  ReconRunCreatePayload,
  ReconRunCreateResult,
  ReconRunQuery,
  ReconRunVO,
} from '@/types/finance-recon'

/** 把查询对象里的空值剔除（后端 @RequestParam/LocalDateTime 收到空串会 400） */
function compactParams(params: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (typeof value === 'string' && value.trim() === '') return
    result[key] = value
  })
  return result
}

/** R1：发起对账跑批（幂等；force=true 强制重建） */
export async function createReconRun(payload: ReconRunCreatePayload): Promise<ReconRunCreateResult> {
  const data = await post<ReconRunCreateResult | null>('/v1/finance/recon-runs', {
    accountId: payload.accountId,
    businessDate: payload.businessDate,
    ...(payload.force === undefined ? {} : { force: payload.force }),
  })
  // 防御：后端返回为空时给出明确错误，避免页面拿到 undefined 崩在渲染层
  if (!data || !data.run) {
    throw new Error('跑批接口未返回批次信息')
  }
  return { reused: Boolean(data.reused), run: data.run }
}

/** R2：跑批列表 */
export async function listReconRuns(query: ReconRunQuery = {}): Promise<ReconPageResult<ReconRunVO>> {
  const params = compactParams({
    accountId: query.accountId,
    businessDate: query.businessDate,
    status: query.status,
    current: query.current ?? 1,
    size: query.size ?? 20,
  })
  const data = await get<ReconPageResult<ReconRunVO> | null>('/v1/finance/recon-runs', params)
  return {
    records: data?.records ?? [],
    total: data?.total ?? 0,
    current: data?.current ?? Number(params.current ?? 1),
    size: data?.size ?? Number(params.size ?? 20),
    pages: data?.pages ?? 0,
  }
}

/** R3：跑批详情（含 differenceCount / matchRate，AC-46a 的唯一数据源） */
export async function getReconRun(runId: number): Promise<ReconRunVO> {
  const data = await get<ReconRunVO | null>(`/v1/finance/recon-runs/${encodeURIComponent(String(runId))}`)
  if (!data) {
    throw new Error('跑批详情为空')
  }
  return data
}

/** R8：差异池列表（from/to 可选，ISO-8601 本地时间，半开区间 [from, to)） */
export async function listReconDifferences(
  query: ReconDifferenceQuery = {},
): Promise<ReconPageResult<ReconDifferenceVO>> {
  const params = compactParams({
    status: query.status,
    type: query.type,
    runId: query.runId,
    assigneeId: query.assigneeId,
    from: query.from,
    to: query.to,
    current: query.current ?? 1,
    size: query.size ?? 20,
  })
  const data = await get<ReconPageResult<ReconDifferenceVO> | null>('/v1/finance/recon-differences', params)
  return {
    records: data?.records ?? [],
    total: data?.total ?? 0,
    current: data?.current ?? Number(params.current ?? 1),
    size: data?.size ?? Number(params.size ?? 20),
    pages: data?.pages ?? 0,
  }
}

/**
 * R8 全量分页拉取（AC-46b 的基础能力）。
 *
 * 严格按分页逐页拉到末页，绝不按 total 粗略估算；`pageSize` 默认 200。
 * 返回全量差异行（未被前端过滤，筛选条件全部交给后端）。
 */
export async function listAllReconDifferences(
  query: ReconDifferenceQuery = {},
  pageSize = 200,
): Promise<ReconDifferenceVO[]> {
  const all: ReconDifferenceVO[] = []
  let current = 1
  // 防御性上限：即使后端 total 异常也不至于无限循环
  const MAX_PAGES = 200
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const res = await listReconDifferences({ ...query, current, size: pageSize })
    const records = res.records ?? []
    all.push(...records)
    const pages = typeof res.pages === 'number' && res.pages > 0
      ? res.pages
      : Math.ceil((res.total || 0) / pageSize)
    if (records.length === 0 || current >= pages) break
    current += 1
  }
  return all
}

/** R9：认领差异（OPEN → ASSIGNED） */
export async function claimReconDifference(differenceId: number): Promise<ReconDifferenceVO> {
  const data = await post<ReconDifferenceVO | null>(
    `/v1/finance/recon-differences/${encodeURIComponent(String(differenceId))}/claim`,
  )
  if (!data) {
    throw new Error('认领接口未返回差异信息')
  }
  return data
}

/**
 * R10：处理差异。
 *
 * 【契约】后端 ReconResolveDTO 是 `{ resolution, remark, reverseFlowNo? }`：
 * 用户填写的「原因」必须放在 **remark**（@NotBlank），resolutionRemark 只是 VO 回显字段。
 */
export async function resolveReconDifference(
  differenceId: number,
  payload: ReconResolvePayload,
): Promise<ReconDifferenceVO> {
  const data = await post<ReconDifferenceVO | null>(
    `/v1/finance/recon-differences/${encodeURIComponent(String(differenceId))}/resolve`,
    {
      resolution: payload.resolution,
      remark: payload.remark,
      ...(payload.reverseFlowNo ? { reverseFlowNo: payload.reverseFlowNo } : {}),
    },
  )
  if (!data) {
    throw new Error('处理接口未返回差异信息')
  }
  return data
}

export const financeReconApi = {
  createReconRun,
  listReconRuns,
  getReconRun,
  listReconDifferences,
  listAllReconDifferences,
  claimReconDifference,
  resolveReconDifference,
}

export default financeReconApi
