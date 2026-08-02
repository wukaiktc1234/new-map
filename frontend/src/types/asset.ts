/**
 * Asset资产管理模块 - TypeScript类型定义
 *
 * 【层级】L2 - 类型定义层(Type)
 * 【遵循规范】docs/spec/13-编码规范.md (TypeScript章节)
 * 【严格模式】noImplicitAny=true, strictNullChecks=true
 *
 * 已合并 finance-asset.ts，统一管理资产全生命周期类型
 */

// ========== 枚举与常量 ==========

/** 资产状态枚举（7种，覆盖资产全生命周期） */
export enum AssetStatus {
  /** 在用 - 正常使用中 */
  ACTIVE = 'active',
  /** 闲置 - 暂未使用，可重新分配 */
  IDLE = 'idle',
  /** 维修中 - 正在维修 */
  MAINTENANCE = 'maintenance',
  /** 待处置 - 已提交处置申请，等待审批 */
  TO_BE_DISPOSED = 'to_be_disposed',
  /** 已处置 - 已完成处置（出售/捐赠/调拨出） */
  DISPOSED = 'disposed',
  /** 已报废 - 已报废销账 */
  SCRAPPED = 'scrapped',
  /** 已调拨 - 已调拨至其他门店/部门 */
  TRANSFERRED = 'transferred',
}

/** 资产状态 → StatusTag映射 */
export const AssetStatusTagMap: Record<AssetStatus, { status: string; label: string }> = {
  [AssetStatus.ACTIVE]: { status: 'active', label: '在用' },
  [AssetStatus.IDLE]: { status: 'warning', label: '闲置' },
  [AssetStatus.MAINTENANCE]: { status: 'info', label: '维修中' },
  [AssetStatus.TO_BE_DISPOSED]: { status: 'error', label: '待处置' },
  [AssetStatus.DISPOSED]: { status: 'inactive', label: '已处置' },
  [AssetStatus.SCRAPPED]: { status: 'inactive', label: '已报废' },
  [AssetStatus.TRANSFERRED]: { status: 'info', label: '已调拨' },
}

/** 资产状态选项（用于下拉选择） */
export const AssetStatusOptions = Object.entries(AssetStatusTagMap).map(([value, { label }]) => ({
  value,
  label,
}))

/** 折旧方法枚举 */
export enum DepreciationMethod {
  /** 直线法 */
  STRAIGHT_LINE = 'straight_line',
  /** 双倍余额递减法 */
  DOUBLE_DECLINING = 'double_declining',
  /** 年数总和法 */
  SUM_OF_YEARS = 'sum_of_years',
}

/** 折旧方法选项 */
export const DepreciationMethodOptions = [
  { value: DepreciationMethod.STRAIGHT_LINE, label: '直线法' },
  { value: DepreciationMethod.DOUBLE_DECLINING, label: '双倍余额递减法' },
  { value: DepreciationMethod.SUM_OF_YEARS, label: '年数总和法' },
]

/** 盘点状态枚举 */
export enum InventoryStatus {
  /** 草稿 */
  DRAFT = 'draft',
  /** 进行中 */
  IN_PROGRESS = 'in_progress',
  /** 已完成 */
  COMPLETED = 'completed',
  /** 已取消 */
  CANCELLED = 'cancelled',
}

/** 盘点状态 → StatusTag映射 */
export const InventoryStatusTagMap: Record<InventoryStatus, { status: string; label: string }> = {
  [InventoryStatus.DRAFT]: { status: 'info', label: '草稿' },
  [InventoryStatus.IN_PROGRESS]: { status: 'warning', label: '进行中' },
  [InventoryStatus.COMPLETED]: { status: 'active', label: '已完成' },
  [InventoryStatus.CANCELLED]: { status: 'inactive', label: '已取消' },
}

/** 盘点类型 */
export type InventoryCheckType = 'full' | 'sample' | 'cyclic'

/** 盘点类型选项 */
export const InventoryCheckTypeOptions = [
  { value: 'full', label: '全盘' },
  { value: 'sample', label: '抽盘' },
  { value: 'cyclic', label: '循环盘点' },
]

