/**
 * 分类管理API
 * 对应后端: /v1/product-center/categories
 *
 * 使用标准 get/post/put/del 请求，错误由 request 拦截器统一处理。
 * 本文件保留 try/catch 用于将请求异常转换为业务可读的错误信息向上抛出。
 */
import { get, post, put, del } from '@/api/request'
import { productDataConverter } from './converters'
import type {
  CategoryBackend,
  FoodCategory,
  CategoryFormData,
  CategoryOption,
  ProductPageResponse,
  ProductStatus,
} from '@/types/product'

/**
 * 递归过滤树形数据，保留匹配节点及其所有祖先链
 * 若某节点的子树中存在匹配项，则该节点也会被保留（祖先链不断裂）
 * @param categories - 树形分类数据
 * @param predicate - 匹配谓词，返回 true 表示该节点自身匹配
 * @returns 过滤后的新树（不修改原数据）
 */
function filterTree(categories: FoodCategory[], predicate: (c: FoodCategory) => boolean): FoodCategory[] {
  const result: FoodCategory[] = []
  for (const category of categories) {
    const matchedChildren = category.children?.length ? filterTree(category.children, predicate) : []
    const matched = predicate(category)
    if (matched || matchedChildren.length > 0) {
      result.push({ ...category, children: matchedChildren })
    }
  }
  return result
}

export const categoryApi = {
  async getList(params: { page?: number; size?: number; keyword?: string; status?: ProductStatus }): Promise<ProductPageResponse<FoodCategory>> {
    // 使用 getTree() 获取树形数据，保证表格能正确展示父子层级关系
    const tree = await this.getTree()
    let filtered = tree

    // 关键字过滤：保留名称匹配节点及其祖先链
    if (params.keyword) {
      const kw = params.keyword.toLowerCase()
      filtered = filterTree(filtered, c => c.categoryName.toLowerCase().includes(kw))
    }
    // FoodCategory 使用 categoryStatus 字段（经 converter 转换后），而非后端的 status
    if (params.status) {
      filtered = filterTree(filtered, c => c.categoryStatus === params.status)
    }

    const page = params.page || 1
    const size = params.size || 10

    // 树形数据不切片分页，整树返回以支持表格的树形展示（default-expand-all + tree-props.children）
    return {
      records: filtered,
      total: filtered.length,
      current: page,
      size,
      pages: 1,
    }
  },

  async getTree(): Promise<FoodCategory[]> {
    try {
      const res = await get<CategoryBackend[]>('/v1/product-center/categories/tree')
      if (!res) return []
      return res.map((item: CategoryBackend) => productDataConverter.categoryToFrontend(item))
    } catch {
      return []
    }
  },

  async getEnabledTree(): Promise<FoodCategory[]> {
    try {
      const res = await get<CategoryBackend[]>('/v1/product-center/categories/tree/enabled')
      if (!res) return []
      return res.map((item: CategoryBackend) => productDataConverter.categoryToFrontend(item))
    } catch {
      return []
    }
  },

  async listAll(): Promise<FoodCategory[]> {
    try {
      const res = await get<CategoryBackend[]>('/v1/product-center/categories/list')
      if (!res) return []
      return res.map((item: CategoryBackend) => productDataConverter.categoryToFrontend(item))
    } catch {
      return await this.getTree()
    }
  },

  async getById(id: number): Promise<FoodCategory> {
    try {
      const res = await get<CategoryBackend>(`/v1/product-center/categories/${id}`)
      return productDataConverter.categoryToFrontend(res)
    } catch {
      throw new Error('获取分类失败')
    }
  },

  async create(data: CategoryFormData): Promise<FoodCategory> {
    try {
      const dto = productDataConverter.categoryToCreateDTO(data)
      const res = await post<CategoryBackend>('/v1/product-center/categories', dto)
      return productDataConverter.categoryToFrontend(res)
    } catch {
      throw new Error('创建分类失败，后端API不可用')
    }
  },

  async update(id: number, data: CategoryFormData): Promise<FoodCategory> {
    try {
      const dto = productDataConverter.categoryToCreateDTO(data)
      const res = await put<CategoryBackend>(`/v1/product-center/categories/${id}`, dto)
      return productDataConverter.categoryToFrontend(res)
    } catch {
      throw new Error('更新分类失败，后端API不可用')
    }
  },

  async delete(id: number): Promise<void> {
    try {
      await del(`/v1/product-center/categories/${id}`)
    } catch {
      throw new Error('删除分类失败，后端API不可用')
    }
  },

  /**
   * 更新分类状态
   * 使用 converters.ts 统一的状态映射：active=1, inactive=0
   */
  async updateStatus(id: number, status: ProductStatus): Promise<void> {
    try {
      const statusCode = productDataConverter.statusToBackend(status)
      await put(`/v1/product-center/categories/${id}/status/${statusCode}`)
    } catch {
      throw new Error('更新状态失败，后端API不可用')
    }
  },

  async getCategoryOptions(): Promise<CategoryOption[]> {
    try {
      const tree = await this.getEnabledTree()
      if (tree.length > 0) return productDataConverter.categoryListToOptions(tree)
      return []
    } catch {
      return []
    }
  },
}

export default categoryApi
