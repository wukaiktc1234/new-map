/**
 * 运营策略 API
 * 对应后端: PromotionController (/v1/promotions)
 *
 * 前端 Campaign 字段与后端 MarketingPromotion 字段存在差异，需做双向转换。
 *
 * 字段映射说明：
 * - 前端 id (string) ↔ 后端 promotionId (Long)
 * - 前端 name ↔ 后端 promotionName
 * - 前端 startDate/endDate (YYYY-MM-DD) ↔ 后端 startTime/endTime (LocalDateTime)
 * - 前端 budget (元) ↔ 后端 budgetTotal (分)  ← 元转分：×100
 * - 前端 actualCost (元) ↔ 后端 budgetUsed (分)  ← 元转分：×100
 * - 前端 status (in_progress/completed/pending) ↔ 后端 status (1草稿/2进行中/3已结束/4已暂停/5已作废)
 *   pending → 1, in_progress → 2, completed → 3
 * - 前端 channel/conversionRate/roi/targetConversionRate/storeIds：后端无对应字段，保留前端态
 *   （创建/更新时不向后端同步这些字段，仅在前端维护）
 *
 * 后端端点：
 * - GET    /v1/promotions                查询所有活动
 * - POST   /v1/promotions                创建促销活动
 * - PUT    /v1/promotions/{promotionId}  更新促销活动
 * - POST   /v1/promotions/{promotionId}/start  启动活动（对应前端"发布"）
 * - POST   /v1/promotions/{promotionId}/end    结束活动
 * - POST   /v1/promotions/{promotionId}/pause  暂停活动
 * - POST   /v1/promotions/{promotionId}/cancel 作废活动
 */
import { get, post, put } from '@/api/request'
// 金额转换统一委托给 utils/money（fenToYuan 返回 number，匹配 Campaign.budget 字段类型）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

// ============================================================
// 类型定义（与 StrategyWorkshop.vue 内部类型保持一致）
// ============================================================

/** 渠道类型 */
export type ChannelType = 'wechat_moments' | 'douyin' | 'meituan' | 'offline' | 'sms'

/** 活动状态 */
export type CampaignStatus = 'in_progress' | 'completed' | 'pending'

/** 促销活动（前端模型） */
export interface Campaign {
  id: string
  name: string
  channel: ChannelType
  startDate: string
  endDate: string
  budget: number
  actualCost: number
  conversionRate: number
  roi: number
  status: CampaignStatus
  targetConversionRate: number
  storeIds: string[]
}

/** 创建/编辑活动表单数据 */
export interface CampaignFormData {
  name: string
  channel: ChannelType | ''
  startDate: string
  endDate: string
  budget: number | undefined
  targetConversionRate: number | undefined
  storeIds: string[]
}

// ============================================================
// 后端响应接口（MarketingPromotion 实体字段）
// ============================================================

interface PromotionBackend {
  promotionId?: number
  promotionName?: string
  promotionCode?: string
  promotionType?: number
  discountRule?: string
  startTime?: string
  endTime?: string
  targetAudience?: number
  targetLevelIds?: string
  participationCondition?: string
  budgetTotal?: number
  budgetUsed?: number
  participantCount?: number
  successCount?: number
  salesAmount?: number
  discountAmount?: number
  status?: number
  description?: string
  createTime?: string
  updateTime?: string
}

/** 创建活动 DTO（对应后端 PromotionCreateDTO） */
interface PromotionCreateDTO {
  promotionName: string
  promotionCode: string
  promotionType: number
  discountRule: string
  startTime: string
  endTime: string
  targetAudience?: number
  budgetTotal?: number
  description?: string
}

/** 更新活动 DTO（对应后端 PromotionUpdateDTO） */
interface PromotionUpdateDTO {
  promotionName?: string
  discountRule?: string
  startTime?: string
  endTime?: string
  targetAudience?: number
  budgetTotal?: number
  status: number
  description?: string
}

// ============================================================
// 状态映射
// ============================================================

/** 后端状态码 → 前端状态 */
const BACKEND_TO_FRONTEND_STATUS: Record<number, CampaignStatus> = {
  1: 'pending',      // 草稿
  2: 'in_progress',  // 进行中
  3: 'completed',    // 已结束
  4: 'in_progress',  // 已暂停（前端归入进行中）
  5: 'completed',    // 已作废（前端归入已完成）
}

/** 前端状态 → 后端状态码 */
const FRONTEND_TO_BACKEND_STATUS: Record<CampaignStatus, number> = {
  pending: 1,        // 草稿
  in_progress: 2,    // 进行中
  completed: 3,      // 已结束
}

// ============================================================
// 字段适配器
// ============================================================