/** 处置类型枚举 */
export enum DisposalType {
  /** 出售 */
  SALE = 'sale',
  /** 报废 */
  SCRAP = 'scrap',
  /** 捐赠 */
  DONATE = 'donate',
  /** 调拨出 */
  TRANSFER_OUT = 'transfer_out',
}

/** 处置类型选项 */
export const DisposalTypeOptions = [
  { value: DisposalType.SALE, label: '出售' },
  { value: DisposalType.SCRAP, label: '报废' },
  { value: DisposalType.DONATE, label: '捐赠' },
  { value: DisposalType.TRANSFER_OUT, label: '调拨出' },
]

/** 处置审批状态 */
export enum DisposalStatus {
  /** 待审批 */
  PENDING = 'pending',
  /** 审批通过 */
  APPROVED = 'approved',
  /** 已完成（已执行处置） */
  COMPLETED = 'completed',
  /** 已驳回 */
  REJECTED = 'rejected',
}

/** 处置审批状态 → StatusTag映射 */
export const DisposalStatusTagMap: Record<DisposalStatus, { status: string; label: string }> = {
  [DisposalStatus.PENDING]: { status: 'warning', label: '待审批' },
  [DisposalStatus.APPROVED]: { status: 'info', label: '已审批' },
  [DisposalStatus.COMPLETED]: { status: 'active', label: '已完成' },
  [DisposalStatus.REJECTED]: { status: 'error', label: '已驳回' },
}

/** 维修状态 */
export type MaintenanceStatus = 'pending' | 'in_progress' | 'completed'

/** 维修状态 → StatusTag映射 */
export const MaintenanceStatusTagMap: Record<MaintenanceStatus, { status: string; label: string }> = {
  pending: { status: 'warning', label: '待维修' },
  in_progress: { status: 'info', label: '维修中' },
  completed: { status: 'active', label: '已完成' },
}

/** 维修类型 */
export type RepairType = 'internal' | 'external'

/** 维修类型选项 */
export const RepairTypeOptions = [
  { value: 'internal', label: '内部维修' },
  { value: 'external', label: '外部送修' },
]

/** 调拨类型 */
export type TransferType = 'between_departments' | 'between_stores'

/** 调拨类型选项 */
export const TransferTypeOptions = [
  { value: 'between_departments', label: '部门间调拨' },
  { value: 'between_stores', label: '门店间调拨' },
]

/** 调拨状态 */
export enum TransferStatus {
  /** 待审批 */
  PENDING = 'pending',
  /** 已审批 */
  APPROVED = 'approved',
  /** 已完成 */
  COMPLETED = 'completed',
  /** 已驳回 */
  REJECTED = 'rejected',
}

/** 调拨状态 → StatusTag映射 */
export const TransferStatusTagMap: Record<TransferStatus, { status: string; label: string }> = {
  [TransferStatus.PENDING]: { status: 'warning', label: '待审批' },
  [TransferStatus.APPROVED]: { status: 'info', label: '已审批' },
  [TransferStatus.COMPLETED]: { status: 'active', label: '已完成' },
  [TransferStatus.REJECTED]: { status: 'error', label: '已驳回' },
}

// ========== 核心实体接口 ==========

/** 资产主表实体 */
export interface Asset {
  /** 资产ID（雪花算法生成） */
  id: string
  /** 资产编号（唯一，如FA-2024-001） */
  assetCode: string
  /** 资产名称 */
  assetName: string
  /** 分类ID */
  categoryId: string
  /** 分类名称（冗余字段，避免联表查询） */
  categoryName?: string
  /** 原值（单位：分） */
  originalValue: number
  /** 净值（单位：分） */
  currentValue: number
  /** 累计折旧（单位：分） */
  accumulatedDepreciation: number
  /** 购置日期 (YYYY-MM-DD) */
  purchaseDate: string
  /** 使用年限（月） */
  usefulLifeMonths: number
  /** 已使用月数 */
  usedMonths: number
  /** 折旧方法 */
  depreciationMethod: DepreciationMethod
  /** 残值率（百分比，如5表示5%） */
  salvageRate: number
  /** 使用部门ID */
  departmentId: string
  /** 使用部门名称 */
  departmentName?: string
  /** 门店ID */
  storeId?: string
  /** 门店名称 */
  storeName?: string
  /** 存放位置 */
  location: string
  /** 资产状态 */
  status: AssetStatus
  /** 负责人ID */
  responsibleUserId?: string
  /** 负责人姓名 */
  responsibleUserName?: string
  /** 供应商ID */
  supplierId?: string
  /** 供应商名称 */
  supplierName?: string
  /** 规格型号 */
  specification?: string
  /** 序列号/资产标签号 */
  serialNumber?: string
  /** 备注 */
  remark?: string
  /** 实物照片URL列表 */
  photoUrls?: string[]
  /** 创建时间 (ISO 8601) */
  createTime: string
  /** 更新时间 (ISO 8601) */
  updateTime: string
}

