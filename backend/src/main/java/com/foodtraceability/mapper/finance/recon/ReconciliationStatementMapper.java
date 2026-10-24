package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.ReconciliationStatement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行存款余额调节表主表 Mapper（reconciliation_statements，设计 §5.10.1）。
 *
 * <p>P1 仅提供基础 CRUD 能力（结构就位）；调节表生成/审核逻辑由 P1 业务卡实现，本卡不写业务逻辑。</p>
 */
@Mapper
public interface ReconciliationStatementMapper extends BaseMapper<ReconciliationStatement> {
}