/**
 * 促销活动扩展字段（前端独有，后端未持久化）
 *
 * TODO（技术债务）：当前后端 marketing_promotion 表无 channel / storeIds /
 * targetConversionRate / conversionRate / roi 字段，前端将这些字段序列化为
 * JSON 字符串后塞入 description 字段持久化。这是临时 hack，待后端添加独立
 * 字段后应迁移至正式字段，并移除本节所有序列化/反序列化逻辑。
 *
 * 迁移步骤（待后端配合）：
 * 1. marketing_promotion 表新增列：channel / store_ids(JSONB) /
 *    target_conversion_rate / conversion_rate / roi
 * 2. MarketingPromotion 实体添加对应字段
 * 3. PromotionCreateDTO / PromotionUpdateDTO 添加字段
 * 4. PromotionServiceImpl 的 create/update 显式赋值
 * 5. 删除本节（PromotionExtension + serializeExtension + deserializeExtension）
 * 6. adaptPromotionToCampaign / convertToCreateDTO / convertToUpdateDTO
 *    改为直接读写 raw.channel / raw.storeIds 等字段
 */
interface PromotionExtension {
  /** 投放渠道 */
  channel?: ChannelType
  /** 适用门店 ID 列表 */
  storeIds?: string[]
  /** 目标转化率（百分比，默认 10） */
  targetConversionRate?: number
  /** 实际转化率（百分比，由后端统计回填） */
  conversionRate?: number
  /** 投资回报率（百分比，由后端统计回填） */
  roi?: number
}

/** 默认扩展字段（反序列化失败或字段缺失时使用） */
const DEFAULT_EXTENSION: Required<Pick<PromotionExtension, 'channel' | 'storeIds' | 'targetConversionRate'>> = {
  channel: 'wechat_moments',
  storeIds: [],
  targetConversionRate: 10,
}

/**
 * 序列化扩展字段为 JSON 字符串（写入 description）
 *
 * 注意：仅持久化 channel / storeIds / targetConversionRate 三个由用户填写的字段，
 * conversionRate / roi 是统计结果，不参与持久化（由后端实时计算回填）。
 */
function serializeExtension(ext: PromotionExtension): string {
  return JSON.stringify({
    channel: ext.channel ?? DEFAULT_EXTENSION.channel,
    storeIds: Array.isArray(ext.storeIds) ? ext.storeIds : [],
    targetConversionRate: typeof ext.targetConversionRate === 'number' && ext.targetConversionRate > 0
      ? ext.targetConversionRate
      : DEFAULT_EXTENSION.targetConversionRate,
  })
}

/**
 * 反序列化 description 中的扩展字段
 *
 * 容错策略：
 * - description 为空 → 返回默认扩展字段
 * - description 非 JSON → 返回默认扩展字段（不抛异常）
 * - JSON 解析成功但字段缺失 → 用默认值补全
 * - 字段类型不匹配 → 用默认值覆盖
 */
function deserializeExtension(description: string | undefined | null): PromotionExtension {
  if (!description) return { ...DEFAULT_EXTENSION }
  try {
    const parsed = JSON.parse(description) as Partial<PromotionExtension>
    return {
      channel: typeof parsed.channel === 'string' ? (parsed.channel as ChannelType) : DEFAULT_EXTENSION.channel,
      storeIds: Array.isArray(parsed.storeIds) ? parsed.storeIds.filter((id): id is string => typeof id === 'string') : [],
      targetConversionRate: typeof parsed.targetConversionRate === 'number' && parsed.targetConversionRate > 0
        ? parsed.targetConversionRate
        : DEFAULT_EXTENSION.targetConversionRate,
      conversionRate: typeof parsed.conversionRate === 'number' ? parsed.conversionRate : 0,
      roi: typeof parsed.roi === 'number' ? parsed.roi : 0,
    }
  } catch {
    // description 不是 JSON（可能是纯文本描述），返回默认值
    return { ...DEFAULT_EXTENSION }
  }
}

/** 日期字符串 (YYYY-MM-DD) → ISO 8601 datetime（后端 LocalDateTime 接收） */
function dateToIso(dateStr: string): string {
  if (!dateStr) return ''
  // 补全为当天 00:00:00 的 ISO 字符串
  return `${dateStr}T00:00:00`
}

/** ISO datetime → 日期字符串 (YYYY-MM-DD) */
function isoToDate(iso: string | undefined | null): string {
  if (!iso) return ''
  // 取前 10 位（YYYY-MM-DD）
  return String(iso).slice(0, 10)
}

/** 后端 Promotion → 前端 Campaign */
function adaptPromotionToCampaign(raw: PromotionBackend): Campaign {
  const backendStatus = Number(raw.status ?? 1)
  const status = BACKEND_TO_FRONTEND_STATUS[backendStatus] || 'pending'

  // 反序列化 description 中存储的扩展字段（channel/storeIds/targetConversionRate/conversionRate/roi）
  const ext = deserializeExtension(raw.description)
  const actualCost = fenToYuan(raw.budgetUsed)

  return {
    id: String(raw.promotionId ?? ''),
    name: raw.promotionName || '未命名活动',
    channel: ext.channel ?? 'wechat_moments',
    startDate: isoToDate(raw.startTime),
    endDate: isoToDate(raw.endTime),
    budget: fenToYuan(raw.budgetTotal),
    actualCost,
    conversionRate: ext.conversionRate ?? 0,
    roi: ext.roi ?? 0,
    status,
    targetConversionRate: ext.targetConversionRate ?? 10,
    storeIds: ext.storeIds ?? [],
  }
}