/** 资产分类实体 */
export interface AssetCategory {
  /** 分类ID */
  id: string
  /** 父分类ID（顶级分类为null或"0"） */
  parentId: string | null
  /** 分类编码 */
  code: string
  /** 分类名称 */
  name: string
  /** 排序序号 */
  sortOrder: number
  /** 折旧年限（月，覆盖默认值） */
  usefulLifeMonthsOverride?: number
  /** 残值率（覆盖默认值） */
  salvageRateOverride?: number
  /** 关联资产数量（统计字段） */
  assetCount?: number
  /** 分类总值（分） */
  totalValue?: number
  /** 月折旧额（分） */
  monthlyDepreciation?: number
  /** 子分类列表（树形结构时使用） */
  children?: AssetCategory[]
  /** 创建时间 */
  createTime: string
}

/** 折旧记录实体 */
export interface DepreciationRecord {
  /** 记录ID */
  id: string
  /** 资产ID */
  assetId: string
  /** 资产编号 */
  assetCode?: string
  /** 资产名称 */
  assetName?: string
  /** 折旧期间 (YYYY-MM) */
  period: string
  /** 本期折旧额（分） */
  depreciationAmount: number
  /** 累计折旧（分） */
  accumulatedDepreciation: number
  /** 折旧后净值（分） */
  netValue: number
  /** 折旧方法 */
  method: DepreciationMethod
  /** 是否手动调整 */
  isManualAdjustment: boolean
  /** 调整原因（手动调整时必填） */
  adjustmentReason?: string
  /** 计算时间 */
  calculatedAt: string
  /** 操作人ID */
  operatorId?: string
  /** 操作人姓名 */
  operatorName?: string
}

/** 盘点任务实体 */
export interface InventoryCheck {
  /** 任务ID */
  id: string
  /** 任务编号 */
  checkCode: string
  /** 任务标题 */
  title: string
  /** 盘点状态 */
  status: InventoryStatus
  /** 盘点类型（全盘/抽盘/循环盘点） */
  checkType: InventoryCheckType
  /** 计划开始时间 */
  plannedStartTime: string
  /** 计划结束时间 */
  plannedEndTime: string
  /** 实际开始时间 */
  actualStartTime?: string
  /** 实际结束时间 */
  actualEndTime?: string
  /** 发起人ID */
  creatorId: string
  /** 发起人姓名 */
  creatorName: string
  /** 审核人姓名 */
  auditorName?: string
  /** 盘点范围描述 */
  scopeDescription?: string
  /** 总资产数 */
  totalAssetCount: number
  /** 已盘点数 */
  countedCount: number
  /** 盘盈数量 */
  overageCount: number
  /** 盘亏数量 */
  shortageCount: number
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
}

/** 盘点明细项 */
export interface InventoryCheckItem {
  /** 明细ID */
  id: string
  /** 盘点任务ID */
  checkId: string
  /** 资产ID */
  assetId: string
  /** 资产编号 */
  assetCode: string
  /** 资产名称 */
  assetName: string
  /** 账面数量 */
  bookQuantity: number
  /** 实盘数量 */
  actualQuantity: number
  /** 差异数量（正=盘盈，负=盘亏） */
  diffQuantity: number
  /** 差异原因 */
  diffReason?: string
  /** 盘点人ID */
  counterId?: string
  /** 盘点人姓名 */
  counterName?: string
  /** 盘点时间 */
  countedAt?: string
}

