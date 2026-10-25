package com.foodtraceability.service.finance;

import com.foodtraceability.dto.finance.recon.StoreReconDailyVO;

import java.time.LocalDate;

/**
 * 门店对账视图服务（AC-49，**只读**：不写 fund_flows / bank_accounts / deposit_slips）。
 */
public interface StoreReconService {

    /**
     * 门店日对账。
     *
     * <p><b>数据范围强制过滤（AC-49 核心）</b>：由服务端按登录用户判定，不信任前端传参：
     * <ul>
     *   <li>data_scope=all（FINANCE_DIRECTOR / FINANCE_MANAGER / AUDITOR）→ 任意店，但必须显式传 storeId；</li>
     *   <li>非 all（店长等）→ 只能查自己的门店；请求别店抛 403（**不是 404**）；</li>
     *   <li>既无归属门店又非全局 → 403（不返回空集、不回退默认店）。</li>
     * </ul>
     *
     * @param storeId 请求门店（全局用户必填；店长可省略，省略即自己的店）
     * @param date    业务日期（缺省=今天）
     */
    StoreReconDailyVO daily(Long storeId, LocalDate date);
}
