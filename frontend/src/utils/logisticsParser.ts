/**
 * 物流文本智能解析
 * 从一段自由文本（如供应商微信消息）中识别车牌号、司机、联系方式、物流公司、运费等物流信息
 */
import type { ShipmentStatus, TransportMode, VehicleType } from '@/types/purchase-arrival'
import { ShipmentStatusOptions, TransportModeOptions, VehicleTypeOptions } from '@/types/purchase-arrival'

export interface ParsedLogistics {
  logisticsNo?: string
  logisticsCompany?: string
  transportMode?: TransportMode
  vehiclePlateNo?: string
  vehicleType?: VehicleType
  driverName?: string
  driverPhone?: string
  freightAmount?: number
  estimatedArrivalDate?: string
  shipmentStatus?: ShipmentStatus
}

/** 常见物流公司名称（按长度倒序优先匹配） */
const LOGISTICS_COMPANY_KEYWORDS = [
  '京东物流',
  '中通快递',
  '顺丰速运',
  '申通快递',
  '韵达快递',
  '圆通速递',
  '圆通快递',
  '极兔速递',
  '跨越速运',
  '壹米滴答',
  '百世快递',
  '百世物流',
  '中国邮政',
  '德邦物流',
  '安能物流',
  '天地华宇',
  '速尔快递',
  '天天快递',
  '优速快递',
  '京东',
  '顺丰',
  '中通',
  '圆通',
  '申通',
  '韵达',
  '极兔',
  '德邦',
  '邮政',
  'EMS',
  '邮政特快',
]

/** 常见物流单号前缀（公司拼音/缩写） */
const TRACKING_PREFIX_RE = /\b(?:SF|JDVA|YT|ZTO|STO|YD|HTKY|YZPY|EMS|DB|UBI|EXPRESS)\d[\dA-Za-z]{5,}\b/

function extractPlate(text: string): string {
  const m = text.match(
    /[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,6}(?:挂|学|警)?/,
  )
  return m ? m[0] : ''
}

function extractPhone(text: string): string {
  const m = text.match(/1[3-9]\d{9}/)
  if (m) {
    const idx = m.index ?? 0
    const prev = text[idx - 1]
    const next = text[idx + m[0].length]
    if ((!prev || !/\d/.test(prev)) && (!next || !/\d/.test(next))) {
      return m[0]
    }
  }
  const tel = text.match(/(?:^|[^\d])0\d{2,3}-?\d{7,8}(?:[^\d]|$)/)
  return tel ? tel[0].replace(/[^0-9-]/g, '') : ''
}

function extractCarrierName(t: string): string | undefined {
  const labelMatch = t.match(/(?:物流公司|快递公司|承运商)\s*[:：=]?\s*([^\s，。；;、]{2,12})/)
  if (labelMatch) return labelMatch[1].trim()
  for (const name of LOGISTICS_COMPANY_KEYWORDS) {
    if (t.includes(name)) return name
  }
  return undefined
}

function extractTrackingNo(t: string): string | undefined {
  const labelMatch = t.match(/(?:物流单号|快递单号|运单号|单号)\s*[:：=]?\s*([A-Za-z0-9]{6,32})/)
  if (labelMatch) return labelMatch[1].trim()
  const prefixMatch = TRACKING_PREFIX_RE.exec(t)
  if (prefixMatch) return prefixMatch[0]
  const run = t.match(/(?<![\dA-Za-z-])[A-Za-z]{1,2}\d{10,}(?![\dA-Za-z-])/)
  return run ? run[0] : undefined
}

function extractDriverName(t: string): string | undefined {
  const m = t.match(/(?:司机|驾驶员|送货人|师傅)（?[:：=]?\s*([\u4e00-\u9fa5·]{2,4})/)
  if (m) return m[1].trim()
  return undefined
}

function extractTransportMode(t: string): TransportMode | undefined {
  if (/自提|自寻/.test(t)) return 'self'
  if (/空运|航空/.test(t)) return 'air'
  if (/海运|航运|水运|船运/.test(t)) return 'sea'
  if (/陆运|公路|汽运|厢货|货车/.test(t)) return 'land'
  if (/快递|同城|即时/.test(t)) return 'express'
  return undefined
}

function extractVehicleType(t: string): VehicleType | undefined {
  if (/冷藏|冷冻|冷柜[车]?/.test(t)) return 'refrigerated'
  if (/平板/.test(t)) return 'flatbed'
  if (/面包|小型/.test(t)) return 'minivan'
  if (/厢式|厢货|货车/.test(t)) return 'van'
  if (/其他|自卸|罐车/.test(t)) return 'other'
  return undefined
}

