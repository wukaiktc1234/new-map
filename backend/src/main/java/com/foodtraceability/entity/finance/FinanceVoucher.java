package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账凭证实体类
 * 财务核心单据，记录企业所有经济业务的会计分录
 * 凭证状态流转：暂存(0) -> 已审核(1) -> 已过账(2)
 * 审核后的凭证不可修改，只能红字冲销
 */
@TableName("finance_vouchers")
public class FinanceVoucher extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证ID */
    @TableId(type = IdType.AUTO)
    private Long voucherId;

    /** 凭证号，唯一标识 */
    private String voucherNo;

    /** 凭证日期 */
    private LocalDate voucherDate;

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

    /** 借方合计（单位：分） */
    private Long totalDebit;

    /** 贷方合计（单位：分） */
    private Long totalCredit;

    /** 附件数量 */
    private Integer attachmentCount;

    /** 关联单据号 */
    private String referenceNo;

    /**
     * 来源类型
     * 1-手动 2-采购入库 3-销售出库 4-收款 5-付款 6-费用报销 7-工资 8-折旧
     */
    private Integer sourceType;

    /** 来源单据ID */
    private Long sourceId;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 审核人ID */
    private Long approveUserId;

    /** 审核时间 */
    private LocalDateTime approveTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

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

    public Long getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(Long totalDebit) {
        this.totalDebit = totalDebit;
    }

    public Long getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(Long totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Long getApproveUserId() {
        return approveUserId;
    }

    public void setApproveUserId(Long approveUserId) {
        this.approveUserId = approveUserId;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }
}
