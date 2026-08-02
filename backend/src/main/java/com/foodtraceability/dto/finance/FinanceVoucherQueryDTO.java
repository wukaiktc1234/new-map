package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 记账凭证查询DTO
 */
public class FinanceVoucherQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 准确号（模糊查询） */
    private String voucherNo;

    /** 凭证日期起 */
    private LocalDate startDate;

    /** 凭证日期止 */
    private LocalDate endDate;

    /**
     * 凭证类型
     * 1-手工凭证 2-采购入库 3-销售出库 4-费用 5-付款 6-收款 7-转账
     */
    private Integer voucherType;

    /**
     * 凭证状态
     * 0-暂存 1-已审核 2-已过账 3-已作废
     */
    private Integer voucherStatus;

    /** 关联单据号 */
    private String referenceNo;

    /** 当前页码 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 20;

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(Integer voucherType) {
        this.voucherType = voucherType;
    }

    public Integer getVoucherStatus() {
        return voucherStatus;
    }

    public void setVoucherStatus(Integer voucherStatus) {
        this.voucherStatus = voucherStatus;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
