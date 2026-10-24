package com.foodtraceability.service.finance.recon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.recon.BankTransactionQueryDTO;
import com.foodtraceability.dto.finance.recon.BankTransactionVO;

/** 外部流水只读查询（I6）。P0 不提供任何写接口。 */
public interface BankTransactionQueryService {
    IPage<BankTransactionVO> page(BankTransactionQueryDTO query);
}
