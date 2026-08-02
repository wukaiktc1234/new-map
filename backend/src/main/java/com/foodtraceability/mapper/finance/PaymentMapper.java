package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款单Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
