package com.foodtraceability.dto;

/**
 * 合同签署请求DTO
 */
public class ContractSignDTO {
    private String signDate;

    public ContractSignDTO() {
    }

    public String getSignDate() {
        return this.signDate;
    }

    public void setSignDate(final String signDate) {
        this.signDate = signDate;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ContractSignDTO)) return false;
        final ContractSignDTO other = (ContractSignDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$signDate = this.getSignDate();
        final java.lang.Object other$signDate = other.getSignDate();
        if (this$signDate == null ? other$signDate != null : !this$signDate.equals(other$signDate)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ContractSignDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $signDate = this.getSignDate();
        result = result * PRIME + ($signDate == null ? 43 : $signDate.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ContractSignDTO(signDate=" + this.getSignDate() + ")";
    }
}
