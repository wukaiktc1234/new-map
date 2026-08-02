package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 财务流水VO
 * 金额字段以元为单位（字符串），与前端契约一致
 * 变动金额带正负号（正=增加，负=减少）
 */
@Schema(description = "财务流水信息")
public class RechargeFinanceLogVO {

    @Schema(description = "日志ID")
    private String logId;

    @Schema(description = "关联流水号")
    private String recordNo;

    @Schema(description = "会员ID")
    private String memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "流水类型: recharge/consume/refund/bonus_expire/bonus_grant")
    private String financeType;

    @Schema(description = "本金变动（元，正=增加负=减少）")
    private String principalChange;

    @Schema(description = "赠送变动（元）")
    private String bonusChange;

    @Schema(description = "收入变动（元）")
    private String revenueChange;

    @Schema(description = "变动后余额（元）")
    private String balanceAfter;

    @Schema(description = "变动后本金余额（元）")
    private String principalAfter;

    @Schema(description = "变动后赠送余额（元）")
    private String bonusAfter;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private String createdAt;

    // ==================== Getter & Setter ====================

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getFinanceType() { return financeType; }
    public void setFinanceType(String financeType) { this.financeType = financeType; }

    public String getPrincipalChange() { return principalChange; }
    public void setPrincipalChange(String principalChange) { this.principalChange = principalChange; }

    public String getBonusChange() { return bonusChange; }
    public void setBonusChange(String bonusChange) { this.bonusChange = bonusChange; }

    public String getRevenueChange() { return revenueChange; }
    public void setRevenueChange(String revenueChange) { this.revenueChange = revenueChange; }

    public String getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(String balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getPrincipalAfter() { return principalAfter; }
    public void setPrincipalAfter(String principalAfter) { this.principalAfter = principalAfter; }

    public String getBonusAfter() { return bonusAfter; }
    public void setBonusAfter(String bonusAfter) { this.bonusAfter = bonusAfter; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
