/**

 * 本文件提供项目中所有常用的数据格式化功能，


 *
 * formatters.date(new Date())                    // '2024-01-15'
 * formatters.datetime(new Date())                // '2024-01-15 14:30:00'
 * formatters.amount(12345.678)                  // '12,345.68'
 * formatters.phone('13800138000')               // '138****3800'
 * formatters.status('active')                   // '启用'
 * formatters.gender('male')
 * formatters.fileSize(1048576)                 // '1.00 MB'
 *



 */
// 类型定义

/** 日期格式化支持的输出格式
 */
type DateFormat =
  | 'YYYY-MM-DD'         // 2024-01-15
  | 'YYYY-MM-DD HH:mm:ss'// 2024-01-15 14:30:00
  | 'MM/DD/YYYY'         // 01/15/2024
  | 'DD-MM-YYYY'         // 15-01-2024
  | 'YYYY年MM月DD日'
  | 'HH:mm';

/** 状态码映射配置
 */
interface StatusMapping {
  /** 状态码
 */
  code: string;
  /** 显示文本
 */
  label: string;
  /** 标签类型
 */
  type: 'success' | 'warning' | 'danger' | 'info' | 'primary';
}



/**



 * 使用正则一次性替换所有占位符，避免链式replace的误匹配问题

 * @example
 * formatters.date(new Date('2024-01-15'))           // '2024-01-15'
 * formatters.date(new Date(), 'MM/DD/YYYY')        // '01/15/2024'
 * formatters.date('2024-01-15T14:30:00', 'DD-MM-YYYY') // '15-01-2024'
 * formatters.date(new Date(), 'MM-DD')             // '01-15'
 * formatters.date(new Date(), 'HH:mm')             // '14:30'
 */
const formatDate = (
  date: Date | string | number,
  format: DateFormat = 'YYYY-MM-DD',
): string => {
  const d = new Date(date);

  if (isNaN(d.getTime())) {
    return '';
  }

  const replacements: Record<string, string> = {
    YYYY: String(d.getFullYear()),
    MM: String(d.getMonth() + 1).padStart(2, '0'),
    DD: String(d.getDate()).padStart(2, '0'),
    HH: String(d.getHours()).padStart(2, '0'),
    mm: String(d.getMinutes()).padStart(2, '0'),
    ss: String(d.getSeconds()).padStart(2, '0'),
  };

  return format.replace(/YYYY|MM|DD|HH|mm|ss/g, (match) => replacements[match]);
};

/**


 *
 * @example
 * formatters.datetime(new Date())  // '2024-01-15 14:30:00'
 */
const formatDateTime = (
  date: Date | string | number,
): string => {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss');
};



/**

 * @returns 格式化后的金额字符串
 *
 * @example
 * formatters.amount(12345.678)                          // '12,345.68'
 * formatters.amount(12345.678, { prefix: '¥' })        // '¥12,345.68'
 * formatters.amount(12345.678, { decimals: 0 })        // '12,346'
 * formatters.amount(0)                                  // '0.00'
 */
const formatAmount = (
  amount: number,
  options: {
    /** 货币前缀符号，默认无
 */
    prefix: string;
    /** 小数位数，默
 */
    decimals: number;
    /** 是否显示千分位分隔符，默认true
 */
    thousandsSeparator: boolean;
  },
): string => {
  if (typeof amount !== 'number' || isNaN(amount)) {
    return '0.00';
  }

  const {
    prefix = '',
    decimals = 2,
    thousandsSeparator = true,
  } = options || {};

  const fixedAmount = Math.abs(amount).toFixed(decimals);
  let formatted = thousandsSeparator
    ? fixedAmount.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    : fixedAmount;

  if (prefix) {
    formatted = `${prefix}${formatted}`;
  }

  return amount < 0 ? `-${formatted}` : formatted;
};



/**
 * 手机号脱敏处理（隐藏中间4位）
 * @param phone - 手机号字符串

 *
 * @example
 * formatters.phone('13800138000')   // '138****3800'
 * formatters.phone('13800138')      // '138****38' (不足11位时智能处理)
 * formatters.phone('')             // ''
 * formatters.phone(null)          // ''
 */
const formatPhone = (phone: string | null | undefined): string => {
  if (!phone || typeof phone !== 'string') {
    return '';
  }

  const trimmed = phone.trim();

  if (trimmed.length >= 7) {
    // 保留首位和末位，中间替代
    const start = trimmed.substring(0, 3);
    const end = trimmed.substring(trimmed.length - 4);
    return `${start}****${end}`;
  } else {
    const start = trimmed.substring(0, 2);
    const end = trimmed.substring(trimmed.length - 2);
    return `${start}**${end}`;
  }
};



/** 通用状态映射表
 */