/** 前端表单数据 → 后端创建 DTO */
function convertToCreateDTO(data: CampaignFormData): PromotionCreateDTO {
  // 前端独有字段（channel/storeIds/targetConversionRate）序列化到 description
  // 详见 PromotionExtension 的 TODO 注释
  const description = serializeExtension({
    channel: data.channel || undefined,
    storeIds: data.storeIds,
    targetConversionRate: data.targetConversionRate,
  })

  return {
    promotionName: data.name,
    promotionCode: `CAMP_${Date.now()}`,  // 自动生成活动编码
    promotionType: 1,                      // 默认满减类型
    discountRule: '{}',                    // 默认空规则
    startTime: dateToIso(data.startDate),
    endTime: dateToIso(data.endDate),
    targetAudience: 1,                     // 默认全部会员
    budgetTotal: data.budget ? yuanToFen(data.budget) : 0,
    description,
  }
}

/** 前端表单数据 → 后端更新 DTO */
function convertToUpdateDTO(data: CampaignFormData, status: CampaignStatus): PromotionUpdateDTO {
  // 前端独有字段（channel/storeIds/targetConversionRate）序列化到 description
  // 详见 PromotionExtension 的 TODO 注释
  const description = serializeExtension({
    channel: data.channel || undefined,
    storeIds: data.storeIds,
    targetConversionRate: data.targetConversionRate,
  })

  return {
    promotionName: data.name,
    discountRule: '{}',
    startTime: dateToIso(data.startDate),
    endTime: dateToIso(data.endDate),
    targetAudience: 1,
    budgetTotal: data.budget ? yuanToFen(data.budget) : 0,
    status: FRONTEND_TO_BACKEND_STATUS[status],
    description,
  }
}

/** 解析前端 ID 为后端 Long（无效 ID 抛出错误） */
function parsePromotionId(id: string): number {
  const num = Number(id)
  if (Number.isNaN(num) || num <= 0) {
    throw new Error(`无效的活动 ID: ${id}`)
  }
  return num
}

// ============================================================
// API 实现
// ============================================================

export const strategyApi = {
  /**
   * 获取活动列表
   * GET /v1/promotions
   */
  async getList(): Promise<Campaign[]> {
    const res = await get<PromotionBackend[]>('/v1/promotions')
    return Array.isArray(res) ? res.map(adaptPromotionToCampaign) : []
  },

  /**
   * 创建活动
   * POST /v1/promotions
   * @param data 活动表单数据
   */
  async create(data: CampaignFormData): Promise<Campaign> {
    const dto = convertToCreateDTO(data)
    const res = await post<PromotionBackend>('/v1/promotions', dto)
    return adaptPromotionToCampaign(res)
  },

  /**
   * 更新活动
   * PUT /v1/promotions/{promotionId}
   * @param id 活动 ID
   * @param data 活动表单数据
   * @param status 当前活动状态（用于后端 status 字段）
   */
  async update(id: string, data: CampaignFormData, status: CampaignStatus): Promise<Campaign> {
    const promotionId = parsePromotionId(id)
    const dto = convertToUpdateDTO(data, status)
    const res = await put<PromotionBackend>(`/v1/promotions/${promotionId}`, dto)
    return adaptPromotionToCampaign(res)
  },

  /**
   * 发布活动（pending → in_progress）
   * POST /v1/promotions/{promotionId}/start
   * @param id 活动 ID
   */
  async publish(id: string): Promise<void> {
    const promotionId = parsePromotionId(id)
    await post<void>(`/v1/promotions/${promotionId}/start`)
  },

  /**
   * 结束活动（in_progress → completed）
   * POST /v1/promotions/{promotionId}/end
   * @param id 活动 ID
   */
  async end(id: string): Promise<void> {
    const promotionId = parsePromotionId(id)
    await post<void>(`/v1/promotions/${promotionId}/end`)
  },

  /**
   * 暂停活动
   * POST /v1/promotions/{promotionId}/pause
   * @param id 活动 ID
   */
  async pause(id: string): Promise<void> {
    const promotionId = parsePromotionId(id)
    await post<void>(`/v1/promotions/${promotionId}/pause`)
  },

  /**
   * 作废活动
   * POST /v1/promotions/{promotionId}/cancel
   * @param id 活动 ID
   */
  async cancel(id: string): Promise<void> {
    const promotionId = parsePromotionId(id)
    await post<void>(`/v1/promotions/${promotionId}/cancel`)
  },
}

export default strategyApi
