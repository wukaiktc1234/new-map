package com.foodtraceability.service.finance.impl;

import com.foodtraceability.dto.finance.FinanceStatisticsQueryDTO;
import com.foodtraceability.dto.finance.FinanceStatisticsVO;
import com.foodtraceability.entity.finance.FundFlow;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
import com.foodtraceability.mapper.finance.FundFlowMapper;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * FinanceStatisticsServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-017 (TDD)：验证 getOverview() 返回当月收入/支出/利润 + 环比变化率。</p>
 *
 * <p>聚合规则（ADR-003 4 表聚合）：</p>
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
 * <p>注：现有测试未 stub financeVoucherMapper.selectList()，Mockito 默认返回空列表，
 * 故凭证维度贡献为 0，3 表聚合期望值保持不变。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FinanceStatisticsServiceImpl 单元测试")
class FinanceStatisticsServiceImplTest {

    @Mock
    private ReceivableMapper receivableMapper;

    @Mock
    private PayableMapper payableMapper;

    @Mock
    private FundFlowMapper fundFlowMapper;

    @Mock
    private FinanceVoucherMapper financeVoucherMapper;

    @InjectMocks
    private FinanceStatisticsServiceImpl service;

    @Test
    @DisplayName("主流程：getOverview 返回当月收入/支出/利润 + 环比变化率")
    void getOverview_success() {
        // given：当月应收已收 100000 + 50000 = 150000 分（1500元）
        Receivable r1 = new Receivable();
        r1.setReceivedAmount(100000L);
        r1.setLastPaymentDate(LocalDate.of(2026, 6, 10));
        Receivable r2 = new Receivable();
        r2.setReceivedAmount(50000L);
        r2.setLastPaymentDate(LocalDate.of(2026, 6, 20));

        // 上月应收已收 80000 分（800元）
        Receivable r3 = new Receivable();
        r3.setReceivedAmount(80000L);
        r3.setLastPaymentDate(LocalDate.of(2026, 5, 15));

        // receivableMapper 调用顺序：1.当月 2.上月
        when(receivableMapper.selectList(any())).thenReturn(Arrays.asList(r1, r2), Collections.singletonList(r3));

        // 当月应付已付 60000 + 40000 = 100000 分（1000元）
        Payable p1 = new Payable();
        p1.setPaidAmount(60000L);
        Payable p2 = new Payable();
        p2.setPaidAmount(40000L);
        // 上月应付已付 50000 分
        Payable p3 = new Payable();
        p3.setPaidAmount(50000L);

        // payableMapper 调用顺序：1.当月 2.上月
        when(payableMapper.selectList(any())).thenReturn(Arrays.asList(p1, p2), Collections.singletonList(p3));

        // 当月资金流水：收入 30000 + 支出 20000
        FundFlow incomeFlow = new FundFlow();
        incomeFlow.setFlowDirection(1);
        incomeFlow.setAmount(30000L);
        incomeFlow.setBusinessDate(LocalDate.of(2026, 6, 5));
        FundFlow expenseFlow = new FundFlow();
        expenseFlow.setFlowDirection(2);
        expenseFlow.setAmount(20000L);
        expenseFlow.setBusinessDate(LocalDate.of(2026, 6, 8));
        // 上月流水：收入 10000 + 支出 5000
        FundFlow lastIncomeFlow = new FundFlow();
        lastIncomeFlow.setFlowDirection(1);
        lastIncomeFlow.setAmount(10000L);
        lastIncomeFlow.setBusinessDate(LocalDate.of(2026, 5, 3));
        FundFlow lastExpenseFlow = new FundFlow();
        lastExpenseFlow.setFlowDirection(2);
        lastExpenseFlow.setAmount(5000L);
        lastExpenseFlow.setBusinessDate(LocalDate.of(2026, 5, 18));

        // fundFlowMapper 调用顺序：1.当月收入 2.当月支出 3.上月收入 4.上月支出
        when(fundFlowMapper.selectList(any())).thenReturn(
                Collections.singletonList(incomeFlow),    // 当月收入
                Collections.singletonList(expenseFlow),   // 当月支出
                Collections.singletonList(lastIncomeFlow),// 上月收入
                Collections.singletonList(lastExpenseFlow) // 上月支出
        );

        // 构造查询条件（period 优先）
        FinanceStatisticsQueryDTO query = new FinanceStatisticsQueryDTO();
        query.setPeriod("2026-06");

        // when
        FinanceStatisticsVO result = service.getOverview(query);

        // then
        assertNotNull(result);
        // 当月收入 = 150000（应收）+ 30000（流水）= 180000
        // 当月支出 = 100000（应付）+ 20000（流水）= 120000
        // 当月利润 = 180000 - 120000 = 60000
        // 上月收入 = 80000 + 10000 = 90000
        // 上月支出 = 50000 + 5000 = 55000
        // 上月利润 = 90000 - 55000 = 35000
        // 收入环比 = (180000 - 90000) / 90000 * 100 = 100.0
        // 支出环比 = (120000 - 55000) / 55000 * 100 ≈ 118.18
        // 利润环比 = (60000 - 35000) / 35000 * 100 ≈ 71.43
        assertAll("当月数据验证",
                () -> assertEquals(180000L, result.getMonthlyIncome()),
                () -> assertEquals(120000L, result.getMonthlyExpense()),
                () -> assertEquals(60000L, result.getMonthlyProfit()),
                () -> assertEquals("2026-06", result.getPeriod())
        );
        assertAll("上月数据验证",
                () -> assertEquals(90000L, result.getLastMonthIncome()),
                () -> assertEquals(55000L, result.getLastMonthExpense()),
                () -> assertEquals(35000L, result.getLastMonthProfit())
        );
        assertAll("环比变化率验证",
                () -> assertEquals(100.0, result.getMonthlyIncomeChange(), 0.01),
                () -> assertEquals(118.18, result.getMonthlyExpenseChange(), 0.01),
                () -> assertEquals(71.43, result.getMonthlyProfitChange(), 0.01)
        );
    }

