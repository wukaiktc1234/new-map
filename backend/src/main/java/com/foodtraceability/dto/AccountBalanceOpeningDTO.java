package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 期初余额人工录入/调整请求DTO（PD-003，产品决策 2026-08-11）
 * <p>期初金额由授权人员人工录入，调整必须记录操作人/时间/原因（原因必填，经 OperationLog 审计落库）。
 */
@Schema(description = "期初余额录入/调整请求")
public class AccountBalanceOpeningDTO {

    /** 科目编码 */
    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    /** 会计期间（YYYY-MM） */
    @Schema(description = "会计期间（YYYY-MM）", example = "2026-08")
    private String period;

    /** 期初金额（正数；按科目类别自动归入期初借方或期初贷方，与既有期末计算口径一致） */
    @Schema(description = "期初金额", example = "10000.00")
    private BigDecimal openingAmount;

    /** 调整原因（必填，写入审计日志） */
    @Schema(description = "调整原因（必填）", example = "期初建账人工录入")
    private String adjustReason;

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getOpeningAmount() {
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount) {
        this.openingAmount = openingAmount;
    }

    public String getAdjustReason() {
        return adjustReason;
    }

    public void setAdjustReason(String adjustReason) {
        this.adjustReason = adjustReason;
    }
}
