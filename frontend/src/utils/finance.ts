// 财务相关工具函数

/**
 * 格式化金额 * @param amount 金额数值或字符 * @returns 格式化后的金额字符串
 */
export const formatMoney = (amount: string | number): string => {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  const formatted = new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num);
  // 确保负数符号在人民币符号之后
return formatted.replace(/^-¥/, '¥-');
};

/**
 * 获取变化率样式类
 * @param change 变化率数值或字符 * @returns 样式类名
 */
export const getChangeClass = (change: string | number): string => {
  const value = typeof change === 'string' ? parseFloat(change) : change;
  if (value > 0) return 'change-positive';
  if (value < 0) return 'change-negative';
  return 'change-neutral';
};

/**
 * 获取利润率样式类
 * @param margin 利润率数值或字符 * @returns 样式类名
 */
export const getMarginClass = (margin: string | number): string => {
  const value = typeof margin === 'string' ? parseFloat(margin) : margin;
  if (value >= 20) return 'margin-high';
  if (value < 10) return 'margin-low';
  return '';
};

/**
 * 计算财务总计数据
 * @param data 财务汇总数据数 * @returns 总计数据
 */
export const calculateSummaryTotals = (data: Array<{ revenue: string; expense: string; profit: string }>) => {
  return data.reduce(
    (totals, row) => {
      return {
        revenue: totals.revenue + parseFloat(row.revenue),
        expense: totals.expense + parseFloat(row.expense),
        profit: totals.profit + parseFloat(row.profit)
      };
    },
{ revenue: 0,
expense: 0,
profit: 0 }
  );
};

/**
 * 获取统计卡片的样式类 * @param index 卡片索引
 * @returns 样式类名
 */
export const getStatCardClass = (index: number): string => {
  const classes = [
    'stat-primary',
    'stat-danger',
    'stat-success',
    'stat-warning'
  ];
  return classes[index % classes.length];
};

/**
 * 表格行类名生成器
 * @param rowIndex 行索 * @returns 样式类名
 */
export const tableRowClassName = ({ rowIndex }: { rowIndex: number }): string => {
  return rowIndex % 2 === 1 ? 'even-row' : 'odd-row';
};
