/**
 * 资产调拨管理API
 *
 * 注意：后端暂无对应的资产调拨 Controller（/v1/asset/transfer）。
 * 所有方法均抛出异常，提示后端接口未实现。
 * 待后端实现后，可参照其他模块（如 disposal.ts）补充真实 API 调用。
 */
import type {
  AssetTransfer,
  TransferCreateDTO,
  TransferApprovalDTO,
  TransferStatus,
  PageResponse,
} from '../../types/asset'

/** 空分页响应（后端未实现时返回空列表） */
function emptyPageResponse<T>(): PageResponse<T> {
  return {
    records: [],
    total: 0,
    current: 1,
    size: 20,
    pages: 0,
  }
}

export const transferApi = {
  /**
   * 获取调拨记录列表
   * 后端无对应接口，返回空列表
   */
  async getList(_params?: { status?: TransferStatus; transferType?: string }): Promise<PageResponse<AssetTransfer>> {
    return emptyPageResponse<AssetTransfer>()
  },

  /**
   * 根据ID获取调拨记录
   * 后端无对应接口，抛出异常提示
   */
  async getById(_id: string): Promise<AssetTransfer | null> {
    throw new Error('后端暂未实现资产调拨接口')
  },

  /**
   * 创建调拨申请
   * 后端无对应接口，抛出异常提示
   */
  async create(_data: TransferCreateDTO): Promise<AssetTransfer> {
    throw new Error('后端暂未实现资产调拨接口')
  },

  /**
   * 更新调拨记录
   * 后端无对应接口，抛出异常提示
   */
  async update(_id: string, _data: Partial<AssetTransfer>): Promise<AssetTransfer | null> {
    throw new Error('后端暂未实现资产调拨接口')
  },

  /**
   * 删除调拨记录
   * 后端无对应接口，抛出异常提示
   */
  async delete(_id: string): Promise<boolean> {
    throw new Error('后端暂未实现资产调拨接口')
  },

  /**
   * 审批调拨申请
   * 后端无对应接口，抛出异常提示
   */
  async approve(_data: TransferApprovalDTO): Promise<AssetTransfer | null> {
    throw new Error('后端暂未实现资产调拨接口')
  },

  /**
   * 驳回调拨申请
   * 后端无对应接口，抛出异常提示
   */
  async reject(_id: string, _rejectReason: string): Promise<AssetTransfer | null> {
    throw new Error('后端暂未实现资产调拨接口')
  },

  /**
   * 完成调拨（确认资产已交接）
   * 后端无对应接口，抛出异常提示
   */
  async complete(_id: string): Promise<AssetTransfer | null> {
    throw new Error('后端暂未实现资产调拨接口')
  },
}
