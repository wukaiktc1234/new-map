package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.Receipt;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收款单Mapper
 */
@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {
}
