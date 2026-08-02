package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.finance.CostRecord;
import com.foodtraceability.entity.finance.FinanceRecord;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.entity.finance.Receivable;
import com.foodtraceability.mapper.finance.CostRecordMapper;
import com.foodtraceability.mapper.finance.FinanceRecordMapper;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import com.foodtraceability.mapper.finance.ReceivableMapper;
import com.foodtraceability.service.finance.FinancialReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 财务报表Service实现
 * 提供利润表、收支明细、应收应付统计等报表数据
 */
@Service
public class FinancialReportServiceImpl extends ServiceImpl<com.foodtraceability.mapper.finance.ProfitStatementMapper, com.foodtraceability.entity.finance.ProfitStatement>
        implements FinancialReportService {

    private static final Logger log = LoggerFactory.getLogger(FinancialReportServiceImpl.class);

    private final ReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final FinanceRecordMapper financeRecordMapper;
    private final CostRecordMapper costRecordMapper;

    public FinancialReportServiceImpl(ReceivableMapper receivableMapper,
                                     PayableMapper payableMapper,
                                     FinanceRecordMapper financeRecordMapper,
                                     CostRecordMapper costRecordMapper) {
        this.receivableMapper = receivableMapper;
        this.payableMapper = payableMapper;
        this.financeRecordMapper = financeRecordMapper;
        this.costRecordMapper = costRecordMapper;
    }

    @Override
    public Map<String, Object> getProfitStatement(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 计算营业收入
        long revenueAmount = sumFinanceRecords(1, startDate, endDate);
        // 计算营业成本
        long cogsAmount = sumCostRecords(startDate, endDate);
        // 计算运营费用
        long expenseAmount = sumFinanceRecordsByCategory(startDate, endDate);

        long grossProfit = revenueAmount - cogsAmount;
        long netProfit = grossProfit - expenseAmount;

        result.put("revenueAmount", revenueAmount);
        result.put("cogsAmount", cogsAmount);
        result.put("grossProfit", grossProfit);
        result.put("operatingExpenses", expenseAmount);
        result.put("netProfit", netProfit);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());

        return result;
    }

    @Override
    public Map<String, Object> getIncomeExpenseSummary(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();

        long totalIncome = sumFinanceRecords(1, startDate, endDate);
        long totalExpense = sumFinanceRecords(2, startDate, endDate);

        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netCashFlow", totalIncome - totalExpense);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());

        return result;
    }

    @Override
    public Map<String, Object> getReceivableStatistics() {
        Map<String, Object> result = new LinkedHashMap<>();

        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        List<Receivable> all = receivableMapper.selectList(wrapper);

        long totalOriginal = 0L;
        long totalReceived = 0L;
        long totalBalance = 0L;
        long overdueAmount = 0L;
        int overdueCount = 0;

        for (Receivable r : all) {
            if (r.getOriginalAmount() != null) totalOriginal += r.getOriginalAmount();
            if (r.getReceivedAmount() != null) totalReceived += r.getReceivedAmount();
            if (r.getBalanceAmount() != null) totalBalance += r.getBalanceAmount();
            if (r.getStatus() != null && r.getStatus() == 2) {
                overdueCount++;
                if (r.getBalanceAmount() != null) overdueAmount += r.getBalanceAmount();
            }
        }

        result.put("totalOriginal", totalOriginal);
        result.put("totalReceived", totalReceived);
        result.put("totalBalance", totalBalance);
        result.put("overdueCount", overdueCount);
        result.put("overdueAmount", overdueAmount);
        result.put("totalCount", all.size());

        return result;
    }

    @Override
    public Map<String, Object> getPayableStatistics() {
        Map<String, Object> result = new LinkedHashMap<>();

        LambdaQueryWrapper<Payable> wrapper = new LambdaQueryWrapper<>();
        List<Payable> all = payableMapper.selectList(wrapper);

        long totalOriginal = 0L;
        long totalPaid = 0L;
        long totalBalance = 0L;
        int overdueCount = 0;
        long overdueAmount = 0L;

        for (Payable p : all) {
            if (p.getOriginalAmount() != null) totalOriginal += p.getOriginalAmount();
            if (p.getPaidAmount() != null) totalPaid += p.getPaidAmount();
            if (p.getBalanceAmount() != null) totalBalance += p.getBalanceAmount();
            if (p.getStatus() != null && p.getStatus() == 4) {
                overdueCount++;
                if (p.getBalanceAmount() != null) overdueAmount += p.getBalanceAmount();
            }
        }

        result.put("totalOriginal", totalOriginal);
        result.put("totalPaid", totalPaid);
        result.put("totalBalance", totalBalance);
        result.put("overdueCount", overdueCount);
        result.put("overdueAmount", overdueAmount);
        result.put("totalCount", all.size());

        return result;
    }

    @Override
    public List<Map<String, Object>> getAgingAnalysis() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[][] agingBuckets = {
                {"0-30天", "0", "30"},
                {"31-60天", "31", "60"},
                {"61-90天", "61", "90"},
                {"90天以上", "91", "9999"}
        };

        for (String[] bucket : agingBuckets) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bucket", bucket[0]);
            // 使用 ReceivableMapper 的自定义查询或简单统计
            // 这里简化处理，实际应按账龄分组查询
            item.put("count", 0);
            item.put("amount", 0L);
            result.add(item);
        }

        return result;
    }

    @Override
    public Map<String, Object> getCostStructure(String period) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<Integer, Long> typeSummary = new HashMap<>();

        String[] costTypes = {"食材成本", "人工成本", "租金成本", "水电成本", "折旧成本", "包装成本", "其他成本"};
        int[] typeCodes = {1, 2, 3, 4, 5, 6, 7};

        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();
        if (period != null && !period.isBlank()) {
            wrapper.eq(CostRecord::getPeriod, period);
        }
        List<CostRecord> records = costRecordMapper.selectList(wrapper);

        for (CostRecord record : records) {
            if (record.getCostType() != null && record.getAmount() != null) {
                typeSummary.merge(record.getCostType(), record.getAmount(), Long::sum);
            }
        }

        long total = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (int i = 0; i < typeCodes.length; i++) {
            Long amount = typeSummary.getOrDefault(typeCodes[i], 0L);
            total += amount;
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("type", costTypes[i]);
            detail.put("typeCode", typeCodes[i]);
            detail.put("amount", amount);
            detail.put("percentage", total > 0 ? BigDecimal.valueOf(amount * 100.0)
                    .divide(BigDecimal.valueOf(total), 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            details.add(detail);
        }

        result.put("period", period != null ? period : "全部");
        result.put("totalCost", total);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> getBudgetExecution(Integer year, Integer type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("type", type);
        result.put("message", "预算执行数据待实现");
        result.put("budgetAmount", 0L);
        result.put("actualAmount", 0L);
        result.put("variance", 0L);
        result.put("executionRate", BigDecimal.ZERO);
        return result;
    }

    /**
     * 统计指定日期范围和类型的收支记录总额
     */
    private long sumFinanceRecords(int recordType, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceRecord::getRecordType, recordType)
               .eq(FinanceRecord::getApprovalStatus, 1)
               .ge(FinanceRecord::getBusinessDate, startDate)
               .le(FinanceRecord::getBusinessDate, endDate)
               .select(FinanceRecord::getAmount);
        List<FinanceRecord> records = financeRecordMapper.selectList(wrapper);
        return records.stream().mapToLong(r -> r.getAmount() != null ? r.getAmount() : 0L).sum();
    }

    /**
     * 统计指定日期范围内的支出（费用）记录总额
     */
    private long sumFinanceRecordsByCategory(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceRecord::getRecordType, 2)
               .ge(FinanceRecord::getRecordCategory, 200)
               .le(FinanceRecord::getRecordCategory, 299)
               .eq(FinanceRecord::getApprovalStatus, 1)
               .ge(FinanceRecord::getBusinessDate, startDate)
               .le(FinanceRecord::getBusinessDate, endDate)
               .select(FinanceRecord::getAmount);
        List<FinanceRecord> records = financeRecordMapper.selectList(wrapper);
        return records.stream().mapToLong(r -> r.getAmount() != null ? r.getAmount() : 0L).sum();
    }

    /**
     * 统计指定日期范围内的成本记录总额
     */
    private long sumCostRecords(LocalDate startDate, LocalDate endDate) {
        String startPeriod = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        String endPeriod = endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CostRecord::getPeriod, startPeriod)
               .le(CostRecord::getPeriod, endPeriod)
               .select(CostRecord::getAmount);
        List<CostRecord> records = costRecordMapper.selectList(wrapper);
        return records.stream().mapToLong(c -> c.getAmount() != null ? c.getAmount() : 0L).sum();
    }
}
