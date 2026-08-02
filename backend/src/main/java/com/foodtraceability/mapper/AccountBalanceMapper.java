package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AccountBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

/**
 * 科目余额Mapper
 * @author example
 * @since 2025-12-06
 */
@Mapper
public interface AccountBalanceMapper extends BaseMapper<AccountBalance> {
    
    /**
     * 根据会计期间获取科目余额列表
     * @param period 会计期间
     * @return 科目余额列表
     */
    List<AccountBalance> selectByPeriod(String period);
    
    /**
     * 根据科目ID获取科目余额列表
     * @param subjectId 科目ID
     * @return 科目余额列表
     */
    List<AccountBalance> selectBySubjectId(Long subjectId);
    
    /**
     * 根据科目ID和会计期间获取科目余额
     * @param subjectId 科目ID
     * @param period 会计期间
     * @return 科目余额信息
     */
    AccountBalance selectBySubjectIdAndPeriod(@Param("subjectId") Long subjectId, @Param("period") String period);
    
    /**
     * 根据科目类别获取科目余额列表
     * @param category 科目类别
     * @param period 会计期间
     * @return 科目余额列表
     */
    List<AccountBalance> selectByCategoryAndPeriod(@Param("category") String category, @Param("period") String period);
    
    /**
     * 批量更新科目余额
     * @param balances 科目余额列表
     * @return 更新结果
     */
    int batchUpdate(@Param("balances") List<AccountBalance> balances);
    
    /**
     * 批量插入科目余额
     * @param balances 科目余额列表
     * @return 插入结果
     */
    int batchInsert(@Param("balances") List<AccountBalance> balances);
    
    /**
     * 根据会计期间删除科目余额
     * @param period 会计期间
     * @return 删除结果
     */
    int deleteByPeriod(String period);
    
    /**
     * 生成科目余额表
     * @param period 会计期间
     * @return 生成结果
     */
    int generateAccountBalance(@Param("period") String period);
    
    /**
     * 根据日期范围获取科目余额列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 科目余额列表
     */
    List<AccountBalance> selectBalancesByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
    
    /**
     * 更新科目本期发生额
     * @param subjectId 科目ID
     * @param period 会计期间
     * @param debit 借方发生额
     * @param credit 贷方发生额
     * @return 更新结果
     */
    int updateCurrentBalance(@Param("subjectId") Long subjectId, @Param("period") String period, @Param("debit") BigDecimal debit, @Param("credit") BigDecimal credit);
}
