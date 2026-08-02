package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.FinanceVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 记账凭证Mapper接口
 */
@Mapper
public interface FinanceVoucherMapper extends BaseMapper<FinanceVoucher> {

    /**
     * 分页查询凭证
     * @param page 分页参数
     * @param voucherNo 凭证号（模糊）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param voucherType 凭证类型
     * @param voucherStatus 凭证状态
     * @return 凭证分页数据
     */
    IPage<FinanceVoucher> selectVoucherPage(Page<FinanceVoucher> page,
                                           @Param("voucherNo") String voucherNo,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("voucherType") Integer voucherType,
                                           @Param("voucherStatus") Integer voucherStatus);

    /**
     * 根据来源单据查询凭证（用于幂等检查）
     * @param sourceType 来源类型
     * @param sourceId 来源单据ID
     * @return 凭证信息
     */
    FinanceVoucher selectBySource(@Param("sourceType") Integer sourceType,
                                  @Param("sourceId") Long sourceId);
}
