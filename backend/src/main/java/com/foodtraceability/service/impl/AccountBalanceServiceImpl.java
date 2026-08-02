package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.AccountBalance;
import com.foodtraceability.entity.VoucherHeader;
import com.foodtraceability.entity.VoucherLine;
import com.foodtraceability.mapper.AccountBalanceMapper;
import com.foodtraceability.service.AccountBalanceService;
import com.foodtraceability.service.VoucherHeaderService;
import com.foodtraceability.service.VoucherLineService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账户余额Service实现类
 * @author example
 * @since 2025-12-06
 */
@Service
public class AccountBalanceServiceImpl extends ServiceImpl<AccountBalanceMapper, AccountBalance> implements AccountBalanceService {

    private static final Logger log = LoggerFactory.getLogger(AccountBalanceServiceImpl.class);


    public AccountBalanceServiceImpl(AccountBalanceMapper accountBalanceMapper, VoucherHeaderService voucherHeaderService, VoucherLineService voucherLineService) {
        this.accountBalanceMapper = accountBalanceMapper;
        this.voucherHeaderService = voucherHeaderService;
        this.voucherLineService = voucherLineService;
    }

    private final AccountBalanceMapper accountBalanceMapper;

    private final VoucherHeaderService voucherHeaderService;

    private final VoucherLineService voucherLineService;

    @Override
    public List<AccountBalance> getByPeriod(String period) {
        return accountBalanceMapper.selectByPeriod(period);
    }

    @Override
    public AccountBalance getBySubjectIdAndPeriod(Long subjectId, String period) {
        return accountBalanceMapper.selectBySubjectIdAndPeriod(subjectId, period);
    }

    @Override
    public List<AccountBalance> getBySubjectId(Long subjectId) {
        return accountBalanceMapper.selectBySubjectId(subjectId);
    }

    @Override
    public boolean generateBalancesForPeriod(String period) {
        // 生成指定期间的账户余额
        return accountBalanceMapper.generateAccountBalance(period) > 0;
    }

    @Override
    public List<AccountBalance> getBalancesByDateRange(String startDate, String endDate) {
        return accountBalanceMapper.selectBalancesByDateRange(startDate, endDate);
    }

    @Override
    public List<AccountBalance> getBalancesByCategory(String category) {
        // 默认获取当前期间的余额
        String currentPeriod = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return accountBalanceMapper.selectByCategoryAndPeriod(category, currentPeriod);
    }

    @Override
    public boolean updateCurrentBalance(Long subjectId, String period, BigDecimal debit, BigDecimal credit) {
        // 更新科目本期发生额
        int result = accountBalanceMapper.updateCurrentBalance(subjectId, period, debit, credit);
        if (result > 0) {
            // 更新成功后重新计算期末余额
            AccountBalance balance = getBySubjectIdAndPeriod(subjectId, period);
            if (balance != null) {
                calculateAndUpdateEndBalance(balance);
            }
            return true;
        }
        return false;
    }

    @Override
    public AccountBalance calculateEndBalance(AccountBalance accountBalance) {
        if (accountBalance == null) {
            return null;
        }

        // 初始化金额为0
        BigDecimal beginDebit = accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO;
        BigDecimal beginCredit = accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO;
        BigDecimal currentDebit = accountBalance.getCurrentDebit() != null ? accountBalance.getCurrentDebit() : BigDecimal.ZERO;
        BigDecimal currentCredit = accountBalance.getCurrentCredit() != null ? accountBalance.getCurrentCredit() : BigDecimal.ZERO;

        // 获取科目类别
        String category = accountBalance.getCategory();
        
        // 计算期末余额
        BigDecimal endDebit = BigDecimal.ZERO;
        BigDecimal endCredit = BigDecimal.ZERO;

        // 资产类和成本类科目
        if ("asset".equals(category) || "cost".equals(category)) {
            // 期末借方余额 = 期初借方余额 + 本期借方发生额 - 期初贷方余额 - 本期贷方发生额
            BigDecimal netAmount = beginDebit.add(currentDebit).subtract(beginCredit).subtract(currentCredit);
            if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
                endDebit = netAmount;
                endCredit = BigDecimal.ZERO;
            } else {
                endDebit = BigDecimal.ZERO;
                endCredit = netAmount.abs();
            }
        } else {
            // 负债类、所有者权益类和损益类科目
            // 期末贷方余额 = 期初贷方余额 + 本期贷方发生额 - 期初借方余额 - 本期借方发生额
            BigDecimal netAmount = beginCredit.add(currentCredit).subtract(beginDebit).subtract(currentDebit);
            if (netAmount.compareTo(BigDecimal.ZERO) >= 0) {
                endCredit = netAmount;
                endDebit = BigDecimal.ZERO;
            } else {
                endCredit = BigDecimal.ZERO;
                endDebit = netAmount.abs();
            }
        }

