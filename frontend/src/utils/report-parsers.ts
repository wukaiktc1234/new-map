/**
 * 财务报表解析器模块
 * 负责将后端API返回的原始数据解析为前端表格展示格式
 * @module utils/report-parsers
 */


/** 报表预览列定义 */
export interface PreviewColumn {
  prop?: string;
  label?: string;
  isMoney?: boolean;
}

/** 报表行数据（通用） */
export interface PreviewRow {
  [key: string]: unknown;
  itemName?: string;
  isTotal?: boolean;
  isBold?: boolean;
  isHeader?: boolean;
  isHighlight?: boolean;
  indent?: number;
  /** 资产负债表左侧项目名 */
  leftItemName?: string;
  /** 资产负债表左侧金额 */
  leftBalance?: number;
  /** 缩进层级（左侧） */
  leftIndent?: number;
  /** 左侧加粗标识 */
  leftIsBold?: boolean;
  /** 左侧高亮标识 */
  leftIsHighlight?: boolean;
  /** 资产负债表右侧项目名 */
  rightItemName?: string;
  /** 资产负债表右侧金额 */
  rightBalance?: number;
  /** 缩进层级（右侧） */
  rightIndent?: number;
  /** 右侧加粗标识 */
  rightIsBold?: boolean;
  /** 右侧高亮标识 */
  rightIsHighlight?: boolean;
}

/** 解析结果（列 + 数据行） */
export interface ParseResult {
  columns?: PreviewColumn[];
  rows: PreviewRow[];
}


/**
 * 安全提取数值字段
 * 从数据对象中按key提取number类型值，不存在或非数字时返回fallback默认值
 */
function extractNumber(data: Record<string, unknown>, key: string, fallback = 0): number {
  const val = data[key];
  return typeof val === 'number' ? val : fallback;
}

/**
 * 计算变动率百分比字符串
 * @param current - 本期数
 * @param previous - 上期数
 * @returns 变动率字符串，如 '12.50%'、'-'、'N/A'
 */
function calcChangeRate(current: number, previous: number): string {
  if (previous === 0) return current === 0 ? '-' : 'N/A';
  return ((current - previous) / previous * 100).toFixed(2) + '%';
}


/**
 * 解析资产负债表数据为左右分栏标准格式
 *
 * 标准资产负债表采用左右对照式布局：
 * - 左侧：资产（流动资产、非流动资产）
 * - 右侧：负债和所有者权益（流动负债、非流动负债、所有者权益）
 *
 * 数据来源优先使用API真实数据，缺失时使用演示默认值
 *
 * @param data - 后端返回的原始数据对象，字段名为下划线风格
 * @returns 解析后的列定义和数据
 */
