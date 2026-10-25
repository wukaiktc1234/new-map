package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/**
 * 门店日对账视图（AC-49，只读）。
 *
 * <p>金额单位一律**分**；{@code differenceAmount} 为负表示**短款**（实缴 &lt; 应缴）。</p>
 *
 * <p>同店同日若有多张缴款单（历史遗留）：金额字段为**当日合计**，slipId/slipNo/slipStatus/remark
 * 取**最新一张**（按 slipId 倒序），并在 {@code slipCount} 给出张数，避免静默丢数据。</p>
 */
public class StoreReconDailyVO {

    public Long storeId;
    public LocalDate businessDate;
    /** 应缴合计（分） */
    public Long expectedAmount;
    /** 实缴合计（分） */
    public Long actualAmount;
    /** 差额合计（分）：负=短款，正=长款 */
    public Long differenceAmount;
    public Long slipId;
    public String slipNo;
    public String slipStatus;
    public String remark;
    /** 当日缴款单张数（0 表示当日无缴款单，此时金额全为 0） */
    public Integer slipCount;
}
