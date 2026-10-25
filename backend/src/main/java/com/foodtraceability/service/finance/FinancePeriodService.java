package com.foodtraceability.service.finance;

import com.foodtraceability.dto.finance.period.FinancePeriodVO;

import java.util.List;

/**
 * 期末结账 / 反结账（P4-4）。表：accounting_periods + finance_period_account_balance。
 */
public interface FinancePeriodService {

    /** 期间列表（含快照合计） */
    List<FinancePeriodVO> list();

    /** 期间详情（逐账户快照 + 与当前流水的差异标记 dirty） */
    FinancePeriodVO summary(String periodCode);

    /**
     * 结账：RK-8 门禁（无 OPEN 差异 / 无 DRAFT 调节表）→ 逐账户快照 + V1 校验
     * （closing 必须等于期间内最后一笔 balance_after，任一不等 → 400）。
     */
    FinancePeriodVO close(String periodCode, String remark);

    /**
     * 反结账：只允许「当前最后一个已结账期间」；reason ≥10 字。
     * 快照行保留（历史留档），只把期间置回未结账。
     */
    FinancePeriodVO reopen(String periodCode, String reason);
}
