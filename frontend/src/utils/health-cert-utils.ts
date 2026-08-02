/**
 * 健康证管理工具函 * 提供状态映射、文本转换、日期格式化等通用功能
 */

/**
 * 健康证状态类型定
 */
export type HealthCertStatus = 'valid' | 'expiring' | 'expired';

/**
 * 报销状态类型定
 */
export type ExpenseStatus = 'pending' | 'approved' | 'reimbursed' | 'rejected';

/**
 * 获取健康证状态的Element Plus标签类型
 * @param status 健康证状 * @returns Element Plus标签类型
 */
export const getHealthCertStatusType = (
  status: HealthCertStatus
): 'info' | 'success' | 'warning' | 'primary' | 'danger' => {
  switch (status) {
    case 'valid':
      return 'success';
    case 'expiring':
      return 'warning';
    case 'expired':
      return 'danger';
    default:
      return 'info';
  }
};

/**
 * 获取健康证状态的中文文本
 * @param status 健康证状 * @returns 状态文
 */
export const getHealthCertStatusText = (status: HealthCertStatus): string => {
  switch (status) {
    case 'valid':
      return '有效';
    case 'expiring':
      return '即将过期';
    case 'expired':
      return '已过';
    default:
      return '';
  }
};

/**
 * 获取报销状态的Element Plus标签类型
 * @param status 报销状 * @returns Element Plus标签类型
 */
export const getExpenseStatusType = (
  status: ExpenseStatus
): 'info' | 'success' | 'warning' | 'primary' | 'danger' => {
  switch (status) {
    case 'pending':
      return 'warning';
    case 'approved':
      return 'success';
    case 'reimbursed':
      return 'primary';
    case 'rejected':
      return 'danger';
    default:
      return 'info';
  }
};

/**
 * 获取报销状态的中文文本
 * @param status 报销状 * @returns 状态文
 */
export const getExpenseStatusText = (status: ExpenseStatus): string => {
  switch (status) {
    case 'pending':
      return '待审核';
    case 'approved':
      return '已审核';
    case 'reimbursed':
      return '已报销';
    case 'rejected':
      return '已拒绝';
    default:
      return '';
  }
};

/**
 * 格式化剩余天数显 * @param expiryDays 剩余天数
 * @returns 格式化后的文
 */
export const formatExpiryDays = (expiryDays: number): string => {
  if (expiryDays <= 0) {
    return '已过';
  }
  return `${expiryDays}天`;
};

/**
 * 判断剩余天数的文本样式类
 * @param expiryDays 剩余天数
 * @returns CSS类名
 */
export const getExpiryDaysClass = (expiryDays: number): string => {
  if (expiryDays <= 0) {
    return 'text-danger';
  }
  if (expiryDays <= 30) {
    return 'text-warning';
  }
  return '';
};

/**
 * 格式化日期显 * @param date 日期字符 * @returns 格式化后的日
 */
export const formatDate = (date: string | undefined): string => {
  if (!date) return '-';
  return date;
};

/**
 * 格式化金额显 * @param amount 金额
 * @returns 格式化后的金额文
 */
export const formatAmount = (amount: number | undefined): string => {
  if (amount === undefined || amount === null) return '-';
  return `${amount}元`;
};

/**
 * 门店选项配置
 */
export const STORE_OPTIONS = [
{ label: '北京朝阳门店',
value: '北京朝阳门店'},
{ label: '上海静安门店',
value: '上海静安门店'},
{ label: '广州天河门店',
value: '广州天河门店'},
{ label: '深圳南山门店',
value: '深圳南山门店'},
{ label: '杭州西湖门店',
value: '杭州西湖门店'},
{ label: '成都锦江门店',
value: '成都锦江门店'},
{ label: '武汉江汉门店',
value: '武汉江汉门店'},
{ label: '南京玄武门店',
value: '南京玄武门店' }
];

/**
 * 健康证状态选项
 */
export const HEALTH_CERT_STATUS_OPTIONS = [
{ label: '全部',
value: '' },
{ label: '有效',
value: 'valid' },
{ label: '即将过期',
value: 'expiring' },
{ label: '已过',
value: 'expired' }
];

/**
 * 报销状态选项
 */
export const EXPENSE_STATUS_OPTIONS = [
{ label: '全部',
value: '' },
{ label: '待审核',
value: 'pending' },
{ label: '已审核',
value: 'approved' },
{ label: '已报销',
value: 'reimbursed' },
{ label: '已拒绝',
value: 'rejected' }
];