export function parseBalanceSheet(data: Record<string, unknown>): ParseResult {
  const d = data || {};

  /* ---------- 流动资产 ---------- */
  const cash = extractNumber(d, 'cash', 156780);
  const accountsReceivable = extractNumber(d, 'accounts_receivable', 89320);
  const inventory = extractNumber(d, 'inventory', 234500);
  const prepayments = extractNumber(d, 'prepayments', 12800);
  const otherCurrentAssets = extractNumber(d, 'other_current_assets', 5200);
  const totalCurrentAssets = cash + accountsReceivable + inventory + prepayments + otherCurrentAssets;

  /* ---------- 非流动资产 ---------- */
  const fixedAssetsOriginal = extractNumber(d, 'fixed_assets_original', 520000);
  const accumulatedDepreciation = extractNumber(d, 'accumulated_depreciation', -128000);
  const fixedAssetsNet = fixedAssetsOriginal + accumulatedDepreciation;
  const intangibleAssets = extractNumber(d, 'intangible_assets', 45000);
  const longTermReceivables = extractNumber(d, 'long_term_receivables', 20000);
  const totalNonCurrentAssets = fixedAssetsNet + intangibleAssets + longTermReceivables;
  const totalAssets = totalCurrentAssets + totalNonCurrentAssets;

  /* ---------- 流动负债 ---------- */
  const accountsPayable = extractNumber(d, 'accounts_payable', 95600);
  const employeeCompensation = extractNumber(d, 'employee_compensation', 38500);
  const taxesPayable = extractNumber(d, 'taxes_payable', 18600);
  const shortTermBorrowing = extractNumber(d, 'short_term_borrowing', 50000);
  const totalCurrentLiabilities = accountsPayable + employeeCompensation + taxesPayable + shortTermBorrowing;

  /* ---------- 非流动负债 ---------- */
  const longTermBorrowing = extractNumber(d, 'long_term_borrowing', 100000);
  const totalNonCurrentLiabilities = longTermBorrowing;
  const totalLiabilities = totalCurrentLiabilities + totalNonCurrentLiabilities;

  /* ---------- 所有者权益 ---------- */
  const paidInCapital = extractNumber(d, 'paid_in_capital', 500000);
  const capitalReserve = extractNumber(d, 'capital_reserve', 30000);
  const retainedEarnings = extractNumber(d, 'retained_earnings', 189000);
  const totalEquity = paidInCapital + capitalReserve + retainedEarnings;
  const totalLiabilitiesAndEquity = totalLiabilities + totalEquity;

  // 资产负债表使用自定义分栏渲染，columns留空由模板处理

  return {
    columns: [],
    rows: [
      // ===== 流动资产 vs 流动负债 =====
      {
        leftItemName: '一、流动资产',
        leftBalance: undefined,
        leftIndent: 0,
        isHeader: true,
        rightItemName: '一、流动负债',
        rightBalance: undefined,
        rightIndent: 0
      },
      {
        leftItemName: '货币资金',
        leftBalance: cash,
        leftIndent: 1,
        rightItemName: '短期借款',
        rightBalance: shortTermBorrowing,
        rightIndent: 1
      },
      {
        leftItemName: '应收账款',
        leftBalance: accountsReceivable,
        leftIndent: 1,
        rightItemName: '应付账款',
        rightBalance: accountsPayable,
        rightIndent: 1
      },
      {
        leftItemName: '存货',
        leftBalance: inventory,
        leftIndent: 1,
        rightItemName: '应付职工薪酬',
        rightBalance: employeeCompensation,
        rightIndent: 1
      },
      {
        leftItemName: '预付款项',
        leftBalance: prepayments,
        leftIndent: 1,
        rightItemName: '应交税费',
        rightBalance: taxesPayable,
        rightIndent: 1
      },
      {
        leftItemName: '其他流动资产',
        leftBalance: otherCurrentAssets,
        leftIndent: 1,
        rightItemName: '',
        rightBalance: undefined,
        rightIndent: 1
      },
      {
        leftItemName: '流动资产合计',
        leftBalance: totalCurrentAssets,
        leftIndent: 0,
        leftIsBold: true,
        leftIsHighlight: true,
        rightItemName: '流动负债合计',
        rightBalance: totalCurrentLiabilities,
        rightIndent: 0,
        rightIsBold: true
      },

      // ===== 非流动资产 vs 非流动负债 =====
      {
        leftItemName: '二、非流动资产',
        leftBalance: undefined,
        leftIndent: 0,
        isHeader: true,
        rightItemName: '二、非流动负债',
        rightBalance: undefined,
        rightIndent: 0
      },
      {
        leftItemName: '固定资产原价',
        leftBalance: fixedAssetsOriginal,
        leftIndent: 1,
        rightItemName: '长期借款',
        rightBalance: longTermBorrowing,
        rightIndent: 1
      },
      {
        leftItemName: '减：累计折旧',
        leftBalance: accumulatedDepreciation,
        leftIndent: 1,
        rightItemName: '',
        rightBalance: undefined,
        rightIndent: 1
      },
      {
        leftItemName: '固定资产净值',
        leftBalance: fixedAssetsNet,
        leftIndent: 1,
        leftIsBold: true,
        rightItemName: '非流动负债合计',
        rightBalance: totalNonCurrentLiabilities,
        rightIndent: 0,
        rightIsBold: true
      },
      {
        leftItemName: '无形资产',
        leftBalance: intangibleAssets,
        leftIndent: 1,
        rightItemName: '负债合计',
        rightBalance: totalLiabilities,
        rightIndent: 0,
        rightIsBold: true,
        rightIsHighlight: true
      },
      {
        leftItemName: '长期应收款',
        leftBalance: longTermReceivables,
        leftIndent: 1,
        rightItemName: '',
        rightBalance: undefined,
        rightIndent: 0
      },
      {
        leftItemName: '非流动资产合计',
        leftBalance: totalNonCurrentAssets,
        leftIndent: 0,
        leftIsBold: true,
        leftIsHighlight: true,
        rightItemName: '',
        rightBalance: undefined,
        rightIndent: 0
      },

      // ===== 所有者权益 =====
      {
        leftItemName: '',
        leftBalance: undefined,
        leftIndent: 0,
        rightItemName: '三、所有者权益',
        rightBalance: undefined,
        rightIndent: 0,
        isHeader: true
      },
      {
        leftItemName: '',
        leftBalance: undefined,
        leftIndent: 0,
        rightItemName: '实收资本',
        rightBalance: paidInCapital,
        rightIndent: 1
      },
      {
        leftItemName: '',
        leftBalance: undefined,
        leftIndent: 0,
        rightItemName: '资本公积',
        rightBalance: capitalReserve,
        rightIndent: 1
      },
      {
        leftItemName: '',
        leftBalance: undefined,
        leftIndent: 0,
        rightItemName: '未分配利润',
        rightBalance: retainedEarnings,
        rightIndent: 1
      },
      {
        leftItemName: '',
        leftBalance: undefined,
        leftIndent: 0,
        rightItemName: '所有者权益合计',
        rightBalance: totalEquity,
        rightIndent: 0,
        rightIsBold: true,
        rightIsHighlight: true
      },

      // ===== 总计 =====
      {
        leftItemName: '资产总计',
        leftBalance: totalAssets,
        leftIndent: 0,
        leftIsBold: true,
        leftIsHighlight: true,
        rightItemName: '负债和所有者权益总计',
        rightBalance: totalLiabilitiesAndEquity,
        rightIndent: 0,
        rightIsBold: true,
        rightIsHighlight: true
      }
    ] as PreviewRow[]
  };
}