/** 维修记录实体 */
export interface MaintenanceRecord {
  /** 记录ID */
  id: string
  /** 资产ID */
  assetId: string
  /** 资产编号 */
  assetCode?: string
  /** 资产名称 */
  assetName?: string
  /** 故障描述 */
  faultDescription: string
  /** 维修类型（内部维修/外部送修） */
  repairType: RepairType
  /** 维修商ID（外部送修时） */
  vendorId?: string
  /** 维修商名称 */
  vendorName?: string
  /** 维修费用（分） */
  repairCost: number
  /** 维修开始时间 */
  startTime: string
  /** 维修完成时间 */
  completedTime?: string
  /** 维修状态 */
  status: MaintenanceStatus
  /** 维修结果描述 */
  resultDescription?: string
  /** 维修人ID */
  repairmanId?: string
  /** 维修人姓名 */
  repairmanName?: string
  /** 创建时间 */
  createTime: string
}

/** 处置记录实体 */
export interface DisposalRecord {
  /** 处置ID */
  id: string
  /** 处置编号 */
  disposalNo: string
  /** 资产ID */
  assetId: string
  /** 资产编号 */
  assetCode: string
  /** 资产名称 */
  assetName: string
  /** 分类名称 */
  categoryName?: string
  /** 处置类型 */
  disposalType: DisposalType
  /** 账面净值（分） */
  netValue: number
  /** 处置金额（分，出售时为售价，报废时为0） */
  disposalAmount: number
  /** 处置损益（分，正=收益，负=损失） */
  gainLoss: number
  /** 审批状态 */
  status: DisposalStatus
  /** 处置日期 */
  disposalDate?: string
  /** 申请人ID */
  applicantId?: string
  /** 申请人姓名 */
  applicantName?: string
  /** 审批人姓名 */
  approverName?: string
  /** 审批时间 */
  approvalTime?: string
  /** 驳回原因 */
  rejectReason?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
}

/** 调拨记录实体 */
export interface AssetTransfer {
  /** 调拨ID */
  id: string
  /** 调拨编号 */
  transferNo: string
  /** 资产ID */
  assetId: string
  /** 资产编号 */
  assetCode: string
  /** 资产名称 */
  assetName: string
  /** 调拨类型 */
  transferType: TransferType
  /** 调出部门ID */
  fromDepartmentId: string
  /** 调出部门名称 */
  fromDepartmentName: string
  /** 调入部门ID */
  toDepartmentId: string
  /** 调入部门名称 */
  toDepartmentName: string
  /** 调出门店ID */
  fromStoreId?: string
  /** 调出门店名称 */
  fromStoreName?: string
  /** 调入门店ID */
  toStoreId?: string
  /** 调入门店名称 */
  toStoreName?: string
  /** 审批状态 */
  status: TransferStatus
  /** 申请人姓名 */
  applicantName: string
  /** 审批人姓名 */
  approverName?: string
  /** 调拨日期 */
  transferDate?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
}

// ========== 财务联动类型（原 finance-asset.ts 合并） ==========

/** 资产统计概览（财务联动） */
export interface AssetStatistics {
  /** 原值合计（分） */
  originalValue: number
  /** 累计折旧合计（分） */
  accumulatedDepreciation: number
  /** 净值合计（分） */
  netValue: number
  /** 月折旧额合计（分） */
  monthlyDepreciation: number
}

/** 财务同步设置 */
export interface SyncSettings {
  /** 是否自动同步 */
  autoSync: boolean
  /** 同步频率 */
  frequency: 'daily' | 'weekly' | 'monthly'
  /** 同步范围 */
  scope: string[]
}

/** 财务同步日志 */
export interface SyncLog {
  id: string
  syncTime: string
  syncType: 'manual' | 'auto'
  status: 'success' | 'failed' | 'partial'
  totalAssets: number
  newAssets: number
  modifiedAssets: number
  disposedAssets: number
  errorMessage?: string
  operator: string
}

// ========== 表单DTO ==========

/** 创建资产DTO */
export interface AssetCreateDTO {
  assetCode: string
  assetName: string
  categoryId: string
  originalValue: number
  purchaseDate: string
  usefulLifeMonths: number
  depreciationMethod: DepreciationMethod
  departmentId: string
  location: string
  responsibleUserId?: string
  supplierId?: string
  specification?: string
  serialNumber?: string
  remark?: string
}

