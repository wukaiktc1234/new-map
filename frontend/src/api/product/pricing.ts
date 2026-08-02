/**
 * 产品定价API
 * 对应后端: /v1/product-center/pricing
 */
import { get, post } from '@/api/request'
import { productDataConverter } from './converters'
import type {
  PricingBackend,
  PricingRecord,
  PricingFormData,
  ProductPageParams,
  ProductPageResponse,
} from '@/types/product'

export const pricingApi = {
  async queryHistory(params: ProductPageParams & {
    productType?: string
    productId?: number
    /** 起始日期（YYYY-MM-DD，定价历史范围筛选） */
    startDate?: string
    /** 结束日期（YYYY-MM-DD，定价历史范围筛选） */
    endDate?: string
  }): Promise<ProductPageResponse<PricingRecord>> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<ProductPageResponse<PricingBackend>>(
      '/v1/product-center/pricing/history',
      params as unknown as Record<string, unknown>
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: (res.records || []).map(item => productDataConverter.pricingToFrontend(item)),
    }
  },

  async adjustPrice(data: PricingFormData): Promise<PricingRecord> {
    const dto = productDataConverter.pricingToCreateDTO(data)
    const res = await post<PricingBackend>('/v1/product-center/pricing', dto)
    return productDataConverter.pricingToFrontend(res)
  },

  async batchPricing(productType: string, pricingList: PricingFormData[]): Promise<PricingRecord[]> {
    const dtos = pricingList.map(p => productDataConverter.pricingToCreateDTO(p))
    const res = await post<PricingBackend[]>(`/v1/product-center/pricing/batch/${productType}`, dtos)
    return (res || []).map(item => productDataConverter.pricingToFrontend(item))
  },

  async getCurrentPrice(productType: string, productId: number): Promise<string> {
    const res = await get<number>(`/v1/product-center/pricing/current/${productType}/${productId}`)
    return productDataConverter.formatPrice(res != null ? res / 100 : null)
  },

  async getPriceHistory(productType: string, productId: number, limit: number = 20): Promise<PricingRecord[]> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<PricingBackend[]>(
      `/v1/product-center/pricing/history/${productType}/${productId}`,
      { limit }
    )
    return (res || []).map(item => productDataConverter.pricingToFrontend(item))
  },
}

export default pricingApi
