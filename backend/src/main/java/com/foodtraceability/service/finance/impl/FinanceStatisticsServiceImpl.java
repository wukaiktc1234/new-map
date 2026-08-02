package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.finance.FinanceStatisticsQueryDTO;
import com.foodtraceability.dto.finance.FinanceStatisticsVO;
import com.foodtraceability.entity.finance.FinanceVoucher;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import com.foodtraceability.service.finance.FinanceStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 财务统计Service实现
 *
 * <p>Sprint 3.1 P0 F-002 / T-017：财务中心首页统计概览，
 * 聚合 Receivable / Payable / FinanceVoucher / FundFlow 数据（ADR-003 要求 4 表聚合），
 * 计算当月收入/支出/利润及环比变化率。</p>
 *
 * <p>聚合规则：</p>
 * <ul>
 *   <li>当月收入 = Receivable.receivedAmount 之和（当月 lastPaymentDate）
 *       + FundFlow.amount 之和（当月 businessDate, flowDirection=1）
 *       + FinanceVoucher.totalCredit 之和（当月 voucherDate, voucherType=6 收款, voucherStatus=2 已过账）</li>
 *   <li>当月支出 = Payable.paidAmount 之和（当月）
 *       + FundFlow.amount 之和（当月, flowDirection=2）
 *       + FinanceVoucher.totalDebit 之和（当月 voucherDate, voucherType=5 付款, voucherStatus=2 已过账）</li>
 *   <li>当月利润 = 当月收入 - 当月支出</li>
 *   <li>环比变化率 = (当月 - 上月) / 上月 * 100，上月为0时返回 null</li>
 * </ul>
 *
 * <p>金额字段全部 Long（分），无 BigDecimal。构造函数注入 4 个 Mapper。</p>
 */
