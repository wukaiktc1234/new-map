/**
 * 套餐管理API
 * 对应后端: /v1/product-center/combos
 *
 * 使用项目封装的请求方法（get/post/put/del），
 * 直接传 params 对象，禁止 { params } 嵌套。
 */
import { get, post, put, del } from '@/api/request'
import { productDataConverter } from './converters'
import type {
  ComboBackend,
  DishCombo,
  DishComboRow,
  DishComboFormData,
  ProductPageParams,
  ProductPageResponse,
  ProductStatus,
} from '@/types/product'

export const comboApi = {
  async getList(params: ProductPageParams & {
    comboName?: string
    status?: number
  }): Promise<ProductPageResponse<DishComboRow>> {
    const res = await get<ProductPageResponse<ComboBackend>>(
      '/v1/product-center/combos',
      params as unknown as Record<string, unknown>
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    const combos = (res.records || []).map(item => productDataConverter.comboToFrontend(item))
    return {
      ...res,
      records: combos.map(c => productDataConverter.comboToRow(c)),
    }
  },

  async getById(id: number): Promise<DishCombo> {
    const res = await get<ComboBackend>(`/v1/product-center/combos/${id}`)
    return productDataConverter.comboToFrontend(res)
  },

  async create(data: DishComboFormData): Promise<DishCombo> {
    const dto = productDataConverter.comboToCreateDTO(data)
    const res = await post<ComboBackend>('/v1/product-center/combos', dto)
    return productDataConverter.comboToFrontend(res)
  },

  async update(id: string | number, data: DishComboFormData): Promise<DishCombo> {
    const dto = productDataConverter.comboToCreateDTO(data)
    const res = await put<ComboBackend>(`/v1/product-center/combos/${id}`, dto)
    return productDataConverter.comboToFrontend(res)
  },

  async delete(id: number): Promise<void> {
    await del<void>(`/v1/product-center/combos/${id}`)
  },

  /**
   * 更新套餐状态
   * 使用 converters.ts 统一的状态映射：active=1, inactive=0
   */
  async updateStatus(id: number, status: ProductStatus): Promise<void> {
    const statusCode = productDataConverter.statusToBackend(status)
    await put<void>(`/v1/product-center/combos/${id}/status/${statusCode}`)
  },

  async calculateCost(id: number): Promise<string> {
    const res = await get<number>(`/v1/product-center/combos/${id}/cost`)
    return productDataConverter.formatPrice(res != null ? res / 100 : null)
  },

  /** 获取在售套餐列表 */
  async listOnSale(): Promise<DishCombo[]> {
    const res = await get<ComboBackend[]>('/v1/product-center/combos/on-sale')
    return (res || []).map(item => productDataConverter.comboToFrontend(item))
  },
}

export default comboApi
