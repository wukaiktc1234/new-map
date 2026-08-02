package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.VoucherLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.math.BigDecimal;

/**
 * 凭证行Mapper
 * @author example
 * @since 2025-12-06
 */
@Mapper
public interface VoucherLineMapper extends BaseMapper<VoucherLine> {
    
    /**
     * 根据凭证ID获取凭证行列表
     * @param voucherId 凭证ID
     * @return 凭证行列表
     */
    List<VoucherLine> selectByVoucherId(Long voucherId);
    
    /**
     * 根据科目ID获取凭证行列表
     * @param subjectId 科目ID
     * @return 凭证行列表
     */
    List<VoucherLine> selectBySubjectId(Long subjectId);
    
    /**
     * 根据科目ID和会计期间获取凭证行列表
     * @param subjectId 科目ID
     * @param period 会计期间
     * @return 凭证行列表
     */
    List<VoucherLine> selectBySubjectIdAndPeriod(@Param("subjectId") Long subjectId, @Param("period") String period);
    
    /**
     * 批量插入凭证行
     * @param lines 凭证行列表
     * @return 插入结果
     */
    int batchInsert(@Param("lines") List<VoucherLine> lines);
    
    /**
     * 批量更新凭证行
     * @param lines 凭证行列表
     * @return 更新结果
     */
    int batchUpdate(@Param("lines") List<VoucherLine> lines);
    
    /**
     * 根据凭证ID删除凭证行
     * @param voucherId 凭证ID
     * @return 删除结果
     */
    int deleteByVoucherId(Long voucherId);
    
    /**
     * 计算凭证的借方合计
     * @param voucherId 凭证ID
     * @return 借方合计
     */
    BigDecimal calculateDebitTotal(Long voucherId);
    
    /**
     * 计算凭证的贷方合计
     * @param voucherId 凭证ID
     * @return 贷方合计
     */
    BigDecimal calculateCreditTotal(Long voucherId);
    
    /**
     * 根据业务ID和业务类型获取凭证行
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 凭证行列表
     */
    List<VoucherLine> selectByBusinessIdAndType(@Param("businessId") Long businessId, @Param("businessType") String businessType);
}