function extractFreight(t: string): number | undefined {
  const label = t.match(/(?:运费|物流费|运费金额|运费费用)\s*[:：=]?\s*([\d.]+)/)
  const value = label ? label[1] : undefined
  if (value) {
    const n = Number(value)
    if (!Number.isNaN(n) && n >= 0) return n
  }
  const generic = t.match(/[¥￥]?\s*([\d.]+)\s*元运费/)
  if (generic) {
    const n = Number(generic[1])
    if (!Number.isNaN(n) && n >= 0) return n
  }
  return undefined
}

function extractEstimatedDate(t: string): string | undefined {
  const label = t.match(/(?:预计到货|预计到达|到货日期|到达日期|预计抵达|预计[到]?货日?)\s*[:：]?\s*(\d{4}[-/.年]\d{1,2}[-/.月]\d{1,2}日?)/)
  const normalized = label ? normalizeDate(label[1]) : undefined
  if (normalized) return normalized
  const generic = t.match(/(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?/)
  if (generic) return normalizeDate(`${generic[1]}-${generic[2]}-${generic[3]}`)
  const monthDay = t.match(/(?:预计|到货|到达|预计到达)?\s*(\d{1,2})[-/.月](\d{1,2})日?(?:到货|到达|到)?/)
  if (monthDay) return normalizeDate(`${new Date().getFullYear()}-${monthDay[1]}-${monthDay[2]}`)
  return undefined
}

function extractShipmentStatus(t: string): ShipmentStatus | undefined {
  if (/已签收|签收|已送达|已到达|已到货/.test(t)) return 'delivered'
  if (/在途|运输中|已发出|发出|途中/.test(t)) return 'in_transit'
  if (/已发货|发货中|出库|已装车/.test(t)) return 'shipped'
  return undefined
}

function normalizeDate(raw: string): string {
  const m = raw.match(/(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?/)
  if (!m) return ''
  const y = m[1]
  const mo = String(Number(m[2])).padStart(2, '0')
  const d = String(Number(m[3])).padStart(2, '0')
  return `${y}-${mo}-${d}`
}

/** 从自由文本解析物流字段（仅在识别到内容时返回该字段） */
export function parseLogisticsText(text: string): ParsedLogistics {
  const result: ParsedLogistics = {}
  const t = text ?? ''

  const plate = extractPlate(t)
  if (plate) result.vehiclePlateNo = plate

  const phone = extractPhone(t)
  if (phone) result.driverPhone = phone

  const driver = extractDriverName(t)
  if (driver) result.driverName = driver

  const company = extractCarrierName(t)
  if (company) result.logisticsCompany = company

  const no = extractTrackingNo(t)
  if (no) result.logisticsNo = no

  const mode = extractTransportMode(t)
  if (mode) result.transportMode = mode

  const vtype = extractVehicleType(t)
  if (vtype) result.vehicleType = vtype

  const freight = extractFreight(t)
  if (freight != null) result.freightAmount = freight

  const date = extractEstimatedDate(t)
  if (date) result.estimatedArrivalDate = date

  const status = extractShipmentStatus(t)
  if (status) result.shipmentStatus = status

  return result
}

/** 解析结果的简要中文说明（用于填充提示） */
export function buildLogisticsParseSummary(result: ParsedLogistics): string {
  const labelOf = <T extends string>(options: { value: T; label: string }[], value?: T): string =>
    options.find((o) => o.value === value)?.label ?? (value as string) ?? ''
  const parts: string[] = []
  if (result.vehiclePlateNo) parts.push(`车牌号：${result.vehiclePlateNo}`)
  if (result.driverName) parts.push(`司机：${result.driverName}`)
  if (result.driverPhone) parts.push(`电话：${result.driverPhone}`)
  if (result.logisticsCompany) parts.push(`物流公司：${result.logisticsCompany}`)
  if (result.logisticsNo) parts.push(`物流单号：${result.logisticsNo}`)
  const mode = labelOf(TransportModeOptions, result.transportMode)
  if (mode) parts.push(`运输方式：${mode}`)
  const vtype = labelOf(VehicleTypeOptions, result.vehicleType)
  if (vtype) parts.push(`车辆类型：${vtype}`)
  if (result.freightAmount != null) parts.push(`运费：${result.freightAmount}元`)
  if (result.estimatedArrivalDate) parts.push(`预计到货：${result.estimatedArrivalDate}`)
  const status = labelOf(ShipmentStatusOptions, result.shipmentStatus)
  if (status) parts.push(`发货状态：${status}`)
  return parts.join('；')
}