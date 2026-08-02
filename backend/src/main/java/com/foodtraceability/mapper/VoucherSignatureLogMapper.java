package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.VoucherSignatureLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 验签记录Mapper接口
 */
@Mapper
public interface VoucherSignatureLogMapper extends BaseMapper<VoucherSignatureLog> {
    
    /**
     * 根据凭证ID查询验签记录
     */
    List<VoucherSignatureLog> selectByVoucherId(@Param("voucherId") Long voucherId);
}
