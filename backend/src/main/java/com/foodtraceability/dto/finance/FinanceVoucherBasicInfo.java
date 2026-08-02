package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 记账凭证基本信息DTO（用于缓存和批量查询）
 * 仅包含跨模块共享的必要字段，避免传输完整实体
 */
@Schema(description = "记账凭证基本信息")
public class FinanceVoucherBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证ID */
    @Schema(description = "凭证ID", example = "1")
    private Long voucherId;

    /** 凭证号，唯一标识 */
    @Schema(description = "凭证号", example = "PZ-202606-001")
    private String voucherNo;

    /** 凭证日期 */
    @Schema(description = "凭证日期", example = "2026-06-01")
    private LocalDate voucherDate;

    /** 借贷合计金额（单位：分，平衡凭证借方=贷方） */
    @Schema(description = "合计金额（分）", example = "100000")
    private Long totalAmount;

    /** 凭证状态：0-暂存 1-已审核 2-已过账 3-已作废 */
    @Schema(description = "凭证状态: 0暂存 1已审核 2已过账 3已作废", example = "2")
    private Integer status;

    /** 会计期间，格式 yyyy-MM，由凭证日期派生 */
    @Schema(description = "会计期间", example = "2026-06")
    private String period;

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    @Override
    public String toString() {
        return "FinanceVoucherBasicInfo{" +
                "voucherId=" + voucherId +
                ", voucherNo='" + voucherNo + '\'' +
                ", voucherDate=" + voucherDate +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", period='" + period + '\'' +
                '}';
    }
}