    @Test
    @DisplayName("边界：上月数据为0时，环比变化率应为null")
    void getOverview_lastMonthZero_changeIsNull() {
        // 当月有数据，上月无数据
        Receivable r1 = new Receivable();
        r1.setReceivedAmount(100000L);
        r1.setLastPaymentDate(LocalDate.of(2026, 6, 10));
        // receivableMapper: 1.当月[r1] 2.上月[]
        when(receivableMapper.selectList(any())).thenReturn(
                Collections.singletonList(r1), Collections.emptyList());

        Payable p1 = new Payable();
        p1.setPaidAmount(50000L);
        // payableMapper: 1.当月[p1] 2.上月[]
        when(payableMapper.selectList(any())).thenReturn(
                Collections.singletonList(p1), Collections.emptyList());

        FundFlow incomeFlow = new FundFlow();
        incomeFlow.setFlowDirection(1);
        incomeFlow.setAmount(30000L);
        incomeFlow.setBusinessDate(LocalDate.of(2026, 6, 5));
        // fundFlowMapper: 1.当月收入[incomeFlow] 2.当月支出[] 3.上月收入[] 4.上月支出[]
        when(fundFlowMapper.selectList(any())).thenReturn(
                Collections.singletonList(incomeFlow), // 当月收入
                Collections.emptyList(),               // 当月支出
                Collections.emptyList(),               // 上月收入
                Collections.emptyList()                // 上月支出
        );

        FinanceStatisticsQueryDTO query = new FinanceStatisticsQueryDTO();
        query.setPeriod("2026-06");

        FinanceStatisticsVO result = service.getOverview(query);

        assertNotNull(result);
        assertEquals(130000L, result.getMonthlyIncome());
        assertEquals(50000L, result.getMonthlyExpense());
        assertEquals(80000L, result.getMonthlyProfit());
        assertEquals(0L, result.getLastMonthIncome());
        assertEquals(0L, result.getLastMonthExpense());
        assertEquals(0L, result.getLastMonthProfit());
        // 上月为0，环比变化率应为 null（避免除零）
        assertNull(result.getMonthlyIncomeChange(), "上月收入为0时环比变化率应为null");
        assertNull(result.getMonthlyExpenseChange(), "上月支出为0时环比变化率应为null");
        assertNull(result.getMonthlyProfitChange(), "上月利润为0时环比变化率应为null");
    }

    @Test
    @DisplayName("边界：query.period 为null时，默认使用当月")
    void getOverview_nullPeriod_useCurrentMonth() {
        when(receivableMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(payableMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(fundFlowMapper.selectList(any())).thenReturn(Collections.emptyList());

        FinanceStatisticsQueryDTO query = new FinanceStatisticsQueryDTO();
        // period 为 null，startDate/endDate 也为 null
        // service 应默认使用当月

        FinanceStatisticsVO result = service.getOverview(query);

        assertNotNull(result);
        assertNotNull(result.getPeriod(), "period 为 null 时应默认使用当月");
        assertEquals(0L, result.getMonthlyIncome());
        assertEquals(0L, result.getMonthlyExpense());
        assertEquals(0L, result.getMonthlyProfit());
    }
}
