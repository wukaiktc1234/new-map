package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceVoucherDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 凭证明细Mapper接口
 */
@Mapper
public interface FinanceVoucherDetailMapper extends BaseMapper<FinanceVoucherDetail> {

    /**
     * 根据凭证ID查询明细列表
     * @param voucherId 凭证ID
     * @return 明细列表
     */
    List<FinanceVoucherDetail> selectByVoucherId(@Param("voucherId") Long voucherId);

    /**
     * 根据凭证ID删除所有明细（逻辑删除）
     * @param voucherId 凭证ID
     * @return 删除数量
     */
    int deleteByVoucherId(@Param("voucherId") Long voucherId);
}
