package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.ReconciliationItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 未达账项明细 Mapper（reconciliation_items，设计 §5.10.2）。
 *
 * <p>P1 仅提供基础 CRUD 能力（结构就位）；调节表生成/审核逻辑由 P1 业务卡实现，本卡不写业务逻辑。</p>
 */
@Mapper
public interface ReconciliationItemMapper extends BaseMapper<ReconciliationItem> {
}
