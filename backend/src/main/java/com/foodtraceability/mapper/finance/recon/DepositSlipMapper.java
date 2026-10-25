package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.DepositSlip;
import org.apache.ibatis.annotations.Mapper;

/**
 * 现金交款单 Mapper（deposit_slips，设计 §5.7）。
 *
 * <p>本 Mapper 只做 CRUD，**不含任何取号 SQL**：缴款单号发号统一走
 * {@code com.foodtraceability.common.sequence.SequenceAllocator}（P4-1 BL-2 收敛，唯一实现）。</p>
 */
@Mapper
public interface DepositSlipMapper extends BaseMapper<DepositSlip> {

    // P4-1 BL-2：本 Mapper 内**不再有任何取号 SQL** —— 号段发号统一走
    // com.foodtraceability.common.sequence.SequenceAllocator（唯一实现，MANDATORY 事务内执行）。
    // 已删除：nextSlipSequenceWithFloor / initSlipSequenceIfAbsent / nextSlipSequence
}
