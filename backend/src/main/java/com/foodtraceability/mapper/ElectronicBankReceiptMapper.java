package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ElectronicBankReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 银行电子回单Mapper接口
 */
@Mapper
public interface ElectronicBankReceiptMapper extends BaseMapper<ElectronicBankReceipt> {
    
    /**
     * 根据凭证ID查询银行回单
     */
    ElectronicBankReceipt selectByVoucherId(@Param("voucherId") Long voucherId);
}
