package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.ImportBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导入批次 Mapper（import_batches，设计 §5.1）。
 *
 * <p>P0 仅提供基础 CRUD 能力（结构就位）；勾稽/写账逻辑属 P1/P2，不在 P0 范围。</p>
 */
@Mapper
public interface ImportBatchMapper extends BaseMapper<ImportBatch> {
}
