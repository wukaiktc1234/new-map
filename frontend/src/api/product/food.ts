/**
 * 菜品管理API
 * 对应后端: /v1/product-center/foods
 *
 * 使用项目封装的请求方法（get/post/put/del），
 * 直接传 params 对象，禁止 { params } 嵌套。
 */
import { get, post, put, del } from '@/api/request'
import { productDataConverter } from './converters'
import type {
  BatchUploadResult,
  Food,
  FoodBackend,
  FoodFormData,
  ProductPageParams,
  ProductPageResponse,
  ProductStatus,
} from '@/types/product'

export const foodApi = {
  async getList(params: ProductPageParams): Promise<ProductPageResponse<Food>> {
    const res = await get<ProductPageResponse<FoodBackend>>(
      '/v1/product-center/foods',
      params as unknown as Record<string, unknown>
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: productDataConverter.toFrontendList(res.records || []),
    }
  },

  async getById(id: number): Promise<Food> {
    const res = await get<FoodBackend>(`/v1/product-center/foods/${id}`)
    return productDataConverter.toFrontend(res)
  },

  async create(data: FoodFormData): Promise<Food> {
    const dto = productDataConverter.toCreateDTO(data)
    const res = await post<FoodBackend>('/v1/product-center/foods', dto)
    return productDataConverter.toFrontend(res)
  },

  async update(id: number, data: FoodFormData): Promise<Food> {
    const dto = productDataConverter.toUpdateDTO(data)
    const res = await put<FoodBackend>(`/v1/product-center/foods/${id}`, dto)
    return productDataConverter.toFrontend(res)
  },

  async delete(id: number): Promise<void> {
    await del<void>(`/v1/product-center/foods/${id}`)
  },

  /**
   * 更新菜品状态
   * 使用 converters.ts 统一的状态映射：active=1, inactive=0, soldout=2
   */
  async updateStatus(id: number, status: ProductStatus): Promise<void> {
    const statusCode = productDataConverter.statusToBackend(status)
    await put<void>(`/v1/product-center/foods/${id}/status/${statusCode}`)
  },

  /**
   * 批量更新菜品状态
   * 使用 converters.ts 统一的状态映射：active=1, inactive=0, soldout=2
   */
  async batchUpdateStatus(foodIds: number[], status: ProductStatus): Promise<void> {
    const statusCode = productDataConverter.statusToBackend(status)
    await put<void>('/v1/product-center/foods/batch-status', { foodIds, status: statusCode })
  },

  async batchDelete(foodIds: number[]): Promise<void> {
    // DELETE 请求体通过 options.data 传递
    await del<void>('/v1/product-center/foods/batch', undefined, { data: foodIds })
  },

  async listOnSale(): Promise<Food[]> {
    const res = await get<FoodBackend[]>('/v1/product-center/foods/on-sale')
    return productDataConverter.toFrontendList(res || [])
  },

  async listByCategory(categoryId: number): Promise<Food[]> {
    const res = await get<FoodBackend[]>(`/v1/product-center/foods/category/${categoryId}`)
    return productDataConverter.toFrontendList(res || [])
  },

  /** 获取推荐菜品列表 */
  async listRecommend(limit: number = 10): Promise<Food[]> {
    const res = await get<FoodBackend[]>('/v1/product-center/foods/recommend', { limit })
    return productDataConverter.toFrontendList(res || [])
  },

  /**
   * 导入菜品（Excel）
   * 通过 FormData 上传 Excel 文件，后端解析并批量创建
   */
  async importFoods(file: File): Promise<BatchUploadResult> {
    const formData = new FormData()
    formData.append('file', file)
    return post<BatchUploadResult>('/v1/product-center/foods/import', formData)
  },

  /**
   * 导出菜品（Excel）
   * 根据查询条件导出匹配的菜品数据，返回 Blob 供前端触发下载
   */
  async exportFoods(params: ProductPageParams): Promise<Blob> {
    return get<Blob>(
      '/v1/product-center/foods/export',
      params as unknown as Record<string, unknown>,
      { responseType: 'blob' }
    )
  },

  /**
   * 下载导入模板（Excel）
   * 返回包含表头和示例数据的 Excel 模板文件
   */
  async downloadImportTemplate(): Promise<Blob> {
    return get<Blob>(
      '/v1/product-center/foods/import-template',
      {},
      { responseType: 'blob' }
    )
  },
}

export default foodApi
