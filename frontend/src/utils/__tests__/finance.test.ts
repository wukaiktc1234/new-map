import { describe, it, expect } from 'vitest';
import { 
  formatMoney, 
  getChangeClass, 
  getMarginClass, 
  calculateSummaryTotals, 
  getStatCardClass, 
  tableRowClassName 
} from '../finance';

describe('finance工具函数测试', () => {
  describe('formatMoney', () => {
    it('应该正确格式化正数金', () => {
      expect(formatMoney(1000)).toBe('¥1,000.00');
      expect(formatMoney('1000')).toBe('¥1,000.00');
    });

    it('应该正确格式化小数金', () => {
      expect(formatMoney(1234.56)).toBe('¥1,234.56');
      expect(formatMoney('1234.56')).toBe('¥1,234.56');
    });

    it('应该正确格式化负数金', () => {
      expect(formatMoney(-500)).toBe('¥-500.00');
      expect(formatMoney('-500')).toBe('¥-500.00');
    });

    it('应该正确格式化零金额', () => {
      expect(formatMoney(0)).toBe('¥0.00');
      expect(formatMoney('0')).toBe('¥0.00');
    });
  });

  describe('getChangeClass', () => {
    it('应该返回正变化率的样式类', () => {
      expect(getChangeClass(5.2)).toBe('change-positive');
      expect(getChangeClass('5.2')).toBe('change-positive');
    });

    it('应该返回负变化率的样式类', () => {
      expect(getChangeClass(-3.8)).toBe('change-negative');
      expect(getChangeClass('-3.8')).toBe('change-negative');
    });

    it('应该返回零变化率的样式类', () => {
      expect(getChangeClass(0)).toBe('change-neutral');
      expect(getChangeClass('0')).toBe('change-neutral');
    });
  });

  describe('getMarginClass', () => {
    it('应该返回高利润率的样式类', () => {
      expect(getMarginClass(25)).toBe('margin-high');
      expect(getMarginClass('25')).toBe('margin-high');
    });

    it('应该返回低利润率的样式类', () => {
      expect(getMarginClass(8)).toBe('margin-low');
      expect(getMarginClass('8')).toBe('margin-low');
    });

    it('应该返回中等利润率的样式', () => {
      expect(getMarginClass(15)).toBe('');
      expect(getMarginClass('15')).toBe('');
    });
  });

  describe('calculateSummaryTotals', () => {
    it('应该正确计算财务总计', () => {
      const testData = [
{ revenue: '1000',
expense: '500',
profit: '500' },
{ revenue: '2000',
expense: '1200',
profit: '800' },
{ revenue: '1500',
expense: '900',
profit: '600' }
      ];

      const result = calculateSummaryTotals(testData);

      expect(result).toEqual({
        revenue: 4500,
        expense: 2600,
        profit: 1900
      });
    });

    it('应该正确处理空数据', () => {
      const result = calculateSummaryTotals([]);

      expect(result).toEqual({
        revenue: 0,
        expense: 0,
        profit: 0
      });
    });

    it('应该正确处理小数金额', () => {
      const testData = [
{ revenue: '1000.50',
expense: '500.25',
profit: '500.25' },
{ revenue: '2000.75',
expense: '1200.50',
profit: '800.25' }
      ];

      const result = calculateSummaryTotals(testData);

      expect(result.revenue).toBeCloseTo(3001.25);
      expect(result.expense).toBeCloseTo(1700.75);
      expect(result.profit).toBeCloseTo(1300.50);
    });
  });

  describe('getStatCardClass', () => {
    it('应该返回正确的卡片样式类', () => {
      expect(getStatCardClass(0)).toBe('stat-primary');
      expect(getStatCardClass(1)).toBe('stat-danger');
      expect(getStatCardClass(2)).toBe('stat-success');
      expect(getStatCardClass(3)).toBe('stat-warning');
      expect(getStatCardClass(4)).toBe('stat-primary'); // 循环'
    });
  });

  describe('tableRowClassName', () => {
    it('应该返回奇数行的样式', () => {
      expect(tableRowClassName({ rowIndex: 0 })).toBe('odd-row');
      expect(tableRowClassName({ rowIndex: 2 })).toBe('odd-row');
    });

    it('应该返回偶数行的样式', () => {
      expect(tableRowClassName({ rowIndex: 1 })).toBe('even-row');
      expect(tableRowClassName({ rowIndex: 3 })).toBe('even-row');
    });
  });
});
