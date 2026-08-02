package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceInvoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票管理Mapper接口
 */
@Mapper
public interface FinanceInvoiceMapper extends BaseMapper<FinanceInvoice> {
}
