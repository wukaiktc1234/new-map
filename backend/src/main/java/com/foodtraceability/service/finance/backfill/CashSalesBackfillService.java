package com.foodtraceability.service.finance.backfill;

import com.foodtraceability.dto.finance.recon.CashSalesBackfillRequest;
import com.foodtraceability.dto.finance.recon.CashSalesBackfillVO;

/**
 * 现金销售 SALE 流水补记（P2-B，历史欠账场景）。
 *
 * <p>口径与 consistency_check A1 完全一致：orders.payment_status=2 且存在现金支付记录（payment_method=1）
 * 但缺少 SALE 流水（fund_flows.order_id 命中 + flow_direction=1 + flow_category=1 + deleted=0）的订单。</p>
 *
 * <p>幂等键 [posShift=<shift_id>][cashBackfill] 写在 remark；重复调用新增 0 条。</p>
 */
public interface CashSalesBackfillService {

    CashSalesBackfillVO backfill(CashSalesBackfillRequest req);
}
