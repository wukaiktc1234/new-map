package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.AccountBalance;

import java.math.BigDecimal;
import java.util.List;

/**
 * 科目余额服务接口
 * @author example
 * @since 2025-12-06
 */
public interface AccountBalanceService extends IService<AccountBalance> {

    /**
     * 根据会计期间获取科目余额列表
     * @param period 会计期间
     * @return 科目余额列表
     */
    List<AccountBalance> getByPeriod(String period);

    /**
     * 根据科目ID和会计期间获取科目余额
     * @param subjectId 科目ID
     * @param period 会计期间
     * @return 科目余额
     */
    AccountBalance getBySubjectIdAndPeriod(Long subjectId, String period);

    /**
     * 根据科目ID获取科目余额列表
     * @param subjectId 科目ID
     * @return 科目余额列表
     */
    List<AccountBalance> getBySubjectId(Long subjectId);

    /**
     * 根据会计期间生成科目余额
     * @param period 会计期间
     * @return 是否生成成功
     */
    boolean generateBalancesForPeriod(String period);

    /**
     * 根据日期范围获取科目余额
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 科目余额列表
     */
    List<AccountBalance> getBalancesByDateRange(String startDate, String endDate);

    /**
     * 根据科目类别获取科目余额
     * @param category 科目类别
     * @return 科目余额列表
     */
    List<AccountBalance> getBalancesByCategory(String category);

    /**
     * 更新科目本期发生额
     * @param subjectId 科目ID
     * @param period 会计期间
     * @param debit 借方发生额
     * @param credit 贷方发生额
     * @return 是否更新成功
     */
    boolean updateCurrentBalance(Long subjectId, String period, BigDecimal debit, BigDecimal credit);

    /**
     * 计算期末余额
     * @param accountBalance 科目余额
     * @return 计算后的科目余额
     */
    AccountBalance calculateEndBalance(AccountBalance accountBalance);

    /**
     * 批量计算期末余额
     * @param accountBalances 科目余额列表
     * @return 计算后的科目余额列表
     */
    List<AccountBalance> batchCalculateEndBalances(List<AccountBalance> accountBalances);

    /**
     * 从凭证数据更新科目余额
     * @param voucherId 凭证ID
     * @return 是否更新成功
     */
    boolean updateBalancesFromVoucher(Long voucherId);

    /**
     * 批量从凭证数据更新科目余额
     * @param voucherIds 凭证ID列表
     * @return 是否更新成功
     */
    boolean batchUpdateBalancesFromVouchers(List<Long> voucherIds);

    /**
     * 结转损益
     * @param period 会计期间
     * @return 是否结转成功
     */
    boolean closeProfitAndLoss(String period);

    /**
     * 结转本年利润
     * @param period 会计期间
     * @return 是否结转成功
     */
    boolean closeYearProfit(String period);

    /**
     * 根据科目类别和会计期间获取科目余额
     * @param category 科目类别
     * @param period 会计期间
     * @return 科目余额列表
     */
    List<AccountBalance> getByCategoryAndPeriod(String category, String period);
}
