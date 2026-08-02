package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.InvoiceReimbursementItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报销明细Mapper接口
 *
 * <p>Sprint 3.1 P0 F-001：从 mapper/ 根目录迁移至 mapper/finance/。</p>
 */
@Mapper
public interface InvoiceReimbursementItemMapper extends BaseMapper<InvoiceReimbursementItem> {
}
