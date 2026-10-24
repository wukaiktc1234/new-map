package com.foodtraceability.service.finance.recon;

import com.foodtraceability.dto.finance.recon.LedgerDailyVO;
import com.foodtraceability.dto.finance.recon.LedgerSummaryVO;

import java.time.LocalDate;

/** 资金日记账与月结（S1/S2，只读；不改任何数据）。 */
public interface LedgerService {

    /** S1 日记账：期初/收入/支出/净额/期末 + V1 校验 */
    LedgerDailyVO daily(Long accountId, LocalDate from, LocalDate to);

    /** S2 月结汇总 */
    LedgerSummaryVO summary(Long accountId, String month);
}