/** 更新资产DTO */
export interface AssetUpdateDTO extends Partial<AssetCreateDTO> {
  id: string
  status?: AssetStatus
}

/** 资产查询DTO */
export interface AssetQueryDTO {
  keyword?: string
  categoryId?: string
  departmentId?: string
  status?: AssetStatus | ''
  purchaseDateStart?: string
  purchaseDateEnd?: string
  originalValueMin?: number
  originalValueMax?: number
  page?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

/** 创建分类DTO */
export interface CategoryCreateDTO {
  parentId: string | null
  code: string
  name: string
  usefulLifeMonthsOverride?: number
  salvageRateOverride?: number
  sortOrder?: number
}

/** 更新分类DTO */
export interface CategoryUpdateDTO {
  id: string
  name?: string
  usefulLifeMonthsOverride?: number
  salvageRateOverride?: number
  sortOrder?: number
}

/** 发起盘点DTO */
export interface InventoryCreateDTO {
  title: string
  checkType: InventoryCheckType
  plannedStartTime: string
  plannedEndTime: string
  scopeDescription?: string
  assetIds?: string[]
}

/** 创建维修记录DTO */
export interface MaintenanceCreateDTO {
  assetId: string
  faultDescription: string
  repairType: RepairType
  vendorId?: string
  estimatedCost?: number
}

/** 更新维修记录DTO（完成维修时使用） */
export interface MaintenanceUpdateDTO {
  id: string
  resultDescription?: string
  repairCost?: number
  completedTime?: string
}

/** 创建处置申请DTO */
export interface DisposalCreateDTO {
  assetId: string
  disposalType: DisposalType
  disposalAmount: number
  disposalDate: string
  remark?: string
}

/** 处置审批DTO */
export interface DisposalApprovalDTO {
  id: string
  approved: boolean
  rejectReason?: string
}

/** 创建调拨申请DTO */
export interface TransferCreateDTO {
  assetId: string
  transferType: TransferType
  fromDepartmentId: string
  toDepartmentId: string
  fromStoreId?: string
  toStoreId?: string
  remark?: string
}

/** 调拨审批DTO */
export interface TransferApprovalDTO {
  id: string
  approved: boolean
  rejectReason?: string
}

/** 折旧手动调整DTO */
export interface DepreciationAdjustDTO {
  assetId: string
  period: string
  adjustedAmount: number
  adjustmentReason: string
}

/** 折旧批量计提DTO */
export interface DepreciationBatchDTO {
  period: string
  assetIds?: string[]
}

// ========== 视图对象(VO) ==========

/** 资产视图对象（用于列表展示） */
export interface AssetVO {
  id: string
  assetCode: string
  assetName: string
  categoryName: string
  originalValueYuan: string
  currentValueYuan: string
  purchaseDate: string
  departmentName: string
  location: string
  status: AssetStatus
  statusLabel: string
  responsibleUserName?: string
  specification?: string
  serialNumber?: string
  updateTime: string
}

/** 资产统计视图对象 */
export interface AssetStatisticsVO {
  totalAssets: number
  totalOriginalValue: number
  totalOriginalValueYuan: string
  totalCurrentValue: number
  totalCurrentValueYuan: string
  thisMonthNewCount: number
  thisMonthNewValue: number
  thisMonthNewValueYuan: string
  activeCount: number
  idleCount: number
  maintenanceCount: number
  toBeDisposedCount: number
  disposedCount: number
  scrappedCount: number
  transferredCount: number
}

/** 分类统计视图对象 */
export interface CategoryStatVO {
  categoryId: string
  categoryName: string
  assetCount: number
  totalOriginalValue: number
  totalOriginalValueYuan: string
  percentage: number
}

/** 月度趋势数据 */
export interface MonthlyTrendItem {
  month: string
  totalCount: number
  totalValue: number
  newValue: number
  depreciatedValue: number
}

/** 分页响应通用结构 */
export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 导入结果 */
export interface ImportResult {
  successCount: number
  failCount: number
  errors: Array<{ row: number; message: string }>
}
