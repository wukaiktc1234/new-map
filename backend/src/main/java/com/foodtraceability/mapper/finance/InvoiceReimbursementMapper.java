package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.InvoiceReimbursement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票报销申请Mapper接口
 *
 * <p>Sprint 3.1 P0 F-001：从 mapper/ 根目录迁移至 mapper/finance/。</p>
 */
@Mapper
public interface InvoiceReimbursementMapper extends BaseMapper<InvoiceReimbursement> {
}