/**
 * 解析利润表数据，包含完整的多层计算结构
 *
 * 利润表计算层级：
 * 1. 营业收入（主营业务收入 + 其他业务收入）
 * 2. 减：营业支出（主营业务成本 + 税金及附加 + 销售费用 + 管理费用 + 财务费用）
 * 3. 三、营业利润
 * 4. 加减：营业外收支
 * 5. 五、利润总额
 * 6. 减：所得税费用
 * 7. 六、净利润
 *
 * 每个科目同时展示本期金额和上期金额，自动计算变动额和变动率
 *
 * @param data - 后端返回的原始数据对象
 * @returns 解析后的列定义和数据
 */
export function parseIncomeStatement(data: Record<string, unknown>): ParseResult {
  const d = data || {};

  /* ---- 收入 ---- */
  const mainBusinessIncome = extractNumber(d, 'main_business_income', 680000);
  const otherBusinessIncome = extractNumber(d, 'other_business_income', 12500);
  const totalOperatingRevenue = mainBusinessIncome + otherBusinessIncome;

  /* ---- 成本费用 ---- */
  const mainBusinessCost = extractNumber(d, 'main_business_cost', 420000);
  const taxSurcharges = extractNumber(d, 'tax_surcharges', 18600);
  const sellingExpenses = extractNumber(d, 'selling_expenses', 45800);
  const administrativeExpenses = extractNumber(d, 'administrative_expenses', 35600);
  const financialExpenses = extractNumber(d, 'financial_expenses', 8500);

  /* ---- 营业利润计算 ---- */
  const operatingProfit = totalOperatingRevenue - mainBusinessCost - taxSurcharges - sellingExpenses - administrativeExpenses - financialExpenses;

  /* ---- 营业外收支 ---- */
  const nonOperatingIncome = extractNumber(d, 'non_operating_income', 3500);
  const nonOperatingExpense = extractNumber(d, 'non_operating_expense', 800);
  const totalProfit = operatingProfit + nonOperatingIncome - nonOperatingExpense;

  /* ---- 所得税与净利润 ---- */
  const incomeTaxExpense = extractNumber(d, 'income_tax_expense', Math.round(totalProfit * 0.25));
  const netProfit = totalProfit - incomeTaxExpense;

  /* ---- 上期数据（用于对比分析） ---- */
  const lastMainIncome = extractNumber(d, 'last_main_business_income', 620000);
  const lastOtherIncome = extractNumber(d, 'last_other_business_income', 9800);
  const lastTotalRevenue = lastMainIncome + lastOtherIncome;
  const lastMainCost = extractNumber(d, 'last_main_business_cost', 385000);
  const lastTaxSurcharge = extractNumber(d, 'last_tax_surcharges', 17200);
  const lastSelling = extractNumber(d, 'last_selling_expenses', 42300);
  const lastAdmin = extractNumber(d, 'last_administrative_expenses', 33200);
  const lastFinancial = extractNumber(d, 'last_financial_expenses', 9100);
  const lastOperatingProfit = lastTotalRevenue - lastMainCost - lastTaxSurcharge - lastSelling - lastAdmin - lastFinancial;
  const lastNonOpInc = extractNumber(d, 'last_non_operating_income', 2800);
  const lastNonOpExp = extractNumber(d, 'last_non_operating_expense', 600);
  const lastTotalProfit = lastOperatingProfit + lastNonOpInc - lastNonOpExp;
  const lastIncomeTax = extractNumber(d, 'last_income_tax_expense', Math.round(lastTotalProfit * 0.25));
  const lastNetProfit = lastTotalProfit - lastIncomeTax;

  return {
    columns: [
      { prop: 'itemName', label: '项目' },
      { prop: 'currentAmount', label: '本期金额', isMoney: true },
      { prop: 'lastAmount', label: '上期金额', isMoney: true },
      { prop: 'change', label: '变动额', isMoney: true },
      { prop: 'changePercent', label: '变动率' }
    ],
    rows: [
      // 一、营业收入
      {
        itemName: '一、营业收入',
        indent: 0,
        isHeader: true,
        currentAmount: undefined,
        lastAmount: undefined,
        change: undefined,
        changePercent: undefined
      },
      {
        itemName: '主营业务收入',
        indent: 1,
        currentAmount: mainBusinessIncome,
        lastAmount: lastMainIncome,
        change: mainBusinessIncome - lastMainIncome,
        changePercent: calcChangeRate(mainBusinessIncome, lastMainIncome)
      },
      {
        itemName: '其他业务收入',
        indent: 1,
        currentAmount: otherBusinessIncome,
        lastAmount: lastOtherIncome,
        change: otherBusinessIncome - lastOtherIncome,
        changePercent: calcChangeRate(otherBusinessIncome, lastOtherIncome)
      },
      {
        itemName: '营业总收入',
        indent: 0,
        isBold: true,
        currentAmount: totalOperatingRevenue,
        lastAmount: lastTotalRevenue,
        change: totalOperatingRevenue - lastTotalRevenue,
        changePercent: calcChangeRate(totalOperatingRevenue, lastTotalRevenue)
      },

      // 二、营业支出
      {
        itemName: '二、营业支出',
        indent: 0,
        isHeader: true,
        currentAmount: undefined,
        lastAmount: undefined,
        change: undefined,
        changePercent: undefined
      },
      {
        itemName: '主营业务成本',
        indent: 1,
        currentAmount: mainBusinessCost,
        lastAmount: lastMainCost,
        change: mainBusinessCost - lastMainCost,
        changePercent: calcChangeRate(mainBusinessCost, lastMainCost)
      },
      {
        itemName: '税金及附加',
        indent: 1,
        currentAmount: taxSurcharges,
        lastAmount: lastTaxSurcharge,
        change: taxSurcharges - lastTaxSurcharge,
        changePercent: calcChangeRate(taxSurcharges, lastTaxSurcharge)
      },
      {
        itemName: '销售费用',
        indent: 1,
        currentAmount: sellingExpenses,
        lastAmount: lastSelling,
        change: sellingExpenses - lastSelling,
        changePercent: calcChangeRate(sellingExpenses, lastSelling)
      },
      {
        itemName: '管理费用',
        indent: 1,
        currentAmount: administrativeExpenses,
        lastAmount: lastAdmin,
        change: administrativeExpenses - lastAdmin,
        changePercent: calcChangeRate(administrativeExpenses, lastAdmin)
      },
      {
        itemName: '财务费用',
        indent: 1,
        currentAmount: financialExpenses,
        lastAmount: lastFinancial,
        change: financialExpenses - lastFinancial,
        changePercent: calcChangeRate(financialExpenses, lastFinancial)
      },

      // 三、营业利润
      {
        itemName: '三、营业利润',
        indent: 0,
        isBold: true,
        isHighlight: true,
        currentAmount: operatingProfit,
        lastAmount: lastOperatingProfit,
        change: operatingProfit - lastOperatingProfit,
        changePercent: calcChangeRate(operatingProfit, lastOperatingProfit)
      },

      // 四、营业外收支
      {
        itemName: '四、营业外收支',
        indent: 0,
        isHeader: true,
        currentAmount: undefined,
        lastAmount: undefined,
        change: undefined,
        changePercent: undefined
      },
      {
        itemName: '加：营业外收入',
        indent: 1,
        currentAmount: nonOperatingIncome,
        lastAmount: lastNonOpInc,
        change: nonOperatingIncome - lastNonOpInc,
        changePercent: calcChangeRate(nonOperatingIncome, lastNonOpInc)
      },
      {
        itemName: '减：营业外支出',
        indent: 1,
        currentAmount: nonOperatingExpense,
        lastAmount: lastNonOpExp,
        change: nonOperatingExpense - lastNonOpExp,
        changePercent: calcChangeRate(nonOperatingExpense, lastNonOpExp)
      },

      // 五、利润总额
      {
        itemName: '五、利润总额',
        indent: 0,
        isBold: true,
        currentAmount: totalProfit,
        lastAmount: lastTotalProfit,
        change: totalProfit - lastTotalProfit,
        changePercent: calcChangeRate(totalProfit, lastTotalProfit)
      },

      // 六、所得税与净利润
      {
        itemName: '减：所得税费用',
        indent: 1,
        currentAmount: incomeTaxExpense,
        lastAmount: lastIncomeTax,
        change: incomeTaxExpense - lastIncomeTax,
        changePercent: calcChangeRate(incomeTaxExpense, lastIncomeTax)
      },
      {
        itemName: '六、净利润',
        indent: 0,
        isBold: true,
        isHighlight: true,
        currentAmount: netProfit,
        lastAmount: lastNetProfit,
        change: netProfit - lastNetProfit,
        changePercent: calcChangeRate(netProfit, lastNetProfit)
      }
    ] as PreviewRow[]
  };
}


