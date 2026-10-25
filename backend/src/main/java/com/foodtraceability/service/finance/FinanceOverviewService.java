package com.foodtraceability.service.finance;

import com.foodtraceability.dto.finance.recon.FinanceOverviewDailyVO;

import java.time.LocalDate;

/**
 * 资金总览（日）只读聚合服务（P3-4）。**只读**：不写任何表。
 */
public interface FinanceOverviewService {

    /**
     * 资金总览三段（各店现金户 / 渠道 / 调节表）。
     *
     * <p>范围判定复用 {@code GlobalDataScope}：all|company → 全部门店；
     * store|department|其它 → 仅本人 location_id 换算出的门店；无归属门店 → 403。</p>
     *
     * @param date 业务日期（缺省=今天）
     */
    FinanceOverviewDailyVO daily(LocalDate date);
}
