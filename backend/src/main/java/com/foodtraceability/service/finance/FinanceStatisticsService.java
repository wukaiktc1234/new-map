package com.foodtraceability.service.finance;

import com.foodtraceability.dto.finance.FinanceStatisticsQueryDTO;
import com.foodtraceability.dto.finance.FinanceStatisticsVO;

/**
 * 财务统计Service接口
 *
 * <p>Sprint 3.1 P0 F-002：财务中心首页统计概览，
 * 聚合 Receivable / Payable / FinanceVoucher / FundFlow 数据，
 * 计算当月收入/支出/利润及环比变化率。</p>
 *
 * <p>金额字段全部 Long（分），无 BigDecimal。</p>
 */
public interface FinanceStatisticsService {

    /**
     * 获取财务概览统计
     *
     * <p>按查询条件（startDate/endDate/period）聚合：</p>
     * <ul>
     *   <li>当月收入：Receivable 表当月已收金额 + FundFlow 表当月收入类流水</li>
     *   <li>当月支出：Payable 表当月已付金额 + FundFlow 表当月支出类流水</li>
     *   <li>当月利润 = 当月收入 - 当月支出</li>
     *   <li>环比变化率：(当月 - 上月) / 上月 * 100（上月为0时返回null）</li>
     * </ul>
     *
     * @param query 查询条件（含 startDate/endDate/period）
     * @return 财务统计VO（含当月数据 + 上月数据 + 环比变化率 + period）
     */
    FinanceStatisticsVO getOverview(FinanceStatisticsQueryDTO query);
}