/**
 * 解析现金流量表数据，采用直接法编制
 *
 * 现金流量表三大活动分类：
 * 1. 经营活动产生的现金流量（企业日常经营相关的现金收支）
 * 2. 投资活动产生的现金流量（长期资产购建和处置的现金收支）
 * 3. 筹资活动产生的现金流量（资本及债务规模变化的现金收支）
 *
 * 最终汇总：
 * - 汇率变动对现金的影响
 * - 现金及现金等价物净增加额
 * - 期末现金余额
 *
 * @param data - 后端返回的原始数据对象
 * @returns 解析后的列定义和数据
 */
export function parseCashFlowStatement(data: Record<string, unknown>): ParseResult {
  const d = data || {};

  const cashSales = extractNumber(d, 'cash_sales', 685000);
  const taxRefund = extractNumber(d, 'tax_refund', 3200);
  const otherOperatingCash = extractNumber(d, 'other_operating_cash', 8500);
  const operatingInflow = cashSales + taxRefund + otherOperatingCash;
  const cashPurchase = extractNumber(d, 'cash_purchase', 398000);
  const cashSalary = extractNumber(d, 'cash_salary', 125000);
  const cashTax = extractNumber(d, 'cash_tax', 18600);
  const otherOperatingPayment = extractNumber(d, 'other_operating_payment', 28500);
  const operatingOutflow = cashPurchase + cashSalary + cashTax + otherOperatingPayment;
  const operatingNet = operatingInflow - operatingOutflow;
  const investRecovery = extractNumber(d, 'invest_recovery', 15000);
  const investIncome = extractNumber(d, 'invest_income', 4500);
  const assetDisposal = extractNumber(d, 'asset_disposal', 8000);
  const investInflow = investRecovery + investIncome + assetDisposal;
  const assetPurchase = extractNumber(d, 'asset_purchase', 65000);
  const investPayment = extractNumber(d, 'invest_payment', 20000);
  const investOutflow = assetPurchase + investPayment;
  const investNet = investInflow - investOutflow;
  const financeInvest = extractNumber(d, 'finance_invest', 0);
  const loanReceived = extractNumber(d, 'loan_received', 80000);
  const financeInflow = financeInvest + loanReceived;
  const debtRepayment = extractNumber(d, 'debt_repayment', 50000);
  const dividendPayment = extractNumber(d, 'dividend_payment', 30000);
  const financeOutflow = debtRepayment + dividendPayment;
  const financeNet = financeInflow - financeOutflow;
  const fxImpact = extractNumber(d, 'fx_impact', 0);
  const totalNetIncrease = operatingNet + investNet + financeNet + fxImpact;
  const openingCash = extractNumber(d, 'opening_cash', 134500);
  const closingCash = openingCash + totalNetIncrease;

  return {
    columns: [
      { prop: 'itemName', label: '项目' },
      { prop: 'endingBalance', label: '金额', isMoney: true }
    ],
    rows: [
      // 一、经营活动
      {
        itemName: '一、经营活动产生的现金流量',
        indent: 0,
        isHeader: true,
        endingBalance: undefined
      },
      {
        itemName: '销售商品、提供劳务收到的现金',
        indent: 1,
        endingBalance: cashSales
      },
      {
        itemName: '收到的税费返还',
        indent: 1,
        endingBalance: taxRefund
      },
      {
        itemName: '收到其他与经营活动有关的现金',
        indent: 1,
        endingBalance: otherOperatingCash
      },
      {
        itemName: '经营活动现金流入小计',
        indent: 1,
        isBold: true,
        endingBalance: operatingInflow
      },
      {
        itemName: '购买商品、接受劳务支付的现金',
        indent: 1,
        endingBalance: cashPurchase
      },
      {
        itemName: '支付给职工以及为职工支付的现金',
        indent: 1,
        endingBalance: cashSalary
      },
      {
        itemName: '支付的各项税费',
        indent: 1,
        endingBalance: cashTax
      },
      {
        itemName: '支付其他与经营活动有关的现金',
        indent: 1,
        endingBalance: otherOperatingPayment
      },
      {
        itemName: '经营活动现金流出小计',
        indent: 1,
        isBold: true,
        endingBalance: operatingOutflow
      },
      {
        itemName: '经营活动产生的现金流量净额',
        indent: 1,
        isBold: true,
        isHighlight: true,
        endingBalance: operatingNet
      },

      // 二、投资活动
      {
        itemName: '二、投资活动产生的现金流量',
        indent: 0,
        isHeader: true,
        endingBalance: undefined
      },
      {
        itemName: '收回投资收到的现金',
        indent: 1,
        endingBalance: investRecovery
      },
      {
        itemName: '取得投资收益收到的现金',
        indent: 1,
        endingBalance: investIncome
      },
      {
        itemName: '处置固定资产等收回的现金净额',
        indent: 1,
        endingBalance: assetDisposal
      },
      {
        itemName: '投资活动现金流入小计',
        indent: 1,
        isBold: true,
        endingBalance: investInflow
      },
      {
        itemName: '购建固定资产等支付的现金',
        indent: 1,
        endingBalance: assetPurchase
      },
      {
        itemName: '投资支付的现金',
        indent: 1,
        endingBalance: investPayment
      },
      {
        itemName: '投资活动现金流出小计',
        indent: 1,
        isBold: true,
        endingBalance: investOutflow
      },
      {
        itemName: '投资活动产生的现金流量净额',
        indent: 1,
        isBold: true,
        endingBalance: investNet
      },

      // 三、筹资活动
      {
        itemName: '三、筹资活动产生的现金流量',
        indent: 0,
        isHeader: true,
        endingBalance: undefined
      },
      {
        itemName: '吸收投资收到的现金',
        indent: 1,
        endingBalance: financeInvest
      },
      {
        itemName: '取得借款收到的现金',
        indent: 1,
        endingBalance: loanReceived
      },
      {
        itemName: '筹资活动现金流入小计',
        indent: 1,
        isBold: true,
        endingBalance: financeInflow
      },
      {
        itemName: '偿还债务支付的现金',
        indent: 1,
        endingBalance: debtRepayment
      },
      {
        itemName: '分配股利/利润或偿付利息支付的现金',
        indent: 1,
        endingBalance: dividendPayment
      },
      {
        itemName: '筹资活动现金流出小计',
        indent: 1,
        isBold: true,
        endingBalance: financeOutflow
      },
      {
        itemName: '筹资活动产生的现金流量净额',
        indent: 1,
        isBold: true,
        endingBalance: financeNet
      },

      // 四、汇率变动
      {
        itemName: '四、汇率变动对现金及现金等价物的影响',
        indent: 0,
        endingBalance: fxImpact
      },
      {
        itemName: '五、现金及现金等价物净增加额',
        indent: 0,
        isBold: true,
        isHighlight: true,
        endingBalance: totalNetIncrease
      },
      {
        itemName: '加：期初现金及现金等价物余额',
        indent: 0,
        endingBalance: openingCash
      },
      {
        itemName: '六、期末现金及现金等价物余额',
        indent: 0,
        isBold: true,
        isHighlight: true,
        endingBalance: closingCash
      }
    ] as PreviewRow[]
  };
}


/**
 * 解析通用表格数据（用于收支明细表、成本分析表等未专门实现的报表类型）
 * @param _data - 原始数据对象（暂未使用）
 * @returns 基础列结构和空数据行
 */
export function parseGenericTable(_data: Record<string, unknown>): ParseResult {
  return {
    columns: [
      { prop: 'itemName', label: '项目' },
      { prop: 'amount', label: '金额', isMoney: true },
      { prop: 'remark', label: '备注' }
    ],
    rows: []
  };
}


/**
 * 统一的报表数据解析入口
 * 根据报表类型分发到对应的专用解析器
 *
 * @param reportType - 报表类型标识
 * @param data - 后端返回的原始数据
 * @returns 解析结果（包含列定义和数据行）
 */
export function parseReportData(reportType: string, data: Record<string, unknown>): ParseResult {
  switch (reportType) {
    case 'balance_sheet':
      return parseBalanceSheet(data);
    case 'income_statement':
      return parseIncomeStatement(data);
    case 'cash_flow':
      return parseCashFlowStatement(data);
    default:
      return parseGenericTable(data);
  }
}
