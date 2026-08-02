package com.foodtraceability.dto.finance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 记账凭证创建DTO
 * 包含凭证头信息和分录明细列表
 */
public class FinanceVoucherCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证日期 */
    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    /**
     * 凭证类型
     * 1-手工凭证 2-采购入库 3-销售出库 4-费用 5-付款 6-收款 7-转账
     */
    @NotNull(message = "凭证类型不能为空")
    @Min(value = 1, message = "凭证类型无效")
    @Max(value = 7, message = "凭证类型无效")
    private Integer voucherType;

    /** 附件数量 */
    @Min(value = 0, message = "附件数量不能为负数")
    private Integer attachmentCount;

    /** 关联单据号 */
    @Size(max = 64, message = "关联单据号长度不能超过64个字符")
    private String referenceNo;

    /** 来源类型（自动凭证时使用） */
    @Min(value = 0, message = "来源类型无效")
    private Integer sourceType;

    /** 来源单据ID（自动凭证时使用） */
    private Long sourceId;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 分录明细列表 */
    @NotEmpty(message = "分录明细不能为空")
    @Valid
    private List<VoucherDetailItem> details;

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

    public List<VoucherDetailItem> getDetails() {
        return details;
    }

    public void setDetails(List<VoucherDetailItem> details) {
        this.details = details;
    }

    /**
     * 凭证明细项内部类
     */
    public static class VoucherDetailItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 摘要 */
        @Size(max = 200, message = "摘要长度不能超过200个字符")
        private String summary;

        /** 会计科目ID */
        @NotNull(message = "会计科目ID不能为空")
        private Long subjectId;

        /** 借方金额（单位：分） */
        @Min(value = 0, message = "借方金额不能为负数")
        private Long debitAmount;

        /** 贷方金额（单位：分） */
        @Min(value = 0, message = "贷方金额不能为负数")
        private Long creditAmount;

        /** 辅助核算项JSON */
        @Size(max = 1000, message = "辅助核算项长度不能超过1000个字符")
        private String auxiliaryItem;

        /** 排序号 */
        @Min(value = 0, message = "排序号不能为负数")
        private Integer sortOrder;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public Long getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(Long subjectId) {
            this.subjectId = subjectId;
        }

        public Long getDebitAmount() {
            return debitAmount;
        }

        public void setDebitAmount(Long debitAmount) {
            this.debitAmount = debitAmount;
        }

        public Long getCreditAmount() {
            return creditAmount;
        }

        public void setCreditAmount(Long creditAmount) {
            this.creditAmount = creditAmount;
        }

        public String getAuxiliaryItem() {
            return auxiliaryItem;
        }

        public void setAuxiliaryItem(String auxiliaryItem) {
            this.auxiliaryItem = auxiliaryItem;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
