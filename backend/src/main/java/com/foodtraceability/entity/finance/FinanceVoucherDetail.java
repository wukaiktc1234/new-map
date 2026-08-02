package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;

/**
 * 凭证明细/分录实体类
 * 记账凭证的明细行，每行代表一个会计分录
 * 一张凭证必须包含至少两条分录，且借贷平衡
 */
@TableName("finance_voucher_details")
public class FinanceVoucherDetail extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @TableId(type = IdType.AUTO)
    private Long detailId;

    /** 关联凭证ID */
    private Long voucherId;

    /** 摘要，描述经济业务内容 */
    private String summary;

    /** 会计科目ID */
    private Long subjectId;

    /** 借方金额（单位：分） */
    private Long debitAmount;

    /** 贷方金额（单位：分） */
    private Long creditAmount;

    /** 辅助核算项JSON（部门/供应商/客户等） */
    private String auxiliaryItem;

    /** 排序号 */
    private Integer sortOrder;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getDetailId() {
        return detailId;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

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

    @Override
    public String toString() {
        return "FinanceVoucherDetail{" +
                "detailId=" + detailId +
                ", voucherId=" + voucherId +
                ", summary='" + summary + '\'' +
                ", subjectId=" + subjectId +
                ", debitAmount=" + debitAmount +
                ", creditAmount=" + creditAmount +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
