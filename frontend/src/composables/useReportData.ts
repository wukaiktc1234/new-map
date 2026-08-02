import { ref, type Ref } from 'vue'

export interface HourDataItem {
  hour: string
  revenue: number
  orders: number
}

export interface StoreRankItem {
  rank: number
  name: string
  revenue: number
  orders: number
  avgCheck: number
}

export interface CategoryRankItem {
  name: string
  amount: number
  count: number
  ratio: string
}

export interface ContribItem {
  name: string
  revenue: number
  ratio: number
  trend: number
}

export interface MonthDataItem {
  month: string
  revenue: number
  target: number
}

export interface QuarterDataItem {
  quarter: string
  revenue: number
  cost: number
  profit: number
}

export interface ProfitTrendItem {
  month: string
  revenue: number
  cost: number
  profit: number
}

export interface RevenueSplitItem {
  category: string
  revenue: number
  ratio: number
  color: string
}

export interface StoreAchieveItem {
  name: string
  rate: number
}

export interface CostStructureItem {
  item: string
  amount: number
  ratio: number
}

export function useReportData() {
  const dailySummary = ref({
    totalRevenue: 174500,
    orderCount: 2627,
    avgCheck: 66.4,
    peakHour: '19:00',
    turnoverRate: 2.8,
    foodCostRate: 31.2
  })

  const hourlyData = ref<HourDataItem[]>([])
  const dailyStoreRank = ref<StoreRankItem[]>([])
  const dailyCategoryRank = ref<CategoryRankItem[]>([])

  const weeklySummary = ref({
    totalRevenue: 1186200,
    orderCount: 17850,
    avgDailyRevenue: 169457,
    growthRate: 5.2,
    bestDay: '周六',
    worstDay: '周二',
    lastWeekRevenue: 1127800
  })

  const weeklyTrend = ref<Array<{ day: string; revenue: number; orders: number; lastRevenue: number }>>([])
  const weeklyStoreContrib = ref<ContribItem[]>([])

  const monthlySummary = ref({
    totalRevenue: 5120000,
    targetRevenue: 5000000,
    achieveRate: 102.4,
    yoyGrowth: 12.8,
    momGrowth: 3.5
  })

  const monthlyRevenueSplit = ref<RevenueSplitItem[]>([])
  const monthlyStoreAchieve = ref<StoreAchieveItem[]>([])

  const profitSummary = ref({
    grossProfit: 1780000,
    grossRate: 34.8,
    netProfit: 420000,
    netRate: 8.2,
    foodCost: 2560000,
    laborCost: 680000,
    rentCost: 260000,
    breakEvenPoint: 3850000,
    safetyMargin: 24.8,
    laborCostRate: 13.3,
    rentCostRate: 5.1
  })

  const profitTrend = ref<ProfitTrendItem[]>([])
  const costStructure = ref<CostStructureItem[]>([])

  const quarterlySummary = ref({
    totalRevenue: 15800000,
    targetRevenue: 15000000,
    achieveRate: 105.3,
    qoqGrowth: 8.5,
    yoyGrowth: 15.2,
    avgDailyRevenue: 174200
  })

  const quarterlyTrend = ref<QuarterDataItem[]>([])
  const quarterlyStoreRank = ref<StoreRankItem[]>([])

  const yearlySummary = ref({
    totalRevenue: 58600000,
    targetRevenue: 60000000,
    achieveRate: 97.7,
    yoyGrowth: 22.4,
    bestMonth: '10月',
    worstMonth: '3月',
    avgMonthlyRevenue: 4883333
  })

  const yearlyTrend = ref<MonthDataItem[]>([])
  const yearlyCategory = ref<CategoryRankItem[]>([])

  function generateDailyData(): void {
    const hours: HourDataItem[] = []
    for (let h = 6; h <= 23; h++) {
      let baseRev = 500 + Math.random() * 300
      let baseOrd = 8 + Math.random() * 5
      if (h >= 11 && h <= 14) { baseRev *= 2.5; baseOrd *= 2.5 }
      if (h >= 17 && h <= 21) { baseRev *= 3.2; baseOrd *= 3 }
      hours.push({ hour: String(h).padStart(2, '0') + ':00', revenue: Math.round(baseRev), orders: Math.round(baseOrd) })
    }
    hourlyData.value = hours

    dailyStoreRank.value = [
      { rank: 1, name: '春熙路店', revenue: 35280, orders: 512, avgCheck: 68.9 },
      { rank: 2, name: '宽窄巷子店', revenue: 31200, orders: 468, avgCheck: 66.7 },
      { rank: 3, name: '天府大道店', revenue: 28560, orders: 428, avgCheck: 66.7 },
      { rank: 4, name: '锦江店', revenue: 22680, orders: 345, avgCheck: 65.7 },
      { rank: 5, name: '高新店', revenue: 19850, orders: 298, avgCheck: 66.6 },
      { rank: 6, name: '武侯祠店', revenue: 15670, orders: 238, avgCheck: 65.8 },
      { rank: 7, name: '双流店', revenue: 12340, orders: 186, avgCheck: 66.3 },
      { rank: 8, name: '龙泉店', revenue: 8920, orders: 142, avgCheck: 62.8 }
    ]

    dailyCategoryRank.value = [
      { name: '宫保鸡丁', amount: 12800, count: 245, ratio: '7.3%' },
      { name: '麻婆豆腐', amount: 10500, count: 210, ratio: '6.0%' },
      { name: '回锅肉', amount: 9800, count: 178, ratio: '5.6%' },
      { name: '水煮鱼', amount: 9200, count: 145, ratio: '5.3%' },
      { name: '蒜蓉虾', amount: 8500, count: 120, ratio: '4.9%' },
      { name: '酸辣粉', amount: 7200, count: 198, ratio: '4.1%' },
      { name: '担担面', amount: 6800, count: 210, ratio: '3.9%' },
      { name: '红糖糍粑', amount: 5500, count: 168, ratio: '3.2%' }
    ]
  }

  function generateWeeklyData(): void {
    const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    const baseRev = [142000, 148000, 155000, 162000, 178000, 198500, 203200]
    const baseOrd = [2100, 2250, 2380, 2520, 2780, 3100, 3220]
    const lastRev = [135000, 141000, 148000, 155000, 170000, 189000, 194000]

    weeklyTrend.value = days.map((d, i) => ({
      day: d,
      revenue: baseRev[i] + Math.round(Math.random() * 5000),
      orders: baseOrd[i] + Math.round(Math.random() * 100),
      lastRevenue: lastRev[i] + Math.round(Math.random() * 3000)
    }))

    weeklyStoreContrib.value = [
      { name: '春熙路店', revenue: 242600, ratio: 20.4, trend: 8.2 },
      { name: '宽窄巷子店', revenue: 214800, ratio: 18.1, trend: 5.6 },
      { name: '天府大道店', revenue: 196800, ratio: 16.6, trend: 3.1 },
      { name: '锦江店', revenue: 158400, ratio: 13.4, trend: -1.2 },
      { name: '高新店', revenue: 138600, ratio: 11.7, trend: -3.5 },
      { name: '武侯祠店', revenue: 108200, ratio: 9.1, trend: 2.8 },
      { name: '双流店', revenue: 86200, ratio: 7.3, trend: -8.9 },
      { name: '龙泉店', revenue: 40600, ratio: 3.4, trend: -12.3 }
    ]
  }

  function generateMonthlyData(): void {
    monthlyRevenueSplit.value = [
      { category: '堂食', revenue: 3264000, ratio: 63.7, color: 'primary' },
      { category: '外卖', revenue: 1428000, ratio: 27.9, color: 'success' },
      { category: '自提', revenue: 428000, ratio: 8.4, color: 'info' }
    ]

    monthlyStoreAchieve.value = [
      { name: '春熙路', rate: 108 },
      { name: '宽窄巷子', rate: 103 },
      { name: '天府大道', rate: 98 },
      { name: '锦江店', rate: 95 },
      { name: '高新店', rate: 88 }
    ]
  }

  function generateProfitData(): void {
    const months = ['1月', '2月', '3月', '4月', '5月']

    profitTrend.value = months.map(m => ({
      month: m,
      revenue: 900000 + Math.round(Math.random() * 300000),
      cost: 550000 + Math.round(Math.random() * 150000),
      profit: 250000 + Math.round(Math.random() * 100000)
    }))

    costStructure.value = [
      { item: '食材成本', amount: 2560000, ratio: 50.0 },
      { item: '人工成本', amount: 680000, ratio: 13.3 },
      { item: '租金成本', amount: 320000, ratio: 6.3 },
      { item: '能源水电', amount: 180000, ratio: 3.5 },
      { item: '营销费用', amount: 120000, ratio: 2.3 },
      { item: '折旧摊销', amount: 160000, ratio: 3.1 },
      { item: '其他运营', amount: 1080000, ratio: 21.5 }
    ]
  }

  function generateQuarterlyData(): void {
    quarterlyTrend.value = [
      { quarter: 'Q1', revenue: 13800000, cost: 9200000, profit: 4600000 },
      { quarter: 'Q2', revenue: 15200000, cost: 10100000, profit: 5100000 },
      { quarter: 'Q3', revenue: 15800000, cost: 10500000, profit: 5300000 },
      { quarter: 'Q4', revenue: 16500000, cost: 10800000, profit: 5700000 }
    ]

    quarterlyStoreRank.value = [
      { rank: 1, name: '春熙路店', revenue: 3850000, orders: 58200, avgCheck: 66.2 },
      { rank: 2, name: '宽窄巷子店', revenue: 3420000, orders: 51200, avgCheck: 66.8 },
      { rank: 3, name: '天府大道店', revenue: 2980000, orders: 44800, avgCheck: 66.5 },
      { rank: 4, name: '锦江店', revenue: 2360000, orders: 35800, avgCheck: 65.9 },
      { rank: 5, name: '高新店', revenue: 1980000, orders: 29800, avgCheck: 66.4 }
    ]
  }

  function generateYearlyData(): void {
    const months = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
    const baseRev = [3800000, 3600000, 4200000, 4500000, 4800000, 5200000, 5500000, 5600000, 5200000, 5800000, 5000000, 5400000]
    const baseTarget = [4500000, 4300000, 4800000, 5000000, 5200000, 5500000, 5800000, 6000000, 5800000, 6200000, 5500000, 5000000]

    yearlyTrend.value = months.map((m, i) => ({
      month: m,
      revenue: baseRev[i] + Math.round((Math.random() - 0.5) * 400000),
      target: baseTarget[i]
    }))

    yearlyCategory.value = [
      { name: '川菜系列', amount: 22800000, count: 356800, ratio: '38.9%' },
      { name: '火锅/串串', amount: 14200000, count: 189200, ratio: '24.2%' },
      { name: '面食小吃', amount: 9800000, count: 287600, ratio: '16.7%' },
      { name: '饮品甜品', amount: 7200000, count: 168900, ratio: '12.3%' },
      { name: '其他菜品', amount: 4600000, count: 98200, ratio: '7.9%' }
    ]
  }

  function initAllData(): void {
    generateDailyData()
    generateWeeklyData()
    generateMonthlyData()
    generateProfitData()
    generateQuarterlyData()
    generateYearlyData()
  }

  return {
    dailySummary,
    hourlyData,
    dailyStoreRank,
    dailyCategoryRank,
    weeklySummary,
    weeklyTrend,
    weeklyStoreContrib,
    monthlySummary,
    monthlyRevenueSplit,
    monthlyStoreAchieve,
    profitSummary,
    profitTrend,
    costStructure,
    quarterlySummary,
    quarterlyTrend,
    quarterlyStoreRank,
    yearlySummary,
    yearlyTrend,
    yearlyCategory,
    initAllData
  }
}
