package com.foodtraceability.dto.finance.recon;

import java.util.ArrayList;
import java.util.List;

/**
 * 渠道手续费汇总（AC-50，只读聚合）。
 *
 * <p>口径：{@code recon_differences.difference_type='CHANNEL_FEE'} 且 {@code external_ref IS NOT NULL}，
 * join {@code bank_transactions.external_txn_id = recon_differences.external_ref}，
 * 按 {@code bank_transactions.source_type} 分组。</p>
 */
public class ChannelFeeSummaryVO {

    /** 分组行（按 sourceType 升序） */
    public List<Row> rows = new ArrayList<>();

    /** 汇总行（所有分组之和） */
    public Row total = new Row();

    public static class Row {
        /** 渠道：ALIPAY / WECHAT / BANK …（汇总行为 "ALL"） */
        public String sourceType;
        /** 该渠道下的 CHANNEL_FEE 差异条数 */
        public Long diffCount = 0L;
        /** SUM(bank_transactions.fee)，单位分 */
        public Long feeTotalFen = 0L;
        /** SUM(recon_differences.diff_amount)，单位分 */
        public Long diffAmountFen = 0L;
    }
}
