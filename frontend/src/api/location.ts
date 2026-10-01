/**
 * 位置（locations）API —— P1-USER-LOCATION-001
 *
 * 对应后端 `LocationController`（`/v1/locations`）：`locations` 表
 * （`location_type` ∈ STORE / CENTRAL / DEPOT / TRANSIT，M1-M2 核心层）。
 *
 * 用途：用户归属位置下拉（design-001 §2.1 / design-002 §5 修订一：
 * 归属字段 `locationId` 的值即 `locations.location_id`）。
 */
import { get } from './request'

/** 位置选项（locations 实体子集，前端下拉用） */
export interface LocationOption {
  locationId: number
  locationCode: string
  locationName: string
  /** STORE=门店 / CENTRAL=中央仓 / DEPOT=普通仓储仓 / TRANSIT=在途（一期未启用） */
  locationType: 'STORE' | 'CENTRAL' | 'DEPOT' | 'TRANSIT'
  status?: number
}

export const locationApi = {
  /**
   * 活跃位置列表
   * 对应 `GET /v1/locations`（可选 `locationType` 过滤）
   */
  async listActive(locationType?: string): Promise<LocationOption[]> {
    return await get<LocationOption[]>('/v1/locations', locationType ? { locationType } : undefined)
  },
}