const statusMappings: Record<string, StatusMapping> = {
active: {
code: 'active',
label: '启用',
type: 'success' },
inactive: {
code: 'inactive',
label: '停用',
type: 'danger' },
pending: {
code: 'pending',
label: '待处理',
type: 'warning' },
processing: {
code: 'processing',
label: '处理中',
type: 'primary' },
completed: {
code: 'completed',
label: '已完成',
type: 'success' },
cancelled: {
code: 'cancelled',
label: '已取消',
type: 'info' },
rejected: {
code: 'rejected',
label: '已拒绝',
type: 'danger' },
approved: {
code: 'approved',
label: '已审核',
type: 'success' },
enabled: {
code: 'enabled',
label: '正常',
type: 'success' },
disabled: {
code: 'disabled',
label: '禁用',
type: 'danger' },
online: {
code: 'online',
label: '在线',
type: 'success' },
offline: {
code: 'offline',
label: '离线',
type: 'info' },
fault: {
code: 'fault',
label: '故障',
type: 'danger' },
locked: {
code: 'locked',
label: '锁定',
type: 'warning' },
};

/**


 * @example
 * formatters.status('active')       // '启用'
 * formatters.status('inactive')     // '停用'
 * formatters.status('unknown')
 * formatters.status('unknown', '-')
 */
const formatStatus = (
  status: string | null | undefined,
  fallback: string,
): string => {
  if (!status) {
    return fallback || '';
  }

  const mapping = statusMappings[status];
  return mapping.label || fallback || status;
};

/**

 * @example
 * formatters.statusType('active')    // 'success'
 * formatters.statusType('inactive')  // 'danger'
 */
const formatStatusType = (
  status: string | null | undefined,
): 'success' | 'warning' | 'danger' | 'info' | 'primary' | '' => {
  if (!status) {
    return '';
  }

  const mapping = statusMappings[status];
  return mapping.type || '';
};

// 5. 性别码转显示

/** 性别映射
 */
const genderMappings: Record<string, { code: string; label: string }> = {
male: {
code: 'male',
label: '男' },
female: {
code: 'female',
label: '女' },
other: {
code: 'other',
label: '其他' },
};

/**
 * 性别码转中文显示

 * @returns 性别中文文本
 *
 * @example
 * formatters.gender('male')
 * formatters.gender('female')
 * formatters.gender('unknown', '-')  // '-'
 */
const formatGender = (
  gender: string | null | undefined,
  fallback: string,
): string => {
  if (!gender) {
    return fallback || '';
  }

  const mapping = genderMappings[gender];
  return mapping.label || fallback || gender;
};



/**
 * 文件大小格式化（自动选择合适单位）

 *
 * @example
 * formatters.fileSize(0)             // '0 B'
 * formatters.fileSize(1024)          // '1.00 KB'
 * formatters.fileSize(1048576)       // '1.00 MB'
 * formatters.fileSize(1073741824)    // '1.00 GB'
 * formatters.fileSize(1099511627776) // '1.00 TB'
 */
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) {
    return '0 B';
  }

  if (!Number.isFinite(bytes) || bytes < 0) {
    return '0 B';
  }

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  const index = Math.min(i, sizes.length - 1);
  const value = bytes / Math.pow(k, index);

  return `${value.toFixed(2)} ${sizes[index]}`;
};



/**
 * 数字补零（固定位数）


 * @example
 * formatters.padZero(5)     // '05'
 * formatters.padZero(5, 3)  // '005'
 */
const padZero = (num: number | string, length = 2): string => {
  return String(num).padStart(length, '0');
};

/**
 * 百分比格式化

 * @returns 百分比字符串
 *
 * @example
 * formatters.percent(0.756)            // '75.60%'
 * formatters.percent(75.6, { isRaw: true }) // '75.60%'
 */
const percent = (
  value: number,
options: {
decimals: number;
isRaw: boolean },
): string => {
  if (typeof value !== 'number' || isNaN(value)) {
    return '0%';
  }

  const { decimals = 2, isRaw = false } = options || {};
  const displayValue = isRaw ? value : value * 100;
  return `${displayValue.toFixed(decimals)}%`;
};

// 导出统一的formatters对象

/**
 * 统一格式化器对象
 */
export const formatters = {
  /** 日期相关
 */
  date: formatDate,
  datetime: formatDateTime,

  /** 金额相关
 */
  amount: formatAmount,

  /** 手机号脱
 */
  phone: formatPhone,

  /** 状态转
 */
  status: formatStatus,
  statusType: formatStatusType,

  /** 性别转换
 */
  gender: formatGender,

  /** 文件大小
 */
  fileSize: formatFileSize,

  /** 数字工具
 */
  padZero,
  percent,
} as const;

/** 导出单个函数供按需导入
 */
export {
  formatDate,
  formatDateTime,
  formatAmount,
  formatPhone,
  formatStatus,
  formatStatusType,
  formatGender,
  formatFileSize,
  padZero,
  percent,
};
