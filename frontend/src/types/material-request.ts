/**
 * 物资需求提报相关类型定义
 * 对应后端实体: MaterialRequest (material_request, material_request_item)
 *
 * 状态编码（数据库 INTEGER）：
 *   0=草稿 1=待审核 2=已审核 3=已驳回 4=已转采购申请
 * 前端状态字符串：'draft' 'pending' 'approved' 'rejected' 'converted'
 * 通过 DataConverter 在 API 边界转换
 *
 * 金额单位：后端存储分（Long），前端显示元（number）
 */

/**
 * 物资需求提报状态
 */
export type MaterialRequestStatus =
  | 'draft'      // 草稿
  | 'pending'    // 待审核
  | 'approved'   // 已审核
  | 'rejected'   // 已驳回
  | 'converted'; // 已转采购申请

/**
 * 物资需求提报明细
 */
export interface MaterialRequestItem {
  /** 物料ID（关联 material_archives，临时物料为空） */
  materialId: string;
  /** 物料名称 */
  materialName: string;
  /** 规格型号 */
  specification?: string;
  /** 数量 */
  quantity: number;
  /** 单位（kg/斤/袋等） */
  unit: string;
  /** 预估单价（元），门店提报时可不填，由采购部补充 */
  estimatedPrice?: number;
  /** 小计金额（元，quantity × estimatedPrice） */
  subtotalAmount?: number;
  /** 备注 */
  remark: string;
}

/**
 * 物资需求提报信息
 */
export interface MaterialRequestInfo {
  /** 提报ID */
  requestId: string;
  /** 提报单号（格式 MR+yyyyMMdd+4位序号） */
  requestNo: string;
  /** 需求标题 */
  title: string;
  /** 提报门店 */
  storeName: string;
  /** 申请人ID（统一命名） */
  createBy: string;
  /** 申请人姓名（统一命名） */
  createByName: string;
  /** 期望到货日期 */
  expectedDate: string;
  /** 状态 */
  status: MaterialRequestStatus;
  /** 总金额（元） */
  totalAmount: number;
  /** 转换后的采购申请单号（未转换为空） */
  convertedRequestNo: string;
  /** 备注（驳回时附加驳回原因） */
  remark: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
  /** 需求明细列表（详情接口返回） */
  items?: MaterialRequestItem[];
}

/**
 * 物资需求提报查询参数
 */
export interface MaterialRequestQueryForm {
  /** 提报单号 */
  requestNo?: string;
  /** 状态 */
  status?: MaterialRequestStatus | null;
  /** 提报门店 */
  storeName?: string;
  /** 申请人ID（统一命名） */
  createBy?: string;
  /** 开始日期 */
  startDate?: string;
  /** 结束日期 */
  endDate?: string;
  /** 搜索关键词（提报单号/标题模糊匹配） */
  keyword?: string;
}

/**
 * 物资需求提报表单数据（新建/编辑）
 */
export interface MaterialRequestFormData {
  /** 需求标题 */
  title: string;
  /** 提报门店 */
  storeName: string;
  /** 期望到货日期 */
  expectedDate: string;
  /** 备注 */
  remark: string;
  /** 需求明细列表 */
  items: MaterialRequestItem[];
}

/** 物资需求提报状态选项 */
export const MaterialRequestStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审核', value: 'pending' },
  { label: '已审核', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已转采购申请', value: 'converted' },
] as const;