@Service
public class FinanceStatisticsServiceImpl implements FinanceStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(FinanceStatisticsServiceImpl.class);

    /** 资金流水方向常量 */
    private static final int FLOW_DIRECTION_INCOME = 1;
    private static final int FLOW_DIRECTION_EXPENSE = 2;

    /** 凭证状态：已过账（仅统计已过账凭证） */
    private static final int VOUCHER_STATUS_POSTED = 2;

    /** 凭证类型：收款（贷方计入收入） */
    private static final int VOUCHER_TYPE_RECEIPT = 6;

    /** 凭证类型：付款（借方计入支出） */
    private static final int VOUCHER_TYPE_PAYMENT = 5;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final FundFlowMapper fundFlowMapper;
    private final FinanceVoucherMapper financeVoucherMapper;

    public FinanceStatisticsServiceImpl(ReceivableMapper receivableMapper,
                                        PayableMapper payableMapper,
                                        FundFlowMapper fundFlowMapper,
                                        FinanceVoucherMapper financeVoucherMapper) {
        this.receivableMapper = receivableMapper;
        this.payableMapper = payableMapper;
        this.fundFlowMapper = fundFlowMapper;
        this.financeVoucherMapper = financeVoucherMapper;
    }

    @Override
    public FinanceStatisticsVO getOverview(FinanceStatisticsQueryDTO query) {
        // 1. 解析统计周期
        YearMonth currentMonth = resolvePeriod(query);
        YearMonth lastMonth = currentMonth.minusMonths(1);
        log.info("财务统计概览，当月：{}，上月：{}", currentMonth, lastMonth);

        LocalDate currentStart = currentMonth.atDay(1);
        LocalDate currentEnd = currentMonth.atEndOfMonth();
        LocalDate lastStart = lastMonth.atDay(1);
        LocalDate lastEnd = lastMonth.atEndOfMonth();

        // 2. 查询应收账款（按 lastPaymentDate 过滤）
        long currentReceivableIncome = sumReceivableIncome(currentStart, currentEnd);
        long lastReceivableIncome = sumReceivableIncome(lastStart, lastEnd);

        // 3. 查询应付账款（按 createTime 过滤，Payable 无独立付款日期字段，使用 createTime 近似）
        long currentPayableExpense = sumPayableExpense(currentStart, currentEnd);
        long lastPayableExpense = sumPayableExpense(lastStart, lastEnd);

        // 4. 查询资金流水（按 businessDate 过滤）
        long currentFundIncome = sumFundFlow(FLOW_DIRECTION_INCOME, currentStart, currentEnd);
        long currentFundExpense = sumFundFlow(FLOW_DIRECTION_EXPENSE, currentStart, currentEnd);
        long lastFundIncome = sumFundFlow(FLOW_DIRECTION_INCOME, lastStart, lastEnd);
        long lastFundExpense = sumFundFlow(FLOW_DIRECTION_EXPENSE, lastStart, lastEnd);

        // 5. 查询已过账凭证（按 voucherDate 过滤，ADR-003 第 4 表聚合）
        long currentVoucherIncome = sumVoucherIncome(currentStart, currentEnd);
        long currentVoucherExpense = sumVoucherExpense(currentStart, currentEnd);
        long lastVoucherIncome = sumVoucherIncome(lastStart, lastEnd);
        long lastVoucherExpense = sumVoucherExpense(lastStart, lastEnd);

        // 6. 汇总（4 表聚合：Receivable + FundFlow + FinanceVoucher → 收入；Payable + FundFlow + FinanceVoucher → 支出）
        long monthlyIncome = currentReceivableIncome + currentFundIncome + currentVoucherIncome;
        long monthlyExpense = currentPayableExpense + currentFundExpense + currentVoucherExpense;
        long monthlyProfit = monthlyIncome - monthlyExpense;

        long lastMonthIncome = lastReceivableIncome + lastFundIncome + lastVoucherIncome;
        long lastMonthExpense = lastPayableExpense + lastFundExpense + lastVoucherExpense;
        long lastMonthProfit = lastMonthIncome - lastMonthExpense;

        // 7. 计算环比变化率
        Double incomeChange = calculateChangeRate(monthlyIncome, lastMonthIncome);
        Double expenseChange = calculateChangeRate(monthlyExpense, lastMonthExpense);
        Double profitChange = calculateChangeRate(monthlyProfit, lastMonthProfit);

        // 8. 构造VO
        FinanceStatisticsVO vo = new FinanceStatisticsVO();
        vo.setMonthlyIncome(monthlyIncome);
        vo.setMonthlyExpense(monthlyExpense);
        vo.setMonthlyProfit(monthlyProfit);
        vo.setMonthlyIncomeChange(incomeChange);
        vo.setMonthlyExpenseChange(expenseChange);
        vo.setMonthlyProfitChange(profitChange);
        vo.setLastMonthIncome(lastMonthIncome);
        vo.setLastMonthExpense(lastMonthExpense);
        vo.setLastMonthProfit(lastMonthProfit);
        vo.setPeriod(currentMonth.format(PERIOD_FORMATTER));
        return vo;
    }

    // ============================================================
    // 私有辅助方法
    // ============================================================

    /** 解析查询周期：优先使用 query.period，其次 startDate/endDate 推断，最后默认当月 */
    private YearMonth resolvePeriod(FinanceStatisticsQueryDTO query) {
        if (query.getPeriod() != null && !query.getPeriod().isBlank()) {
            try {
                return YearMonth.parse(query.getPeriod(), PERIOD_FORMATTER);
            } catch (Exception e) {
                log.warn("period 格式错误：{}，回退到当月", query.getPeriod());
            }
        }
        if (query.getStartDate() != null) {
            return YearMonth.from(query.getStartDate());
        }
        return YearMonth.now();
    }

    /** 汇总应收账款已收金额（按 lastPaymentDate 范围过滤） */
    private long sumReceivableIncome(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Receivable::getLastPaymentDate)
               .ge(Receivable::getLastPaymentDate, start)
               .le(Receivable::getLastPaymentDate, end);
        List<Receivable> list = receivableMapper.selectList(wrapper);
        return list.stream()
                .mapToLong(r -> r.getReceivedAmount() != null ? r.getReceivedAmount() : 0L)
                .sum();
    }

    /** 汇总应付账款已付金额（按 createTime 范围过滤，Payable 无独立付款日期） */
    private long sumPayableExpense(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Payable> wrapper = new LambdaQueryWrapper<>();
        // Payable 继承 BaseEntity，使用 createTime 过滤
        wrapper.ge(Payable::getCreateTime, start.atStartOfDay())
               .le(Payable::getCreateTime, end.atTime(23, 59, 59));
        List<Payable> list = payableMapper.selectList(wrapper);
        return list.stream()
                .mapToLong(p -> p.getPaidAmount() != null ? p.getPaidAmount() : 0L)
                .sum();
    }

    /** 汇总资金流水金额（按 businessDate 范围 + flowDirection 过滤） */
    private long sumFundFlow(int direction, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<FundFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundFlow::getFlowDirection, direction)
               .ge(FundFlow::getBusinessDate, start)
               .le(FundFlow::getBusinessDate, end);
        List<FundFlow> list = fundFlowMapper.selectList(wrapper);
        return list.stream()
                .mapToLong(f -> f.getAmount() != null ? f.getAmount() : 0L)
                .sum();
    }

    /**
     * 汇总已过账收款凭证的贷方金额（voucherStatus=2, voucherType=6 收款）
     *
     * <p>ADR-003 第 4 表聚合：将已过账的收款凭证纳入收入统计，
     * 避免与 Receivable/FundFlow 重复计算（凭证维度独立统计已记账的收款）。</p>
     */
    private long sumVoucherIncome(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceVoucher::getVoucherStatus, VOUCHER_STATUS_POSTED)
               .eq(FinanceVoucher::getVoucherType, VOUCHER_TYPE_RECEIPT)
               .ge(FinanceVoucher::getVoucherDate, start)
               .le(FinanceVoucher::getVoucherDate, end);
        List<FinanceVoucher> list = financeVoucherMapper.selectList(wrapper);
        return list.stream()
                .mapToLong(v -> v.getTotalCredit() != null ? v.getTotalCredit() : 0L)
                .sum();
    }

    /**
     * 汇总已过账付款凭证的借方金额（voucherStatus=2, voucherType=5 付款）
     *
     * <p>ADR-003 第 4 表聚合：将已过账的付款凭证纳入支出统计，
     * 避免与 Payable/FundFlow 重复计算（凭证维度独立统计已记账的付款）。</p>
     */
    private long sumVoucherExpense(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<FinanceVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceVoucher::getVoucherStatus, VOUCHER_STATUS_POSTED)
               .eq(FinanceVoucher::getVoucherType, VOUCHER_TYPE_PAYMENT)
               .ge(FinanceVoucher::getVoucherDate, start)
               .le(FinanceVoucher::getVoucherDate, end);
        List<FinanceVoucher> list = financeVoucherMapper.selectList(wrapper);
        return list.stream()
                .mapToLong(v -> v.getTotalDebit() != null ? v.getTotalDebit() : 0L)
                .sum();
    }

    /** 计算环比变化率：(current - last) / last * 100，last 为 0 时返回 null */
    private Double calculateChangeRate(long current, long last) {
        if (last == 0L) {
            return null;
        }
        return (current - last) * 100.0 / last;
    }
}
