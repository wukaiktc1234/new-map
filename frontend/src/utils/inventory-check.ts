/**
 * 盘点任务类型定义
 */
export interface InventoryItem {
  productId: number;
  productName: string;
  category: string;
  systemStock: number;
  actualStock: number;
  difference: number;
  unit: string;
  price: number;
  differenceAmount: number;
  remark: string;
}

/**
 * 盘点任务类型定义
 */
export interface InventoryTask {
  id: number;
  taskName: string;
  checkType: CheckType;
  status: CheckStatus;
  warehouseName: string;
  checkDate: string;
  checker: string;
  difference: number;
  remark: string;
  totalItems: number;
  differenceItems: number;
  totalDifference: number;
  differenceAmount: number;
  items: InventoryItem[];
}

/**
 * 盘点类型
 */
export type CheckType = 'regular' | 'temporary';

/**
 * 盘点状
 */
export type CheckStatus = 'pending' | 'in_progress' | 'completed' | 'approved' | 'rejected';

/**
 * 盘点统计数据
 */
export interface CheckStats {
  totalItems: number;
  differenceItems: number;
  totalDifference: number;
  differenceAmount: number;
}

/**
 * 盘点表单数据
 */
export interface CheckFormData {
  id?: number;
  taskName: string;
  checkType: CheckType;
  warehouseId: string;
  checkDate: Date;
  checker: string;
  remark: string;
}

/**
 * 格式化日期
 */
export function formatDate(date: string | Date): string {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * 计算盘点统计数据
 */
export function calculateCheckStats(task: InventoryTask): CheckStats {
  if (!task.items || task.items.length === 0) {
    return {
      totalItems: 0,
      differenceItems: 0,
      totalDifference: 0,
      differenceAmount: 0,
    };
  }

  const totalItems = task.items.length;
  const differenceItems = task.items.filter((item) => item.difference !== 0).length;
  const totalDifference = task.items.reduce((sum, item) => sum + item.difference, 0);
  const differenceAmount = task.items.reduce((sum, item) => sum + item.differenceAmount, 0);

  return {
    totalItems,
    differenceItems,
    totalDifference,
    differenceAmount,
  };
}

/**
 * 处理库存变化，自动计算差
 */
export function handleStockChange(item: InventoryItem): void {
  // 计算差异
  item.difference = item.actualStock - item.systemStock;
  // 计算差异金额
  item.differenceAmount = item.difference * item.price;
}

/**
 * 获取状态标签类
 */
export function getStatusType(status: CheckStatus): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const typeMap: Record<CheckStatus, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: 'info',
    in_progress: 'warning',
    completed: 'success',
    approved: 'primary',
    rejected: 'danger',
  };
  return typeMap[status] || 'info';
}

/**
 * 获取状态文
 */
export function getStatusText(status: CheckStatus): string {
  const textMap: Record<CheckStatus, string> = {
    pending: '待开',
    in_progress: '进行中',
    completed: '已完成',
    approved: '已审核',
    rejected: '已拒绝',
  };
  return textMap[status] || '未知';
}

/**
 * 获取盘点类型文本
 */
export function getCheckTypeText(type: CheckType): string {
  return type === 'regular' ? '定期盘点' : '临时盘点';
}

/**
 * 创建新的盘点任务
 */
export function createNewTask(formData: CheckFormData): InventoryTask {
  return {
    id: Date.now(),
    taskName: formData.taskName,
    checkType: formData.checkType,
    status: 'pending',
    warehouseName: formData.warehouseId === '1' ? '主仓库' : '备用仓库',
    checkDate: formatDate(formData.checkDate),
    checker: formData.checker,
    difference: 0,
    remark: formData.remark || '',
    totalItems: 0,
    differenceItems: 0,
    totalDifference: 0,
    differenceAmount: 0,
    items: [],
  };
}

/**
 * 更新盘点任务
 */
export function updateTask(task: InventoryTask, formData: CheckFormData):
InventoryTask {
  return {
    ...task,
    taskName: formData.taskName,
    checkType: formData.checkType,
    warehouseName: formData.warehouseId === '1' ? '主仓库' : '备用仓库',
    checkDate: formatDate(formData.checkDate),
    checker: formData.checker,
    remark: formData.remark,
  };
}

/**
 * 更新任务状
 */
export function updateTaskStatus(task: InventoryTask, newStatus: CheckStatus):
InventoryTask {
  return {
    ...task,
    status: newStatus,
  };
}

/**
 * 验证盘点表单
 */
export function validateCheckForm(formData: CheckFormData): boolean {
  return !!(formData.taskName && formData.checker);
}
