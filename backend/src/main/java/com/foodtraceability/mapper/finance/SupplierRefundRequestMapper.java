package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.SupplierRefundRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商退款申请单数据访问接口
 */
@Mapper
public interface SupplierRefundRequestMapper extends BaseMapper<SupplierRefundRequest> {
}