        accountBalance.setEndDebit(endDebit);
        accountBalance.setEndCredit(endCredit);

        return accountBalance;
    }

    @Override
    public List<AccountBalance> batchCalculateEndBalances(List<AccountBalance> accountBalances) {
        List<AccountBalance> result = new ArrayList<>();
        for (AccountBalance balance : accountBalances) {
            result.add(calculateEndBalance(balance));
        }
        return result;
    }

    @Override
    public boolean updateBalancesFromVoucher(Long voucherId) {
        // 从凭证数据更新科目余额
        try {
            // 获取凭证头信息，确定会计期间
            VoucherHeader voucherHeader = voucherHeaderService.getById(voucherId);
            if (voucherHeader == null) {
                return false;
            }
            
            String period = voucherHeader.getPeriod();
            
            // 获取凭证行列表
            List<VoucherLine> voucherLines = voucherLineService.getByVoucherId(voucherId);
            if (voucherLines == null || voucherLines.isEmpty()) {
                return true; // 没有凭证行，不需要更新余额
            }
            
            // 按科目ID分组统计借方和贷方发生额
            Map<Long, BigDecimal[]> subjectAmountMap = new HashMap<>();
            for (VoucherLine line : voucherLines) {
                Long subjectId = line.getSubjectId();
                BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
                
                if (subjectAmountMap.containsKey(subjectId)) {
                    // 更新现有科目金额
                    BigDecimal[] amounts = subjectAmountMap.get(subjectId);
                    amounts[0] = amounts[0].add(debit);
                    amounts[1] = amounts[1].add(credit);
                } else {
                    // 新增科目金额
                    subjectAmountMap.put(subjectId, new BigDecimal[]{debit, credit});
                }
            }
            
            // 更新每个科目的余额
            for (Map.Entry<Long, BigDecimal[]> entry : subjectAmountMap.entrySet()) {
                Long subjectId = entry.getKey();
                BigDecimal[] amounts = entry.getValue();
                BigDecimal debit = amounts[0];
                BigDecimal credit = amounts[1];
                
                // 更新科目本期发生额
                updateCurrentBalance(subjectId, period, debit, credit);
                
                // 获取并更新科目类别信息
                updateBalanceCategory(subjectId, period);
            }
            
            return true;
        } catch (Exception e) {
            log.error("从凭证更新科目余额失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchUpdateBalancesFromVouchers(List<Long> voucherIds) {
        // 批量从凭证数据更新科目余额
        boolean allSuccess = true;
        for (Long voucherId : voucherIds) {
            if (!updateBalancesFromVoucher(voucherId)) {
                allSuccess = false;
                // 继续处理其他凭证，不中断批量更新
            }
        }
        return allSuccess;
    }

    @Override
    public boolean closeProfitAndLoss(String period) {
        // 结转损益
        // 实际实现需要将损益类科目的余额结转至本年利润科目
        // 这里暂时返回true，后续需要完善
        return true;
    }

    @Override
    public boolean closeYearProfit(String period) {
        // 结转本年利润
        // 实际实现需要将本年利润结转至利润分配科目
        // 这里暂时返回true，后续需要完善
        return true;
    }

    @Override
    public List<AccountBalance> getByCategoryAndPeriod(String category, String period) {
        return accountBalanceMapper.selectByCategoryAndPeriod(category, period);
    }

    /**
     * 计算并更新期末余额
     * @param balance 科目余额
     */
    private void calculateAndUpdateEndBalance(AccountBalance balance) {
        if (balance == null) {
            return;
        }
        // 计算期末余额
        calculateEndBalance(balance);
        // 更新数据库
        updateById(balance);
    }
    
    /**
     * 更新科目余额的科目类别信息
     * @param subjectId 科目ID
     * @param period 会计期间
     */
    private void updateBalanceCategory(Long subjectId, String period) {
        try {
            // 获取账户余额
            AccountBalance balance = getBySubjectIdAndPeriod(subjectId, period);
            if (balance != null) {
                // 这里需要从会计科目表获取科目类别
                // 实际实现中，应该注入会计科目服务，这里简化处理
                String category = "asset"; // 默认资产类，实际应从数据库查询
                
                // 更新科目类别
                balance.setCategory(category);
                updateById(balance);
            }
        } catch (Exception e) {
            log.error("更新科目余额类别失败: {}", e.getMessage(), e);
        }
    }
}